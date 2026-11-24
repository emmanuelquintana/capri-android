package io.github.emmanuelquintana.capri

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import io.github.emmanuelquintana.voice.VoiceClip
import io.github.emmanuelquintana.voice.VoiceClipLibrary
import io.github.emmanuelquintana.voice.VoicePackManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.text.Normalizer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * (1.11.0) Capri Voice V8: her own recorded voice, phrase by phrase, offline. The packs (100 phrases in
 * Spanish with SIGNATURE V7.2 already in them, 102 in English) are optional and live in the app's own
 * folder; nothing ships inside the APK and no model is loaded to play one. The pack files are checked
 * against the V8 catalog's SHA-256 before anything is unpacked, installed in one move (VoicePackManager)
 * and every phrase is checked again before it plays.
 *
 * Since 2026-11-21 (Emma confirmed the permission of the people responsible) the packs are on Capri's public
 * voice channel [CHANNEL], a pre-release of this app's GitHub repo that the updater and the web ignore: the
 * app downloads one when you ask (and offers it once after the update). A pack from your files works too.
 * No token, no private repository.
 *
 * Her new answers (Gemini, Grok, any other) are never swapped for a phrase that says something else:
 * a phrase plays only for the exact event it was made for, or when the answer is word for word one of them.
 */
object CapriVoice {
    data class Pack(val lang: String, val id: String, val file: String, val bytes: Long, val sha: String, val clips: Int, val version: String)

    // clip-pack-catalog.json of Capri Voice V8 8.0.0 (capri-voice-packs 207cc6747e40ff007c2b3ef70b2f1ea3661eb5e1):
    // names, sizes and fingerprints only, no audio
    val PACKS = listOf(
        // (1.12.0, Emma: "pregraba las frases con la voz nueva") 9.0.0: the same phrases and ids, in her Fish Audio voice
        Pack("es-MX", "capri-clips-es-MX-private-production", "capri-clips-es-MX.zip", 1_494_683,
            "c665260e5a5187c976a28d6e20e53035b54f9c018051387c79a2247b82eefa27", 100, "9.0.0"),
        Pack("en-US", "capri-clips-en-US-private-production", "capri-clips-en-US.zip", 1_391_608,
            "cb4ea4c0755df6a43b16ff9d05dce81117625d8533c5fa46d2a3e492434fbf4f", 102, "9.0.0"),
    )

    /** Where her packs and profiles are downloaded from (`<CHANNEL>/<file>`): a public GitHub pre-release. */
    const val CHANNEL = "https://github.com/emmanuelquintana/capri-android/releases/download/capri-voz-9.0.0"

    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor(Threads.factory)
    @Volatile private var libs: Map<String, VoiceClipLibrary> = emptyMap()
    @Volatile private var byText: Map<String, Map<String, VoiceClip>> = emptyMap()
    @Volatile private var loaded = false
    @Volatile var busy = ""          // what the packs' worker is doing, for the app's screen
        private set
    @Volatile var progress = -1f     // a download, 0..1
        private set
    @Volatile var lastError = ""
        private set
    private val cancel = AtomicBoolean(false)
    private val upgraded = java.util.Collections.synchronizedSet(HashSet<String>()) // (once per run: offline isn't a loop)

    fun root(context: Context) = File(context.filesDir, "capri-v8")
    private fun archives(context: Context) = File(root(context), "archives")
    private fun manager(context: Context) = VoicePackManager(root(context), allowPrivate = true)

    /** Her phrases for that language of hers (es → es-MX, en → en-US), null for the others. */
    fun voiceLang(lang: String): String? = when (lang.substringBefore('-').lowercase()) {
        "es" -> "es-MX"
        "en" -> "en-US"
        else -> null
    }

    fun clipsOn(context: Context) = Store(context).configFlag("voiceClips", true)

    /** What says her new answers: "system" (the phone's voice, as before), "text" (in writing only) or
     *  "qwen-mobile" (V8.5: her neural voice on the phone, experimental, CapriNeural.kt). */
    fun engine(context: Context): String = Store(context).configString("voiceEngine")?.takeIf { it in ENGINES } ?: "system"

