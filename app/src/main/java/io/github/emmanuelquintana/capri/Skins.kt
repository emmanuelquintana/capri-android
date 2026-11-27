package io.github.emmanuelquintana.capri

import android.content.Context
import android.graphics.BitmapFactory
import android.webkit.WebResourceResponse
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.UUID

/**
 * Her community skins (1.13.0): outfits people drew for her, from the community store (the website's
 * /api/skins, which Emma fills with the ones she approves). The store page (assets/android/store.js,
 * in her Vestuario screen) only shows small previews; a skin's whole picture is downloaded here, and
 * only when you add it. It's kept in the app's own files (skins/<id>.png, checked: from the website,
 * the size and SHA-256 the store said, a 240x420 picture), with its credits in skins/index.json, and
 * from then on it's one more outfit in her wardrobe (wardrobe.js reads /skins/index.json and
 * /skins/<id>.png, served by her pages as their own files).
 */
object Skins {
    const val SITE = "https://capri-web-sdkn.vercel.app"
    private const val HOST = "capri-web-sdkn.vercel.app"
    private const val MAX_BYTES = 3 * 1024 * 1024
    private val ID = Regex("^[a-z0-9][a-z0-9-]{1,40}$")
    private val lock = Any()

    private fun dir(c: Context) = File(c.filesDir, "skins").apply { mkdirs() }
    private fun indexFile(c: Context) = File(dir(c), "index.json")

    /** The skins on this phone, with their credits: [{id, name, author, authorUrl, method, added, desc}]. */
    fun list(c: Context): JSONArray = synchronized(lock) {
        runCatching { JSONArray(indexFile(c).readText()) }.getOrElse { JSONArray() }
    }

    private fun write(c: Context, arr: JSONArray) = synchronized(lock) {
        val f = indexFile(c)
        val tmp = File(f.parentFile, "index.json.tmp")
        tmp.writeText(arr.toString())
        tmp.renameTo(f)
    }

    /** A random id of this installation, only for the hearts (one per installation). Not the phone's. */
    fun device(c: Context): String {
        val p = c.getSharedPreferences("app", Context.MODE_PRIVATE)
        return p.getString("skinsId", null) ?: UUID.randomUUID().toString().also { p.edit().putString("skinsId", it).apply() }
    }

    /**
     * Downloads a skin the store listed and keeps it. [meta] is the store's entry. Network: call it
     * off the main thread. Returns null when it's in, or why not.
     */
    fun add(c: Context, meta: JSONObject): String? {
        val id = meta.optString("id")
        if (!ID.matches(id)) return "id"
        val image = meta.optString("image")
        val url = runCatching { URL(image) }.getOrNull() ?: return "url"
        if (url.protocol != "https" || url.host != HOST) return "url"
        val want = meta.optString("sha256").lowercase()
        val size = meta.optInt("bytes", 0)
        if (want.length != 64 || size <= 0 || size > MAX_BYTES) return "meta"
        val bytes = try {
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = false
            }
            try {
                if (conn.responseCode != 200) return "http ${conn.responseCode}"
                conn.inputStream.use { input ->
                    val out = java.io.ByteArrayOutputStream(size)
                    val buf = ByteArray(16 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        if (out.size() > MAX_BYTES) return "big"
                    }
                    out.toByteArray()
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            return "net"
        }
        if (bytes.size != size) return "size"
        val got = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        if (got != want) return "sha"
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        if (opts.outWidth != 240 || opts.outHeight != 420 || opts.outMimeType != "image/png") return "png"
        synchronized(lock) {
            val tmp = File(dir(c), "$id.png.tmp")
            tmp.writeBytes(bytes)
            if (!tmp.renameTo(File(dir(c), "$id.png"))) return "disk"
            val keep = JSONObject()
            for (k in listOf("id", "name", "author", "authorUrl", "method", "added", "desc")) {
                if (meta.has(k) && !meta.isNull(k)) keep.put(k, meta.get(k))
            }
            val arr = list(c)
            val next = JSONArray()
            for (i in 0 until arr.length()) arr.optJSONObject(i)?.takeIf { it.optString("id") != id }?.let { next.put(it) }
            next.put(keep)
            write(c, next)
        }
        Diag.add("I", "skins: + $id")
        return null
    }

    /** Takes a skin off this phone (if she's wearing it, she goes back to her own suit). */
    fun drop(c: Context, id: String) {
        if (!ID.matches(id)) return
        synchronized(lock) {
            File(dir(c), "$id.png").delete()
            val arr = list(c)
            val next = JSONArray()
            for (i in 0 until arr.length()) arr.optJSONObject(i)?.takeIf { it.optString("id") != id }?.let { next.put(it) }
            write(c, next)
        }
        Diag.add("I", "skins: - $id")
    }

    /** Her pages' /skins/… files: index.json and the pictures, nothing else. */
    fun serve(c: Context, path: String): WebResourceResponse? {
        if (!path.startsWith("/skins/")) return null
        val name = path.removePrefix("/skins/")
        val headers = mapOf("Cache-Control" to "no-cache")
        if (name == "index.json") {
            return WebResourceResponse("application/json", "utf-8", list(c).toString().byteInputStream()).apply { responseHeaders = headers }
        }
        val id = name.removeSuffix(".png")
        val f = File(dir(c), "$id.png")
        if (!name.endsWith(".png") || !ID.matches(id) || !f.isFile) {
            return WebResourceResponse("text/plain", "utf-8", 404, "Not Found", emptyMap(), "".byteInputStream())
        }
        return WebResourceResponse("image/png", null, FileInputStream(f)).apply { responseHeaders = headers }
    }
}
