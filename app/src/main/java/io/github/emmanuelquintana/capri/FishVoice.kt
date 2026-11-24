package io.github.emmanuelquintana.capri

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * (1.12.0, Emma: "que utilice una api como cuando vas a elegir una ia pero con fish audio para que hable en tiempo
 * real con cada mensaje suyo") Her voice in the cloud, from Fish Audio, with your own key (it stays on this phone,
 * sealed, and never goes to your account's copy). Her answer is sent sentence by sentence as her AI writes it, and
 * each one is heard as it arrives (about half a second to the first sound). Without internet, or if your key is out
 * of credit, she goes back to her voice on the phone. Same events as her neural voice (CapriNeural), so her page,
 * her chat's meter and her ears treat it the same.
 */
object FishVoice {
    const val ID = "fish"
    private const val API = "https://api.fish.audio/v1/tts"
    private const val RATE = 24_000
    private const val PREFS = "fish"

    /** Capri's own voice on Fish Audio (Emma's), per language; empty until it exists. */
    private val CAPRI = mapOf("es" to "0a56e85c27c6490caf07bf61d2a229f0", "en" to "")

    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor(Threads.factory)
    private val generation = AtomicLong(0)
    @Volatile private var track: AudioTrack? = null
    @Volatile private var conn: HttpURLConnection? = null
    @Volatile private var feedQueue: LinkedBlockingQueue<String>? = null
    @Volatile private var feedTicket = 0L
    private const val END_OF_ANSWER = "\u0000end"
    @Volatile var lastError = ""

    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun key(c: Context): String = prefs(c).getString("key", null)?.let { Store(c).open(it) }.orEmpty()
    fun hasKey(c: Context) = !prefs(c).getString("key", null).isNullOrEmpty()
    fun on(c: Context) = hasKey(c) && prefs(c).getBoolean("on", true)
    fun setOn(c: Context, on: Boolean) = prefs(c).edit().putBoolean("on", on).apply()
    fun forget(c: Context) = prefs(c).edit().clear().apply()

    /** The voice you pasted (any Fish Audio voice), else hers. */
    fun customVoice(c: Context) = prefs(c).getString("voice", null).orEmpty()
    fun setCustomVoice(c: Context, id: String) = prefs(c).edit().putString("voice", id.trim()).apply()
    fun voiceFor(c: Context, lang: String): String =
        customVoice(c).ifEmpty { CAPRI[lang.substringBefore('-')].orEmpty().ifEmpty { CAPRI["en"].orEmpty() } }
    fun hasCapri(lang: String) = CAPRI[lang.substringBefore('-')].orEmpty().isNotEmpty()

    /** Ready to answer in [lang] now: a key, a voice, switched on, and internet (and it didn't just fail). */
    fun usable(c: Context, lang: String): Boolean =
        on(c) && voiceFor(c, lang).isNotEmpty() && android.os.SystemClock.elapsedRealtime() >= restUntil && online(c)

    /** After a failure her other voice answers a while: 10 minutes for a key or credit problem, 1 for the network. */
    @Volatile private var restUntil = 0L
    fun rest(why: String) {
        restUntil = android.os.SystemClock.elapsedRealtime() + if (why == "fish:key" || why == "fish:credit") 600_000L else 60_000L
    }

    private fun online(c: Context): Boolean = runCatching {
        val cm = c.getSystemService(ConnectivityManager::class.java) ?: return true
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }.getOrDefault(true)

    /**
     * Your key checked with a tiny real sentence (Fish has no free "who am I"), then kept sealed. Null when it works,
     * else why ("badkey", "credit", or the error). Network: not on the main thread.
     */
    fun setKey(c: Context, key: String): String? {
        val k = key.trim()
        if (k.length < 16) return "badkey"
        val code = runCatching {
            val h = open(k, JSONObject().put("text", "Hola.").put("format", "pcm").put("sample_rate", RATE))
            try { h.responseCode.also { if (it == 200) h.inputStream.use { s -> s.read(ByteArray(256)) } } } finally { h.disconnect() }
        }.getOrElse { return it.message ?: "offline" }
        return when (code) {
            200 -> { prefs(c).edit().putString("key", Store(c).seal(k)).putBoolean("on", true).apply(); restUntil = 0L; null }
            401, 403 -> "badkey"
            402 -> "credit"
            else -> "HTTP $code"
        }
    }

    private fun open(key: String, body: JSONObject): HttpURLConnection {
        val h = URL(API).openConnection() as HttpURLConnection
        h.requestMethod = "POST"
        h.doOutput = true
        h.connectTimeout = 10_000
        h.readTimeout = 20_000
        h.setRequestProperty("Authorization", "Bearer $key")
        h.setRequestProperty("Content-Type", "application/json")
        h.setRequestProperty("model", "s2.1-pro")
        h.setRequestProperty("User-Agent", "Capri-Android")
        h.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        return h
    }

    /** More of the answer being said (streaming), as her AI writes it; [last]: that was all. */
    fun feed(text: String, last: Boolean): Boolean {
        val q = feedQueue ?: return false
        if (feedTicket != generation.get()) return false
        if (text.isNotBlank()) q.put(text)
        if (last) q.put(END_OF_ANSWER)
        return true
    }

    fun cancel() {
        generation.incrementAndGet()
        runCatching { conn?.disconnect() }
        stopTrack()
    }

    private fun stopTrack() {
        val t = track
        track = null
        if (t != null) runCatching { t.pause(); t.flush(); t.stop(); t.release() }
    }

    /**
     * Her answer [text] in her cloud voice. Events as CapriNeural's: VOICE_PREPARING, VOICE_READY (first sound
     * on its way), VOICE_STARTED, VOICE_PROGRESS, VOICE_FINISHED; VOICE_UNAVAILABLE (why: "fish:key",
     * "fish:credit", "fish:net") before any sound, so another voice can say it instead.
     */
    fun speak(context: Context, id: String, text: String, lang: String, events: CapriNeural.Events, streaming: Boolean = false) {
        val app = context.applicationContext
        cancel()
        val ticket = generation.get()
        val incoming = LinkedBlockingQueue<String>()
        feedTicket = ticket
        feedQueue = if (streaming) incoming else null
        val emit: (String, JSONObject) -> Unit = { state, extra -> main.post { if (ticket == generation.get() || state == "VOICE_CANCELLED") events.on(state, extra.put("source", ID)) } }
        val live = { ticket == generation.get() }
        worker.execute {
            if (!live()) return@execute
            val key = key(app)
            val voice = voiceFor(app, lang)
            if (key.isEmpty() || voice.isEmpty()) return@execute emit("VOICE_UNAVAILABLE", JSONObject().put("why", "fish:key"))
            emit("VOICE_PREPARING", JSONObject().put("pct", 0))
            val store = Store(app)
            val speed = store.configNumber("voiceRate", 1.0).coerceIn(0.5, 2.0)
            val sentences = CapriNeural.split(text).toMutableList()
            var ended = !streaming
            val more: () -> Boolean = more@{
                var waited = 0
                while (!ended && live()) {
                    val piece = incoming.poll(250, TimeUnit.MILLISECONDS)
                    if (piece == null) { if (++waited > 120) ended = true; continue }
                    waited = 0
                    if (piece == END_OF_ANSWER) { ended = true; break }
                    val added = CapriNeural.split(piece, shortFirst = false).filter { it.isNotBlank() }
                    if (added.isEmpty()) continue
                    sentences += added
                    return@more true
                }
                false
            }
            val minBuffer = AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val t = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(maxOf(minBuffer, RATE)) // (half a second)
                .build()
            t.setVolume(store.configNumber("voiceClipVolume", 1.0).toFloat().coerceIn(0f, 1f))
            if (!live()) { t.release(); return@execute }
            track = t
            var frames = 0L
            var started = false
            var n = -1
            try {
                while (true) {
                    n++
                    if (n > sentences.lastIndex && !more()) break
                    if (!live()) break
                    val body = JSONObject().put("text", sentences[n]).put("reference_id", voice).put("format", "pcm")
                        .put("sample_rate", RATE).put("latency", "balanced").put("normalize", true)
                        .put("language", if (lang.startsWith("es")) "es-MX" else lang)
                        .put("prosody", JSONObject().put("speed", speed))
                    val h = open(key, body)
                    conn = h
                    try {
                        val code = h.responseCode
                        if (code != 200) {
                            val why = when (code) { 401, 403 -> "fish:key"; 402 -> "fish:credit"; else -> "fish:HTTP $code" }
                            lastError = why
                            Diag.add("W", "voice $ID: $why")
                            rest(why)
                            if (!started) return@execute emit("VOICE_UNAVAILABLE", JSONObject().put("why", why))
                            break
                        }
                        h.inputStream.use { input ->
                            val buf = ByteArray(4096)
                            var odd: Int = -1 // (a sample split between two reads)
                            while (live()) {
                                val r = input.read(buf)
                                if (r < 0) break
                                if (r == 0) continue
                                var bytes = buf.copyOf(r)
                                if (odd >= 0) { bytes = byteArrayOf(odd.toByte()) + bytes; odd = -1 }
                                if (bytes.size % 2 == 1) { odd = bytes.last().toInt() and 0xff; bytes = bytes.copyOf(bytes.size - 1) }
                                if (bytes.isEmpty()) continue
                                if (!started) {
                                    started = true
                                    emit("VOICE_READY", JSONObject().put("pct", 0))
                                    t.play()
                                    watch(t, ticket, emit)
                                }
                                var off = 0
                                while (off < bytes.size && live()) {
                                    val w = t.write(bytes, off, bytes.size - off, AudioTrack.WRITE_BLOCKING)
                                    if (w <= 0) break
                                    off += w
                                }
                                frames += bytes.size / 2
                            }
                        }
                    } finally {
                        h.disconnect()
                        conn = null
                    }
                    if (started && (n < sentences.lastIndex || !ended)) {
                        val pct = ((n + 1) * 100 / (sentences.size + if (ended) 0 else 1)).coerceIn(0, 99)
                        emit("VOICE_PROGRESS", JSONObject().put("generating", true).put("pct", pct))
                    }
                }
                if (!live()) return@execute
                if (!started) return@execute emit("VOICE_UNAVAILABLE", JSONObject().put("why", "fish:noaudio"))
                // the end: a little silence, so the last word isn't clipped
                val pad = ShortArray(RATE * 3 / 10)
                t.write(pad, 0, pad.size, AudioTrack.WRITE_BLOCKING)
                frames += pad.size
                emit("VOICE_PROGRESS", JSONObject().put("generating", false).put("pct", 100))
                done(t, ticket, frames, emit)
            } catch (e: Throwable) {
                if (!live()) return@execute
                lastError = e.message ?: e.javaClass.simpleName
                Diag.add("W", "voice $ID: $lastError")
                rest("fish:net")
                if (!started) emit("VOICE_UNAVAILABLE", JSONObject().put("why", "fish:net"))
                else { stopTrack(); emit("VOICE_ERROR", JSONObject().put("error", lastError)) }
            }
        }
    }

    private fun watch(t: AudioTrack, ticket: Long, emit: (String, JSONObject) -> Unit) {
        var started = false
        var last = 0L
        val tick = object : Runnable {
            override fun run() {
                if (ticket != generation.get() || track !== t) return
                val pos = runCatching { t.playbackHeadPosition.toLong() and 0xffffffffL }.getOrDefault(0L)
                if (!started && pos > 0) { started = true; emit("VOICE_STARTED", JSONObject()) }
                if (started && pos - last >= RATE / 2) { last = pos; emit("VOICE_PROGRESS", JSONObject().put("ms", pos * 1000 / RATE)) }
                main.postDelayed(this, if (started) 100L else 50L)
            }
        }
        main.post(tick)
    }

    /** All of it written: finished once the speaker played the last frame (or it stopped moving). */
    private fun done(t: AudioTrack, ticket: Long, frames: Long, emit: (String, JSONObject) -> Unit) {
        var lastPos = -1L
        var still = 0
        val tick = object : Runnable {
            override fun run() {
                if (ticket != generation.get() || track !== t) return
                val pos = runCatching { t.playbackHeadPosition.toLong() and 0xffffffffL }.getOrDefault(frames)
                still = if (pos == lastPos) still + 1 else 0
                lastPos = pos
                if (pos >= frames || still > 30) {
                    stopTrack()
                    emit("VOICE_FINISHED", JSONObject().put("ms", pos * 1000 / RATE))
                    return
                }
                main.postDelayed(this, 50)
            }
        }
        main.post(tick)
    }
}
