package io.github.emmanuelquintana.capri

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import android.widget.Toast
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/**
 * "Compartir → Capri" (1.9.0): a picture, a video or a text from any app, to her, as you'd send it
 * to a friend. She opens her chat and reacts to it (share.js); a video goes as three of its frames.
 * Nothing is saved: the picture waits in memory until her page takes it, and only her AI (the one
 * you chose, as for her looks at your screen) ever sees it.
 */
class ShareActivity : Activity() {
    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(Lang.wrap(newBase))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, R.string.ob_need_overlay, Toast.LENGTH_LONG).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        val type = intent?.type.orEmpty()
        val text = listOfNotNull(intent?.getStringExtra(Intent.EXTRA_SUBJECT), intent?.getStringExtra(Intent.EXTRA_TEXT))
            .joinToString("\n").trim().take(2000)
        @Suppress("DEPRECATION")
        val uri = if (Build.VERSION.SDK_INT >= 33) intent?.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        else intent?.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
        Toast.makeText(this, R.string.share_sending, Toast.LENGTH_SHORT).show()
        // read while this screen is open (the app that shared lets us read it only meanwhile)
        Threads.make {
            val kind = when {
                uri != null && type.startsWith("video/") -> "video"
                uri != null && type.startsWith("image/") -> "image"
                else -> "text"
            }
            val image = runCatching {
                when (kind) {
                    "video" -> frames(uri!!)
                    "image" -> picture(uri!!)
                    else -> null
                }
            }.onFailure { Diag.add("W", "share: ${it.message}") }.getOrNull()
            if (image == null && text.isEmpty()) {
                runOnUiThread {
                    Toast.makeText(this, R.string.share_failed, Toast.LENGTH_LONG).show()
                    finish()
                }
                return@make
            }
            pending = JSONObject().put("kind", if (image == null) "text" else kind).put("text", text).apply { if (image != null) put("image", image) }
            Diag.add("I", "share: a $kind${if (text.isNotEmpty()) " with text" else ""}")
            runOnUiThread {
                if (CapriService.running) CapriService.send(this, CapriService.ACTION_SHARE)
                else CapriService.start(this, CapriService.ACTION_SHARE)
                finish()
            }
        }.start()
    }

    /** A picture, at most MAX px on its long side, as a JPEG in base64. */
    private fun picture(uri: Uri): String? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX) sample *= 2
        val bmp = contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        return jpeg(fit(bmp, MAX))
    }

    /** Three frames of a video (a bit in, the middle, near the end), side by side in one picture. */
    private fun frames(uri: Uri): String? {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(this, uri)
            val ms = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val shots = listOf(0.15, 0.5, 0.85).mapNotNull { at ->
                val us = (ms * at * 1000).toLong()
                val f = if (Build.VERSION.SDK_INT >= 27) r.getScaledFrameAtTime(us, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, FRAME, FRAME)
                else r.getFrameAtTime(us, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                f?.let { fit(it, FRAME) }
            }
            if (shots.isEmpty()) return null
            val h = shots.maxOf { it.height }
            val w = shots.sumOf { it.width } + GAP * (shots.size - 1)
            val sheet = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
            val c = Canvas(sheet)
            c.drawColor(Color.BLACK)
            var x = 0f
            for (s in shots) {
                c.drawBitmap(s, x, (h - s.height) / 2f, null)
                x += s.width + GAP
            }
            return jpeg(sheet)
        } finally {
            runCatching { r.release() }
        }
    }

    private fun fit(bmp: Bitmap, max: Int): Bitmap {
        val k = max.toFloat() / maxOf(bmp.width, bmp.height)
        return if (k >= 1f) bmp else Bitmap.createScaledBitmap(bmp, (bmp.width * k).toInt().coerceAtLeast(1), (bmp.height * k).toInt().coerceAtLeast(1), true)
    }

    private fun jpeg(bmp: Bitmap): String {
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 80, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    companion object {
        private const val MAX = 1280
        private const val FRAME = 640
        private const val GAP = 8

        /** What was shared, until her page takes it (share:take): { kind, text, image? } */
        @Volatile var pending: JSONObject? = null

        fun take(): JSONObject? = pending.also { pending = null }
    }
}
