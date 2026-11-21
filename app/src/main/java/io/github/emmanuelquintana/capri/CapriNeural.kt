package io.github.emmanuelquintana.capri

import android.app.ActivityManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.Uri
import android.os.Build
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import com.qwen.tts.studio.engine.CpuFeatures
import com.qwen.tts.studio.engine.QwenEngine
import io.github.emmanuelquintana.voicefx.CapriCyberVoiceFX
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.ZipInputStream

/**
 * (1.11.0) Capri Voice V8.5: her new answers in a neural voice on the phone itself, no internet once installed.
 *
 * Engine "qwen-mobile": Qwen3-TTS 12Hz 0.6B Base, quantized Q4_K_M (GGUF, Serveurperso/Qwen3-TTS-GGUF at
 * b7ee2e8c), run by the native library of Capri Voice V8 (Qwen/GGML, MIT) that comes in the APK for
 * arm64-v8a and x86_64. The library is built for Android 12 (API 31): below that, or on other processors,
 * this engine simply isn't offered and Capri goes on with her recorded phrases and the voice you picked.
 *
 * Her identity, the same as on PC (Emma, 2026-11-21: "que alguna sea idéntica o lo más parecida a la de PC"):
 * - es-MX: the approved voice es-MX-best is Qwen 0.6B in ICL mode: her reference sound's codes + its words +
 *   her speaker vector, all from the approved prompt.npz and profile.json, untouched. The native library has
 *   that ICL path (QwenEngine.synthesizeWithIclPrompt), so the phone uses exactly the same prompt as PC.
 *   SIGNATURE V7.2 (0.26, 76 Hz) is applied once, after the clean synthesis, as in the approved chain.
 * - en-US: Qwen in ICL mode from the English source sound and its words (conditioning.wav/.json), the same
 *   prompt Capri PC's Qwen engines use for English (its Chatterbox has no phone runtime). DSP off.
 * What changes is only the weights' precision, which you choose (and can add or remove) in its options:
 * Q4_K_M (light), Q8_0 (high) or BF16 (the same precision as PC, for phones with plenty of memory).
 *
 * Weights (0.9 to 2.2 GB) are downloaded on request from Hugging Face (public, Apache-2.0) at a fixed revision or
 * imported from a file, checked byte for byte (size + SHA-256) and never shipped in the APK. The profile is
 * private: it's only installed from the private profiles ZIP you hand it (each file checked by SHA-256).
 * Nothing is loaded until her first new answer, one synthesis at a time, cancellable, and the model is
 * unloaded after a few idle minutes, when the phone is short of memory or when you pick another engine.
 */
object CapriNeural {
    const val ID = "qwen-mobile"
    private const val REVISION = "b7ee2e8c7459c3bea99da23e3d178125a7d1713c"
    private const val SOURCE = "https://huggingface.co/Serveurperso/Qwen3-TTS-GGUF/resolve/$REVISION"

    /** One file of a variant: its name on Hugging Face, its size and SHA-256, and its name on the phone. */
    data class Model(val name: String, val bytes: Long, val sha: String, val local: String = name)
    /**
     * The precisions you can install, each on its own, side by side (and remove each one): more precision is
     * closer to PC and needs more memory. [ramGb] is the least RAM it's offered on, as phones are sold, [needMb] the free
     * memory it needs while it speaks. The native library looks for the tokenizer by these lowercase names.
     */
    data class Variant(val id: String, val dir: String, val files: List<Model>, val ramGb: Int, val needMb: Long, val fidelity: String) {
        val bytes get() = files.sumOf { it.bytes }
        val talker get() = files.first()
        val tokenizer get() = files.last()
    }
    val VARIANTS = listOf(
        Variant("q4", "qwen-0.6b-q4km", listOf(
            Model("qwen-talker-0.6b-base-Q4_K_M.gguf", 628_905_056L, "4b468ec7b1f62b90ef4ca316c0aa57deadfd54b2cf9651703ea753cedaf04226"),
            Model("qwen-tokenizer-12hz-Q4_K_M.gguf", 254_974_752L, "cf3788b4d50aaa665fb6e57c170396aae03a3555fea52d2b5d0cda902d658039"),
        ), ramGb = 3, needMb = 1_400, fidelity = "light"),
        Variant("q8", "qwen-0.6b-q8", listOf(
            Model("qwen-talker-0.6b-base-Q8_0.gguf", 992_615_488L, "d54dbaf10591421fa764ed630d764efa717ae40cd959bd48c66d4eb1af226426"),
            Model("qwen-tokenizer-12hz-Q8_0.gguf", 291_150_624L, "1883beeed99348fc35e23dd225e9082f93f6f8c109330a33d935baa8acdbfd94", "qwen-tokenizer-12hz-q8_0.gguf"),
        ), ramGb = 4, needMb = 2_000, fidelity = "high"),
        Variant("bf16", "qwen-0.6b-bf16", listOf(
            Model("qwen-talker-0.6b-base-BF16.gguf", 1_841_716_288L, "f0070d96aa4e1a437e2963cc2925b0c9b1e307e334faa36c2120f39fc0b729eb"),
            Model("qwen-tokenizer-12hz-BF16.gguf", 358_980_384L, "cc04d7352ff4cb339435cdccf59cf38e6e92f3cf789bc94a6a1b467093bafa18", "qwen-tokenizer-12hz-bf16.gguf"),
        ), ramGb = 6, needMb = 3_000, fidelity = "max"),
    )
    /** The light one: what CI tests on its emulator. */
    val MODELS = VARIANTS[0].files
    val MODEL_BYTES = VARIANTS[0].bytes

    /** The private profiles of Capri Voice V8 (capri-voice-profiles-private.zip): the files used, by SHA-256. */
    /** Her profiles on the public channel (only what the phone and PC use). */
    private const val PROFILES_FILE = "capri-voice-profiles.zip"
    private const val PROFILES_BYTES = 2_443_571L
    private const val PROFILES_SHA = "e217a9c89d32ed34275b69cd0ddb447d743350abc5d9f65a8915cba2d7e86dc6"
    private const val PROMPT = "runtime/profiles/es-MX-best/prompt.npz"
    private const val PROMPT_SHA = "27556e28d26e369b37de987873e325313032eea5d45d4193a1f6f1a13950637a"
    private const val ENGLISH = "runtime/profiles/en-US-source/conditioning.wav"
    private const val ENGLISH_SHA = "8fae883872eb306218cc84b520c0d298b2ad65dcdc282a6be5b98a437bd36233"
    private const val SPANISH_INFO = "runtime/profiles/es-MX-best/profile.json"
    private const val SPANISH_INFO_SHA = "7583a7b6a95cd9d6aea0916034e0cd9144a2dce0daa7b335c2f392e45e79c59e"
    private const val ENGLISH_INFO = "runtime/profiles/en-US-source/conditioning.json"
    private const val ENGLISH_INFO_SHA = "38df2eb853c6714b8dbffa55a99f660986fcf0a6abdbeda896d444431c2283dd"

