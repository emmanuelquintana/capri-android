package io.github.emmanuelquintana.capri

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.ConnectivityManager
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * (1.12.0, Emma: "lo del micrófono se activa a cada rato y suena, debe funcionar parecido a «Hey Google» u «Oye
 * Siri», que escuche pasivamente") Her passive ears.
 *
 * Until now she listened with Android's speech recognizer, which only listens a few seconds at a time on most
 * phones: it was switched on and off again all day, with its beep and the mic icon blinking. Now a small speech
 * model runs on the phone itself (Vosk, offline, about 40 MB per language, downloaded once): one steady stream
 * from the microphone, no beeps, nothing leaves the phone, and she only reacts to her name. What you ask her
 * after it goes on as before (Ears.kt). Without the model (not downloaded yet, or a phone it can't run on) her
 * ears work the old way.
 */
class WakeWord(private val context: Context, private val onHeard: (String) -> Unit) {
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var running = false
    @Volatile private var gen = 0
    @Volatile private var failedAt = 0L
    private var thread: Thread? = null
    @Volatile private var model: org.vosk.Model? = null
    @Volatile private var modelLang = ""
    @Volatile private var speakers: org.vosk.SpeakerModel? = null // (1.12.0) only your voice (VoiceId.kt)
    /** It couldn't run here (its library or its model failed): her ears go the old way. */
    @Volatile var broken = false
        private set

    val on get() = running

    /** Listening, in [lang] ("es", "en"…), if its model is here. False: the old way. */
    fun start(lang: String): Boolean {
        if (running) return true
        if (broken || !Models.installed(context, lang)) return false
        // (it just failed, the mic busy elsewhere: the old way for a while, not a restart loop)
        if (System.currentTimeMillis() - failedAt < 30_000) return false
        running = true
        val before = thread
        val mine = ++gen
        thread = Thread({
            runCatching { before?.join(1500) } // (the one before let go of the mic first)
            if (mine == gen) loop(lang, mine)
        }, "capri-wake").apply { start() }
        return true
    }

    fun stop() {
        running = false
    }

    /** Its model out of memory (her ears off for a while, the screen off). */
    fun release() {
        stop()
        val t = thread
        Thread {
            runCatching { t?.join(3000) }
            synchronized(this) {
                if (running) return@Thread // (listening again meanwhile: it stays)
                val m = model
                model = null
                modelLang = ""
                runCatching { m?.close() }
                runCatching { speakers?.close() }
                speakers = null
            }
        }.start()
    }

    @SuppressLint("MissingPermission")
    private fun loop(lang: String, mine: Int) {
        var rec: org.vosk.Recognizer? = null
        var mic: AudioRecord? = null
        try {
            rec = synchronized(this) {
                if (model == null || modelLang != lang) {
                    runCatching { model?.close() }
                    runCatching { org.vosk.LibVosk.setLogLevel(org.vosk.LogLevel.WARNINGS) }
                    model = org.vosk.Model(Models.dir(context, lang).absolutePath)
                    modelLang = lang
                }
                org.vosk.Recognizer(model, RATE.toFloat()).also { r ->
                    // (1.12.0) only your voice: each result also brings a voiceprint of who said it
                    if (VoiceId.enabled(context) && Models.installed(context, VoiceId.MODEL)) runCatching {
                        if (speakers == null) speakers = org.vosk.SpeakerModel(Models.dir(context, VoiceId.MODEL).absolutePath)
                        r.setSpeakerModel(speakers)
                    }.onFailure { Diag.add("W", "ears: voiceprints off (${it.message ?: it.javaClass.simpleName})") }
                }
            }
            val min = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            mic = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, maxOf(min, RATE / 5 * 2) * 2)
            check(mic.state == AudioRecord.STATE_INITIALIZED) { "mic" }
            mic.startRecording()
            val buf = ShortArray(RATE / 10) // 100 ms
            var named = 0L // her name in what's being said: the end of it is waited for (what comes after it)
            while (running && mine == gen) {
                val n = mic.read(buf, 0, buf.size)
                if (n < 0) error("mic $n")
                if (n == 0) continue
                if (rec.acceptWaveForm(buf, n)) {
                    val said = JSONObject(rec.result)
                    val text = said.optString("text").trim()
                    named = 0L
                    if (text.isNotEmpty() && running && mine(text, said)) main.post { onHeard(text) }
                } else {
                    val partial = JSONObject(rec.partialResult).optString("partial")
                    // her name, and then silence for a moment: that's it (no need to wait for the model's own end)
                    if (named == 0L && Ears.namedIn(partial)) named = System.currentTimeMillis()
                    if (named > 0L && System.currentTimeMillis() - named > 1800 && partial.isNotBlank()) {
                        val said = JSONObject(rec.finalResult)
                        val text = said.optString("text").trim()
                        named = 0L
                        if (text.isNotEmpty() && running && mine(text, said)) main.post { onHeard(text) }
                    }
                }
            }
        } catch (t: Throwable) {
            Diag.add("W", "ears: passive listening failed (${t.message ?: t.javaClass.simpleName}), the old way")
            failedAt = System.currentTimeMillis()
            broken = t is UnsatisfiedLinkError || t is NoClassDefFoundError || t.message?.contains("model", true) == true
            if (mine == gen) {
                running = false
                main.post { onHeard("") }
            } // (her ears go on the old way)
        } finally {
            runCatching { mic?.stop() }
            runCatching { mic?.release() }
            runCatching { rec?.close() }
        }
    }

    /** (1.12.0) Her name in your voice, when "only my voice" is on (VoiceId.kt); anything without her name goes on. */
    private fun mine(text: String, said: JSONObject): Boolean = !Ears.namedIn(text) || VoiceId.accept(context, said.optJSONArray("spk"))

    /** The small speech models, one per language, from Vosk's own site (Apache 2.0). */
    object Models {
        private const val BASE = "https://alphacephei.com/vosk/models"
        val NAMES = mapOf(
            "es" to "vosk-model-small-es-0.42",
            "en" to "vosk-model-small-en-us-0.15",
            "fr" to "vosk-model-small-fr-0.22",
            "pt" to "vosk-model-small-pt-0.3",
            "ru" to "vosk-model-small-ru-0.22",
            "de" to "vosk-model-small-de-0.15",
            "it" to "vosk-model-small-it-0.22",
            "vi" to "vosk-model-small-vn-0.4",
            "ja" to "vosk-model-small-ja-0.22",
            VoiceId.MODEL to "vosk-model-spk-0.4", // (1.12.0) voiceprints, for "only my voice"
        )

        fun dir(context: Context, lang: String) = File(context.filesDir, "ears/$lang")
        fun installed(context: Context, lang: String) =
            File(dir(context, lang), ".ok").readTextOrNull()?.trim() == NAMES[lang]

        @Volatile var busy = ""      // the language being downloaded
            private set
        @Volatile var progress = -1  // its percentage
            private set
        @Volatile var lastError = ""
            private set

        /** Downloaded now if it isn't here, on Wi-Fi (or any network when [anyNetwork], you asked). */
        fun fetch(context: Context, lang: String, anyNetwork: Boolean = false, done: (String?) -> Unit = {}) {
            val app = context.applicationContext
            val name = NAMES[lang] ?: return done("lang")
            if (installed(app, lang)) return done(null)
            if (busy.isNotEmpty()) return done("busy")
            val cm = app.getSystemService(ConnectivityManager::class.java)
            if (!anyNetwork && (cm == null || cm.isActiveNetworkMetered)) return done("metered")
            busy = lang
            progress = 0
            Thread({
                val dir = dir(app, lang)
                val tmp = File(app.filesDir, "ears/.$lang.part").apply { deleteRecursively() }
                val why = runCatching {
                    tmp.mkdirs()
                    val conn = (URL("$BASE/$name.zip").openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15_000
                        readTimeout = 30_000
                        instanceFollowRedirects = true
                    }
                    check(conn.responseCode in 200..299) { "HTTP ${conn.responseCode}" }
                    val total = conn.contentLengthLong.takeIf { it > 0 } ?: 50_000_000L
                    var read = 0L
                    val counting = object : java.io.FilterInputStream(conn.inputStream) {
                        override fun read(b: ByteArray, off: Int, len: Int): Int = super.read(b, off, len).also {
                            if (it > 0) { read += it; progress = (read * 100 / total).toInt().coerceIn(0, 99) }
                        }
                    }
                    ZipInputStream(counting.buffered()).use { zip ->
                        val root = tmp.canonicalPath + File.separator
                        while (true) {
                            val e = zip.nextEntry ?: break
                            // its first folder (the model's name) dropped; nothing outside the folder
                            val rel = e.name.substringAfter('/', "")
                            if (rel.isEmpty()) continue
                            val out = File(tmp, rel)
                            check(out.canonicalPath.startsWith(root)) { "zip" }
                            if (e.isDirectory) out.mkdirs() else { out.parentFile?.mkdirs(); out.outputStream().use { zip.copyTo(it) } }
                        }
                    }
                    check(File(tmp, "am").isDirectory || File(tmp, "am/final.mdl").isFile || File(tmp, "conf").isDirectory || File(tmp, "mfcc.conf").isFile) { "model" }
                    File(tmp, ".ok").writeText(name)
                    dir.deleteRecursively()
                    dir.parentFile?.mkdirs()
                    check(tmp.renameTo(dir)) { "rename" }
                    Diag.add("I", "ears: passive listening ready in $lang")
                }.exceptionOrNull()?.let { it.message ?: it.javaClass.simpleName }
                if (why != null) {
                    tmp.deleteRecursively()
                    Diag.add("W", "ears: passive model ($lang) not downloaded: $why")
                }
                lastError = why ?: ""
                busy = ""
                progress = -1
                Handler(Looper.getMainLooper()).post { done(why) }
            }, "capri-ears-model").start()
        }

        fun bytes(context: Context): Long = File(context.filesDir, "ears").walkTopDown().filter { it.isFile }.sumOf { it.length() }

        private fun File.readTextOrNull() = runCatching { readText() }.getOrNull()
    }

    companion object {
        private const val RATE = 16_000
    }
}
