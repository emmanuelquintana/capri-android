package io.github.emmanuelquintana.capri

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.ImageView
import org.json.JSONObject

/**
 * Capri peeking in your notification panel (1.9.0, Emma: "que capri pueda escurrirse entre las
 * notificaciones del panel… o del de ajustes"). Android draws its notification and quick settings
 * panel over every app that floats, Capri too; only an accessibility service may draw above it. So
 * this one, off unless you turn it on (Personalizar › Se asoma en tus notificaciones), does only that:
 * Android tells it when its own panel (com.android.systemui) opens, and she peeks in from the side,
 * waving, for a few seconds. It reads nothing (canRetrieveWindowContent false, the event's text is
 * never looked at), takes no gesture, watches no other app. Touches go through her. Only while she's on.
 */
class PeekService : AccessibilityService() {
    private val main = Handler(Looper.getMainLooper())
    private var view: ImageView? = null
    private var frames: List<Bitmap> = emptyList()
    private var fps = 8
    private var at = 0

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (event.packageName?.toString() != SYSTEM_UI) return
        val cls = event.className?.toString().orEmpty()
        // its volume slider, its dialogs: not the panel
        if (Regex("volume|dialog|toast|keyguard", RegexOption.IGNORE_CASE).containsMatchIn(cls)) return
        if (!CapriService.running) return
        if (getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true) return // (not on the lock screen)
        // 1.9.1 (Samsung, Android 16: "aparece siempre"): some phones' system UI sends these all the
        // time, and each one kept her there longer. Now: one visit, then a rest before the next.
        val now = System.currentTimeMillis()
        if (view != null || now - lastGone < REST_MS) return
        if (seen.add(cls)) Diag.add("I", "peek: system panel ${cls.substringAfterLast('.')}") // (a class name only)
        show()
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        hide(now = true)
        super.onDestroy()
    }

    private fun load() {
        if (frames.isNotEmpty()) return
        runCatching {
            val meta = JSONObject(assets.open("$ANIMS/anims.json").bufferedReader().use { it.readText() }).getJSONObject(ANIM)
            val n = meta.getInt("n")
            val w = meta.getInt("w")
            val h = meta.getInt("h")
            fps = meta.optInt("fps", 8).coerceIn(2, 24)
            val strip = assets.open("$ANIMS/$ANIM.png").use { BitmapFactory.decodeStream(it) }
            frames = (0 until n).map { Bitmap.createBitmap(strip, it * w, 0, w, h) }
        }.onFailure { Diag.add("W", "peek: ${it.message}") }
    }

    private var lastGone = 0L
    private val seen = HashSet<String>()

    private fun show() = main.post {
        load()
        if (frames.isEmpty() || view != null) return@post
        run {
            val wm = getSystemService(WindowManager::class.java) ?: return@post
            val h = (resources.displayMetrics.density * 150).toInt()
            val w = h * frames[0].width / frames[0].height
            val v = ImageView(this).apply {
                setImageBitmap(frames[0])
                scaleType = ImageView.ScaleType.FIT_CENTER
                // her side to the panel: she leans in from the right edge
                translationX = w.toFloat()
            }
            val p = WindowManager.LayoutParams(
                w, h,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.END or Gravity.TOP
                y = (resources.displayMetrics.heightPixels * 0.42f).toInt()
                x = -(w * 0.18f).toInt() // a bit of her past the edge: peeking, not standing there
            }
            runCatching { wm.addView(v, p) }.onFailure {
                Diag.add("W", "peek: ${it.message}")
                return@post
            }
            view = v
            v.animate().translationX(0f).setDuration(380).setInterpolator(android.view.animation.DecelerateInterpolator()).start()
            at = 0
            main.post(tick)
        }
        main.postDelayed(goAway, STAY_MS)
    }

    private val tick: Runnable = object : Runnable {
        override fun run() {
            val v = view ?: return
            at = (at + 1) % frames.size
            v.setImageBitmap(frames[at])
            main.postDelayed(this, 1000L / fps)
        }
    }

    private val goAway = Runnable { hide() }

    private fun hide(now: Boolean = false) {
        val v = view ?: return
        view = null
        lastGone = System.currentTimeMillis()
        main.removeCallbacks(tick)
        main.removeCallbacks(goAway)
        val wm = getSystemService(WindowManager::class.java)
        val remove = { runCatching { wm?.removeView(v) } }
        if (now) remove() else v.animate().translationX(v.width.toFloat()).setDuration(300).withEndAction { remove() }.start()
    }

    companion object {
        private const val SYSTEM_UI = "com.android.systemui"
        private const val ANIMS = "capri/assets/capri/anims"
        private const val ANIM = "wave"
        private const val STAY_MS = 6500L
        private const val REST_MS = 120_000L // at most one visit every two minutes

        /** Turned on in Android's Accessibility settings (only there: the app can't do it itself). */
        fun enabled(context: Context): Boolean {
            val on = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
            return on.split(':').any { it.equals("${context.packageName}/${PeekService::class.java.name}", ignoreCase = true) ||
                it.equals("${context.packageName}/.PeekService", ignoreCase = true) }
        }
    }
}
