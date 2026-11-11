package io.github.emmanuelquintana.capri

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import org.json.JSONObject
import java.util.Locale

/**
 * "Capri, …": her ears and her voice. Only when you turned it on (the app's Settings), with the
 * microphone allowed, and only while the screen is on: Android's speech recognizer, over and over,
 * waiting for her name; what comes after it goes to her page (listen.js), which answers, and she
 * says it out loud (TextToSpeech). Nothing is recorded or kept: the recognizer hands back words,
 * and only the ones after her name leave this class.
 */
class Ears(private val service: CapriService) {
    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var listening = false   // the recognizer is on right now
    private var wanted = false      // it should be (on, allowed, screen on, not talking)
    private var speaking = false
    private var awakeUntil = 0L     // just her name: what comes next is for her, even without it
    private var failures = 0
    private var torch = false

    /** Switched on or off, the screen on or off, the mic allowed or not: listening or not. */
    fun update() = main.post {
        val pm = service.getSystemService(PowerManager::class.java)!!
        val should = enabled(service) && allowed(service) && pm.isInteractive && SpeechRecognizer.isRecognitionAvailable(service) && !CapriService.gamer
        status = when {
            !enabled(service) -> ""
            !allowed(service) -> "nomic"
            !SpeechRecognizer.isRecognitionAvailable(service) -> "norecognizer"
            !pm.isInteractive -> "screenoff"
            CapriService.gamer -> "gamer"
            else -> status
        }
        if (should == wanted) return@post
        wanted = should
        if (should) {
            failures = 0
            Diag.add("I", "listening for her name")
            next(0)
        } else stop()
    }

    private fun stop() {
        main.removeCallbacksAndMessages(null)
        listening = false
        recognizer?.let { runCatching { it.cancel() }; runCatching { it.destroy() } }
        recognizer = null
    }

    fun destroy() {
        wanted = false
        stop()
        tts?.shutdown()
        tts = null
    }

    private fun next(delay: Long) {
        main.removeCallbacks(go)
        main.postDelayed(go, delay)
    }

    private val go = Runnable { start() }

