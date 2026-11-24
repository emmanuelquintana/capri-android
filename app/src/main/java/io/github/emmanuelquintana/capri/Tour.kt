package io.github.emmanuelquintana.capri

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import org.json.JSONObject

/**
 * (1.11.0, Emma: "una vista guiada hacia lo nuevo… con opacidad para lo que no es nuevo… un sistema que cada
 * update podamos casi reutilizar") Her guided tour of what's new.
 *
 * Reusable: the steps live in assets/android/tour.json, one list per version. For a new version, change its
 * "id" (shown once per id) and its steps; this code stays. Each step names what to light up by a key the app
 * knows (MainActivity.tourTarget): "bar", "tab:<chat|news|community|games|style|cmds|settings>",
 * "settings:<group key>" (an entry of Ajustes), "voice:<get|test|engines|answers>" (on Ajustes de voz), and its words in es/en/fr/pt/ru (Spanish if one is missing).
 * Everything but that is dimmed; a tap on the dim does nothing (Siguiente or Saltar move on).
 */
class Tour(
    private val a: Activity,
    private val ui: Ui,
    private val host: FrameLayout,
    private val steps: List<Step>,
    private val find: (String) -> View?,
    private val done: () -> Unit,
) {
    class Step(val target: String, val title: String, val text: String)

    private var at = -1
    private val hole = RectF()
    private val shade = object : View(a) {
        private val dim = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xD9050508.toInt() }
        private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = Hud.YELLOW; strokeWidth = ui.dp(2.5f) }
        private val path = Path()
        override fun onDraw(c: Canvas) {
            path.reset()
            path.fillType = Path.FillType.EVEN_ODD
            path.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
            val r = ui.dp(16f)
            if (!hole.isEmpty) path.addRoundRect(hole, r, r, Path.Direction.CW)
            c.drawPath(path, dim)
            if (!hole.isEmpty) c.drawRoundRect(hole, r, r, ring)
        }
    }.apply { isClickable = true } // the dim keeps your taps: Siguiente / Saltar move on
    private val card = ui.card(Hud.YELLOW, Hud.MANE)
    private val cardParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
        leftMargin = ui.dpi(18); rightMargin = ui.dpi(18)
    }

    fun start() {
        // over everything, the pill bar too (it has its own elevation: without this it stayed lit and un-ringed)
        shade.elevation = ui.dp(24f)
        card.elevation = ui.dp(26f)
        host.addView(shade, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        host.addView(card, cardParams)
        shade.alpha = 0f
        shade.animate().alpha(1f).setDuration(220).start()
        next()
    }

    private var over = false
    private fun end() {
        if (over) return
        over = true
        host.removeView(shade)
        host.removeView(card)
        done()
    }

    private fun next() {
        if (over) return
        at++
        if (at >= steps.size) { end(); return }
        val step = steps[at]
        card.visibility = View.INVISIBLE
        hole.setEmpty()
        shade.invalidate()
        // what the step needs on screen first (its tab), then where it is once laid out
        val target = find(step.target)
        host.postDelayed({ if (!over) place(step, target) }, 380)
    }

    /** Where the lit view is now, in the host's coordinates. */
    private fun measure(target: View) {
        val t = IntArray(2); val h = IntArray(2)
        target.getLocationInWindow(t); host.getLocationInWindow(h)
        val pad = ui.dp(6f)
        hole.set(t[0] - h[0] - pad, t[1] - h[1] - pad, t[0] - h[0] + target.width + pad, t[1] - h[1] + target.height + pad)
    }

    /** The part of the host the card may use: below the status bar, above the navigation bar. */
    private fun safeTop(): Float {
        val h = IntArray(2).also { host.getLocationInWindow(it) }
        @Suppress("DEPRECATION") val inset = host.rootWindowInsets?.systemWindowInsetTop ?: 0
        return maxOf(0, inset - h[1]) + ui.dp(12f)
    }
    private fun safeBottom(): Float {
        @Suppress("DEPRECATION") val inset = host.rootWindowInsets?.systemWindowInsetBottom ?: 0
        return host.height - inset - ui.dp(12f)
    }

    private fun scrollerOf(v: View): android.widget.ScrollView? {
        var p = v.parent
        while (p is View) { if (p is android.widget.ScrollView) return p; p = p.parent }
        return null
    }

    private fun place(step: Step, target: View?) {
        fill(step)
        card.measure(View.MeasureSpec.makeMeasureSpec(host.width - ui.dpi(36), View.MeasureSpec.EXACTLY), View.MeasureSpec.UNSPECIFIED)
        val cardH = card.measuredHeight
        val gap = ui.dp(18f)
        val top = safeTop()
        val bottom = safeBottom()
        val lit = target != null && target.isShown
        if (lit) {
            target!!.requestRectangleOnScreen(Rect(0, 0, target.width, target.height), true)
            measure(target)
            // (1.12.1, in Portuguese the card ran off the screen and covered what was lit) no room for the card
            // above or below: its top goes up so the card fits under it
            val fits = hole.bottom + gap + cardH <= bottom || hole.top - gap - cardH >= top
            if (!fits) scrollerOf(target)?.let { sv ->
                sv.scrollBy(0, (hole.top - top).toInt())
                measure(target)
            }
        } else hole.setEmpty()
        // the card next to what's lit, on the side with room (in the middle when nothing is lit); when it fits
        // on neither side it sits at the bottom, fully readable, and the light stops above it
        cardParams.gravity = Gravity.TOP
        when {
            hole.isEmpty -> { cardParams.gravity = Gravity.CENTER_VERTICAL; cardParams.topMargin = 0 }
            hole.bottom + gap + cardH <= bottom -> cardParams.topMargin = (hole.bottom + gap).toInt()
            hole.top - gap - cardH >= top -> cardParams.topMargin = (hole.top - gap - cardH).toInt()
            else -> {
                val y = maxOf(top, bottom - cardH)
                cardParams.topMargin = y.toInt()
                hole.bottom = minOf(hole.bottom, y - gap)
                if (hole.bottom - hole.top < ui.dp(24f)) hole.setEmpty()
            }
        }
        if (!hole.isEmpty) { hole.top = maxOf(hole.top, top - ui.dp(6f)); hole.bottom = minOf(hole.bottom, bottom) }
        shade.invalidate()
        card.layoutParams = cardParams
        card.visibility = View.VISIBLE
        ui.slideIn(card)
    }

    /** (1.12.1) Back during the tour closes the tour, not the screen under it (it left the light on the wrong spot). */
    fun skip() = end()

    private fun fill(step: Step) {
        card.removeAllViews()
        card.addView(ui.label(a.getString(R.string.tour_step, at + 1, steps.size), Hud.YELLOW))
        card.addView(ui.text(step.title, 18f, Hud.PAPER, ui.head), ui.block(6))
        card.addView(ui.text(step.text, 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.2f) }, ui.block(4))
        val row = ui.row()
        val last = at == steps.size - 1
        if (!last) row.addView(ui.button(a.getString(R.string.tour_skip), Ui.Kind.LINE) { end() }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { rightMargin = ui.dpi(10) })
        row.addView(ui.button(a.getString(if (last) R.string.tour_done else R.string.tour_next), Ui.Kind.MAIN) { next() }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        card.addView(row, ui.block(14))
    }

    companion object {
        /**
         * This version's tour (assets/android/tour.json), or another tour in the same format (1.11.3: the voice
         * tutorial, assets/android/tour-voz.json): its id and its steps in the app's language, or null.
         */
        fun load(a: android.content.Context, file: String = "android/tour.json"): Pair<String, List<Step>>? = runCatching {
            val json = JSONObject(a.assets.open(file).bufferedReader().use { it.readText() })
            val lang = Lang.current(a).take(2)
            fun word(o: JSONObject) = o.optString(lang).ifEmpty { o.optString("en") }.ifEmpty { o.optString("es") }
            val list = json.getJSONArray("steps")
            json.getString("id") to (0 until list.length()).map { i ->
                val s = list.getJSONObject(i)
                Step(s.getString("target"), word(s.getJSONObject("title")), word(s.getJSONObject("text")))
            }
        }.getOrNull()?.takeIf { it.second.isNotEmpty() }

        /** A new install already sees everything in her welcome: this version's tour counts as seen. */
        fun seen(context: android.content.Context) {
            val id = load(context)?.first ?: return
            context.getSharedPreferences("app", android.content.Context.MODE_PRIVATE).edit().putString("tourSeen", id).apply()
        }
    }
}
