package io.github.emmanuelquintana.capri

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import android.view.View
import java.io.File

/**
 * (1.12.0, Emma: "quiero íconos de perfil, fotos de perfil, nombres… ahí lo presionas en lugar de ajustes personales")
 * Your profile: a photo of yours (kept only on this phone, small) or one of her icons, with your name. Shown at the
 * top of Ajustes; a tap opens Ajustes personales.
 */
object Profile {
    private const val PREFS = "app"
    /** Her icons for you: a shape and a color each. */
    val ICONS = listOf(
        "person" to Hud.YELLOW, "spark" to Hud.MANE, "moon" to Hud.CYAN, "pad" to Hud.GREEN,
        "chat" to Hud.YELLOW, "hanger" to Hud.MANE, "globe" to Hud.CYAN, "eye" to Hud.GREEN,
    )

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun photo(c: Context) = File(c.filesDir, "avatar.png")

    /** "photo", or the index of her icon (0 by default). */
    fun choice(c: Context): String = prefs(c).getString("avatar", null)?.takeIf { it != "photo" || photo(c).exists() } ?: "0"

    fun setIcon(c: Context, i: Int) {
        prefs(c).edit().putString("avatar", i.coerceIn(0, ICONS.lastIndex).toString()).apply()
        runCatching { Account.changed(c) }
    }

    /** Your picture, cut square from its middle and made small (256 px). False if it couldn't be read. */
    fun setPhoto(c: Context, uri: Uri): Boolean = runCatching {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        var sample = 1
        while (minOf(opts.outWidth, opts.outHeight) / (sample * 2) >= 256) sample *= 2
        val full = c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
            ?: return false
        val side = minOf(full.width, full.height)
        val square = Bitmap.createBitmap(full, (full.width - side) / 2, (full.height - side) / 2, side, side)
        val small = Bitmap.createScaledBitmap(square, 256, 256, true)
        photo(c).outputStream().use { small.compress(Bitmap.CompressFormat.PNG, 100, it) }
        cached = null
        prefs(c).edit().putString("avatar", "photo").apply()
        Account.changed(c)
        true
    }.getOrDefault(false)

    private var cached: Pair<Long, Bitmap>? = null

    /** Your photo (read once and kept while it doesn't change: every chat bubble of yours shows it). */
    fun bitmap(c: Context): Bitmap? {
        if (choice(c) != "photo") return null
        val f = photo(c)
        val at = f.lastModified()
        cached?.let { (t, b) -> if (t == at) return b }
        return runCatching { BitmapFactory.decodeFile(f.path) }.getOrNull()?.also { cached = at to it }
    }

    /** (1.12.0) For your account: the icon, or the photo as a small JPEG (base64). */
    fun export(c: Context): org.json.JSONObject {
        val o = org.json.JSONObject().put("avatar", choice(c))
        bitmap(c)?.let { b ->
            val out = java.io.ByteArrayOutputStream()
            b.compress(Bitmap.CompressFormat.JPEG, 85, out)
            o.put("photo", android.util.Base64.encodeToString(out.toByteArray(), android.util.Base64.NO_WRAP))
        }
        return o
    }

    /** From your account onto this phone. */
    fun import(c: Context, o: org.json.JSONObject) {
        val avatar = o.optString("avatar", "0")
        if (avatar == "photo") {
            val bytes = runCatching { android.util.Base64.decode(o.optString("photo"), android.util.Base64.DEFAULT) }.getOrNull() ?: return
            val b = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return
            photo(c).outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
            cached = null
            prefs(c).edit().putString("avatar", "photo").apply()
        } else avatar.toIntOrNull()?.let { setIcon(c, it) }
    }

    /** A round avatar: your photo, or her icon on its color. */
    class Avatar(context: Context, private var icon: Int = -1) : View(context) {
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        private val clip = Path()
        private var pic: Bitmap? = null
        var picked = false
            set(v) { field = v; invalidate() }

        init { reload() }

        /** Mine (what you chose), or always icon [icon] when given (the icons to pick from). */
        fun reload() {
            pic = if (icon >= 0) null else bitmap(context)
            invalidate()
        }

        override fun onDraw(c: Canvas) {
            val s = minOf(width, height).toFloat()
            val cx = width / 2f
            val cy = height / 2f
            val r = s / 2f - s * 0.04f
            val i = if (icon >= 0) icon else choice(context).toIntOrNull() ?: 0
            val (kind, color) = ICONS[i.coerceIn(0, ICONS.lastIndex)]
            val p = pic
            if (p != null) {
                clip.reset(); clip.addCircle(cx, cy, r, Path.Direction.CW)
                c.save(); c.clipPath(clip)
                c.drawBitmap(p, null, RectF(cx - r, cy - r, cx + r, cy + r), fill)
                c.restore()
                ring.color = Hud.YELLOW
            } else {
                fill.color = Hud.INK
                c.drawCircle(cx, cy, r, fill)
                val d = Ui.Icon(kind, color)
                val m = (r * 0.55f).toInt()
                d.setBounds((cx - m).toInt(), (cy - m).toInt(), (cx + m).toInt(), (cy + m).toInt())
                d.draw(c)
                ring.color = color
            }
            ring.strokeWidth = s * if (picked) 0.08f else 0.04f
            if (picked) ring.color = Hud.YELLOW
            c.drawCircle(cx, cy, r, ring)
        }
    }
}
