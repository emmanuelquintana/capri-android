package io.github.emmanuelquintana.capri

import android.app.Activity
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * The app's center (1.11.0, Emma: "que el centro de la app sea directamente un chat con ella, para
 * chatearte con Capri cómodamente; y que en las otras opciones estén las que ya tenemos"): her chat as
 * the first tab. Your whole history with her (ChatLog: this tab and her bubble over your screen), and
 * what you write here goes to her (AppChat), with her own brain, memory and powers.
 */
class ChatTab(private val a: Activity, private val ui: Ui, private val help: () -> Unit) {
    private lateinit var list: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var field: EditText
    private lateinit var sendBtn: TextView
    private lateinit var statusLine: TextView
    private lateinit var headFace: ImageView
    private lateinit var input: LinearLayout
    private var waiting: String? = null // the id of the message she's answering
    private var typingBubble: TextView? = null
    private val dots = object : Runnable {
        override fun run() {
            val t = typingBubble ?: return
            t.text = when (t.text.length) { 1 -> "••"; 2 -> "•••"; else -> "•" }
            t.postDelayed(this, 380)
        }
    }
    private val giveUp = Runnable {
        if (waiting != null) answered(AppChat.Reply(waiting!!, a.getString(R.string.chat_noreply), "aside", false))
    }

    /** The tab: her at the top, the conversation, and the line to write in (above the pill bar). */
    fun build(): View {
        val root = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        // her, at the top: face, name, how she is
        val head = ui.row().apply { setPadding(ui.dpi(20), ui.dpi(18), ui.dpi(20), ui.dpi(10)) }
        headFace = ImageView(a).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = Hud.Plate(Hud.INK_2, Hud.YELLOW, Hud.MANE, cut = ui.dp(8f), stroke = ui.dp(1.5f), offset = ui.dp(3f))
            setPadding(ui.dpi(3), ui.dpi(3), ui.dpi(6), ui.dpi(6))
            ui.face("grin")?.let { setImageBitmap(it) }
        }
        head.addView(headFace, LinearLayout.LayoutParams(ui.dpi(52), ui.dpi(52)))
        head.addView(ui.col().apply {
            setPadding(ui.dpi(12), 0, 0, 0)
            addView(ui.text("CAPRI", 26f, Hud.YELLOW, ui.head).apply { letterSpacing = 0.06f; includeFontPadding = false })
            statusLine = ui.text("", 11f, Hud.GREEN, ui.mono).apply { letterSpacing = 0.1f }
            addView(statusLine)
        }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        // what you can ask her (the old «Comandos» tab)
        head.addView(ui.text("?", 18f, Hud.YELLOW, ui.head).apply {
            gravity = Gravity.CENTER
            background = Hud.Plate(Hud.INK, Hud.YELLOW, cut = ui.dp(6f), stroke = ui.dp(1f), offset = 0f)
            contentDescription = a.getString(R.string.chat_help)
            isClickable = true
            setOnClickListener { help() }
        }, LinearLayout.LayoutParams(ui.dpi(40), ui.dpi(40)))
        root.addView(head)
        root.addView(View(a).apply { setBackgroundColor(Hud.LINE_STRONG) }, LinearLayout.LayoutParams(MATCH_PARENT, ui.dpi(1)))

        list = ui.col().apply { setPadding(ui.dpi(14), ui.dpi(12), ui.dpi(14), ui.dpi(12)) }
        scroll = ScrollView(a).apply { isFillViewport = true; addView(list); isVerticalScrollBarEnabled = false }
        root.addView(scroll, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))

        // the line to write in
        input = ui.row().apply {
            setPadding(ui.dpi(12), ui.dpi(8), ui.dpi(12), ui.dpi(8))
            gravity = Gravity.BOTTOM
        }
        field = EditText(a).apply {
            hint = a.getString(R.string.chat_hint)
            setHintTextColor(Hud.PAPER_3)
            setTextColor(Hud.PAPER)
            typeface = ui.body
            textSize = 16f
            maxLines = 5
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            imeOptions = EditorInfo.IME_ACTION_SEND
            background = Hud.Plate(Hud.INK_2, Hud.LINE_STRONG, cut = ui.dp(8f), stroke = ui.dp(1f), offset = 0f)
            setPadding(ui.dpi(12), ui.dpi(10), ui.dpi(12), ui.dpi(10))
            setOnEditorActionListener { _, id, _ -> if (id == EditorInfo.IME_ACTION_SEND) { send(); true } else false }
        }
        input.addView(field, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        sendBtn = ui.text("➤", 20f, Hud.INK, ui.head).apply {
            gravity = Gravity.CENTER
            background = Hud.Plate(Hud.YELLOW, Hud.MANE, Hud.MANE, cut = ui.dp(8f), stroke = 0f, offset = ui.dp(3f))
            contentDescription = a.getString(R.string.chat_send)
            isClickable = true
            setOnClickListener { send() }
        }
        input.addView(sendBtn, LinearLayout.LayoutParams(ui.dpi(48), ui.dpi(46)).apply { leftMargin = ui.dpi(8) })
        root.addView(input)
        return root
    }

