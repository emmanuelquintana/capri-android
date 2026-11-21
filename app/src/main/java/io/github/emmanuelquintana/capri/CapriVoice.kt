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
 * The packs are private for now (their rights to share them with everyone are still pending): the app
 * only installs one you hand it from your phone's files, or downloads one from [CHANNEL] once an
 * authorized public channel exists. No token, no private repository.
 *
 * Her new answers (Gemini, Grok, any other) are never swapped for a phrase that says something else:
 * a phrase plays only for the exact event it was made for, or when the answer is word for word one of them.
 */
object CapriVoice {
    data class Pack(val lang: String, val id: String, val file: String, val bytes: Long, val sha: String, val clips: Int, val version: String)

    // clip-pack-catalog.json of Capri Voice V8 8.0.0 (capri-voice-packs 207cc6747e40ff007c2b3ef70b2f1ea3661eb5e1):
    // names, sizes and fingerprints only, no audio
    val PACKS = listOf(
        Pack("es-MX", "capri-clips-es-MX-private-production", "capri-clips-es-MX.zip", 2_792_445,
            "9d30e7f06cf9432e4bf6bfb5174874661e73e9e00eafb39e870415c1ae90bf7a", 100, "8.0.0"),
        Pack("en-US", "capri-clips-en-US-private-production", "capri-clips-en-US.zip", 2_406_080,
            "72a382be2a249878a4efa12c818b508e36f73ee0c5df81a587fa740751994f7a", 102, "8.0.0"),
    )

    /** Where authorized packs are downloaded from (`<CHANNEL>/<file>`). Empty: no public channel yet. */
    const val CHANNEL = ""

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
    val ENGINES = setOf("system", "text", CapriNeural.ID)

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
        var have = if (part.length() <= pack.bytes) part.length() else 0L.also { part.delete() }
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
