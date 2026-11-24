package io.github.emmanuelquintana.capri

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
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
import io.github.emmanuelquintana.voice.CapriVoiceClipPlayer
import io.github.emmanuelquintana.voice.ClipPlaybackResult
import io.github.emmanuelquintana.voice.VoiceClip
import io.github.emmanuelquintana.voice.VoiceClipLibrary
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
    private var movedAt = 0L        // when the phone last moved in your hand (the rustle it makes isn't you talking)

    // (1.9.0) "activa el micro como loco aun si no hay ningún sonido": in silence the recognizer gives
    // up after a few seconds and was started again at once, so the mic went on and off all the time.
    // Android 13+ can keep one session open, handing back each thing said on its own (a "segmented
    // session"): the mic stays on, steady, and starts again about once a minute. A phone whose
    // recognizer won't do it is noticed (sessions that keep ending early with nothing) and goes back to
    // the old way, with a short rest between tries when nothing at all was heard.
    // (1.9.1) remembered: a phone that closed her each time it lost her picture started over, trying the
    // long session again (and failing, "error 5") after every restart. Known again after an update.
    private var segmented: Boolean? = service.getSharedPreferences("app", Context.MODE_PRIVATE)
        .getString(SEG_PREF, null)?.takeIf { it.startsWith(SEG_KEY) }?.let { it.endsWith(":1") }
    private var longNow = false     // this session is a segmented one
    private var langStep = 0        // (1.10.0) how plainly the recognizer is asked, after errors 12/13
    // (1.10.1) "en Huawei nunca reconoce el permiso de micrófono": error 9 there comes from the phone's
    // speech app (Google's or Huawei's) lacking the microphone, not from her. Another recognizer is
    // tried; when none can, she says it's that app that needs it ("recmic"), not her.
    private var permMiss = 0
    private var sessionAt = 0L
    private var segments = 0
    private var shortOnes = 0
    private var quiet = 0           // sessions in a row that heard nothing at all (the old way)
    private var peak = NO_RMS       // the loudest moment of what's being said (how close the voice is)

    // (1.12.0) her passive ears (WakeWord.kt): a small offline model listening for her name, steady, no beeps.
    // The phone's recognizer is now only opened for what you say right after her name.
    private val passive = WakeWord(service) { text -> passiveHeard(text) }

    private fun passiveHeard(text: String) {
        // (it stopped working: the old way from now on)
        if (text.isEmpty()) { if (wanted && !speaking) next(300); return }
        if (!wanted || speaking) return
        heard(listOf(text))
        // her name alone: what you say next goes to the phone's recognizer (better with whole sentences)
        if (System.currentTimeMillis() < awakeUntil) {
            passive.stop()
            next(350) // (once it let go of the mic)
        }
    }

    private fun knowSegmented(can: Boolean) {
        segmented = can
        service.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putString(SEG_PREF, "$SEG_KEY${if (can) 1 else 0}").apply()
    }

    // (1.9.1) "se escucha un pitido cada vez que activa y desactiva el micro": the recognizer's own beep,
    // on the media sound. Only that one, never your calls, alarms or notifications, is
    // muted while her mic opens and closes, and never while something plays (she lets go of the mic as
    // soon as a video or song starts). Each comes back as it was: a sound you had muted yourself isn't
    // touched, and what she muted is written down, so it comes back even if the app closed meanwhile.
    private val hushed = HashSet<Int>()
    private val unhushNow = Runnable { unhush() }

    private var soundUntil = 0L

    /** Her page is about to make a sound of its own (a blip, her hello): heard, not muted. */
    fun sound() = main.post {
        soundUntil = System.currentTimeMillis() + 4000
        unhush()
    }

    /** Her beep muted for [ms] more (each call moves the end). */
    private fun hush(ms: Long) {
        if (System.currentTimeMillis() < soundUntil) return
        val audio = service.getSystemService(AudioManager::class.java) ?: return
        var changed = false
        for (stream in HUSH) {
            if (stream in hushed || audio.isStreamMute(stream)) continue
            if (runCatching { audio.adjustStreamVolume(stream, AudioManager.ADJUST_MUTE, 0) }.isSuccess) {
                hushed.add(stream)
                changed = true
            }
        }
        if (changed) saveHushed()
        main.removeCallbacks(unhushNow)
        if (hushed.isNotEmpty()) main.postDelayed(unhushNow, ms)
    }

    private fun unhush() {
        main.removeCallbacks(unhushNow)
        if (hushed.isEmpty()) return
        val audio = service.getSystemService(AudioManager::class.java)
        hushed.forEach { stream -> runCatching { audio?.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, 0) } }
        hushed.clear()
        saveHushed()
    }

    private fun saveHushed() {
        service.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putString(HUSH_PREF, hushed.joinToString(",")).commit()
    }

    init {
        // left muted by a closing she didn't see coming: back as they were
        val left = service.getSharedPreferences("app", Context.MODE_PRIVATE).getString(HUSH_PREF, "").orEmpty()
        if (left.isNotBlank()) {
            hushed.addAll(left.split(',').mapNotNull { it.trim().toIntOrNull() }.filter { it in HUSH })
            unhush()
        }
    }

    // (1.9.0) the phone moving: no permission, only how hard, never where
    private val motion = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent) {
            val v = e.values
            val g = Math.sqrt((v[0] * v[0] + v[1] * v[1] + v[2] * v[2]).toDouble())
            if (Math.abs(g - SensorManager.GRAVITY_EARTH) > MOVED) movedAt = System.currentTimeMillis()
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
    // (1.9.0) "pongo el video en pausa para hablar con Capri y no me escucha o solo la mitad": she waited
    // for the sound to stop by checking every 3 s. Android now tells her the moment a player stops, and
    // she starts listening right then.
    private val players = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: MutableList<android.media.AudioPlaybackConfiguration>?) {
            if (!wanted || listening || speaking) return
            main.removeCallbacks(quietNow)
            main.postDelayed(quietNow, 250)
        }
    }
    private val quietNow = Runnable {
        val audio = service.getSystemService(AudioManager::class.java) ?: return@Runnable
        if (!wanted || listening || speaking) return@Runnable
        if (!audio.isMusicActive) next(0) else main.postDelayed(quietLater, 600)
    }
    private val quietLater = Runnable {
        val audio = service.getSystemService(AudioManager::class.java) ?: return@Runnable
        if (wanted && !listening && !speaking && !audio.isMusicActive) next(0)
    }
    private var watchingPlayers = false
    private fun watchPlayers(on: Boolean) {
        if (on == watchingPlayers) return
        val audio = service.getSystemService(AudioManager::class.java) ?: return
        watchingPlayers = on
        runCatching { if (on) audio.registerAudioPlaybackCallback(players, main) else audio.unregisterAudioPlaybackCallback(players) }
    }

    private var sensing = false
    private fun sense(on: Boolean) {
        if (on == sensing) return
        val sm = service.getSystemService(SensorManager::class.java) ?: return
        sensing = on
        if (on) sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sm.registerListener(motion, it, SensorManager.SENSOR_DELAY_NORMAL) }
        else sm.unregisterListener(motion)
    }

    /** (1.12.0) The mic lent to something else for a moment (a song she listens to): her ears step aside. */
    @Volatile var held = false

    /**
     * (1.12.0) "¿Qué canción es esta?": [seconds] of what the mic hears, as a WAV (16 kHz mono) in base64, her ears
     * aside meanwhile. Null without the mic. Blocking: called from her bridge's worker thread.
     */
    @android.annotation.SuppressLint("MissingPermission")
    fun record(seconds: Int): String? {
        if (!allowed(service)) return null
        held = true
        update()
        try {
            Thread.sleep(500) // (her recognizer lets go of the mic)
            val rate = 16_000
            val min = android.media.AudioRecord.getMinBufferSize(rate, android.media.AudioFormat.CHANNEL_IN_MONO, android.media.AudioFormat.ENCODING_PCM_16BIT)
            val mic = android.media.AudioRecord(android.media.MediaRecorder.AudioSource.MIC, rate, android.media.AudioFormat.CHANNEL_IN_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT, maxOf(min, rate))
            if (mic.state != android.media.AudioRecord.STATE_INITIALIZED) { mic.release(); return null }
            val pcm = java.io.ByteArrayOutputStream()
            val buf = ByteArray(rate / 5)
            mic.startRecording()
            val end = System.currentTimeMillis() + seconds * 1000L
            while (System.currentTimeMillis() < end) {
                val n = mic.read(buf, 0, buf.size)
                if (n < 0) break
                pcm.write(buf, 0, n)
            }
            runCatching { mic.stop() }
            mic.release()
            val data = pcm.toByteArray()
            val head = java.nio.ByteBuffer.allocate(44).order(java.nio.ByteOrder.LITTLE_ENDIAN).apply {
                put("RIFF".toByteArray()); putInt(36 + data.size); put("WAVE".toByteArray())
                put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1); putInt(rate); putInt(rate * 2); putShort(2); putShort(16)
                put("data".toByteArray()); putInt(data.size)
            }.array()
            return android.util.Base64.encodeToString(head + data, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            Diag.add("W", "song: mic ${e.message}")
            return null
        } finally {
            held = false
            update()
        }
    }

    /** Switched on or off, the screen on or off, the mic allowed or not: listening or not. */
    /** (1.12.0) Her passive ears again from scratch (only your voice was turned on or off). */
    fun restartPassive() = main.post {
        if (!passive.on) return@post
        passive.release()
        main.removeCallbacks(go)
        main.postDelayed(go, 800)
    }

    fun update() = main.post {
        val pm = service.getSystemService(PowerManager::class.java)!!
        val should = enabled(service) && allowed(service) && pm.isInteractive && SpeechRecognizer.isRecognitionAvailable(service) && !CapriService.gamer && !CapriService.saving && !held
        status = when {
            !enabled(service) -> ""
            !allowed(service) -> "nomic"
            !SpeechRecognizer.isRecognitionAvailable(service) -> "norecognizer"
            !pm.isInteractive -> "screenoff"
            CapriService.gamer -> "gamer"
            CapriService.saving -> "saver"
            else -> status
        }
        if (should == wanted) return@post
        wanted = should
        sense(should)
        watchPlayers(should)
        if (should) {
            failures = 0
            Diag.add("I", "listening for her name")
            next(0)
        } else stop()
    }

    private fun stop() {
        main.removeCallbacksAndMessages(null)
        passive.release()
        listening = false
        recognizer?.let { runCatching { it.cancel() }; runCatching { it.destroy() } }
        recognizer = null
        unhush()
    }

    fun destroy() {
        sense(false)
        watchPlayers(false)
        wanted = false
        stop()
        stopVoice()
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
            passive.stop()
            return next(5000)
        }
        // (1.12.0) not right after her name: her passive ears, when their model is here (they don't take the
        // sound from a video or a song either: they listen while it plays)
        if (System.currentTimeMillis() >= awakeUntil) {
            val base = language().substringBefore('-')
            if (passive.start(base)) {
                status = "listening"
                return
            }
            // (its model downloaded on Wi-Fi, once: until then, the old way)
            if (!passive.broken) WakeWord.Models.fetch(service, base) { if (it == null && wanted && !speaking && !listening) { passive.stop(); next(500) } }
        } else if (passive.on) {
            passive.stop()
            return next(350) // (once it let go of the mic)
        }
        // a video or music playing: the recognizer takes the sound from it each time it starts (the
        // video pauses, the song stops), so she waits for it to end
        if (audio.isMusicActive) {
            status = "media"
            return next(700) // (1.9.0: was 3 s, so after you paused a video she was deaf for up to 3 s)
        }
        val r = recognizer ?: create().also { recognizer = it }
        val lang = language()
        // how you talk (the app's Settings › Her ears): a slow talker gets longer pauses
        val pause = when (Store(service).configString("voicePace")) { "slow" -> 2600L; "fast" -> 1000L; else -> 1500L }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .apply { if (langStep < 3) putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (langStep >= 2) lang.substringBefore('-') else lang) }
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            .putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, true)
            // a short pause after "Capri" doesn't end what you're saying
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, pause)
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, pause - 300L)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, service.packageName)
        // Android 13+: her name weighs more than the noise around (a TV, people talking). Only with her
        // filter at normal: biased like that, the rustle of the phone moving in your hand came out as
        // "Capri" (1.9.0: "muevo el celular sin decir nada y se activa")
        if (Build.VERSION.SDK_INT >= 33 && langStep < 1 && (Store(service).configString("voiceFilter") ?: "normal") == "normal") {
            intent.putExtra(RecognizerIntent.EXTRA_ENABLE_BIASING_DEVICE_CONTEXT, true)
                .putStringArrayListExtra(RecognizerIntent.EXTRA_BIASING_STRINGS, arrayListOf("Capri", "Kapri", "Капри", "カプリ"))
        }
        // one long session (Android 13+) when this phone's recognizer keeps it open
        longNow = Build.VERSION.SDK_INT >= 33 && segmented != false
        if (longNow) {
            intent.putExtra(RecognizerIntent.EXTRA_SEGMENTED_SESSION, RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS)
                .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, SESSION_MS)
        }
        sessionAt = System.currentTimeMillis()
        segments = 0
        peak = NO_RMS
        listening = true
        playing = 0
        main.removeCallbacks(mediaCheck)
        main.postDelayed(mediaCheck, MEDIA_EVERY)
        // her beep, as the mic opens (a short session: also as it closes, a few seconds later)
        hush(if (longNow) 2500L else 12_000L)
        main.removeCallbacks(hushEnd)
        if (longNow) main.postDelayed(hushEnd, SESSION_MS - 1500L)
        runCatching { r.startListening(intent) }.onFailure {
            listening = false
            failed(SpeechRecognizer.ERROR_CLIENT)
        }
    }

    // a long session about to end on its own: her beep as it closes
    private val hushEnd = Runnable { if (listening && longNow) hush(4000L) }

    // a video or song starts while she listens: she lets go of the mic (it would take its sound, and
    // its sound would be muted). Twice in a row, so her own short beep doesn't count.
    private var playing = 0
    private val mediaCheck: Runnable = object : Runnable {
        override fun run() {
            if (!listening) return
            playing = if (service.getSystemService(AudioManager::class.java)?.isMusicActive == true) playing + 1 else 0
            if (playing >= 2) {
                recognizer?.let { runCatching { it.cancel() } }
                listening = false
                main.removeCallbacks(hushEnd)
                unhush()
                status = "media"
                next(3000)
                return
            }
            main.postDelayed(this, MEDIA_EVERY)
        }
    }

    /** A session ended. Segmented ones that end early with nothing, again and again: this phone can't. */
    private fun ended(gotWords: Boolean) {
        if (!longNow) return
        val short = System.currentTimeMillis() - sessionAt < SESSION_MS / 3
        if (segments > 0 || !short) {
            if (segmented != true) {
                Diag.add("I", "voice: one long session (the mic stays on)")
                knowSegmented(true)
            }
            shortOnes = 0
        } else if (!gotWords && segmented == null && ++shortOnes >= 3) {
            knowSegmented(false)
            Diag.add("I", "voice: this phone's recognizer listens a few seconds at a time")
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
            google != null && langStep < 3 && failures % 3 != 2 && permMiss % 2 == 0 -> SpeechRecognizer.createSpeechRecognizer(service, google)
            else -> SpeechRecognizer.createSpeechRecognizer(service)
        }
        r.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                listening = false
                failures = 0
                permMiss = 0
                quiet = 0
                hush(800) // (her closing beep; the next session starts before it ends)
                val words = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                ended(words.isNotEmpty())
                heard(words, results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES))
                next(150)
            }
            // a long session: each thing said, as it ends, and she keeps listening
            override fun onSegmentResults(segmentResults: Bundle) {
                segments++
                failures = 0
                val words = segmentResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                if (words.any { it.isNotBlank() }) heard(words, segmentResults.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES))
                peak = NO_RMS
                if (status != "heard" && status != "moved" && status != "other" && status != "far") status = "listening"
            }
            override fun onEndOfSegmentedSession() {
                listening = false
                hush(800)
                ended(true)
                next(150)
            }
            override fun onError(error: Int) {
                listening = false
                hush(800)
                failed(error)
            }
            override fun onReadyForSpeech(params: Bundle?) {
                status = "listening"
                // the mic is open, its beep played: a long session gives your sounds back now
                if (longNow) hush(400)
            }
            // you started or stopped talking: her sign that she's listening (hearing.js), only
            // shown right after her name
            override fun onBeginningOfSpeech() = speech(true)
            override fun onRmsChanged(rmsdB: Float) {
                if (rmsdB > peak) peak = rmsdB
            }
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
            // (the old way, a few quiet ones in a row: a short rest between them, not a mic blinking nonstop)
            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                failures = 0
                ended(false)
                quiet = if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) quiet + 1 else 0
                // (1.10.0) "el micrófono se prende y apaga a cada rato": on a phone that listens a few seconds
                // at a time, a quiet room now rests her mic a little longer between tries (up to 3.5 s)
                next(if (longNow || quiet < 3) 200L else minOf(3500L, 500L * quiet))
            }
            // (1.9.1) "Falló el reconocedor de voz (error 5)" over and over: 5 is the recognizer saying it
            // can't do what was asked (a long session, on many phones) or that it was still busy with the
            // last one. Not your microphone failing: a fresh recognizer, the short way if it was the
            // long session, soon, and the error only shown if it keeps happening.
            SpeechRecognizer.ERROR_CLIENT, SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                recognizer?.let { runCatching { it.cancel() }; runCatching { it.destroy() } }
                recognizer = null
                if (error == SpeechRecognizer.ERROR_CLIENT && longNow && segmented != true) {
                    knowSegmented(false)
                    Diag.add("I", "voice: no long session here (error 5), the short way")
                    return next(400)
                }
                failures++
                if (failures >= 4) status = "error:$error"
                if (failures == 1 || failures % 10 == 0) Diag.add("W", "voice: recognizer error $error")
                next(minOf(15_000L, 600L * failures))
            }
            // (1.10.0) "Arreglen el micrófono": error 12/13, the recognizer won't take the language as asked
            // (a Samsung on Android 16). Asked more plainly each time, until one way works: without her
            // name's weight, the short way, the language alone ("es" for "es-MX"), and at last no
            // language at all on the phone's own recognizer (it listens in the phone's language).
            12, 13 -> {
                recognizer?.let { runCatching { it.cancel() }; runCatching { it.destroy() } }
                recognizer = null
                if (longNow && segmented != true) {
                    knowSegmented(false)
                    Diag.add("I", "voice: error $error on the long session, the short way")
                    return next(400)
                }
                if (langStep < 3) {
                    langStep++
                    Diag.add("I", "voice: error $error, asking more plainly (step $langStep)")
                    return next(400)
                }
                failures++
                status = "error:$error"
                if (failures == 1 || failures % 10 == 0) Diag.add("W", "voice: recognizer error $error")
                next(minOf(60_000L, 1000L * failures * failures))
            }
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                if (allowed(service) && permMiss < 3) {
                    // she has it: the speech app doesn't. Another recognizer, a moment later.
                    permMiss++
                    Diag.add("W", "voice: the speech app has no microphone (try $permMiss)")
                    recognizer?.let { runCatching { it.destroy() } }
                    recognizer = null
                    return next(1500)
                }
                status = if (allowed(service)) "recmic" else "nomic"
                Diag.add("W", if (status == "recmic") "voice: the phone's speech app has no microphone" else "voice: no microphone permission")
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
        val named0 = if (at >= 0) plain[at] else null
        heardLast = options.firstOrNull() ?: ""
        // (1.9.0) noise heard as words: her name alone, unsure, or one or two letters after it. With her
        // filter on, her name alone must be clear (it opens 10 s where anything you say is for her)
        val restOf = { s: String -> s.substring(NAME.find(s)!!.range.last + 1).trim(' ', ',', '.', '!', '?', '、', '。', '！', '？') }
        val alone = named0 != null && restOf(named0).isEmpty()
        val sure = { i: Int -> scores == null || i >= scores.size || scores[i] < 0f || scores.all { it == 0f } || scores[i] >= minScore + 0.2f }
        // the phone moved while that was heard, with her filter on: its rustle, not you, unless it's very clear
        val shaken = filter != "normal" && now - movedAt < SHAKEN_MS &&
            !(scores != null && scores.isNotEmpty() && scores[0] >= 0.85f)
        if (shaken) {
            status = "moved"
            return
        }
        // (1.9.0) her strictest filter: a voice from across the room (the TV, someone else) is quieter
        // than yours at the phone. Only when the recognizer says how loud it was.
        val loud = peak
        peak = NO_RMS
        if (filter == "strictest" && loud > NO_RMS && loud < FAR_RMS && plain.any { NAME.containsMatchIn(it) }) {
            status = "far"
            Diag.add("I", "voice: too far away (${"%.1f".format(loud)} dB)")
            return
        }
        val named = when {
            named0 == null -> null
            alone && filter != "normal" && !sure(at) -> null
            !alone && restOf(named0).replace(" ", "").length < 3 && filter != "normal" -> null
            else -> named0
        }
        val text = when {
            named != null -> options[at] // as you said it (her page finds her name in it)
            // what comes after her name alone: words, clear ones with her filter on (not the phone moving)
            now < awakeUntil && options.isNotEmpty() && plain[0].replace(" ", "").length >= 3 && (filter == "normal" || clear(0)) -> options[0]
            else -> {
                status = "other"
                // (1.12.0) it sounded like a request, but without her name: she says how (listen.js, now and then)
                if (options.isNotEmpty() && REQUEST.containsMatchIn(plain[0]) && (filter == "normal" || clear(0))) service.overlay?.emit("capri:unnamed", "{}")
                return
            }
        }
        status = "heard"
        val rest = if (named != null) named.substring(NAME.find(named)!!.range.last + 1).trim(' ', ',', '.', '!', '?', '、', '。', '！', '？') else text
        // just her name: what you say in the next few seconds is for her too
        awakeUntil = if (rest.isEmpty() && named != null) now + 10_000 else 0L
        service.overlay?.emit("capri:heard", JSONObject().put("text", text).toString())
    }

    // (1.11.0) Capri Voice V8 (CapriVoice.kt): one voice at a time, her recorded phrase when there's one for
    // exactly that, else her answer as it was written, in the voice you picked. Her page hears how it goes
    // ("voice:event": VOICE_REQUESTED … VOICE_STARTED, VOICE_PROGRESS, VOICE_FINISHED, VOICE_CANCELLED,
    // VOICE_ERROR, VOICE_UNAVAILABLE) and her ears open again when the sound really ended, not when it was ready.
    private var sayId = ""
    private var said = 0
    private var clipPlayer: CapriVoiceClipPlayer? = null
    private var focus: AudioFocusRequest? = null

    private var sayKind = ""

    private fun voiceEvent(id: String, state: String, extra: JSONObject.() -> Unit = {}) {
        val ev = JSONObject().put("id", id).put("state", state).apply(extra)
        if (BuildConfig.DEBUG && state != "VOICE_PROGRESS") android.util.Log.i("CapriVoiceEvent", ev.toString()) // (VoiceSelfTest reads these)
        service.overlay?.emit("voice:event", ev.toString())
        // (1.11.8) the app's Chat tab shows how her voice for each answer goes (its percentage)
        runCatching { voiceListener?.invoke(ev) }
    }

    /** Sound for her voice: others lower theirs a moment (a call or another app taking it stops her). */
    private fun takeFocus(): Boolean {
        val audio = service.getSystemService(AudioManager::class.java) ?: return true
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setOnAudioFocusChangeListener({ change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) hushVoice()
            }, main)
            .build()
        focus = req
        return audio.requestAudioFocus(req) != AudioManager.AUDIOFOCUS_REQUEST_FAILED
    }

    private fun dropFocus() {
        val req = focus ?: return
        focus = null
        service.getSystemService(AudioManager::class.java)?.abandonAudioFocusRequest(req)
    }

    /** Whatever she's saying stops now (a newer answer, you asked, her page closed it). */
    fun hushVoice(id: String = "") = main.post {
        if (id.isNotEmpty() && id != sayId) return@post
        stopVoice()
        sayKind = ""
        if (speaking) spoke()
    }

    private fun stopVoice() {
        if (neuralId.isNotEmpty()) {
            val was = neuralId
            neuralId = ""
            if (neuralFish) FishVoice.cancel() else CapriNeural.cancel()
            voiceEvent(was, "VOICE_CANCELLED") { put("source", if (neuralFish) FishVoice.ID else CapriNeural.ID) }
        }
        val p = clipPlayer
        clipPlayer = null
        p?.let { runCatching { it.stop() } } // (its VOICE_CANCELLED goes to her page)
        if (speaking && tts != null) runCatching { tts?.stop() } // (and the phone voice's, from onStop)
        dropFocus()
    }

    /** Her answer, out loud (not on silent or vibrate), and not heard by her own ears meanwhile.
     *  [opts]: id (to follow or cancel it), kind ("answer": her words as written; "event": one of her
     *  phrases, shown only if it can't play), clip (that phrase's id). */
    fun say(text: String, lang: String, opts: JSONObject = JSONObject()) = main.post {
        val part = opts.optString("part")
        if (part.isNotEmpty()) sayPart(text, lang, opts, part == "last") else sayNow(text, lang, opts)
    }

    // ---- (1.11.8) her answer said while her AI is still writing it (chatvoice.js sends it sentence by sentence:
    // part "more", then "last"). Her neural voice starts on the first sentence; any other voice waits for the
    // whole answer, as before.
    private var streamId = ""
    private var streamNeural = false
    private var streamFish = false // (1.12.0) the voice being fed is her cloud one (FishVoice)
    private val streamText = StringBuilder()

    private fun sayPart(text: String, lang: String, opts: JSONObject, last: Boolean) {
        val id = opts.optString("id")
        val whole = JSONObject(opts.toString()).apply { remove("part") }
        if (id.isNotEmpty() && id == streamId) {
            streamText.append(' ').append(text)
            if (streamNeural) {
                // (stopped meanwhile, by you or a newer answer: the rest isn't said either)
                // (1.12.0) fed while that answer is still the one being made (CapriNeural checks it): not tied to
                // neuralId, which a late "cancelled" of an older try with the same id could clear
                if (streamFish) FishVoice.feed(text, last) else CapriNeural.feed(text, last)
                if (last) streamId = ""
                return
            }
            if (last) {
                // not her neural voice (or it couldn't): the whole answer now, in the voice that says it
                streamId = ""
                sayNow(streamText.toString().trim(), lang, whole)
            }
            return
        }
        streamId = id
        streamText.setLength(0)
        streamText.append(text)
        val eng = CapriVoice.answerEngine(service, lang)
        streamNeural = eng == CapriNeural.ID || eng == FishVoice.ID
        streamFish = eng == FishVoice.ID
        if (last) streamId = ""
        if (streamNeural || last) sayNow(text, lang, whole, streaming = streamNeural && !last)
    }

    private fun sayNow(text: String, lang: String, opts: JSONObject, streaming: Boolean = false) = run sayNow@{
        val id = opts.optString("id").ifEmpty { "say-${++said}" }
        val kind = opts.optString("kind", "answer")
        val audio = service.getSystemService(AudioManager::class.java)!!
        voiceEvent(id, "VOICE_REQUESTED") { put("kind", kind) }
        // her own beep's mute off first: her voice goes through the media volume too
        unhush()
        main.removeCallbacks(hushEnd)
        val why = when {
            text.isBlank() -> "empty"
            // out loud through the media volume (what music uses): her ears asked you for it, even on silent
            audio.getStreamVolume(AudioManager.STREAM_MUSIC) == 0 -> "muted"
            // (1.10.0) her voice switched off (Personalizar › Su voz): she answers in writing only
            !Store(service).configFlag("speak", true) -> "off"
            // a phrase of her own (a pat, a hello) on silent: shown, not heard. (1.11.7) On vibrate she does say it:
            // most phones live on vibrate, and that left her phrases mute for nearly everyone (Emma: "No habla").
            // The media volume is what you turn down for her, as for a video.
            kind == "event" && audio.ringerMode == AudioManager.RINGER_MODE_SILENT -> "silent"
            else -> null
        }
        if (why != null) return@sayNow voiceEvent(id, "VOICE_UNAVAILABLE") { put("why", why) }
        // (1.11.8, "hay gente que dice que no le responde") one of her little phrases (a pat, a throw) never cuts
        // her answer: before, touching her while her voice was being made threw the whole answer away
        if (kind == "event" && sayKind != "event" && (neuralId.isNotEmpty() || speaking))
            return@sayNow voiceEvent(id, "VOICE_UNAVAILABLE") { put("why", "busy") }
        // one voice at a time: the newer one cancels the one before
        stopVoice()
        sayId = id
        sayKind = kind
        val found = if (streaming) null else runCatching { CapriVoice.clipFor(service, lang, opts.optString("clip"), text) }.getOrNull()
        if (found != null) return@sayNow playClip(id, found.first, found.second)
        // one of her phrases without its recording: she doesn't read it out in another voice
        if (kind == "event") return@sayNow voiceEvent(id, "VOICE_UNAVAILABLE") { put("why", "noclip") }
        // her new answers: in the voice you picked (the phone's, as before), or in writing only
        // (V8.5) the A/B comparison asks for one engine by name (only for tests, never for her answers)
        val engine = opts.optString("engine").takeIf { kind == "test" && it in CapriVoice.ENGINES } ?: CapriVoice.answerEngine(service, lang)
        if (engine == "text") return@sayNow voiceEvent(id, "VOICE_UNAVAILABLE") { put("why", "text") }
        // (1.12.0) her neural voice stuck on something it never finishes: the phone's voice now, not silence
        if (engine == CapriNeural.ID && CapriNeural.wedged()) {
            if (streaming) streamNeural = false
            Diag.add("W", "voice: her voice engine is stuck, phone's voice this time")
            if (!streaming) systemVoice(id, text, lang)
            return@sayNow
        }
        if (engine == FishVoice.ID) return@sayNow neuralVoice(id, text, lang, streaming, fish = true)
        if (engine == CapriNeural.ID) return@sayNow neuralVoice(id, text, lang, streaming)
        if (streaming) streamNeural = false
        if (engine == "system") phoneVoiceWhy(lang)
        systemVoice(id, text, lang)
    }

    // (1.12.0, community: "aparece otra voz de IA diferente", "su voz no se escucha") why her answer is in the phone's
    // voice: in the report's log, and her missing profile fetched on its own (2 MB), so her own voice comes next time
    private var phoneWhy = ""
    private fun phoneVoiceWhy(lang: String) {
        val mode = Store(service).configString("voiceMode") ?: "automatic"
        val tag = CapriVoice.voiceLang(lang)
        val why = when {
            mode == "light" || mode == "custom" -> "mode $mode"
            tag == null -> "her voice doesn't speak $lang yet"
            else -> CapriNeural.status(service, tag)
        }
        if (why == phoneWhy) return
        phoneWhy = why
        Diag.add("I", "voice: phone's voice for $lang ($why)")
        if (why == "noprofile") CapriNeural.downloadProfiles(service) { err -> Diag.add("I", "voice: her profiles ${err ?: "installed"}") }
    }

    // (1.11.0, V8.5) her new answer in the neural engine you picked (CapriNeural.kt). If it can't now (no model,
    // short of memory, too hot) it never switches voice behind your back: VOICE_UNAVAILABLE says why and she
    // answers in writing, unless you chose "if it can't, the phone's voice" (voiceFallback), and then the
    // event says so (fallback: system).
    private var neuralId = ""
    private var neuralSeq = 0 // (1.12.0) which try is the current one (an older one's late events don't count)

    private var neuralFish = false // (1.12.0) the voice being made is her cloud one (FishVoice), not the phone's engine

    private fun neuralVoice(id: String, text: String, lang: String, streaming: Boolean = false, fish: Boolean = false) {
        // (her cloud voice speaks every language: the tag is only her neural engine's)
        val tag = if (fish) lang else CapriVoice.voiceLang(lang)
        if (tag == null) return neuralFailed(id, text, lang, "lang")
        neuralFish = fish
        neuralId = id
        val mine = ++neuralSeq
        // (1.11.8) her ears stay open while her voice is being made (it can take a while): they close when
        // the sound is ready to play, not before. Before, she couldn't hear you for the whole generation.
        main.removeCallbacks(unstuck)
        main.postDelayed(unstuck, 180_000)
        // (1.12.0, Emma: "ahorita ya no habló") her voice engine not even starting on it (still busy with something
        // that hung): after 20 s this answer goes in the phone's voice instead of nothing
        val notStarted = Runnable {
            if (neuralId != id || neuralSeq != mine) return@Runnable
            neuralId = ""
            if (fish) { FishVoice.cancel(); FishVoice.rest("fish:net") } else CapriNeural.cancel()
            Diag.add("W", "voice: her voice ${if (fish) "in the cloud" else "engine"} didn't start in 20 s, phone's voice this time")
            neuralFailed(id, text, lang, "stuck")
        }
        main.postDelayed(notStarted, 20_000)
        val events = CapriNeural.Events { state, extra ->
            main.removeCallbacks(notStarted)
            if (neuralId != id || neuralSeq != mine) return@Events
            if (state == "VOICE_UNAVAILABLE") {
                neuralId = ""
                return@Events neuralFailed(id, text, lang, extra.optString("why"))
            }
            if (state == "VOICE_READY" && !speaking) {
                speaking = true
        passive.stop() // (1.12.0: her own voice isn't heard by her passive ears)
                recognizer?.let { runCatching { it.cancel() } }
                listening = false
                takeFocus()
            }
            voiceEvent(id, state) {
                val keys = extra.keys()
                while (keys.hasNext()) { val k = keys.next(); put(k, extra.get(k)) }
            }
            if (state == "VOICE_PROGRESS") { main.removeCallbacks(unstuck); main.postDelayed(unstuck, 60_000) }
            if (state in END) {
                neuralId = ""
                dropFocus()
                spoke()
            }
        }
        if (fish) FishVoice.speak(service, id, text, tag, events, streaming) else CapriNeural.speak(service, id, text, tag, events, streaming)
    }

    private fun neuralFailed(id: String, text: String, lang: String, why: String) {
        // (1.11.8) an answer still being written: the voice that says it instead waits for all of it (sayPart)
        if (id == streamId) {
            streamNeural = false
            voiceEvent(id, "VOICE_UNAVAILABLE") { put("why", why).put("source", CapriNeural.ID) }
            CapriNeural.lastFallback = why
            return
        }
        voiceEvent(id, "VOICE_UNAVAILABLE") { put("why", why).put("source", CapriNeural.ID) }
        CapriNeural.lastFallback = why
        // (1.12.0) her cloud voice couldn't (no internet, your key, its credit): her voice on the phone says it
        if (why.startsWith("fish:")) {
            Diag.add("I", "voice: her cloud voice couldn't ($why), her other voice this time")
            val next = CapriVoice.answerEngine(service, lang)
            voiceEvent(id, "VOICE_REQUESTED") { put("kind", "answer").put("fallback", next) }
            return if (next == CapriNeural.ID) neuralVoice(id, text, lang) else systemVoice(id, text, lang)
        }
        val store = Store(service)
        // (a stuck engine: always the phone's voice, whatever you chose; silence isn't a voice)
        if (why == "stuck" || store.configString("voiceFallback") == "system" || (store.configString("voiceMode") ?: "automatic") in setOf("automatic", "balanced")) {
            voiceEvent(id, "VOICE_REQUESTED") { put("kind", "answer").put("fallback", "system") }
            return systemVoice(id, text, lang)
        }
        if (speaking) spoke()
    }

    private fun playClip(id: String, lib: VoiceClipLibrary, clip: VoiceClip) {
        val store = Store(service)
        val player = CapriVoiceClipPlayer(lib)
        clipPlayer = player
        speaking = true
        passive.stop() // (1.12.0: her own voice isn't heard by her passive ears)
        recognizer?.let { runCatching { it.cancel() } }
        listening = false
        takeFocus()
        main.removeCallbacks(unstuck)
        main.postDelayed(unstuck, clip.durationMs + 10_000L)
        val res = runCatching {
            player.playEvent(id, clip.eventId, clip.language, clip.emotion,
                volume = store.configNumber("voiceClipVolume", 1.0).toFloat().coerceIn(0f, 1f),
                speed = store.configNumber("voiceClipSpeed", 1.0).toFloat().coerceIn(0.75f, 1.25f)) { ev ->
                val state = ev.state.name
                if (state == "VOICE_REQUESTED") return@playEvent // (already said, above)
                if (state == "VOICE_PROGRESS" && ev.positionMs % 500 >= 100) return@playEvent // (every half second is plenty)
                voiceEvent(ev.requestId, state) {
                    put("source", "clip").put("clip", clip.id).put("ms", ev.positionMs).put("duration", clip.durationMs)
                        .put("hint", clip.animationHint).put("text", clip.text)
                    ev.error?.let { put("error", it) }
                }
                // (stopped by a newer one: that one goes on, her ears don't open in between)
                if (state in END && clipPlayer === player) {
                    clipPlayer = null
                    dropFocus()
                    if (state == "VOICE_ERROR") Diag.add("W", "voice clip ${clip.id}: ${ev.error}")
                    spoke()
                }
            }
        }.getOrElse {
            Diag.add("W", "voice clip ${clip.id}: ${it.message}")
            null
        }
        if (res != ClipPlaybackResult.QUEUED) {
            clipPlayer = null
            dropFocus()
            voiceEvent(id, "VOICE_ERROR") { put("source", "clip").put("error", res?.name ?: "error") }
            spoke()
        }
    }

    private fun systemVoice(id: String, text: String, lang: String) {
        val speak = {
            voice(accent(lang))
            // her voice as you set it (the app's Settings › Her ears)
            val store = Store(service)
            tts?.setPitch(store.configNumber("voicePitch", 1.0).toFloat().coerceIn(0.5f, 2f))
            tts?.setSpeechRate(store.configNumber("voiceRate", 1.0).toFloat().coerceIn(0.5f, 2f))
            speaking = true
        passive.stop() // (1.12.0: her own voice isn't heard by her passive ears)
            recognizer?.let { runCatching { it.cancel() } }
            listening = false
            voiceEvent(id, "VOICE_PREPARING") { put("source", "system") }
            // (1.12.0, community: "el sonido se corta a la mitad") the whole answer: it was cut at 600 characters. In
            // pieces of whole sentences, one after another; only the last one carries the answer's id (its end)
            val whole = text.take(4000)
            val pieces = CapriNeural.split(whole, shortFirst = false).fold(mutableListOf<String>()) { acc, p ->
                if (acc.isNotEmpty() && acc.last().length + p.length < 480) acc[acc.lastIndex] = acc.last() + " " + p else acc += p
                acc
            }.ifEmpty { listOf(whole) }
            pieces.forEachIndexed { i, p ->
                val last = i == pieces.lastIndex
                tts?.speak(p, if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, if (last) id else "$id#$i")
            }
            // never deaf for good if the voice engine forgets to say it's done (longer for a longer answer)
            main.removeCallbacks(unstuck)
            main.postDelayed(unstuck, 30_000L + whole.length * 90L)
            Unit
        }
        if (tts != null && ttsReady) {
            speak()
            return
        }
        if (tts == null) {
            tts = TextToSpeech(service) { status ->
                main.post {
                    ttsReady = status == TextToSpeech.SUCCESS
                    if (ttsReady && sayId == id) speak()
                    else if (!ttsReady) voiceEvent(id, "VOICE_ERROR") { put("source", "system").put("error", "tts") }
                }
            }.apply {
                setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = main.post {
                        val u = utteranceId.orEmpty()
                        if (u.substringAfterLast('#', "").let { it.isNotEmpty() && it != "0" }) return@post // (a later piece of the same answer)
                        voiceEvent(u.substringBefore('#'), "VOICE_STARTED") { put("source", "system") }
                    }.let {}
                    override fun onDone(utteranceId: String?) = main.post { if ('#' !in utteranceId.orEmpty()) ttsEnded(utteranceId.orEmpty(), "VOICE_FINISHED") }.let {}
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) = main.post { ttsEnded(utteranceId.orEmpty().substringBefore('#'), "VOICE_ERROR") }.let {}
                    override fun onStop(utteranceId: String?, interrupted: Boolean) = main.post { ttsEnded(utteranceId.orEmpty().substringBefore('#'), "VOICE_CANCELLED") }.let {}
                })
            }
        }
    }

    private fun ttsEnded(id: String, state: String) {
        voiceEvent(id, state) { put("source", "system") }
        // an older answer's end isn't the newer one's
        if (id != sayId || clipPlayer != null || neuralId.isNotEmpty()) return
        spoke()
    }

    private val unstuck = Runnable { if (speaking || neuralId.isNotEmpty()) { stopVoice(); spoke() } }

    private fun spoke() {
        main.removeCallbacks(unstuck)
        speaking = false
        dropFocus()
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
            "vi" -> "vi-VN"
            "ja" -> "ja-JP"
            "de" -> "de-DE"
            "it" -> "it-IT"
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
        // (1.12.0) the one you picked in Ajustes de voz, when it's still on the phone
        Store(service).configString("phoneVoice_${want.language}")?.let { name ->
            val mine = runCatching { t.voices?.firstOrNull { it.name == name && "notInstalled" !in it.features } }.getOrNull()
            if (mine != null && runCatching { t.setVoice(mine) }.getOrDefault(TextToSpeech.ERROR) == TextToSpeech.SUCCESS) return
        }
        val v = runCatching {
            t.voices?.filter { it.locale.language == want.language && it.locale.country == want.country && "notInstalled" !in it.features }
                ?.sortedWith(compareBy({ it.isNetworkConnectionRequired }, { -it.quality }))?.firstOrNull()
        }.getOrNull()
        if (v != null && runCatching { t.setVoice(v) }.getOrDefault(TextToSpeech.ERROR) == TextToSpeech.SUCCESS) return
        t.language = want
    }

    companion object {
        /** (1.11.8) Every voice event, on the main thread (the app's Chat tab follows its answers' voice with it). */
        @Volatile var voiceListener: ((JSONObject) -> Unit)? = null
        // her name as recognizers write it: Capri, Kapri, Capry, Cabri, Caprí, Capris… (Капри in Russian,
        // カプリ in Japanese, "ca pri" as Vietnamese can split it)
        // (1.12.0, community: "muitas vezes ela não reconhece a palavra Capri… que responda por capi, cabri, cabi")
        // the start of a request, in her languages (lower case, no accents)
        private val REQUEST = Regex("^(?:abre|pon|ponme|reproduce|enciende|prende|apaga|llama|busca|baila|dime|open|play|turn|call|search|dance|tell me|ouvre|mets|joue|allume|appelle|cherche|abra|toca|liga|desliga|procura|открой|включи|выключи|позвони|найди|mo |bat |tat |goi |tim |offne|spiel|mach|ruf|such|apri|metti|accendi|spegni|chiama|cerca)(?![a-z])")
        private val NAME = Regex("(?<![a-zа-я])(?:[ck]a ?[pb]r[iy]s?|[ck]a[pb]i|ка[пб]р[иы]|ка[пб]и|カプリ|かぷり)(?![a-zа-я])")
        private const val GOOGLE = "com.google.android.googlequicksearchbox"
        private const val MOVED = 2.2 // m/s² off gravity: the phone moved in your hand, not just held
        private const val SHAKEN_MS = 4000L
        private const val SESSION_MS = 60_000L // a long session's length (then a new one)
        private const val MEDIA_EVERY = 700L
        private val HUSH = intArrayOf(AudioManager.STREAM_MUSIC) // (not the system sounds: on phones they are the ringtone)
        private const val HUSH_PREF = "voiceHushed"
        private const val SEG_PREF = "voiceLong"
        private val SEG_KEY = "${Build.VERSION.SDK_INT}:${BuildConfig.VERSION_CODE}:"
        private const val NO_RMS = -100f
        private const val FAR_RMS = 4.5f // the recognizer's level (about -2 silence to 10 loud): under it, far away

        /** The accents she can have in each language (1.9.0): the first is hers until you pick another. */
        val ACCENTS = mapOf(
            "es" to listOf("es-MX" to R.string.accent_mx, "es-ES" to R.string.accent_es, "es-AR" to R.string.accent_ar, "es-CO" to R.string.accent_co, "es-US" to R.string.accent_us_latino),
            "en" to listOf("en-US" to R.string.accent_us, "en-GB" to R.string.accent_gb, "en-AU" to R.string.accent_au, "en-IN" to R.string.accent_in),
            "pt" to listOf("pt-BR" to R.string.accent_br, "pt-PT" to R.string.accent_pt),
            "fr" to listOf("fr-FR" to R.string.accent_fr, "fr-CA" to R.string.accent_ca),
        )

        private val END = setOf("VOICE_FINISHED", "VOICE_CANCELLED", "VOICE_ERROR")

        /** What her ears are doing, for the app's Settings: "", listening, heard, other, busy, media, nomic, screenoff, error:N */
        @Volatile var status = ""
            private set
        @Volatile var heardLast = ""
            private set

        // lower case, no accents: "Caprí" is "capri"
        /** (1.12.0) Her name in that (WakeWord.kt, as its model writes it). */
        fun namedIn(s: String) = s.isNotBlank() && NAME.containsMatchIn(plain(s))

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