    /** Room under the line to write in: the pill bar, or the keyboard (then the bar is hidden). */
    fun setBottom(px: Int) = input.setPadding(ui.dpi(12), ui.dpi(8), ui.dpi(12), px)

    fun onShow() {
        if (waiting == null) load()
        AppChat.listener = { answered(it) }
        status()
    }

    fun status() {
        val on = CapriService.running
        statusLine.text = a.getString(if (waiting != null) R.string.chat_typing else if (on) R.string.chat_online else R.string.chat_asleep).uppercase()
        statusLine.setTextColor(if (on || waiting != null) Hud.GREEN else Hud.PAPER_3)
    }

    /** Your history with her, the last of it (bubble chats over your screen too). */
    private fun load() {
        list.removeAllViews()
        val log = ChatLog.all(a)
        if (log.length() == 0) bubble(a.getString(R.string.chat_hello, CapriSettings.name(a).ifBlank { a.getString(R.string.chat_you) }).replace(", .", "."), false, "grin")
        var lastDay = ""
        val day = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM)
        for (i in maxOf(0, log.length() - 80) until log.length()) {
            val e = log.optJSONObject(i) ?: continue
            val d = day.format(java.util.Date(e.optLong("at")))
            if (d != lastDay) { lastDay = d; dayLine(d) }
            val q = e.optString("q").trim()
            if (q.isNotEmpty()) bubble(q, true, null)
            bubble(e.optString("a"), false, null)
        }
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun dayLine(d: String) {
        list.addView(ui.text("// ${d.uppercase()}", 10f, Hud.PAPER_3, ui.mono).apply {
            gravity = Gravity.CENTER
            letterSpacing = 0.12f
        }, ui.block(10).apply { bottomMargin = ui.dpi(2) })
    }

    private fun bubble(text: String, mine: Boolean, face: String?): TextView {
        val row = ui.row().apply { gravity = if (mine) Gravity.END else Gravity.START or Gravity.BOTTOM }
        if (!mine) row.addView(ImageView(a).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            (face?.let { ui.face(it) } ?: ui.face("smug"))?.let { setImageBitmap(it) }
        }, LinearLayout.LayoutParams(ui.dpi(30), ui.dpi(30)).apply { rightMargin = ui.dpi(6) })
        val t = ui.text(text, 15f, if (mine) Hud.INK else Hud.PAPER, ui.body).apply {
            setLineSpacing(0f, 1.15f)
            setTextIsSelectable(true)
            background = if (mine) Hud.Plate(Hud.YELLOW, Hud.YELLOW, cut = ui.dp(8f), stroke = 0f, offset = 0f)
            else Hud.Plate(Hud.INK_2, Hud.MANE, cut = ui.dp(8f), stroke = ui.dp(1f), offset = 0f)
            setPadding(ui.dpi(12), ui.dpi(9), ui.dpi(12), ui.dpi(10))
            maxWidth = (a.resources.displayMetrics.widthPixels * 0.74).toInt()
        }
        row.addView(t, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        list.addView(row, ui.block(8))
        return t
    }

    private fun send() {
        val text = field.text.toString().trim()
        if (text.isEmpty() || waiting != null) return
        if (!Settings.canDrawOverlays(a)) {
            Toast.makeText(a, R.string.ob_need_overlay, Toast.LENGTH_LONG).show()
            return
        }
        field.setText("")
        bubble(text, true, null)
        val wasOn = CapriService.running
        waiting = AppChat.ask(a, text)
        typingBubble = bubble("•", false, "thinking").also { it.post(dots) }
        if (!wasOn) bubble(a.getString(R.string.chat_waking), false, "wide").alpha = 0.7f
        status()
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
        list.removeCallbacks(giveUp)
        list.postDelayed(giveUp, 150_000)
    }

    private fun answered(r: AppChat.Reply) {
        if (r.id != waiting) return
        waiting = null
        list.removeCallbacks(giveUp)
        typingBubble?.let { tb ->
            tb.removeCallbacks(dots)
            tb.text = r.text
            ((tb.parent as? LinearLayout)?.getChildAt(0) as? ImageView)?.let { img -> ui.face(r.face)?.let { img.setImageBitmap(it) } }
            ui.glitchIn(tb)
        }
        typingBubble = null
        ui.face(r.face)?.let { headFace.setImageBitmap(it) }
        status()
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }
}
