package io.github.emmanuelquintana.capri

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import io.github.emmanuelquintana.voice.CapriVoiceClipPlayer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * (1.11.3, Emma: "revisa que la voz sí funcione explícitamente en celulares") What a user does, on CI's emulator
 * (debug builds only, `am start-foreground-service -a capri.VOICETEST`): «Bajar su voz» from Capri's real public
 * channel (her phrases in both languages, then her profile), her Spanish prompt read back, then one of her
 * phrases played out loud from start to end. Everything goes to logcat as CapriVoiceTest; the last line is
 * "DONE ok" or "FAIL <why>".
 */
object VoiceSelfTest {
    private fun log(msg: String) = android.util.Log.i("CapriVoiceTest", msg)

    /** One of the app's own jobs (they answer on the main thread), waited for here. */
    private fun await(what: String, minutes: Long, start: ((String?) -> Unit) -> Unit) {
        val done = CountDownLatch(1)
        var err: String? = "timeout"
        val t0 = System.currentTimeMillis()
        Handler(Looper.getMainLooper()).post { start { e -> err = e; done.countDown() } }
        done.await(minutes, TimeUnit.MINUTES)
        log("$what ms=${System.currentTimeMillis() - t0} result=${err ?: "ok"}")
        check(err == null) { "$what: $err" }
    }

    fun run(context: Context) = Thread {
        val app = context.applicationContext
        try {
            log("api=${Build.VERSION.SDK_INT} abis=${Build.SUPPORTED_ABIS.joinToString()} neural=${CapriNeural.unsupported() ?: "ok"}")
            for (pack in CapriVoice.PACKS) {
                if (CapriVoice.installed(pack.lang) != null) CapriVoice.remove(app, pack.lang) {}.also { Thread.sleep(1500) }
                await("phrases ${pack.lang}", 5) { d -> CapriVoice.download(app, pack.lang, d) }
            }
            await("profiles", 5) { d -> CapriNeural.downloadProfiles(app, d) }
            CapriVoice.reload(app) // (off the main thread: right now)
            for (pack in CapriVoice.PACKS) log("installed ${pack.lang} clips=${CapriVoice.installed(pack.lang)?.listClips()?.size ?: 0}")
            log("profile es-MX=${CapriNeural.profileInstalled(app, "es-MX")} en-US=${CapriNeural.profileInstalled(app, "en-US")}")
            log("icl es-MX ${CapriNeural.iclSummary(app, "es-MX") ?: error("no es-MX prompt")}")
            val audio = app.getSystemService(AudioManager::class.java)
            log("volume music=${audio.getStreamVolume(AudioManager.STREAM_MUSIC)}/${audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)}")
            for (lang in listOf("es-MX", "en-US")) {
                val lib = CapriVoice.installed(lang) ?: error("no $lang phrases")
                val clip = lib.listClips().first { it.eventId.startsWith("greeting") }
                val end = CountDownLatch(1)
                var last = ""
                Handler(Looper.getMainLooper()).post {
                    val player = CapriVoiceClipPlayer(lib)
                    val res = player.playEvent("selftest-$lang", clip.eventId, clip.language, clip.emotion) { ev ->
                        if (ev.state.name != "VOICE_PROGRESS") log("play $lang ${ev.state.name} ms=${ev.positionMs} clip=${ev.clip.id} ${ev.error ?: ""}".trim())
                        if (ev.state.name in setOf("VOICE_FINISHED", "VOICE_CANCELLED", "VOICE_ERROR")) { last = ev.state.name; end.countDown() }
                    }
                    log("play $lang result=$res duration_ms=${clip.durationMs}")
                }
                end.await(clip.durationMs + 20_000L, TimeUnit.MILLISECONDS)
                check(last == "VOICE_FINISHED") { "play $lang ended ${last.ifEmpty { "never" }}" }
            }
            // the same phrases through Capri herself (Ears.say, what her page calls when you pet her, greet her…):
            // by its id, and by its words alone, as her page sends them
            val ears = CapriService.current?.ears
            log("capri running=${ears != null}")
            if (ears != null) for (lang in listOf("es-MX", "en-US")) {
                val clip = CapriVoice.installed(lang)!!.listClips().first { it.eventId.startsWith("greeting") }
                for ((how, opts) in listOf(
                    "id" to org.json.JSONObject().put("id", "st-id-$lang").put("clip", clip.id).put("kind", "event"),
                    "words" to org.json.JSONObject().put("id", "st-words-$lang").put("kind", "event"))) {
                    android.util.Log.i("CapriVoiceEvent", "mark ${opts.getString("id")}")
                    ears.say(clip.text, lang, opts)
                    Thread.sleep(clip.durationMs + 4_000L)
                    log("capri $how $lang text=\"${clip.text}\"")
                }
            }
            log("DONE ok")
        } catch (t: Throwable) {
            log("FAIL ${t.javaClass.simpleName}: ${t.message}")
        }
    }.start()
}