    data class Profile(val id: String, val lang: String, val source: String, val dsp: String)
    val PROFILES = listOf(
        Profile("es-MX-best-icl", "es-MX", "es-MX-best (ICL: prompt.npz + profile.json, igual que PC)", "SIGNATURE"),
        Profile("en-US-source-icl", "en-US", "en-US-source (ICL: conditioning.wav + conditioning.json, igual que Qwen en PC)", "OFF"),
    )
    /** Same identity as PC; the precision is still to be judged by ear on a phone (no listening test yet). */
    const val FIDELITY = "pending-listening"

    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor(Threads.factory)
    private val jobs = Executors.newSingleThreadExecutor(Threads.factory)
    private val generation = AtomicLong()
    private val cancelDownload = AtomicBoolean(false)

    @Volatile private var engine: QwenEngine? = null
    @Volatile private var loadedLang = ""
    @Volatile private var track: AudioTrack? = null
    @Volatile var busy = ""
        private set
    @Volatile var progress = -1f
        private set
    @Volatile var lastError = ""
        private set
    @Volatile var metrics = JSONObject()
        private set
    /** Why the last answer couldn't use it (shown in the app's card), "" if it could. */
    @Volatile var lastFallback = ""
    private val unload = Runnable { worker.execute { dispose("idle") } }

    fun root(context: Context) = File(CapriVoice.root(context), "neural")
    private fun models(context: Context, v: Variant = VARIANTS[0]) = File(root(context), v.dir)
    private fun profiles(context: Context) = File(root(context), "profiles")
    private fun embedding(context: Context, lang: String) = File(profiles(context), "$lang.json") // (V8.5 x-vector, no longer used)
    private fun icl(context: Context, lang: String) = File(profiles(context), "$lang.icl.json")
    private fun englishWav(context: Context) = File(profiles(context), "en-US-source-24k.wav")
    private fun englishText(context: Context) = File(profiles(context), "en-US-source.txt")

    // ---- what this phone can do -------------------------------------------------------------------

    /** Why this phone can't run it (null: it can). */
    fun unsupported(): String? = when {
        Build.VERSION.SDK_INT < 31 -> "api"
        Build.SUPPORTED_ABIS.none { it == "arm64-v8a" || it == "x86_64" } -> "abi"
        !native -> "native"
        else -> null
    }

    // its native libraries load (the same choice QwenEngine makes: the fast ARM build when the CPU has it)
    private val native: Boolean by lazy {
        Build.VERSION.SDK_INT >= 31 && runCatching { CpuFeatures.optimized }.isSuccess
    }

    fun installed(context: Context, v: Variant) = v.files.all { File(models(context, v), it.local).length() == it.bytes } &&
        File(models(context, v), ".verified").isFile
    fun modelsInstalled(context: Context) = VARIANTS.any { installed(context, it) }
    /**
     * The phone's RAM as it's sold (6 GB, 8 GB…). Android reports less than that (the system keeps part of it: a
     * "6 GB" phone says about 5.3–5.6 GiB), and 1.11.0 compared that against 5.5/7.5 GB, so 6 and 8 GB phones were
     * turned away (1.11.1, Discord). Rounded up to the next whole GB, with 0.3 GB of slack under it.
     */
    fun ramGb(context: Context): Int {
        val mem = ActivityManager.MemoryInfo()
        context.getSystemService(ActivityManager::class.java).getMemoryInfo(mem)
        return Math.ceil(mem.totalMem / 1073741824.0 + 0.3).toInt()
    }
    /** Offered on this phone: enough RAM for it (whether it has room right now is checked when she speaks). */
    fun fits(context: Context, v: Variant): Boolean = ramGb(context) >= v.ramGb
    /** The precision her voice uses: the one you chose if it's installed, else the most precise installed. */
    fun variant(context: Context): Variant? {
        val want = Store(context).configString("voiceNeuralModel")
        return VARIANTS.firstOrNull { it.id == want && installed(context, it) } ?: VARIANTS.lastOrNull { installed(context, it) }
    }
    fun profileInstalled(context: Context, lang: String) = icl(context, lang).isFile ||
        (lang == "en-US" && englishWav(context).isFile && englishText(context).isFile)

    /** installed | unsupported:<why> | nomodel | noprofile | loaded | ready */
    fun status(context: Context, lang: String): String {
        unsupported()?.let { return "unsupported:$it" }
        if (!modelsInstalled(context)) return "nomodel"
        if (!profileInstalled(context, lang)) return "noprofile"
        return if (engine != null && loadedLang.isNotEmpty()) "loaded" else "ready"
    }

    fun hardware(context: Context): JSONObject {
        val mem = ActivityManager.MemoryInfo()
        context.getSystemService(ActivityManager::class.java).getMemoryInfo(mem)
        val thermal = if (Build.VERSION.SDK_INT >= 29) context.getSystemService(PowerManager::class.java).currentThermalStatus else -1
        return JSONObject().put("api", Build.VERSION.SDK_INT).put("abis", JSONArray(Build.SUPPORTED_ABIS.toList()))
            .put("cores", Runtime.getRuntime().availableProcessors()).put("ramTotal", mem.totalMem).put("ramFree", mem.availMem)
            .put("lowMemory", mem.lowMemory).put("thermal", thermal)
            .put("disk", root(context).apply { mkdirs() }.usableSpace)
            .put("fastArm", native && runCatching { CpuFeatures.optimized }.getOrDefault(false))
    }

    /** Enough free memory right now for the model (from ~1.4 GB for the light one while it speaks). */
    private fun roomFor(context: Context): String? {
        val mem = ActivityManager.MemoryInfo()
        context.getSystemService(ActivityManager::class.java).getMemoryInfo(mem)
        val need = (variant(context) ?: VARIANTS[0]).needMb
        if (mem.lowMemory || mem.availMem < need * 1024 * 1024) return "memory"
        if (Build.VERSION.SDK_INT >= 29 &&
            context.getSystemService(PowerManager::class.java).currentThermalStatus >= PowerManager.THERMAL_STATUS_SEVERE) return "thermal"
        return null
    }