    /**
     * (1.11.0) The voice her new answers use now, by mode (Emma: "que responda con la voz de Capri, todo"):
     * automatic and balanced use her neural voice when it's installed and fits this phone, else the phone's;
     * fidelity uses only her own voice (neural or, without it, writing); light the phone's; custom what you
     * picked. The neural voice is her approved identity (the same ICL prompt as PC), so no mode swaps her
     * identity: without it she only falls back to the phone's voice, as before.
     */
    fun answerEngine(context: Context, lang: String): String {
        val tag = voiceLang(lang)
        val neural = tag != null && CapriNeural.status(context, tag).let { it == "ready" || it == "loaded" }
        val mode = Store(context).configString("voiceMode") ?: "automatic"
        // (1.12.0) her voice in the cloud (Fish Audio, your key): first whenever it's set up and online, except in
        // "light" (the phone's) or a custom pick of another engine
        // (Emma, beta: "puse mi clave de fish y no pareciera que tomara la clave") Custom with her own neural voice
        // picked is still her voice: the cloud one comes first there too; only the phone's voice or writing, picked
        // on purpose, keep it out
        val picked = engine(context)
        // (Emma: "un switch … VOZ LOCAL O EN LA NUBE") local: never the cloud one, whatever the rest says
        val cloud = Store(context).configFlag("voiceCloud", true)
        val fishOk = cloud && mode != "light" && !(mode == "custom" && (picked == "system" || picked == "text"))
        if (fishOk && FishVoice.usable(context, lang)) return FishVoice.ID.also { said(it, "") }
        val eng = when (mode) {
            "custom" -> picked.let { if (it == FishVoice.ID) (if (neural) CapriNeural.ID else "system") else it }
            "light" -> "system"
            "fidelity" -> if (neural) CapriNeural.ID else "text"
            else -> if (neural) CapriNeural.ID else "system"
        }
        said(eng, if (!FishVoice.hasKey(context)) "" else if (!cloud) "local chosen" else if (!fishOk) "mode $mode/$picked" else FishVoice.whyNot(context, lang))
        return eng
    }

    // (1.12.0) in her log, which voice answers and, if your cloud voice is set up, why it isn't that one (once per change)
    @Volatile private var lastSaid = ""
    private fun said(engine: String, whyNotFish: String) {
        val line = if (whyNotFish.isEmpty()) engine else "$engine (cloud voice: $whyNotFish)"
        if (line == lastSaid) return
        lastSaid = line
        Diag.add("I", "voice: answers with $line")
    }
    val ENGINES = setOf("system", "text", CapriNeural.ID, FishVoice.ID)

    fun installed(lang: String): VoiceClipLibrary? = libs[lang]

    /** The phrase to play: the one asked for by id, else one whose words are exactly [text]. */
    fun clipFor(context: Context, lang: String, id: String, text: String): Pair<VoiceClipLibrary, VoiceClip>? {
        if (!loaded) reload(context)
        if (!clipsOn(context)) return null
        val tag = voiceLang(lang) ?: return null
        val lib = libs[tag] ?: return null
        val clip = if (id.isNotEmpty()) lib.listClips().firstOrNull { it.id == id }?.takeIf { same(it.text, text) || text.isBlank() }
        else byText[tag]?.get(plain(text))
        return clip?.let { lib to it }
    }

    /** Her phrases as her page needs them (voicelines.js): per language, id, event, feeling, words, length. */
    fun catalog(context: Context): JSONObject {
        if (!loaded) reload(context)
        val out = JSONObject()
        if (!clipsOn(context)) return out
        for ((lang, lib) in libs) {
            out.put(lang, JSONArray().apply {
                for (c in lib.listClips()) put(JSONObject().put("id", c.id).put("eventId", c.eventId).put("emotion", c.emotion)
                    .put("text", c.text).put("ms", c.durationMs).put("hint", c.animationHint))
            })
        }
        return out
    }

    /** Reads what's installed again (on the packs' worker when called from the main thread). */
    fun reload(context: Context) {
        val app = context.applicationContext
        val read: () -> Unit = {
            val map = HashMap<String, VoiceClipLibrary>()
            runCatching { manager(app).listPacks() }.getOrDefault(emptyList()).forEach { id ->
                runCatching { VoiceClipLibrary(File(root(app), id), allowPrivate = true) }
                    .onSuccess { map[it.language] = it }
                    .onFailure { lastError = "$id: ${it.message}"; Diag.add("W", "voice pack $id: ${it.message}") }
            }
            libs = map
            // (1.12.0) her phrases from an older recording (8.0.0): the new ones come on their own, a pack at a time
            for (pack in PACKS) if (map[pack.lang]?.version.let { it != null && it != pack.version } && busy.isEmpty() && upgraded.add(pack.lang))
                main.post { download(app, pack.lang) { err -> Diag.add("I", "voice: her ${pack.lang} phrases ${err ?: "updated to ${pack.version}"}") } }
            byText = map.mapValues { (_, lib) -> lib.listClips().groupBy { plain(it.text) }.mapValues { it.value.first() } }
            loaded = true
            CapriService.current?.overlay?.emit("voice:packs", catalog(app).toString())
        }
        if (Looper.myLooper() == Looper.getMainLooper() && !loaded) {
            // first time on the main thread: just the manifests (small), so a phrase isn't missed
            read()
        } else if (Looper.myLooper() == Looper.getMainLooper()) worker.execute { read() } else read()
    }

