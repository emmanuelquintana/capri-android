package io.github.emmanuelquintana.capri

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Emma's own early builds (the `next` branch), for her phones only.
 *
 * CI (.github/workflows/beta.yml) builds every push to `next`, signed with the same key as the releases,
 * and leaves it sealed (tools/beta/seal.py) on the repo's `beta` pre-release, which neither the public
 * updater (releases/latest) nor the website ever read. Sealed means AES-256-CTR + HMAC-SHA256 with keys
 * from a code that lives only in the repo's secrets and in Emma's head: without it the files are noise,
 * and nothing about this shows anywhere in the app.
 *
 * She unlocks it by sending that code as a message in the Chat tab (ChatTab.send): it never reaches the
 * AI or her chat. From then on, and only on that phone, the Updates card has the beta switch.
 */
object Beta {
    private const val PREFS = "canal"
    private const val BASE = "https://github.com/${Updater.REPO}/releases/download/beta/"
    private const val MANIFEST = "m.bin"
    private val MAGIC = "CPB1".toByteArray()
    private const val ROUNDS = 100_000

    /** The code's shape (4 groups of 5 letters/digits): anything else is just a chat message. */
    private val SHAPE = Regex("^[A-Z2-7]{5}(-[A-Z2-7]{5}){3}$")

    fun looksLikeCode(text: String) = SHAPE.matches(norm(text))
    private fun norm(text: String) = text.trim().uppercase().replace(" ", "")

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun code(c: Context) = prefs(c).getString("code", null)

    fun unlocked(c: Context) = code(c) != null
    /** Beta builds wanted (only once unlocked). */
    fun on(c: Context) = unlocked(c) && prefs(c).getBoolean("on", true)
    fun setOn(c: Context, on: Boolean) = prefs(c).edit().putBoolean("on", on).apply()
    /** Locked again: the switch is gone until the code is sent again. */
    fun forget(c: Context) = prefs(c).edit().clear().apply()

    /** This build is a beta (CI's -PcapriBeta=N: "1.11.8-beta.N"). */
    val running get() = BuildConfig.VERSION_NAME.contains("-beta.")

    /**
     * The code checked against the real beta channel, kept only when it opens it. Network.
     * null = unlocked; otherwise what went wrong, to show her (a wrong code says only that).
     */
    fun unlock(c: Context, text: String): String? {
        val code = norm(text)
        if (!SHAPE.matches(code)) return "Código no válido"
        return try {
            manifest(code)
            prefs(c).edit().putString("code", code).putBoolean("on", true).apply()
            Diag.add("I", "beta unlocked")
            null
        } catch (e: SecurityException) {
            "Código no válido"
        } catch (e: Throwable) {
            Diag.add("W", "beta unlock: $e")
            "No se pudo comprobar: ${e.javaClass.simpleName} ${e.message ?: ""}".trim()
        }
    }

    /** The newest beta as an update, or null (locked, off, offline, nothing there). Network. */
    fun latest(c: Context): Updater.Release? {
        if (!on(c)) return null
        val code = code(c) ?: return null
        return try {
            val m = manifest(code)
            // Android never installs over a higher versionCode: a beta of an older version isn't one
            if (m.optInt("code") < BuildConfig.VERSION_CODE) return null
            val v = m.getString("version")
            Updater.Release("v$v", BASE + m.getString("apk"), m.optLong("size"), "https://github.com/${Updater.REPO}/commit/" + m.optString("commit"),
                m.optString("notes"), beta = true)
        } catch (e: Exception) {
            Diag.add("W", "beta: ${e.message}")
            null
        }
    }

    private fun manifest(code: String): JSONObject {
        val conn = get(BASE + MANIFEST)
        val sealed = conn.inputStream.use { it.readBytes() }
        val tmp = File.createTempFile("m", ".bin")
        try {
            tmp.writeBytes(sealed)
            val out = File.createTempFile("m", ".json")
            try {
                open(code, tmp, out)
                return JSONObject(out.readText())
            } finally { out.delete() }
        } finally { tmp.delete() }
    }

