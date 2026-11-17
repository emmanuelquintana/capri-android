package io.github.emmanuelquintana.capri

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import org.json.JSONObject
import java.text.Normalizer

/**
 * Capri using your phone for you (1.10.0, Emma: "controlar el celular"). "Capri, ve atrás", "ve al
 * inicio", "abre las notificaciones", "baja", "toca Ajustes", "escribe aquí hola". Only through
 * Android's Accessibility, which you turn on yourself (Ajustes › Capri usa tu celular); off, she
 * says how. She only acts when you ask, one thing each time, and:
 * - never inside a bank or payments app (GameWatch.looksLikeBank), nor on the lock screen;
 * - never types into a password field;
 * - reads the screen only at that moment, to find what you named, and keeps nothing.
 * Her page asks through HostBridge "control:do"; what went wrong comes back as "control:miss".
 */
class ControlService : AccessibilityService() {
    private val main = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        tell("control:state", JSONObject().put("on", true))
        Diag.add("I", "control: on")
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        tell("control:state", JSONObject().put("on", false))
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    // it listens to nothing: the config asks for no events (res/xml/capri_control.xml)
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    private fun tell(channel: String, json: JSONObject) {
        main.post { CapriService.current?.overlay?.emit(channel, json.toString()) }
    }

    private fun miss(why: String, what: String = "") {
        tell("control:miss", JSONObject().put("why", why).put("what", what))
    }

    /** The app you're in: the top app window that isn't hers. */
    private fun target(): AccessibilityNodeInfo? {
        val mine = packageName
        val tops = runCatching { windows }.getOrDefault(emptyList())
            .filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
            .sortedByDescending { it.layer }
        for (w in tops) {
            val root = w.root ?: continue
            if (root.packageName?.toString() != mine) return root
        }
        return rootInActiveWindow?.takeIf { it.packageName?.toString() != mine }
    }

    private fun blocked(root: AccessibilityNodeInfo?): Boolean {
        if (getSystemService(android.app.KeyguardManager::class.java)?.isKeyguardLocked == true) {
            miss("locked")
            return true
        }
        val pkg = root?.packageName?.toString().orEmpty()
        if (pkg.isNotEmpty() && GameWatch.looksLikeBank(this, pkg)) {
            Diag.add("I", "control: not in a bank app ($pkg)")
            miss("bank")
            return true
        }
        return false
    }

    fun act(what: String, text: String) {
        main.post {
            runCatching { doIt(what, text.trim()) }.onFailure {
                Diag.add("W", "control: $what: ${it.message}")
                miss("failed", what)
            }
        }
    }

    private fun doIt(what: String, text: String) {
        when (what) {
            "back" -> global(GLOBAL_ACTION_BACK)
            "home" -> global(GLOBAL_ACTION_HOME)
            "recents" -> global(GLOBAL_ACTION_RECENTS)
            "notifications" -> global(GLOBAL_ACTION_NOTIFICATIONS)
            "quick" -> global(GLOBAL_ACTION_QUICK_SETTINGS)
            "screenshot" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) global(GLOBAL_ACTION_TAKE_SCREENSHOT) else miss("old")
            "down", "up" -> {
                val root = target()
                if (blocked(root)) return
                scroll(root, what == "down")
            }
            "tap" -> {
                val root = target() ?: return miss("nothing", text)
                if (blocked(root)) return
                tap(root, text)
            }
            "type" -> {
                val root = target() ?: return miss("nothing", text)
                if (blocked(root)) return
                type(root, text)
            }
            else -> miss("unknown", what)
        }
    }

    private fun global(action: Int) {
        if (action != GLOBAL_ACTION_HOME && action != GLOBAL_ACTION_NOTIFICATIONS && blocked(target())) return
        if (!performGlobalAction(action)) miss("failed")
    }

    private fun walk(node: AccessibilityNodeInfo?, out: MutableList<AccessibilityNodeInfo>, depth: Int = 0) {
        if (node == null || depth > 40 || out.size > 600) return
        out.add(node)
        for (i in 0 until node.childCount) walk(node.getChild(i), out, depth + 1)
    }

    private fun scroll(root: AccessibilityNodeInfo?, down: Boolean) {
        val all = mutableListOf<AccessibilityNodeInfo>().also { walk(root, it) }
        val area = { n: AccessibilityNodeInfo -> Rect().also { n.getBoundsInScreen(it) }.let { it.width() * it.height() } }
        val list = all.filter { it.isScrollable && it.isVisibleToUser }.maxByOrNull(area)
        val action = if (down) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        if (list?.performAction(action) == true) return
        // no list that scrolls by itself: a swipe in the middle of the screen
        val m = resources.displayMetrics
        val x = m.widthPixels / 2f
        val (from, to) = if (down) m.heightPixels * 0.7f to m.heightPixels * 0.3f else m.heightPixels * 0.3f to m.heightPixels * 0.7f
        val path = Path().apply { moveTo(x, from); lineTo(x, to) }
        val ok = dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 280)).build(), null, null)
        if (!ok) miss("failed")
    }

    private fun tap(root: AccessibilityNodeInfo, said: String) {
        val want = plain(said)
        if (want.isEmpty()) return miss("nothing", said)
        val all = mutableListOf<AccessibilityNodeInfo>().also { walk(root, it) }
        fun label(n: AccessibilityNodeInfo) = plain(listOfNotNull(n.text, n.contentDescription, if (Build.VERSION.SDK_INT >= 26) n.hintText else null).joinToString(" "))
        val seen = all.filter { it.isVisibleToUser }
        // the exact name first, then one that starts with it, then one that has it
        val node = seen.firstOrNull { label(it) == want }
            ?: seen.firstOrNull { label(it).startsWith(want) }
            ?: seen.firstOrNull { label(it).contains(want) }
            ?: return miss("notFound", said)
        if (node.isPassword) return miss("password")
        var n: AccessibilityNodeInfo? = node
        while (n != null && !n.isClickable) n = n.parent
        if (n?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) return
        // nothing says it's clickable: a tap where it is
        val r = Rect().also { node.getBoundsInScreen(it) }
        val path = Path().apply { moveTo(r.exactCenterX(), r.exactCenterY()) }
        val ok = dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 60)).build(), null, null)
        if (!ok) miss("failed")
    }

    private fun type(root: AccessibilityNodeInfo, text: String) {
        if (text.isEmpty()) return miss("nothing")
        val all = mutableListOf<AccessibilityNodeInfo>().also { walk(root, it) }
        val field = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.takeIf { it.isEditable }
            ?: all.firstOrNull { it.isEditable && it.isVisibleToUser }
            ?: return miss("noField")
        if (field.isPassword) return miss("password")
        val before = field.text?.toString().orEmpty().takeUnless { Build.VERSION.SDK_INT >= 26 && field.isShowingHintText }.orEmpty()
        val now = if (before.isBlank()) text else "$before $text"
        val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, now) }
        field.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        if (!field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) miss("failed")
    }

    companion object {
        @Volatile
        var instance: ControlService? = null
            private set

        private fun plain(s: CharSequence?): String = Normalizer.normalize(s?.toString().orEmpty(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "").lowercase().replace(Regex("[^\\p{L}\\p{N} ]+"), " ").replace(Regex("\\s+"), " ").trim()

        /** Turned on in Android's Accessibility settings (only there: the app can't do it itself). */
        fun enabled(context: Context): Boolean {
            val on = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
            return on.split(':').any { it.equals("${context.packageName}/${ControlService::class.java.name}", ignoreCase = true) ||
                it.equals("${context.packageName}/.ControlService", ignoreCase = true) }
        }
    }
}