    /** A pack from your phone's files: it must be one of V8's, byte for byte. */
    fun installFrom(context: Context, uri: Uri, done: (String?) -> Unit) = job(context, "install", done) { app ->
        val tmp = File(app.cacheDir, "capri-voice-pick.zip")
        try {
            app.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "?" }
                tmp.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        total += n
                        require(total <= 80L * 1024 * 1024) { "too big" }
                        out.write(buf, 0, n)
                    }
                }
            }
            val sha = sha(tmp)
            val pack = PACKS.firstOrNull { it.sha == sha && it.bytes == tmp.length() } ?: error("notV8")
            put(app, pack, tmp)
        } finally {
            tmp.delete()
        }
    }

    /** From the authorized channel, with progress; a cut download goes on where it stopped. */
    fun download(context: Context, lang: String, done: (String?) -> Unit) = job(context, "download", done) { app ->
        require(CHANNEL.startsWith("https://")) { "nochannel" }
        val pack = PACKS.first { it.lang == lang }
        val part = File(app.cacheDir, "${pack.file}.part")
        cancel.set(false)
        // (1.12.0) a cut connection goes on from where it was, a few times, before it gives up
        var tries = 0
        while (true) {
            var have = if (part.length() <= pack.bytes) part.length() else 0L.also { part.delete() }
            try {
                val conn = (URL("$CHANNEL/${pack.file}").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    if (have > 0) setRequestProperty("Range", "bytes=$have-")
                }
                try {
                    val code = conn.responseCode
                    if (code == 200) have = 0L else require(code == 206) { "HTTP $code" }
                    conn.inputStream.use { input ->
                        java.io.FileOutputStream(part, have > 0).use { out ->
                            val buf = ByteArray(64 * 1024)
                            while (true) {
                                if (cancel.get()) error("cancelled")
                                val n = input.read(buf)
                                if (n < 0) break
                                have += n
                                require(have <= pack.bytes) { "size" }
                                out.write(buf, 0, n)
                                progress = have.toFloat() / pack.bytes
                            }
                        }
                    }
                } finally {
                    conn.disconnect()
                }
                break
            } catch (e: java.io.IOException) {
                if (cancel.get() || ++tries > 6) throw e
                Diag.add("I", "voice download: ${e.message ?: e.javaClass.simpleName}, again in ${tries * 4} s")
                Thread.sleep(tries * 4000L)
            }
        }
        require(part.length() == pack.bytes && sha(part) == pack.sha) { part.delete(); "sha" }
        put(app, pack, part)
        part.delete()
    }

    fun cancelDownload() = cancel.set(true)

    /** Unpacked again from the pack kept when it was installed (still checked against its SHA-256). */
    fun repair(context: Context, lang: String, done: (String?) -> Unit) = job(context, "repair", done) { app ->
        val pack = PACKS.first { it.lang == lang }
        val kept = File(archives(app), pack.file)
        require(kept.isFile && sha(kept) == pack.sha) { "needFile" }
        manager(app).repair(kept, pack.sha)
    }

    fun remove(context: Context, lang: String, done: (String?) -> Unit) = job(context, "remove", done) { app ->
        val pack = PACKS.first { it.lang == lang }
        if (File(root(app), pack.id).exists()) manager(app).uninstall(pack.id)
        File(archives(app), pack.file).delete()
    }

    /** Every phrase of that pack checked against its fingerprint. */
    fun verify(context: Context, lang: String, done: (String?) -> Unit) = job(context, "verify", done) { app ->
        manager(app).verify(PACKS.first { it.lang == lang }.id)
    }

    private fun put(app: Context, pack: Pack, file: File) {
        val m = manager(app)
        if (File(root(app), pack.id).exists()) m.repair(file, pack.sha) else m.install(file, pack.sha)
        archives(app).mkdirs()
        val kept = File(archives(app), pack.file)
        if (kept.absolutePath != file.absolutePath) file.copyTo(kept, overwrite = true)
    }

    private fun job(context: Context, what: String, done: (String?) -> Unit, work: (Context) -> Unit) {
        val app = context.applicationContext
        if (busy.isNotEmpty()) return done("busy")
        busy = what
        worker.execute {
            val err = runCatching { work(app) }.exceptionOrNull()?.let { it.message ?: it.javaClass.simpleName }
            if (err != null) {
                lastError = err
                Diag.add("W", "voice $what: $err")
            }
            reload(app)
            busy = ""
            progress = -1f
            main.post { done(err) }
        }
    }

    /** How much room her voice takes on the phone. */
    fun bytes(context: Context): Long = root(context).walkBottomUp().filter { it.isFile }.sumOf { it.length() }

    private fun sha(file: File): String {
        val d = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(256 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                d.update(buf, 0, n)
            }
        }
        return d.digest().joinToString("") { "%02x".format(it) }
    }

    /** Words only: lower case, no accents, no punctuation or emoji ("¡Hola!" is "hola"). */
    fun plain(s: String): String = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()

    private fun same(a: String, b: String) = plain(a) == plain(b)
}