    /** The downloaded (sealed) APK into a plain one; throws if it isn't genuine. */
    fun unseal(c: Context, sealed: File, apk: File) {
        val code = code(c) ?: throw IllegalStateException("beta locked")
        open(code, sealed, apk)
    }

    private fun get(url: String): HttpURLConnection {
        var conn = URL(url).openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "Capri-Android")
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        var hops = 0
        while (conn.responseCode in 300..399 && hops++ < 5) {
            val next = conn.getHeaderField("Location") ?: break
            conn.disconnect()
            conn = URL(next).openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Capri-Android")
        }
        if (conn.responseCode != 200) throw IllegalStateException("HTTP ${conn.responseCode}")
        return conn
    }

    /**
     * Sealed file: "CPB1" | salt (16) | iv (16) | AES-256-CTR ciphertext | HMAC-SHA256 (32) of all before it.
     * Keys: PBKDF2-HMAC-SHA256(code, salt, 100 000 rounds, 64 bytes) = AES key | MAC key.
     * The MAC is checked over the whole file before a single byte is decrypted.
     */
    private fun open(code: String, sealed: File, out: File) {
        val len = sealed.length()
        if (len < 4 + 16 + 16 + 32) throw IllegalStateException("short")
        val head = ByteArray(36)
        sealed.inputStream().use { readFully(it, head) }
        if (!head.copyOfRange(0, 4).contentEquals(MAGIC)) throw IllegalStateException("not sealed")
        val salt = head.copyOfRange(4, 20)
        val iv = head.copyOfRange(20, 36)
        val keys = pbkdf2(code.toByteArray(), salt, ROUNDS, 64)
        val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(keys, 32, 32, "HmacSHA256")) }
        val body = len - 32
        val tag = ByteArray(32)
        sealed.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            var left = body
            while (left > 0) {
                val n = input.read(buf, 0, minOf(buf.size.toLong(), left).toInt())
                if (n < 0) throw IllegalStateException("short")
                mac.update(buf, 0, n)
                left -= n
            }
            readFully(input, tag)
        }
        if (!MessageDigest.isEqual(mac.doFinal(), tag)) throw SecurityException("wrong code or damaged file")
        val cipher = Cipher.getInstance("AES/CTR/NoPadding").apply { init(Cipher.DECRYPT_MODE, SecretKeySpec(keys, 0, 32, "AES"), IvParameterSpec(iv)) }
        sealed.inputStream().use { input ->
            input.skipFully(36)
            out.outputStream().use { o ->
                val buf = ByteArray(64 * 1024)
                var left = body - 36
                while (left > 0) {
                    val n = input.read(buf, 0, minOf(buf.size.toLong(), left).toInt())
                    if (n < 0) throw IllegalStateException("short")
                    cipher.update(buf, 0, n)?.let { o.write(it) }
                    left -= n
                }
                cipher.doFinal()?.let { o.write(it) }
            }
        }
    }

    private fun InputStream.skipFully(n: Long) {
        var left = n
        while (left > 0) {
            val s = skip(left)
            if (s <= 0) { if (read() < 0) throw IllegalStateException("short") else left-- } else left -= s
        }
    }

    private fun readFully(input: InputStream, b: ByteArray) {
        var off = 0
        while (off < b.size) {
            val n = input.read(b, off, b.size - off)
            if (n < 0) throw IllegalStateException("short")
            off += n
        }
    }

    /** PBKDF2-HMAC-SHA256 by hand (same bytes on every Android, whatever its crypto provider). */
    private fun pbkdf2(password: ByteArray, salt: ByteArray, rounds: Int, length: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(password, "HmacSHA256")) }
        val out = ByteArray(length)
        var block = 1
        var off = 0
        while (off < length) {
            mac.update(salt)
            mac.update(byteArrayOf((block ushr 24).toByte(), (block ushr 16).toByte(), (block ushr 8).toByte(), block.toByte()))
            var u = mac.doFinal()
            val t = u.copyOf()
            for (i in 1 until rounds) {
                u = mac.doFinal(u)
                for (j in t.indices) t[j] = (t[j].toInt() xor u[j].toInt()).toByte()
            }
            val n = minOf(t.size, length - off)
            System.arraycopy(t, 0, out, off, n)
            off += n
            block++
        }
        return out
    }
}