    private fun start() {
        if (!wanted || speaking || listening) return
        // a call, a voice note, another app recording: the mic is theirs
        val audio = service.getSystemService(AudioManager::class.java)!!
        if (audio.mode != AudioManager.MODE_NORMAL) {
            status = "busy"
            return next(5000)
        }
        // a video or music playing: the recognizer takes the sound from it each time it starts (the
        // video pauses, the song stops), so she waits for it to end
        if (audio.isMusicActive) {
            status = "media"
            return next(3000)
        }
        val r = recognizer ?: create().also { recognizer = it }
        val lang = language()
        // how you talk (the app's Settings › Her ears): a slow talker gets longer pauses
        val pause = when (Store(service).configString("voicePace")) { "slow" -> 2600L; "fast" -> 1000L; else -> 1500L }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            .putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, true)
            // a short pause after "Capri" doesn't end what you're saying
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, pause)
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, pause - 300L)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, service.packageName)
        // Android 13+: her name and her words weigh more than the noise around (a TV, people talking)
        if (Build.VERSION.SDK_INT >= 33) {
            intent.putExtra(RecognizerIntent.EXTRA_ENABLE_BIASING_DEVICE_CONTEXT, true)
                .putStringArrayListExtra(RecognizerIntent.EXTRA_BIASING_STRINGS, arrayListOf("Capri", "Kapri", "Капри"))
        }
        listening = true
        runCatching { r.startListening(intent) }.onFailure {
            listening = false
            failed(SpeechRecognizer.ERROR_CLIENT)
        }
    }

    // on the phone itself when it can (Android 12+, its speech pack installed): nothing goes out
    private fun create(): SpeechRecognizer {
        // Google's when it's there (some phones default to their own, which won't listen from a service),
        // else the phone's default; the on-device one only when those keep failing
        val google = runCatching {
            service.packageManager.queryIntentServices(Intent(android.speech.RecognitionService.SERVICE_INTERFACE), 0)
                .firstOrNull { it.serviceInfo.packageName == GOOGLE }
                ?.let { android.content.ComponentName(it.serviceInfo.packageName, it.serviceInfo.name) }
        }.getOrNull()
        val local = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && failures >= 4 && failures % 2 == 0 &&
            runCatching { SpeechRecognizer.isOnDeviceRecognitionAvailable(service) }.getOrDefault(false)
        val r = when {
            local -> SpeechRecognizer.createOnDeviceSpeechRecognizer(service)
            google != null && failures % 3 != 2 -> SpeechRecognizer.createSpeechRecognizer(service, google)
            else -> SpeechRecognizer.createSpeechRecognizer(service)
        }
        r.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                listening = false
                failures = 0
                heard(
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty(),
                    results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES),
                )
                next(150)
            }
            override fun onError(error: Int) {
                listening = false
                failed(error)
            }
            override fun onReadyForSpeech(params: Bundle?) {
                status = "listening"
            }
            // you started or stopped talking: her sign that she's listening (hearing.js), only
            // shown right after her name
            override fun onBeginningOfSpeech() = speech(true)
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() = speech(false)
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        return r
    }

    private fun speech(on: Boolean) {
        val awake = System.currentTimeMillis() < awakeUntil
        service.overlay?.emit("ears:speech", JSONObject().put("on", on).put("awake", awake).toString())
    }

    private fun failed(error: Int) {
        when (error) {
            // silence, or words that weren't words: listen again right away
            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                failures = 0
                next(200)
            }
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                status = "nomic"
                Diag.add("W", "voice: no microphone permission")
                wanted = false
                stop()
            }
            else -> {
                // busy, no network, the on-device one not ready: a fresh recognizer, a little later each time
                failures++
                recognizer?.let { runCatching { it.destroy() } }
                recognizer = null
                status = "error:$error"
                if (failures == 1 || failures % 10 == 0) Diag.add("W", "voice: recognizer error $error")
                next(minOf(60_000L, 1000L * failures * failures))
            }
        }
    }

    private fun heard(options: List<String>, scores: FloatArray? = null) {
        val now = System.currentTimeMillis()
        val plain = options.map { plain(it) }
        // her noise filter (the app's Settings › Her ears): stricter, she only answers to her name
        // said first, said clearly; the TV or people around saying something like it don't count
        val filter = Store(service).configString("voiceFilter") ?: "normal"
        val minScore = when (filter) { "strict" -> 0.45f; "strictest" -> 0.7f; else -> 0f }
        val clear = { i: Int -> scores == null || i >= scores.size || scores[i] < 0f || scores[i] >= minScore || scores.all { it == 0f } }
        val first = { s: String -> NAME.find(s)?.let { m -> s.substring(0, m.range.first).trim().split(Regex("\\s+")).count { it.isNotEmpty() } <= 1 } ?: false }
        val at = plain.indices.firstOrNull { i ->
            NAME.containsMatchIn(plain[i]) && clear(i) && (filter == "normal" || first(plain[i]))
        } ?: -1
        val named = if (at >= 0) plain[at] else null
        heardLast = options.firstOrNull() ?: ""
        val text = when {
            named != null -> options[at] // as you said it (her page finds her name in it)
            now < awakeUntil && options.isNotEmpty() -> options[0]
            else -> {
                status = "other"
                return
            }
        }
        status = "heard"
        val rest = if (named != null) named.substring(NAME.find(named)!!.range.last + 1).trim(' ', ',', '.', '!', '?') else text
        // just her name: what you say in the next few seconds is for her too
        awakeUntil = if (rest.isEmpty() && named != null) now + 10_000 else 0L
        service.overlay?.emit("capri:heard", JSONObject().put("text", text).toString())
    }

    /** Her answer, out loud (not on silent or vibrate), and not heard by her own ears meanwhile. */
    fun say(text: String, lang: String) = main.post {
        val audio = service.getSystemService(AudioManager::class.java)!!
        // out loud through the media volume (what music uses): her ears asked you for it, even on silent
        if (text.isBlank() || audio.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) return@post
        val speak = {
            voice(accent(lang))
            // her voice as you set it (the app's Settings › Her ears)
            val store = Store(service)
            tts?.setPitch(store.configNumber("voicePitch", 1.0).toFloat().coerceIn(0.5f, 2f))
            tts?.setSpeechRate(store.configNumber("voiceRate", 1.0).toFloat().coerceIn(0.5f, 2f))
            speaking = true
            recognizer?.let { runCatching { it.cancel() } }
            listening = false
            tts?.speak(text.take(600), TextToSpeech.QUEUE_FLUSH, null, "capri")
            // never deaf for good if the voice engine forgets to say it's done
            main.removeCallbacks(unstuck)
            main.postDelayed(unstuck, 30_000)
            Unit
        }
        if (tts != null && ttsReady) {
            speak()
            return@post
        }
        if (tts == null) {
            tts = TextToSpeech(service) { status ->
                main.post {
                    ttsReady = status == TextToSpeech.SUCCESS
                    if (ttsReady) speak()
                }
            }.apply {
                setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) = main.post { spoke() }.let {}
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) = main.post { spoke() }.let {}
                })
            }
        }
    }

    private val unstuck = Runnable { if (speaking) spoke() }

    private fun spoke() {
        main.removeCallbacks(unstuck)
        speaking = false
        if (wanted) next(300)
    }

    /** The flashlight: on, off, or the other way (null). Null back when the phone has none. */
    fun torch(on: Boolean?): Boolean? {
        val cm = service.getSystemService(CameraManager::class.java) ?: return null
        val id = runCatching {
            cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
        }.getOrNull()
        if (id == null) {
            Diag.add("I", "voice: no flashlight on this phone")
            return null
        }
        val want = on ?: !torch
        return runCatching { cm.setTorchMode(id, want); torch = want; want }
            .onFailure { Diag.add("W", "voice: flashlight: ${it.message}") }
            .getOrNull()
    }

    private fun language(): String {
        val lang = Store(service).configString("lang")?.takeIf { it != "auto" } ?: Locale.getDefault().language
        return when (lang.substringBefore('-')) {
            "es" -> accent("es-MX")
            "en" -> accent("en-US")
            "fr" -> accent("fr-FR")
            "pt" -> accent("pt-BR")
            "ru" -> "ru-RU"
            else -> Locale.getDefault().toLanguageTag()
        }
    }

    /** Her accent in that language, as you picked it (the app's Settings › Her ears), else `tag`. */
    private fun accent(tag: String): String {
        val base = tag.substringBefore('-')
        val picked = Store(service).configString("voiceAccent_$base")
        return if (picked != null && ACCENTS[base]?.any { it.first == picked } == true) picked else tag
    }

    /** That accent's voice of the phone's speech engine, installed ones first; its language if it has none. */
    private fun voice(tag: String) {
        val t = tts ?: return
        val want = Locale.forLanguageTag(tag)
        val v = runCatching {
            t.voices?.filter { it.locale.language == want.language && it.locale.country == want.country && "notInstalled" !in it.features }
                ?.sortedWith(compareBy({ it.isNetworkConnectionRequired }, { -it.quality }))?.firstOrNull()
        }.getOrNull()
        if (v != null && runCatching { t.setVoice(v) }.getOrDefault(TextToSpeech.ERROR) == TextToSpeech.SUCCESS) return
        t.language = want
    }

    companion object {
        // her name as recognizers write it: Capri, Kapri, Capry, Cabri, Caprí, Capris… (Капри in Russian)
        private val NAME = Regex("(?<![a-zа-я])(?:[ck]a[pb]r[iy]s?|ка[пб]р[иы])(?![a-zа-я])")
        private const val GOOGLE = "com.google.android.googlequicksearchbox"

        /** The accents she can have in each language (1.9.0): the first is hers until you pick another. */
        val ACCENTS = mapOf(
            "es" to listOf("es-MX" to R.string.accent_mx, "es-ES" to R.string.accent_es, "es-AR" to R.string.accent_ar, "es-CO" to R.string.accent_co, "es-US" to R.string.accent_us_latino),
            "en" to listOf("en-US" to R.string.accent_us, "en-GB" to R.string.accent_gb, "en-AU" to R.string.accent_au, "en-IN" to R.string.accent_in),
            "pt" to listOf("pt-BR" to R.string.accent_br, "pt-PT" to R.string.accent_pt),
            "fr" to listOf("fr-FR" to R.string.accent_fr, "fr-CA" to R.string.accent_ca),
        )

        /** What her ears are doing, for the app's Settings: "", listening, heard, other, busy, media, nomic, screenoff, error:N */
        @Volatile var status = ""
            private set
        @Volatile var heardLast = ""
            private set

        // lower case, no accents: "Caprí" is "capri"
        private fun plain(s: String) = java.text.Normalizer.normalize(s.lowercase(), java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
        private const val PREF = "voice"

        fun enabled(context: Context) = context.getSharedPreferences("app", Context.MODE_PRIVATE).getBoolean(PREF, false)

        fun allowed(context: Context) = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        fun setEnabled(context: Context, on: Boolean) {
            context.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putBoolean(PREF, on).apply()
            CapriService.current?.voiceChanged()
        }
    }
}