    // ---- speaking ---------------------------------------------------------------------------------

    /** Events for [speak]: state (VOICE_*) and extras. Called on the main thread. */
    fun interface Events { fun on(state: String, extra: JSONObject) }

    /**
     * Her answer [text], word for word, in this engine. Sentences are made one after another and played as
     * soon as the first is ready; VOICE_STARTED/FINISHED follow the real sound (the speaker's position), not
     * the end of the generation. A newer [speak] or [cancel] stops it and drops anything made late.
     */
    fun speak(context: Context, id: String, text: String, lang: String, events: Events) {
        val app = context.applicationContext
        val ticket = generation.incrementAndGet()
        runCatching { engine?.cancel() }
        stopTrack()
        main.removeCallbacks(unload)
        val emit: (String, JSONObject) -> Unit = { state, extra -> main.post { if (ticket == generation.get() || state == "VOICE_CANCELLED") events.on(state, extra.put("source", ID)) } }
        worker.execute {
            if (ticket != generation.get()) return@execute
            val store = Store(app)
            try {
                val state = status(app, lang)
                if (state != "ready" && state != "loaded") {
                    emit("VOICE_UNAVAILABLE", JSONObject().put("why", state))
                    return@execute
                }
                val room = if (engine == null) roomFor(app) else null
                if (room != null) {
                    emit("VOICE_UNAVAILABLE", JSONObject().put("why", room))
                    return@execute
                }
                emit("VOICE_PREPARING", JSONObject())
                val loadMs = ensure(app, lang, store)
                val dsp = PROFILES.first { it.lang == lang }.dsp.takeIf { store.configFlag("voiceDsp", true) } ?: "OFF"
                val sentences = split(text)
                val rate = 24_000
                val t = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_FLOAT).setSampleRate(rate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setBufferSizeInBytes(maxOf(AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT), rate * 4))
                    .build()
                t.setVolume(store.configNumber("voiceClipVolume", 1.0).toFloat().coerceIn(0f, 1f))
                if (ticket != generation.get()) { t.release(); return@execute }
                track = t
                var written = 0L
                var genMs = 0L
                var firstAudio = -1L
                val started = SystemClock.elapsedRealtime()
                var peak = Debug.getPss() * 1024L
                val fx = if (dsp == "SIGNATURE") CapriCyberVoiceFX(rate, 1, CapriCyberVoiceFX.Mode.RING, .26f, 76f) else null
                try {
                    for ((n, sentence) in sentences.withIndex()) {
                        if (ticket != generation.get()) break
                        val s0 = SystemClock.elapsedRealtime()
                        val pcm = synth(app, sentence, lang)
                        genMs += SystemClock.elapsedRealtime() - s0
                        peak = maxOf(peak, Debug.getPss() * 1024L)
                        if (ticket != generation.get()) break
                        val out = if (fx != null) dspOnce(fx, pcm) else pcm
                        if (n == 0) {
                            firstAudio = SystemClock.elapsedRealtime() - started
                            emit("VOICE_READY", JSONObject().put("ms", firstAudio))
                            t.play()
                            watch(t, ticket, emit)
                        }
                        var off = 0
                        while (off < out.size && ticket == generation.get()) {
                            val w = t.write(out, off, out.size - off, AudioTrack.WRITE_BLOCKING)
                            if (w <= 0) break
                            off += w
                        }
                        written += out.size
                        totalFrames = written
                    }
                } finally {
                    fx?.close()
                }
                generating = false
                val seconds = written / rate.toDouble()
                metrics = JSONObject().put("engine", ID).put("lang", lang).put("loadMs", loadMs).put("firstAudioMs", firstAudio)
                    .put("generationMs", genMs).put("audioS", seconds).put("rtf", if (seconds > 0) genMs / 1000.0 / seconds else JSONObject.NULL)
                    .put("pssPeakBytes", peak).put("sentences", sentences.size).put("dsp", dsp).put("at", System.currentTimeMillis())
                if (written > 0) lastFallback = ""
                if (written == 0L && ticket == generation.get()) emit("VOICE_ERROR", JSONObject().put("error", "noaudio"))
            } catch (e: Throwable) {
                generating = false
                if (ticket == generation.get()) {
                    lastError = e.message ?: e.javaClass.simpleName
                    Diag.add("W", "voice $ID: $lastError")
                    val why = when {
                        e is OutOfMemoryError -> "memory"
                        lastError.contains("cancel", true) -> null
                        else -> lastError
                    }
                    if (why == null) emit("VOICE_CANCELLED", JSONObject()) else emit("VOICE_ERROR", JSONObject().put("error", why))
                    if (e is OutOfMemoryError) dispose("oom")
                }
            } finally {
                main.removeCallbacks(unload)
                main.postDelayed(unload, idleMs(store))
            }
        }
    }

    @Volatile private var totalFrames = 0L
    @Volatile private var generating = false

    /** The speaker's real position: started, progress every half second, finished when the last frame played. */
    private fun watch(t: AudioTrack, ticket: Long, emit: (String, JSONObject) -> Unit) {
        generating = true
        totalFrames = Long.MAX_VALUE
        var started = false
        var lastProgress = 0L
        val tick = object : Runnable {
            override fun run() {
                if (ticket != generation.get() || track !== t) return
                val pos = runCatching { t.playbackHeadPosition.toLong() and 0xffffffffL }.getOrDefault(0L)
                if (!started && pos > 0) { started = true; emit("VOICE_STARTED", JSONObject()) }
                if (started && pos - lastProgress >= 12_000) { lastProgress = pos; emit("VOICE_PROGRESS", JSONObject().put("ms", pos * 1000 / 24_000)) }
                if (!generating && pos >= totalFrames) {
                    stopTrack()
                    emit("VOICE_FINISHED", JSONObject().put("ms", pos * 1000 / 24_000))
                    return
                }
                main.postDelayed(this, 50)
            }
        }
        main.post(tick)
    }

    fun cancel() {
        generation.incrementAndGet()
        runCatching { engine?.cancel() }
        stopTrack()
    }

    private fun stopTrack() {
        val t = track
        track = null
        if (t != null) runCatching { t.pause(); t.flush(); t.stop(); t.release() }
    }

    /** The phone needs memory back (onTrimMemory), or another engine was picked: the model goes. */
    fun release(why: String) = worker.execute { dispose(why) }

    private fun dispose(why: String) {
        val e = engine ?: return
        engine = null
        loadedLang = ""
        runCatching { e.cancel(); e.close() }
        Diag.add("I", "voice $ID unloaded ($why)")
    }

    private fun idleMs(store: Store) = (store.configNumber("voiceUnloadMin", 2.0).coerceIn(0.5, 30.0) * 60_000).toLong()

    /** The model (once) and her vector for [lang]; returns how long loading took (0 if it was loaded). */
    @Volatile private var loadedVariant = ""
    @Volatile private var encoderLoaded = false

    private fun ensure(app: Context, lang: String, store: Store, pick: Variant? = null): Long {
        val t0 = SystemClock.elapsedRealtime()
        val v = pick ?: variant(app) ?: error("nomodel")
        if (engine != null && loadedVariant != v.id) dispose("variant")
        var e = engine
        val fresh = e == null
        if (e == null) {
            android.system.Os.setenv("QWEN3_TTS_LOW_MEM", "1", true)
            e = QwenEngine()
            e.setBackendPreference(QwenEngine.BACKEND_CPU)
            val threads = store.configNumber("voiceThreads", 0.0).toInt().takeIf { it in 1..8 }
                ?: Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
            e.setCpuThreads(threads)
            for (m in v.files) require(File(models(app, v), m.local).length() == m.bytes) { "model" }
            if (!e.loadModels(models(app, v).absolutePath, v.talker.local)) {
                val err = e.getLastError() ?: "load"
                e.close()
                error(err)
            }
            engine = e
            loadedVariant = v.id
            encoderLoaded = false
        }
        if (lang == "en-US" && !icl(app, lang).isFile) {
            // the English ICL prompt, made once from the English source sound and its words (as Qwen on PC does)
            if (!encoderLoaded) {
                check(e.loadIclPromptEncoder(models(app, v).absolutePath, v.talker.local)) { e.getLastError() ?: "encoder" }
                encoderLoaded = true
            }
            val tmp = File(profiles(app), "en-US.tmp.icl.json")
            e.resetCancel()
            check(e.extractIclPrompt(englishWav(app).absolutePath, englishText(app).readText().trim(), tmp.absolutePath)) { e.getLastError() ?: "icl" }
            readIcl(tmp)
            check(tmp.renameTo(icl(app, lang))) { "rename" }
            // the encoder leaves the engine unable to speak ("Models not loaded", CI 2026-11-21): once the prompt
            // is saved, a clean engine is loaded for speaking (this happens only the first time, in English)
            val made = SystemClock.elapsedRealtime() - t0
            dispose("icl")
            return made + ensure(app, lang, store, v)
        }
        readIcl(icl(app, lang))
        loadedLang = lang
        return if (fresh) SystemClock.elapsedRealtime() - t0 else 0L
    }

    /** (debug self-test) The installed ICL prompt of [lang], checked, in one line; null if there's none. */
    internal fun iclSummary(context: Context, lang: String): String? = icl(context, lang).takeIf { it.isFile }?.let { f ->
        readIcl(f).let { "frames=${it.frames} codebooks=${it.codebooks} text_chars=${it.text.length} codes0=${it.codes.take(16).joinToString(",")}" }
    }

    /** An ICL prompt (qwen3_tts_icl_prompt_v1), checked: its words, her 1024-float vector, whole frames of codes. */
    internal class Icl(val text: String, val embedding: FloatArray, val frames: Int, val codebooks: Int, val codes: LongArray)

    internal fun readIcl(file: File): Icl {
        require(file.length() in 1..8L * 1024 * 1024) { "icl" }
        val o = JSONObject(file.readText())
        require(o.optString("format") == "qwen3_tts_icl_prompt_v1") { "icl:format" }
        val text = o.optString("reference_text")
        val emb = o.getJSONArray("speaker_embedding").let { a -> FloatArray(a.length()) { a.getDouble(it).toFloat() } }
        val rc = o.getJSONObject("reference_codes")
        val frames = rc.getInt("frames"); val books = rc.getInt("codebooks")
        val codes = rc.getJSONArray("codes").let { a -> LongArray(a.length()) { a.getLong(it) } }
        require(emb.size == 1024 && emb.all { it.isFinite() }) { "icl:embedding" }
        require(books == 16 && frames in 8..2000 && codes.size == frames * books && codes.all { it in 0..4095 }) { "icl:codes" }
        return Icl(text, emb, frames, books, codes)
    }

    /** The ICL prompt of her approved voice from prompt.npz (codes [frames, 16], vector) + its words. */
    internal fun iclFromNpz(npz: ByteArray, text: String): ByteArray {
        val (shape, codes) = npyLongs(npzEntry(npz, "ref_code.npy"))
        val vector = npyFloats(npzEntry(npz, "ref_spk_embedding.npy"))
        require(shape.size == 2 && shape[1] == 16 && codes.size == shape[0] * 16) { "npz:codes" }
        require(vector.size == 1024 && vector.all { it.isFinite() } && text.isNotBlank()) { "npz:embedding" }
        return iclJson(text, vector, shape[0], 16, codes)
    }

    internal fun iclJson(text: String, vector: FloatArray, frames: Int, books: Int, codes: LongArray): ByteArray =
        JSONObject().put("format", "qwen3_tts_icl_prompt_v1").put("reference_text", text)
            .put("speaker_embedding", JSONArray().apply { vector.forEach { put(it.toDouble()) } })
            .put("reference_codes", JSONObject().put("frames", frames).put("codebooks", books)
                .put("codes", JSONArray().apply { codes.forEach { put(it) } }))
            .toString(1).toByteArray()

    private fun validate(e: QwenEngine, file: File) {
        require(file.length() in 1..1024 * 1024) { "embedding" }
        val dim = e.getModelCapabilities()?.speakerEmbeddingDim ?: error("capabilities")
        require(dim > 0) { "embedding:$dim" }
        val bytes = file.readBytes()
        val first = bytes.firstOrNull { it > 32 }
        val values: FloatArray = if (first == '['.code.toByte()) {
            val a = JSONArray(String(bytes, Charsets.UTF_8)); FloatArray(a.length()) { a.getDouble(it).toFloat() }
        } else {
            // Formato binario del motor nativo: float32 little-endian, sin cabecera.
            require(bytes.size == dim * 4) { "embedding:bin" }
            val b = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            FloatArray(dim) { b.getFloat() }
        }
        require(values.size == dim) { "embedding:$dim" }
        require(values.all { it.isFinite() }) { "embedding" }
    }

    private fun synth(app: Context, text: String, lang: String): FloatArray {
        val e = engine ?: error("unloaded")
        e.resetCancel()
        var limit = false
        e.setProgressCallback { count, max -> if (count >= max) { limit = true; e.cancel() } }
        try {
            val r = e.synthesizeWithIclPrompt(text, icl(app, lang).absolutePath,
                QwenEngine.NativeParams(languageId = if (lang == "es-MX") 2054 else 2050, maxAudioTokens = 320))
            check(!limit) { "limit" }
            check(r.success) { r.errorMsg ?: "synthesis" }
            val raw = r.audio ?: error("noaudio")
            require(r.sampleRate == 24_000 && raw.isNotEmpty() && raw.all { it.isFinite() }) { "pcm" }
            return FloatArray(raw.size) { raw[it].coerceIn(-1f, 1f) }
        } finally {
            e.setProgressCallback(null)
        }
    }

    /** SIGNATURE V7.2 over the clean synthesis, once (one effect context per answer, in order). */
    private fun dspOnce(fx: CapriCyberVoiceFX, pcm: FloatArray): FloatArray {
        val chunk = 480
        val buffer = ByteBuffer.allocateDirect(chunk * 4).order(ByteOrder.nativeOrder())
        val floats = buffer.asFloatBuffer()
        val out = FloatArray(pcm.size)
        var start = 0
        while (start < pcm.size) {
            val frames = minOf(chunk, pcm.size - start)
            floats.clear(); floats.put(pcm, start, frames)
            fx.process(buffer, frames)
            floats.flip(); floats.get(out, start, frames)
            start += frames
        }
        return out
    }

    /** Sentences of at most ~220 characters (a sentence is ~25 s of audio at most for the engine's limit). */
    fun split(text: String): List<String> {
        val clean = text.replace(Regex("[*_`#>]+"), " ").replace(Regex("\\s+"), " ").trim().take(1200)
        val parts = Regex("[^.!?¡¿…\\n]*[.!?…]+|[^.!?…]+$").findAll(clean).map { it.value.trim() }.filter { it.isNotEmpty() }.toMutableList()
        val out = mutableListOf<String>()
        for (p in parts) {
            if (p.length <= 220) { out += p; continue }
            var rest = p
            while (rest.length > 220) {
                val cut = rest.lastIndexOf(',', 220).takeIf { it > 60 } ?: rest.lastIndexOf(' ', 220).takeIf { it > 60 } ?: 220
                out += rest.substring(0, cut + 1).trim()
                rest = rest.substring(cut + 1).trim()
            }
            if (rest.isNotEmpty()) out += rest
        }
        // tiny pieces ("¡Ok!") go with the next one: fewer, steadier generations
        val merged = mutableListOf<String>()
        for (s in out) if (merged.isNotEmpty() && merged.last().length < 24) merged[merged.lastIndex] = merged.last() + " " + s else merged += s
        return merged.ifEmpty { listOf(clean) }
    }

    // ---- self-test (debug builds only: CI's emulator, `am start-foreground-service -a capri.NEURALTEST`) ----

    /**
     * Real synthesis of new sentences, measured, without the speaker: models from the app's external files
     * folder (pushed by CI, checked by SHA-256), a technical fixture sound for the speaker vector (NOT Capri's
     * voice: CI never sees her private profile), each sentence rendered to a WAV next to it, a cancellation in
     * the middle of a generation and the memory after unloading. Everything goes to logcat as CapriNeuralTest.
     */
    fun selfTest(context: Context) = worker.execute {
        val app = context.applicationContext
        val log: (String) -> Unit = { msg -> android.util.Log.i("CapriNeuralTest", msg) }
        val ext = app.getExternalFilesDir(null) ?: return@execute log("FAIL noexternal")
        try {
            log("support=${unsupported() ?: "ok"} api=${Build.VERSION.SDK_INT} abis=${Build.SUPPORTED_ABIS.joinToString()} hw=${hardware(app)}")
            if (unsupported() != null) return@execute log("DONE unsupported=${unsupported()}")
            val dir = models(app).apply { mkdirs() }
            for (m in MODELS) {
                val src = File(ext, m.name)
                val dst = File(dir, m.name)
                if (src.isFile && !dst.isFile) { src.copyTo(dst, overwrite = true); src.delete() }
                require(dst.length() == m.bytes && sha(dst) == m.sha) { "model sha ${m.name}" }
            }
            File(dir, ".verified").writeText(MODELS.joinToString("\n") { "${it.name} ${it.sha}" })
            log("models verified")
            val fixture = File(ext, "fixture.wav")
            profiles(app).mkdirs()
            atomic(englishWav(app), pcm16Wav(fixture.readBytes()))
            englishText(app).writeText("This is only a technical test tone, made for the continuous integration check.")
            icl(app, "en-US").delete(); icl(app, "es-MX").delete()
            val pss0 = Debug.getPss() * 1024L
            val store = Store(app)
            val load = ensure(app, "en-US", store, VARIANTS[0]) // ICL prompt from the fixture, made by the native encoder
            val made = readIcl(icl(app, "en-US"))
            log("icl frames=${made.frames} codebooks=${made.codebooks} text_chars=${made.text.length} emb0=${made.embedding.take(4)} codes0=${made.codes.take(48).joinToString(",")}")
            // the Spanish path, exactly as her real profile goes: an .npz (int64 codes [frames, 16] + float32
            // vector) turned into the prompt by iclFromNpz (here made from the fixture's, never her voice)
            val npz = java.io.ByteArrayOutputStream().also { bytes ->
                java.util.zip.ZipOutputStream(bytes).use { z ->
                    z.putNextEntry(java.util.zip.ZipEntry("ref_code.npy")); z.write(npy("<i8", intArrayOf(made.frames, 16), made.codes.size * 8) { b -> made.codes.forEach { b.putLong(it) } }); z.closeEntry()
                    z.putNextEntry(java.util.zip.ZipEntry("ref_spk_embedding.npy")); z.write(npy("<f4", intArrayOf(1024), 4096) { b -> made.embedding.forEach { b.putFloat(it) } }); z.closeEntry()
                }
            }.toByteArray()
            atomic(icl(app, "es-MX"), iclFromNpz(npz, "Porque esta es solo una prueba técnica, hecha para revisar la integración continua."))
            log("load_ms=$load pss_before=$pss0 pss_loaded=${Debug.getPss() * 1024L} threads=${engine?.getCpuThreads()} backend=${engine?.getActiveBackendName()}")
            // (1.11.3) a short probe first, its progress to the log as it goes: on CI's slow emulator a whole
            // sentence can outlast the job, and this shows whether it advances (and how fast) or is stuck
            run {
                val e0 = engine!!
                val t0 = SystemClock.elapsedRealtime()
                e0.resetCancel()
                e0.setProgressCallback { c, m -> if (c == 1 || c % 4 == 0) log("probe token=$c/$m ms=${SystemClock.elapsedRealtime() - t0}") }
                val r = e0.synthesizeWithIclPrompt("Hi, choom.", icl(app, "en-US").absolutePath, QwenEngine.NativeParams(languageId = 2050, maxAudioTokens = 24))
                e0.setProgressCallback(null)
                log("probe success=${r.success} error=${r.errorMsg} samples=${r.audio?.size ?: 0} audio_s=${"%.2f".format((r.audio?.size ?: 0) / 24_000.0)} ms=${SystemClock.elapsedRealtime() - t0}")
            }
            for ((lang, text) in listOf(
                "es-MX" to "¡Hey, choom! Encontré tres maneras de resolver ese problema.",
                "en-US" to "Hi, choom. All good.")) { // inglés corto: el emulador x86_64 de CI es muy lento
                ensure(app, lang, store)
                val t0 = SystemClock.elapsedRealtime()
                val pcm = synth(app, text, lang)
                val ms = SystemClock.elapsedRealtime() - t0
                val fx = if (lang == "es-MX") CapriCyberVoiceFX(24_000, 1, CapriCyberVoiceFX.Mode.RING, .26f, 76f) else null
                val out = fx?.use { dspOnce(it, pcm) } ?: pcm
                val seconds = out.size / 24_000.0
                val rms = Math.sqrt(out.sumOf { it.toDouble() * it } / out.size)
                File(ext, "neural-$lang.wav").writeBytes(floatWav(out))
                log("synth lang=$lang chars=${text.length} generation_ms=$ms audio_s=${"%.2f".format(seconds)} rtf=${"%.2f".format(ms / 1000.0 / seconds)} rms=${"%.4f".format(rms)} dsp=${if (fx != null) "SIGNATURE" else "OFF"} pss=${Debug.getPss() * 1024L}")
            }
            // cancellation in the middle of a generation
            val e = engine!!
            val started = java.util.concurrent.CountDownLatch(1)
            e.setProgressCallback { _, _ -> started.countDown() }
            var result: QwenEngine.NativeResult? = null
            e.resetCancel()
            val th = Thread { result = e.synthesizeWithIclPrompt("This long sentence is only here to be stopped halfway, before it ever finishes, by a cancel.",
                icl(app, "en-US").absolutePath, QwenEngine.NativeParams(languageId = 2050, maxAudioTokens = 320)) }
            th.start()
            val got = started.await(120, java.util.concurrent.TimeUnit.SECONDS)
            val c0 = SystemClock.elapsedRealtime()
            e.cancel()
            th.join(60_000)
            e.setProgressCallback(null)
            log("cancel progress_seen=$got stopped=${!th.isAlive} cancel_ms=${SystemClock.elapsedRealtime() - c0} success=${result?.success} error=${result?.errorMsg}")
            dispose("selftest")
            System.gc()
            log("unloaded pss_after=${Debug.getPss() * 1024L}")
            log("DONE ok")
        } catch (t: Throwable) {
            log("FAIL ${t.javaClass.simpleName}: ${t.message}")
            dispose("selftest")
        }
    }

    private fun floatWav(samples: FloatArray): ByteArray {
        val out = ByteBuffer.allocate(44 + samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        out.put("RIFF".toByteArray()).putInt(36 + samples.size * 2).put("WAVE".toByteArray())
            .put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(24_000).putInt(48_000).putShort(2).putShort(16)
            .put("data".toByteArray()).putInt(samples.size * 2)
        for (v in samples) out.putShort((v.coerceIn(-1f, 1f) * 32767f).toInt().toShort())
        return out.array()
    }

    // ---- installing --------------------------------------------------------------------------------

    /** Weights from Hugging Face at the fixed revision (resumes a cut download), checked by size and SHA-256. */
    fun download(context: Context, v: Variant, done: (String?) -> Unit) = job(context, "download", done) { app ->
        cancelDownload.set(false)
        val dir = models(app, v).apply { mkdirs() }
        File(dir, ".verified").delete()
        var before = 0L
        for (m in v.files) {
            val file = File(dir, m.local)
            if (file.length() == m.bytes && sha(file) == m.sha) { before += m.bytes; continue }
            val part = File(dir, "${m.name}.part")
            var have = if (part.length() <= m.bytes) part.length() else 0L.also { part.delete() }
            val conn = (URL("$SOURCE/${m.name}").openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000; readTimeout = 60_000; instanceFollowRedirects = true
                if (have > 0) setRequestProperty("Range", "bytes=$have-")
            }
            try {
                val code = conn.responseCode
                if (code == 200) have = 0L else require(code == 206) { "HTTP $code" }
                conn.inputStream.use { input ->
                    java.io.FileOutputStream(part, have > 0).use { out ->
                        val buf = ByteArray(256 * 1024)
                        while (true) {
                            if (cancelDownload.get()) error("cancelled")
                            val n = input.read(buf)
                            if (n < 0) break
                            have += n
                            require(have <= m.bytes) { "size" }
                            out.write(buf, 0, n)
                            progress = (before + have).toFloat() / v.bytes
                        }
                    }
                }
            } finally { conn.disconnect() }
            require(part.length() == m.bytes && sha(part) == m.sha) { part.delete(); "sha" }
            check(part.renameTo(file)) { "rename" }
            before += m.bytes
        }
        File(dir, ".verified").writeText(v.files.joinToString("\n") { "${it.local} ${it.sha}" })
        if (Store(app).configString("voiceNeuralModel") == null) CapriSettings.set(app, "voiceNeuralModel", v.id)
    }

    fun cancelDownload() = cancelDownload.set(true)

    /** One of the two .gguf files from your phone's files (it must be that file, byte for byte). */
    fun importModel(context: Context, uri: Uri, done: (String?) -> Unit) = job(context, "install", done) { app ->
        val tmp = File(root(app).apply { mkdirs() }, "import.part")
        try {
            copyIn(app, uri, tmp, VARIANTS.maxOf { v -> v.files.maxOf { it.bytes } })
            val digest = sha(tmp)
            val v = VARIANTS.firstOrNull { v -> v.files.any { it.bytes == tmp.length() && it.sha == digest } } ?: error("notmodel")
            val m = v.files.first { it.sha == digest }
            val dir = models(app, v).apply { mkdirs() }
            check(tmp.renameTo(File(dir, m.local))) { "rename" }
            if (v.files.all { File(dir, it.local).length() == it.bytes }) File(dir, ".verified").writeText(v.files.joinToString("\n") { "${it.local} ${it.sha}" })
        } finally { tmp.delete() }
    }

    /** The private profiles ZIP of Capri Voice V8: her vector (es) and the English source sound (en). */
    fun importProfiles(context: Context, uri: Uri, done: (String?) -> Unit) = job(context, "install", done) { app ->
        app.contentResolver.openInputStream(uri).use { raw -> installProfiles(app, requireNotNull(raw) { "?" }) }
    }

    /** Her profiles from Capri's public voice channel (CapriVoice.CHANNEL), checked by SHA-256, then installed. */
    fun downloadProfiles(context: Context, done: (String?) -> Unit) = job(context, "download", done) { app ->
        require(CapriVoice.CHANNEL.startsWith("https://")) { "nochannel" }
        cancelDownload.set(false)
        val part = File(app.cacheDir, "$PROFILES_FILE.part").apply { delete() }
        val conn = (URL("${CapriVoice.CHANNEL}/$PROFILES_FILE").openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000; readTimeout = 60_000; instanceFollowRedirects = true
        }
        try {
            require(conn.responseCode == 200) { "HTTP ${conn.responseCode}" }
            conn.inputStream.use { input -> part.outputStream().use { out ->
                val buf = ByteArray(64 * 1024); var have = 0L
                while (true) {
                    if (cancelDownload.get()) error("cancelled")
                    val n = input.read(buf); if (n < 0) break
                    have += n; require(have <= PROFILES_BYTES) { "size" }
                    out.write(buf, 0, n); progress = have.toFloat() / PROFILES_BYTES
                }
            } }
            require(part.length() == PROFILES_BYTES && sha(part) == PROFILES_SHA) { "sha" }
            part.inputStream().use { installProfiles(app, it) }
        } finally { conn.disconnect(); part.delete() }
    }

    private fun installProfiles(app: Context, raw: java.io.InputStream) {
        profiles(app).mkdirs()
        var found = 0
        val got = HashMap<String, ByteArray>()
        run {
            ZipInputStream(raw.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val name = entry.name.replace('\\', '/').trimStart('/')
                    if (name != PROMPT && name != ENGLISH && name != SPANISH_INFO && name != ENGLISH_INFO) continue
                    val bytes = zip.readBytesLimited(4L * 1024 * 1024)
                    val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
                    require(digest == when (name) { PROMPT -> PROMPT_SHA; ENGLISH -> ENGLISH_SHA; SPANISH_INFO -> SPANISH_INFO_SHA; else -> ENGLISH_INFO_SHA }) { "sha" }
                    got[name] = bytes
                }
            }
        }
        // Spanish: her approved ICL prompt, as PC reads it (the codes and vector of prompt.npz + the words of profile.json)
        val npz = got[PROMPT]; val info = got[SPANISH_INFO]
        if (npz != null && info != null) {
            val text = JSONObject(String(info, Charsets.UTF_8)).getJSONObject("reference").getString("text")
            atomic(icl(app, "es-MX"), iclFromNpz(npz, text))
            embedding(app, "es-MX").delete()
            found++
        }
        // English: its source sound and words; the prompt is made by the native encoder the first time
        val wav = got[ENGLISH]; val words = got[ENGLISH_INFO]
        if (wav != null && words != null) {
            atomic(englishWav(app), pcm16Wav(wav))
            atomic(englishText(app), JSONObject(String(words, Charsets.UTF_8)).getString("text").toByteArray())
            icl(app, "en-US").delete(); embedding(app, "en-US").delete()
            found++
        }
        require(found > 0) { "noprofile" }
    }

    /** what: a variant's id ("q4", "q8", "bf16") or "profile". */
    fun remove(context: Context, what: String, done: (String?) -> Unit) = job(context, "remove", done) { app ->
        dispose("removed")
        val v = VARIANTS.firstOrNull { it.id == what }
        if (v != null) models(app, v).deleteRecursively() else profiles(app).deleteRecursively()
    }

    fun bytes(context: Context): Long = root(context).walkBottomUp().filter { it.isFile }.sumOf { it.length() }

    /** Every installed model file checked again by SHA-256. */
    fun verify(context: Context, done: (String?) -> Unit) = job(context, "verify", done) { app ->
        for (v in VARIANTS) {
            if (!installed(app, v)) continue
            for (m in v.files) {
                val f = File(models(app, v), m.local)
                require(f.length() == m.bytes && sha(f) == m.sha) { File(models(app, v), ".verified").delete(); "sha:${m.name}" }
            }
        }
    }

    private fun job(context: Context, what: String, done: (String?) -> Unit, work: (Context) -> Unit) {
        val app = context.applicationContext
        if (busy.isNotEmpty()) return done("busy")
        busy = what
        jobs.execute {
            val err = runCatching { work(app) }.exceptionOrNull()?.let { it.message ?: it.javaClass.simpleName }
            if (err != null) { lastError = err; Diag.add("W", "voice $ID $what: $err") }
            busy = ""
            progress = -1f
            main.post { done(err) }
        }
    }

    private fun copyIn(app: Context, uri: Uri, to: File, max: Long) {
        app.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "?" }
            to.outputStream().use { out ->
                val buf = ByteArray(256 * 1024)
                var total = 0L
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    total += n
                    require(total <= max) { "too big" }
                    out.write(buf, 0, n)
                    progress = total.toFloat() / max
                }
            }
        }
    }

    private fun ZipInputStream.readBytesLimited(max: Long): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(64 * 1024)
        while (true) {
            val n = read(buf)
            if (n < 0) break
            require(out.size() + n <= max) { "too big" }
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    private fun atomic(file: File, bytes: ByteArray) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeBytes(bytes)
        check(tmp.renameTo(file)) { "rename" }
    }

    /** One array inside an .npz (a ZIP of .npy files). */
    internal fun npzEntry(npz: ByteArray, name: String): ByteArray {
        ZipInputStream(npz.inputStream()).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                if (e.name == name) return zip.readBytesLimited(1024 * 1024)
            }
        }
        error("npz:$name")
    }

    /**
     * A little-endian int64 .npy as its shape and values in C order (row by row). Her approved prompt.npz keeps
     * ref_code [76, 16] in Fortran order (column by column): 1.11.0/1.11.1 refused it ("npy:… 'fortran_order':
     * True"), so «Bajar su voz» failed for everyone. Now a 2-D Fortran array is read and put in C order; the file
     * itself is never changed.
     */
    internal fun npyLongs(npy: ByteArray): Pair<IntArray, LongArray> {
        require(npy.size > 10 && npy[0] == 0x93.toByte() && String(npy, 1, 5, Charsets.US_ASCII) == "NUMPY") { "npy" }
        val bb = ByteBuffer.wrap(npy).order(ByteOrder.LITTLE_ENDIAN)
        val (headerLen, start) = if (npy[6].toInt() == 1) bb.getShort(8).toInt() and 0xffff to 10 else bb.getInt(8) to 12
        val header = String(npy, start, headerLen, Charsets.US_ASCII)
        require("'<i8'" in header) { "npy:$header" }
        val fortran = Regex("'fortran_order':\\s*True").containsMatchIn(header)
        val shape = Regex("'shape':\\s*\\(([^)]*)\\)").find(header)?.groupValues?.get(1)
            ?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.map { it.toInt() }?.toIntArray() ?: error("npy:shape")
        val data = start + headerLen
        val n = shape.fold(1) { a, b -> a * b }
        require(npy.size - data == n * 8) { "npy:size" }
        if (!fortran || shape.size < 2) return shape to LongArray(n) { bb.getLong(data + it * 8) }
        require(shape.size == 2) { "npy:$header" }
        val (rows, cols) = shape[0] to shape[1]
        // Fortran: [r][c] is stored at c * rows + r
        return shape to LongArray(n) { i -> bb.getLong(data + ((i % cols) * rows + i / cols) * 8) }
    }

    /** A version 1 .npy (for the self-test). */
    internal fun npy(dtype: String, shape: IntArray, bytes: Int, fill: (ByteBuffer) -> Unit): ByteArray {
        val dims = if (shape.size == 1) "${shape[0]}," else shape.joinToString(", ")
        var header = "{'descr': '$dtype', 'fortran_order': False, 'shape': ($dims), }"
        while ((10 + header.length + 1) % 64 != 0) header += " "
        header += "\n"
        val b = ByteBuffer.allocate(10 + header.length + bytes).order(ByteOrder.LITTLE_ENDIAN)
        b.put(0x93.toByte()).put("NUMPY".toByteArray()).put(1).put(0).putShort(header.length.toShort()).put(header.toByteArray())
        fill(b)
        return b.array()
    }

    /** A little-endian float32 .npy (version 1 or 2) as floats. */
    internal fun npyFloats(npy: ByteArray): FloatArray {
        require(npy.size > 10 && npy[0] == 0x93.toByte() && String(npy, 1, 5, Charsets.US_ASCII) == "NUMPY") { "npy" }
        val major = npy[6].toInt()
        val bb = ByteBuffer.wrap(npy).order(ByteOrder.LITTLE_ENDIAN)
        val (headerLen, start) = if (major == 1) bb.getShort(8).toInt() and 0xffff to 10 else bb.getInt(8) to 12
        val header = String(npy, start, headerLen, Charsets.US_ASCII)
        require("'<f4'" in header) { "npy:$header" }
        // a vector reads the same in either order; a Fortran table would need reordering, which a vector never is
        val dims = Regex("'shape':\\s*\\(([^)]*)\\)").find(header)?.groupValues?.get(1)?.split(',')?.count { it.isNotBlank() } ?: 1
        require(dims <= 1 || !Regex("'fortran_order':\\s*True").containsMatchIn(header) || header.contains(Regex("'shape':\\s*\\(1,"))) { "npy:$header" }
        val data = start + headerLen
        val n = (npy.size - data) / 4
        return FloatArray(n) { bb.getFloat(data + it * 4) }
    }

    /** A float32 mono 24 kHz WAV as PCM16 (what the native vector extraction reads). */
    internal fun pcm16Wav(wav: ByteArray): ByteArray {
        val bb = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)
        require(String(wav, 0, 4, Charsets.US_ASCII) == "RIFF" && String(wav, 8, 4, Charsets.US_ASCII) == "WAVE") { "wav" }
        var pos = 12
        var format = 0; var channels = 0; var rate = 0; var bits = 0
        var data = -1; var size = 0
        while (pos + 8 <= wav.size) {
            val id = String(wav, pos, 4, Charsets.US_ASCII)
            val len = bb.getInt(pos + 4)
            if (id == "fmt ") { format = bb.getShort(pos + 8).toInt(); channels = bb.getShort(pos + 10).toInt(); rate = bb.getInt(pos + 12); bits = bb.getShort(pos + 22).toInt() }
            if (id == "data") { data = pos + 8; size = minOf(len, wav.size - data) }
            pos += 8 + len + (len and 1)
        }
        require(data > 0 && channels == 1 && rate == 24_000) { "wav:$format/$channels/$rate/$bits" }
        val samples: FloatArray = when {
            format == 3 && bits == 32 -> FloatArray(size / 4) { bb.getFloat(data + it * 4) }
            format == 1 && bits == 16 -> FloatArray(size / 2) { bb.getShort(data + it * 2) / 32768f }
            else -> error("wav:$format/$bits")
        }
        val out = ByteBuffer.allocate(44 + samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        out.put("RIFF".toByteArray()).putInt(36 + samples.size * 2).put("WAVE".toByteArray())
            .put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(24_000).putInt(48_000).putShort(2).putShort(16)
            .put("data".toByteArray()).putInt(samples.size * 2)
        for (s in samples) out.putShort((s.coerceIn(-1f, 1f) * 32767f).toInt().toShort())
        return out.array()
    }

    private fun sha(file: File): String {
        val d = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(1024 * 1024)
            while (true) { val n = input.read(buf); if (n < 0) break; d.update(buf, 0, n) }
        }
        return d.digest().joinToString("") { "%02x".format(it) }
    }
}
