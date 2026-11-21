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

    private fun end() {
        host.removeView(shade)
        host.removeView(card)
        done()
    }

    private fun next() {
        at++
        if (at >= steps.size) { end(); return }
        val step = steps[at]
        card.visibility = View.INVISIBLE
        hole.setEmpty()
        shade.invalidate()
        // what the step needs on screen first (its tab), then where it is once laid out
        val target = find(step.target)
        host.postDelayed({ place(step, target) }, 380)
    }

    private fun place(step: Step, target: View?) {
        if (target != null && target.isShown) {
            target.requestRectangleOnScreen(Rect(0, 0, target.width, target.height), true)
            val t = IntArray(2); val h = IntArray(2)
            target.getLocationInWindow(t); host.getLocationInWindow(h)
            val pad = ui.dp(6f)
            hole.set(t[0] - h[0] - pad, t[1] - h[1] - pad, t[0] - h[0] + target.width + pad, t[1] - h[1] + target.height + pad)
        } else hole.setEmpty()
        shade.invalidate()
        fill(step)
        // the card next to what's lit, on the side with room (in the middle when nothing is lit)
        card.measure(View.MeasureSpec.makeMeasureSpec(host.width - ui.dpi(36), View.MeasureSpec.EXACTLY), View.MeasureSpec.UNSPECIFIED)
        val gap = ui.dp(18f)
        when {
            hole.isEmpty -> { cardParams.gravity = Gravity.CENTER_VERTICAL; cardParams.topMargin = 0 }
            hole.centerY() > host.height / 2f -> {
                cardParams.gravity = Gravity.TOP
                cardParams.topMargin = maxOf(ui.dpi(40), (hole.top - gap - card.measuredHeight).toInt())
            }
            else -> { cardParams.gravity = Gravity.TOP; cardParams.topMargin = (hole.bottom + gap).toInt() }
        }
        card.layoutParams = cardParams
        card.visibility = View.VISIBLE
        ui.slideIn(card)
    }

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
            fun word(o: JSONObject) = o.optString(lang).ifEmpty { o.optString("es") }
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
