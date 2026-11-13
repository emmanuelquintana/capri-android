package io.github.emmanuelquintana.capri

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * The app's screen, dressed as her HUD, with a pill bar at the bottom: What's new (her status,
 * Start, updates and what each version brought), Permissions (what each is for) and Settings
 * (what she calls you, her AI, the log). The first time, her welcome comes first.
 */
class MainActivity : Activity() {
    override fun attachBaseContext(newBase: android.content.Context) = super.attachBaseContext(Lang.wrap(newBase))

    private lateinit var ui: Ui
    private lateinit var content: FrameLayout
    private val tabs = mutableListOf<Tab>()
    private var current = 0

    // What's new
    private lateinit var statusDot: View
    private lateinit var statusWord: TextView
    private lateinit var statusText: TextView
    private lateinit var startBtn: TextView
    private lateinit var updateText: TextView
    private lateinit var updateBtn: TextView
    // Emma's beta channel (Beta.kt): only on a phone where her code was sent in the chat, hidden everywhere else
    private lateinit var betaBox: LinearLayout
    private lateinit var betaText: TextView
    private lateinit var betaBtn: TextView
    private var release: Updater.Release? = null
    private var downloading = false

    // Permissions
    private val permRows = mutableListOf<() -> Unit>()

    // Settings
    private lateinit var nameField: android.widget.EditText
    private lateinit var aiText: TextView
    private lateinit var memoryBox: LinearLayout
    private var shownFacts: List<String>? = null
    private lateinit var voiceText: TextView
    private lateinit var voiceBtn: TextView
    private lateinit var voiceState: TextView
    private lateinit var passiveState: TextView
    private lateinit var passiveBtn: TextView
    private lateinit var lookBtn: TextView
    private val choices = mutableListOf<() -> Unit>()

    /** A row of options, the chosen one in her green; a tap picks it (and saves it). */
    private fun <T> choice(options: List<Pair<Int, T>>, current: () -> T, same: (T, T) -> Boolean, pick: (T) -> Unit): LinearLayout =
        choiceText(options.map { (label, value) -> getString(label) to value }, current, same, pick)

    /** The same, with its labels as they are (a language's own name). */
    private fun <T> choiceText(options: List<Pair<String, T>>, current: () -> T, same: (T, T) -> Boolean, pick: (T) -> Unit): LinearLayout {
        val row = ui.row()
        val buttons = options.map { (label, value) ->
            ui.text(label.uppercase(), 11f, Hud.PAPER, ui.mono).apply {
                gravity = Gravity.CENTER
                letterSpacing = 0.04f
                isClickable = true
                setPadding(ui.dpi(4), ui.dpi(10), ui.dpi(4), ui.dpi(10))
                setOnClickListener {
                    pick(value)
                    postDelayed({ choices.forEach { it() } }, 300)
                }
            } to value
        }
        for ((b, _) in buttons) row.addView(b, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { rightMargin = ui.dpi(4) })
        val redraw = {
            val now = current()
            for ((b, v) in buttons) {
                val on = same(now, v)
                b.setTextColor(if (on) Hud.INK else Hud.PAPER_2)
                b.background = if (on) Hud.Plate(Hud.GREEN, 0, cut = ui.dp(6f), stroke = 0f, offset = 0f)
                else Hud.Plate(Hud.INK, Hud.LINE_STRONG, cut = ui.dp(6f), stroke = ui.dp(1f), offset = 0f)
            }
        }
        redraw()
        choices.add(redraw)
        return row
    }
    private val ticker = android.os.Handler(android.os.Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            showVoice()
            ticker.postDelayed(this, 1500)
        }
    }

    private lateinit var chat: ChatTab

    private class Tab(val icon: Ui.Icon, val label: TextView, val button: LinearLayout, val page: View)

    // (1.11.0) the two screens of Ajustes, over the tabs, with their way back
    private lateinit var personal: View
    private lateinit var voice: View
    private var sub: View? = null

    // (1.11.0) Ajustes is a menu: each group a screen ("sub" names it, for the emulator's screenshots too)
    private class Group(val key: String, val icon: String, val color: Int, val title: Int, val how: Int, val page: View)
    private val groups = mutableListOf<Group>()
    private val entries = mutableMapOf<String, View>()

    // (1.11.0) the guided tour of what's new (Tour.kt + assets/android/tour.json): its frame, and the pill bar
    private lateinit var shell: FrameLayout
    private lateinit var pill: View

    // (1.12.0) a screen inside a screen of Ajustes (Ajustes de voz › Voz en la nube…): its way back is to that screen
    private val parentOf = HashMap<View, View>()
    private val spotPage = HashMap<String, View>() // (the voice tutorial: which screen holds each spot)
    private fun child(page: View, parent: View) {
        parentOf[page] = parent
        content.addView(page.apply { visibility = View.GONE }, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
    }

    private fun openSub(page: View, animate: Boolean = true) {
        val parent = parentOf[page]
        if (parent != null) {
            if (sub !== parent) openSub(parent, animate = false)
            sub?.visibility = View.GONE
        } else {
            if (current != TAB_SETTINGS || sub != null) show(TAB_SETTINGS, animate = false)
            tabs[current].page.visibility = View.GONE
        }
        hideKeyboard()
        sub = page
        (page as? ScrollView)?.scrollTo(0, 0)
        page.visibility = View.VISIBLE
        if (animate) ui.slideIn(page)
        refresh()
    }

    private fun closeSub(animate: Boolean = true): Boolean {
        val open = sub ?: return false
        hideKeyboard()
        open.visibility = View.GONE
        parentOf[open]?.let { up ->
            sub = up
            up.visibility = View.VISIBLE
            if (animate) ui.slideIn(up)
            refresh()
            return true
        }
        sub = null
        tabs.getOrNull(current)?.page?.let { it.visibility = View.VISIBLE; if (animate) ui.slideIn(it) }
        return true
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        tour?.let { it.skip(); return }
        if (closeSub()) return
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }

    /** "‹ Ajustes": back from one of its screens. */
    private fun backRow() = ui.text("‹ " + getString(R.string.tab_settings).uppercase(), 13f, Hud.YELLOW, ui.mono).apply {
        letterSpacing = 0.1f
        isClickable = true
        setPadding(0, ui.dpi(4), ui.dpi(16), ui.dpi(14))
        setOnClickListener { closeSub() }
    }

    /** A row that opens a screen: its icon, its name and what's in it, and a chevron. */
    private fun entry(icon: String, color: Int, title: Int, how: Int, open: () -> Unit) = ui.card(color).apply {
        val row = ui.row().apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(View(this@MainActivity).apply { background = Ui.Icon(icon, color) }, LinearLayout.LayoutParams(ui.dpi(26), ui.dpi(26)).apply { rightMargin = ui.dpi(14) })
        val texts = ui.col()
        texts.addView(ui.text(getString(title), 16f, Hud.PAPER, ui.head))
        texts.addView(ui.text(getString(how), 12f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(3))
        row.addView(texts, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        row.addView(ui.text("›", 22f, color, ui.head).apply { setPadding(ui.dpi(10), 0, 0, 0) })
        addView(row)
        isClickable = true
        setOnClickListener { open() }
    }

    /** The language this screen was made in (Lang.get), to notice it changed. */
    private var madeIn: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        madeIn = Lang.get(this)
        Diag.init(this)
        if (OnboardingActivity.needed(this)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }
        UpdateJob.schedule(this)
        Agenda.plan(this)
        wantsUpdate = intent?.getBooleanExtra(EXTRA_UPDATE, false) == true
        window.statusBarColor = Hud.INK
        window.navigationBarColor = Hud.INK
        ui = Ui(this)

        val root = FrameLayout(this).apply { background = Hud.Backdrop(ui.dp(3f)) }
        content = FrameLayout(this)
        root.addView(content, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        // 1.9.0 (Emma: "necesitamos organizar mejor la UI"): you, her looks and the general settings
        // each in its own tab; the permissions moved into Ajustes.
        // 1.11.0 (Emma: "que el centro de la app sea un chat con ella… el menú de juegos debe tener su parte
        // en el menú de píldora"): her chat first, where the app opens; the games get a tab; Comunidad
        // went into Novedades (Discord, TikTok, Donar) and Comandos behind the «?» of her chat.
        // (1.11.0, Emma: "el menú de píldora está llegando a un nivel sobrecargado, debe haber novedades,
        // comunidad y personalizar Capri, y otro para comandos de voz, otro menú para ajustes"): Comunidad and
        // Comandos are tabs again; Tú became Ajustes › Ajustes personales, her voice Ajustes › Ajustes de voz.
        chat = ChatTab(this, ui) { showCmds() }
        personal = youPage()
        voice = voicePage()
        for (k in voiceKids) child(k, voice)
        for (k in personalKids) child(k, personal)
        groups += Group("personal", "person", Hud.YELLOW, R.string.settings_personal, R.string.settings_personal_how, personal)
        groups += Group("voice", "mic", Hud.MANE, R.string.settings_voice_screen, R.string.settings_voice_screen_how, voice)
        groups += Group("perms", "shield", Hud.GREEN, R.string.g_perms, R.string.g_perms_how, permsPage())
        groups += Group("ai", "globe", Hud.CYAN, R.string.g_ai, R.string.g_ai_how, aiLangPage())
        groups += Group("screen", "eye", Hud.GREEN, R.string.g_screen, R.string.g_screen_how, screenPage())
        groups += Group("modes", "pad", Hud.GREEN, R.string.g_modes_only, R.string.g_modes_only_how, modesPage())
        groups += Group("phone", "phone", Hud.YELLOW, R.string.g_phone, R.string.g_phone_how, phonePage())
        groups += Group("report", "warn", Hud.RED, R.string.g_report, R.string.g_report_how, reportPage())
        val pages = listOf(chat.build(), newsPage(), communityPage(), gamesPage(), funcsPage(), stylePage(), settingsPage())
        val bar = ui.row().apply {
            background = Ui.Pill(Hud.INK, Hud.MANE, ui.dp(1.5f))
            setPadding(ui.dpi(6), ui.dpi(6), ui.dpi(6), ui.dpi(6))
            elevation = ui.dp(6f)
        }
        // (1.12.0, Emma: "elimina lo de comunidad y deja esa pantalla solo a los comandos, cambia el icono"; "modo sueño,
        // tus clases y te cuida deben ser funciones… pon una pantalla nueva en el menú de píldora")
        val labels = listOf(R.string.tab_chat to "chat", R.string.tab_news to "spark", R.string.tab_cmds to "terminal", R.string.tab_games to "pad",
            R.string.tab_funcs to "moon", R.string.tab_style to "hanger", R.string.tab_settings to "sliders")
        for ((i, pair) in labels.withIndex()) {
            val (label, kind) = pair
            val icon = Ui.Icon(kind, Hud.PAPER_2)
            val btn = ui.row().apply {
                gravity = Gravity.CENTER
                setPadding(ui.dpi(9), ui.dpi(10), ui.dpi(9), ui.dpi(10)) // (seven tabs: they fit a 360 dp phone)
                isClickable = true
                contentDescription = getString(label)
                setOnClickListener { show(i) }
            }
            btn.addView(View(this).apply { background = icon }, LinearLayout.LayoutParams(ui.dpi(20), ui.dpi(20)))
            val text = ui.text(getString(label), 13f, Hud.PAPER_2, ui.head).apply { setPadding(ui.dpi(8), 0, 0, 0) }
            btn.addView(text)
            bar.addView(btn, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
            content.addView(pages[i], FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            tabs.add(Tab(icon, text, btn, pages[i]))
        }
        for (sub in groups.map { it.page }) content.addView(sub.apply { visibility = View.GONE }, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        val barParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
            bottomMargin = ui.dpi(16)
        }
        root.addView(bar, barParams)
        shell = root
        pill = bar
        // Android 15 draws the app under the status and navigation bars: her pages start below the
        // first and the pill bar sits above the second
        // (1.11.3, Emma: "la pantalla para elegir tapa la de los mensajes") her chat's line to write in always sits
        // above the pill bar, measured from the bar itself (a bigger font makes it taller). Before Android 15 the
        // system keeps the window off its bars and the insets below may never arrive here: then the keyboard is
        // found by how much of the window it covers, and nothing waits for insets to set the room under the chat.
        var insetsSeen = false
        var typingNow = false
        val roomUnderChat = {
            if (!typingNow) chat.setBottom(maxOf(ui.dpi(92), bar.height + barParams.bottomMargin + ui.dpi(12)))
        }
        bar.addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom -> if (bottom - top != oldBottom - oldTop) roomUnderChat() }
        roomUnderChat()
        root.viewTreeObserver.addOnGlobalLayoutListener {
            if (insetsSeen) return@addOnGlobalLayoutListener
            val r = android.graphics.Rect()
            root.getWindowVisibleDisplayFrame(r)
            val typing = root.rootView.height - r.bottom > ui.dpi(160)
            if (typing == typingNow) return@addOnGlobalLayoutListener
            typingNow = typing
            bar.visibility = if (typing) View.GONE else View.VISIBLE
            if (typing) chat.setBottom(ui.dpi(8)) else roomUnderChat()
        }
        ui.underSystemBars(root) { top, bottom ->
            content.setPadding(0, top, 0, 0)
            // (1.11.0) the keyboard up (writing to her): the pill bar steps aside, her chat sits right on it
            val typing = bottom > ui.dpi(160)
            if (top > 0 || bottom > 0) insetsSeen = true
            typingNow = typing
            bar.visibility = if (typing) View.GONE else View.VISIBLE
            barParams.bottomMargin = ui.dpi(16) + bottom
            bar.layoutParams = barParams
            content.setPadding(0, top, 0, if (typing) bottom else 0)
            if (typing) chat.setBottom(ui.dpi(8)) else roomUnderChat()
        }
        setContentView(root)
        show(savedInstanceState?.getInt("tab")?.takeIf { it in 0 until TABS } ?: tabAsked(intent) ?: TAB_NEWS, animate = false) // (1.12.0, Emma: "que esa pestaña de novedades sea la principal")
        if (savedInstanceState == null && tabAsked(intent) == null) root.postDelayed({ if (!isFinishing && !tourOnce() && !offerVoiceOnce()) inviteOnce() }, 1500)
        // a screen of Ajustes to open on ("sub": a Group key): the emulator's screenshots ask for one
        intent?.getStringExtra(EXTRA_SUB)?.let { k -> groups.firstOrNull { it.key == k }?.let { openSub(it.page, animate = false) } }
    }

    // a tab to open on ("tab", 0..4): the emulator's screenshots ask for one
    private fun tabAsked(intent: Intent?) = intent?.getIntExtra(EXTRA_TAB, -1)?.takeIf { it in 0 until TABS }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("tab", current)
    }

    /** One tab on: its icon and label in ink on a yellow capsule, the others quiet. */
    private fun show(i: Int, animate: Boolean = true) {
        while (closeSub(animate = false)) Unit
        current = i
        hideKeyboard()
        for ((j, t) in tabs.withIndex()) {
            val on = i == j
            t.button.background = if (on) Ui.Pill(Hud.YELLOW, 0, 0f) else null
            t.icon.color = if (on) Hud.INK else Hud.PAPER_2
            t.icon.invalidateSelf()
            t.label.setTextColor(if (on) Hud.INK else Hud.PAPER_2)
            // only the chosen one shows its name: the pill stays short on a narrow phone
            t.label.visibility = if (on) View.VISIBLE else View.GONE
            t.page.visibility = if (on) View.VISIBLE else View.GONE
        }
        // 1.9.0 ("cuando cambio entre pestañas se traba un poco"): a short smooth slide on the GPU, and
        // the page brought up to date once it's in, not in the middle of it
        if (i == TAB_SETTINGS) (tabs[i].page as? ScrollView)?.scrollTo(0, 0)
        if (i == TAB_CHAT) chat.onShow()
        if (animate) {
            ui.slideIn(tabs[i].page)
            content.removeCallbacks(refreshLater)
            content.postDelayed(refreshLater, 200)
        } else refresh()
    }
    private val refreshLater = Runnable { if (!isFinishing) refresh() }

    override fun onResume() {
        super.onResume()
        // (1.12.2) the language changed while you were away (her own menu): the app in it now
        if (madeIn != null && Lang.get(this) != madeIn) { recreate(); return }
        if (!::ui.isInitialized) return
        // back in the app: her service can take the microphone again if her ears are on (Android
        // only lets it while the app is in front)
        if (Ears.enabled(this)) CapriService.current?.voiceChanged()
        refresh()
        checkUpdate()
        Stats.maybePing(this)
        ticker.post(tick)
        cloudNewer()
    }

    override fun onPause() {
        super.onPause()
        ticker.removeCallbacks(tick)
        // (1.12.0) your classes, games, sleep… saved to your account too (only what changed goes up, a little later)
        if (Account.signedIn(this)) Account.changed(this)
    }

    // (1.12.0, community: "sincronizar sus conversaciones en diferentes dispositivos") your other phone saved
    // something newer to your account: offered here when you open the app (your chats here stay, joined with those)
    private var cloudCheckedAt = 0L
    private fun cloudNewer() {
        if (!Account.signedIn(this) || System.currentTimeMillis() - cloudCheckedAt < 20 * 60_000L) return
        cloudCheckedAt = System.currentTimeMillis()
        Threads.make {
            val copy = runCatching { Account.download(this) }.getOrNull() ?: return@make
            val here = android.os.Build.MODEL.take(40)
            if (copy.first <= Account.lastSaved(this) + 60_000L || copy.second == here) return@make
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                val day = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT).format(java.util.Date(copy.first))
                android.app.AlertDialog.Builder(this)
                    .setMessage(getString(R.string.account_newer, copy.second.ifEmpty { "?" }, day))
                    .setPositiveButton(R.string.account_found_yes) { _, _ -> restoreCopy(copy.third) }
                    .setNegativeButton(R.string.account_found_no) { _, _ -> inBackground({ Account.upload(this) }) { say(R.string.account_ok) } }
                    .show()
            }
        }.start()
    }

    override fun onDestroy() {
        AppChat.listener = null
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        // the microphone just allowed from "Turn on": her ears go on with it
        if (requestCode == Permissions.MIC && wantVoice && Ears.allowed(this)) Ears.setEnabled(this, true)
        wantVoice = false
        // your calendar just allowed from «Tú»: her reminders from it go on with it
        if (requestCode == Permissions.CALENDAR) {
            Agenda.setCalendar(this, Agenda.calendarAllowed(this))
            if (!Agenda.calendarAllowed(this)) Toast.makeText(this, R.string.you_calendar_denied, Toast.LENGTH_LONG).show()
        }
        CapriService.current?.voiceChanged()
        refresh()
    }

    private var wantVoice = false
    private var wantsUpdate = false // opened from the "new version" notification: straight into it

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_UPDATE, false)) {
            wantsUpdate = true
            if (::ui.isInitialized) show(TAB_NEWS)
        } else tabAsked(intent)?.let { if (::ui.isInitialized) show(it) }
        if (::ui.isInitialized) intent.getStringExtra(EXTRA_SUB)?.let { k -> groups.firstOrNull { it.key == k }?.let { openSub(it.page) } }
    }

    private fun toggleVoice() {
        if (Ears.enabled(this)) return Ears.setEnabled(this, false).also { refresh() }
        if (!android.speech.SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, if (Vendor.huawei) R.string.settings_voice_none_huawei else R.string.settings_voice_none, Toast.LENGTH_LONG).show()
            return
        }
        if (!Ears.allowed(this)) {
            wantVoice = true
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), Permissions.MIC)
            return
        }
        Ears.setEnabled(this, true)
        refresh()
    }

    private fun page(build: LinearLayout.() -> Unit): View {
        val col = ui.col().apply {
            setPadding(ui.dpi(20), ui.dpi(24), ui.dpi(20), ui.dpi(110)) // room for the pill bar
            build()
        }
        return ScrollView(this).apply { isFillViewport = true; addView(col) }
    }

    // ---------- What's new ----------

    private fun newsPage() = page {
        // header: her face in a cut frame, the project line in cyan, her name in yellow
        val header = ui.row()
        val face = FrameLayout(this@MainActivity).apply {
            background = Hud.Plate(Hud.INK_2, Hud.YELLOW, Hud.MANE, cut = ui.dp(12f), stroke = ui.dp(2f), offset = ui.dp(4f))
            setPadding(ui.dpi(4), ui.dpi(4), ui.dpi(8), ui.dpi(8))
            addView(ImageView(this@MainActivity).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                ui.face("grin")?.let { setImageBitmap(it) }
            }, FrameLayout.LayoutParams(ui.dpi(72), ui.dpi(72)))
        }
        header.addView(face)
        header.addView(ui.col().apply {
            setPadding(ui.dpi(16), 0, 0, 0)
            addView(ui.text("/// PROJECT CAPRICORN", 11f, Hud.MANE, ui.mono).apply { letterSpacing = 0.12f })
            addView(ui.text("CAPRI", 44f, Hud.YELLOW, ui.head).apply { letterSpacing = 0.06f; includeFontPadding = false })
            addView(ui.text("ANDROID // v${Beta.shown(this@MainActivity)}", 10f, Hud.PAPER_3, ui.mono).apply { letterSpacing = 0.1f })
        }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        addView(header)
        // a WebView too old for her page (1.9.0): she can't show up at all until it's updated
        if (Perf.webViewTooOld()) addView(ui.card(Hud.RED).apply {
            addView(ui.label(getString(R.string.webview_old_title), Hud.RED))
            addView(ui.text(getString(R.string.webview_old_how, Perf.webView()), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(6))
            addView(ui.button(getString(R.string.webview_update) + " ↗", Ui.Kind.MAIN) {
                val pkg = Perf.webViewPackage()
                Vendor.store(this@MainActivity, pkg) // (1.11.0: AppGallery on a Huawei without Play)
            }, ui.block(10))
        }, ui.block(16))

        // status and Start
        val status = ui.card()
        val statusRow = ui.row()
        statusDot = View(this@MainActivity)
        statusRow.addView(statusDot, LinearLayout.LayoutParams(ui.dpi(10), ui.dpi(10)).apply { rightMargin = ui.dpi(10) })
        statusWord = ui.text("", 13f, Hud.PAPER, ui.mono).apply { letterSpacing = 0.14f }
        statusRow.addView(statusWord)
        status.addView(statusRow)
        statusText = ui.text("", 14f, Hud.PAPER_2, ui.body)
        status.addView(statusText, ui.block(6))
        addView(status, ui.block(20))
        startBtn = ui.button("", Ui.Kind.MAIN) {
            if (!Settings.canDrawOverlays(this@MainActivity)) {
                Toast.makeText(this@MainActivity, R.string.ob_need_overlay, Toast.LENGTH_LONG).show()
                show(TAB_SETTINGS) // (its permissions, at the top)
                return@button
            }
            if (CapriService.running) CapriService.send(this@MainActivity, CapriService.ACTION_QUIT) else CapriService.start(this@MainActivity)
            startBtn.postDelayed({ refresh() }, 700)
        }
        addView(startBtn, ui.block(14))
        // her Discord, right under her button (1.9.0, the community asked); TikTok with it (1.11.0: the
        // Comunidad tab went here) and DONAR (1.11.0, Emma: "debajo del botón de iniciar Capri y Discord que
        // diga DONAR y abra el popup con el link de donar")
        addView(ui.button(getString(R.string.community_discord) + " ↗", Ui.Kind.LINE) { openLink(DISCORD) }, ui.block(10))
        addView(ui.button("♥ " + getString(R.string.main_donate), Ui.Kind.GREEN) { showSupport(asked = true) }, ui.block(8))
        // (1.12.0, Emma: "añade lo de seguir al tiktok al inicio abajo de donar")
        addView(ui.button(getString(R.string.community_tiktok) + " ↗", Ui.Kind.LINE) { openLink(TIKTOK) }, ui.block(8))
        // (1.12.0, Emma: "añade también mi Twitter")
        addView(ui.button(getString(R.string.community_x) + " ↗", Ui.Kind.LINE) { openLink(X) }, ui.block(8))

        // updates
        updateText = ui.text("", 13f, Hud.PAPER_2, ui.mono).apply { letterSpacing = 0.02f; setLineSpacing(0f, 1.15f) }
        updateBtn = ui.button(getString(R.string.update_go), Ui.Kind.GREEN) { update() }.apply { visibility = View.GONE }
        // (la versión de Play se actualiza desde la tienda: sin esta placa)
        if (Updater.ENABLED) addView(ui.card().apply {
            addView(ui.label(getString(R.string.update_head)))
            addView(updateText, ui.block(6))
            addView(updateBtn, ui.block(10))
            betaText = ui.text("", 12f, Hud.MANE, ui.mono).apply { letterSpacing = 0.04f; setLineSpacing(0f, 1.15f) }
            betaBtn = ui.button("", Ui.Kind.LINE) {
                Beta.setOn(this@MainActivity, !Beta.on(this@MainActivity))
                release = null
                checkUpdate()
            }
            betaBox = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                visibility = View.GONE
                addView(betaText)
                addView(betaBtn, ui.block(8))
                // (1.12.0, Emma: "en las actualizaciones beta … tenga acceso al registro del teléfono para enviártelo
                // y detectar fallos") only where the beta code was sent
                addView(ui.button("Guardar registro del teléfono", Ui.Kind.GREEN) { saveLog() }, ui.block(8))
                addView(ui.button("Enviar registro a Discord", Ui.Kind.LINE) { sendLog() }, ui.block(6))
                addView(ui.button("Olvidar código en este teléfono", Ui.Kind.LINE) {
                    Beta.forget(this@MainActivity)
                    release = null
                    checkUpdate()
                }, ui.block(6))
            }
            addView(betaBox, ui.block(14))
        }, ui.block(22))

        // what each version brought (CHANGELOG.md)
        // (1.11.0) the guided tour of what's new, again whenever you want (tour.json)
        addView(ui.button(getString(R.string.tour_again), Ui.Kind.LINE) { tourOnce(force = true) }, ui.block(16))
        addView(ui.button(getString(R.string.vt_open), Ui.Kind.LINE) { voiceTutorial() }, ui.block(8))
        addView(ui.label(getString(R.string.news_head)), ui.block(26))
        // folded: just the version and how many changes; a tap opens it (and folds it again)
        for ((i, v) in changelog().take(8).withIndex()) {
            val card = ui.card(if (i == 0) Hud.YELLOW else Hud.LINE_STRONG, if (i == 0) Hud.MANE else 0)
            val head = ui.row().apply { gravity = Gravity.CENTER_VERTICAL }
            head.addView(ui.text("v" + v.first, 13f, if (i == 0) Hud.YELLOW else Hud.PAPER_2, ui.mono).apply { letterSpacing = 0.08f })
            // (1.12.0, Emma: "sepáralas por puntos, como listas de cambios y bugs") a line that's only a bold title heads a list
            val heading = { item: String -> Regex("^\\*\\*[^*]+\\*\\*$").matches(item.trim()) }
            val count = v.second.count { !heading(it) }
            head.addView(ui.text(resources.getQuantityString(R.plurals.news_changes, count, count), 12f, Hud.PAPER_3, ui.body).apply {
                setPadding(ui.dpi(10), 0, 0, 0)
            }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            val chevron = ui.text("▾", 16f, if (i == 0) Hud.MANE else Hud.PAPER_3, ui.mono)
            head.addView(chevron)
            card.addView(head)
            val items = ui.col().apply { visibility = View.GONE }
            for (item in v.second) items.addView(
                if (heading(item)) ui.label(item.trim().removePrefix("**").removeSuffix("**")) else bullet(item, i == 0),
                ui.block(if (heading(item)) 16 else 8))
            card.addView(items)
            card.isClickable = true
            card.setOnClickListener {
                val open = items.visibility != View.VISIBLE
                items.visibility = if (open) View.VISIBLE else View.GONE
                chevron.text = if (open) "▴" else "▾"
                if (open) ui.glitchIn(items)
            }
            addView(card, ui.block(10))
        }

        // (1.8.2, Emma: no credit line here) the license and its Required Notice stay a tap away, as on the web
        addView(ui.text(getString(R.string.main_license_link), 11f, Hud.PAPER_3, ui.mono).apply {
            letterSpacing = 0.08f
            isClickable = true
            setPadding(0, ui.dpi(6), 0, ui.dpi(6))
            setOnClickListener { showLicense() }
        }, ui.block(24))
    }

    /** The license (assets/capri/LICENSE, with its Required Notice), in full. */
    private fun showLicense() {
        val text = runCatching { assets.open("capri/LICENSE").bufferedReader().use { it.readText() } }.getOrDefault("")
        val body = ui.text(text, 11f, Hud.PAPER_2, ui.mono).apply {
            setPadding(ui.dpi(20), ui.dpi(16), ui.dpi(20), ui.dpi(16))
            setTextIsSelectable(true)
        }
        android.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(getString(R.string.main_license_link))
            .setView(ScrollView(this).apply { setBackgroundColor(Hud.INK); addView(body) })
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    /** "- **Title:** the rest" → a spark, the title in her display face, the rest as body text. */
    private fun bullet(item: String, fresh: Boolean): TextView {
        val clean = item.replace("`", "")
        val m = Regex("^\\*\\*(.+?)\\*\\*\\s*(.*)$", RegexOption.DOT_MATCHES_ALL).find(clean)
        val plain = { t: String -> t.replace("*", "") } // *italics* and any ** left: just the words
        val s = SpannableStringBuilder("✦ ")
        s.setSpan(ForegroundColorSpan(if (fresh) Hud.MANE else Hud.PAPER_3), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        if (m != null) {
            val start = s.length
            s.append(plain(m.groupValues[1]))
            s.setSpan(FaceSpan(ui.head), start, s.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            s.setSpan(ForegroundColorSpan(Hud.PAPER), start, s.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            s.append(" ").append(plain(m.groupValues[2]))
        } else s.append(plain(clean))
        return ui.text(s, 14f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.18f) }
    }

    /**
     * CHANGELOG.md, from the APK: [(version, [items])], newest first. In the app's language when
     * changelog/<lang>.md has that version (Spanish, the original, otherwise).
     */
    private fun changelog(): List<Pair<String, List<String>>> {
        val spanish = changelog("CHANGELOG.md")
        val lang = Lang.current(this)
        if (lang == "es") return spanish
        val mine = changelog("changelog/$lang.md").toMap()
        // (1.11.8) a language added later has only its newest notes: the older ones in English, not Spanish
        val english = if (lang == "en") emptyMap() else changelog("changelog/en.md").toMap()
        return spanish.map { (v, items) -> v to (mine[v]?.takeIf { it.isNotEmpty() } ?: english[v]?.takeIf { it.isNotEmpty() } ?: items) }
    }

    private fun changelog(file: String): List<Pair<String, List<String>>> = runCatching {
        val out = mutableListOf<Pair<String, MutableList<String>>>()
        assets.open(file).bufferedReader().useLines { lines ->
            for (raw in lines) {
                val line = raw.trimEnd()
                when {
                    line.startsWith("## ") -> out.add(line.removePrefix("## ").trim() to mutableListOf())
                    out.isEmpty() -> Unit
                    line.startsWith("- ") -> out.last().second.add(line.removePrefix("- "))
                    line.isNotBlank() && out.last().second.isNotEmpty() -> {
                        val items = out.last().second
                        items[items.size - 1] = items.last() + " " + line.trim()
                    }
                }
            }
        }
        out.map { it.first to it.second.toList() }
    }.getOrDefault(emptyList())

    // ---------- Commands: what she can do, so you know what to ask ----------

    // (1.12.0, Emma: "unifica el menú de comunidad y de comandos de voz, siento muy cargado el menú de píldora"):
    // her voice commands live in the Comunidad tab now, under the community card (the «?» of her chat jumps there)
    private var cmdsTop: View? = null
    private fun cmdsInto(box: LinearLayout) = box.run {
        addView(ui.label(getString(R.string.tab_cmds)).also { cmdsTop = it }, ui.block(26))
        addView(ui.text(getString(R.string.cmds_intro), 15f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.2f) }, ui.block(8))
        // (1.11.0) her ears and her voice are one tap away: Ajustes › Ajustes de voz
        addView(entry("mic", Hud.MANE, R.string.settings_voice_screen, R.string.cmds_voice_how) { openSub(voice) }, ui.block(14))
        val colors = listOf(Hud.MANE, Hud.GREEN, Hud.YELLOW)
        var card: LinearLayout? = null
        var n = 0
        for (item in resources.getStringArray(R.array.cmds)) {
            if (item.startsWith("#")) {
                // (1.12.0, Emma: "que sean colapsables para que se vean mejor organizados") a tap on its title opens it
                val shell = ui.card(colors[n++ % colors.size])
                val body = ui.col().apply { visibility = View.GONE }
                val arrow = ui.text("▸", 16f, Hud.YELLOW, ui.mono)
                shell.addView(ui.row().apply {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(ui.label(item.removePrefix("#")), LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
                    addView(arrow)
                    isClickable = true
                    setOnClickListener {
                        val open = body.visibility != View.VISIBLE
                        body.visibility = if (open) View.VISIBLE else View.GONE
                        arrow.text = if (open) "▾" else "▸"
                    }
                })
                shell.addView(body, ui.block(4))
                card = body
                addView(shell, ui.block(if (n == 1) 18 else 12))
            } else {
                val quoted = item.startsWith("«") || item.startsWith("“")
                card?.addView(ui.text(item, if (quoted) 15f else 14f, if (quoted) Hud.PAPER else Hud.PAPER_2, if (quoted) ui.head else ui.body).apply {
                    setLineSpacing(0f, 1.18f)
                }, ui.block(8))
            }
        }
    }

    // ---------- You (1.9.0): what she calls you, your birthday, what she remembers, how she looks after you ----------

    // (1.12.0, Emma: "ajustemos en pantallas los ajustes… y así aplicar esa misma lógica para todos los ajustes")
    // Ajustes personales: your name here, and each of the rest on its own screen
    private val personalKids = ArrayList<View>()
    private var funcsCare: LinearLayout.() -> Unit = {} // (built with Ajustes personales, shown in Funciones)
    private fun youPage(): View {
        fun kid(build: LinearLayout.() -> Unit) = page {
            addView(ui.text("‹ " + getString(R.string.settings_personal).uppercase(), 13f, Hud.YELLOW, ui.mono).apply {
                letterSpacing = 0.1f
                isClickable = true
                setPadding(0, ui.dpi(4), ui.dpi(16), ui.dpi(14))
                setOnClickListener { closeSub() }
            })
            build()
        }.also { personalKids += it }
        val account = kid { addView(accountCard(), ui.block(4)) }
        val memoryPage = kid {
        // what she remembers about you from her chats (memory.js): on or off, and each thing, to forget
        addView(ui.card(Hud.MANE).apply {
            addView(ui.label(getString(R.string.settings_memory)))
            addView(ui.text(getString(R.string.settings_memory_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(6))
            addView(choice(listOf(R.string.off to false, R.string.on to true),
                { Store(this@MainActivity).configFlag("longMemory", true) }, { a, b -> a == b }) { CapriSettings.set(this@MainActivity, "longMemory", it) }, ui.block(8))
            memoryBox = ui.col()
            addView(memoryBox, ui.block(12))
        }, ui.block(16))

        }
        val neuroPage = kid {
        // (1.11.0) her neurodanzas: braindances she records for you after a lot of affection (Neuro.kt)
        addView(ui.card(Hud.MANE, Hud.CYAN).apply {
            addView(ui.label(getString(R.string.neuro_title), Hud.MANE))
            addView(ui.text(getString(R.string.neuro_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            val count = ui.text("", 14f, Hud.PAPER, ui.body)
            addView(count, ui.block(8))
            addView(ui.button(getString(R.string.neuro_open), Ui.Kind.LINE) { PageActivity.open(this@MainActivity, "neuro") }, ui.block(10))
            neuroDraw = {
                val all = Neuro.all(this@MainActivity).length()
                val fresh = Neuro.unseen(this@MainActivity)
                count.text = when {
                    all == 0 -> getString(R.string.neuro_none)
                    fresh > 0 -> resources.getQuantityString(R.plurals.neuro_new, fresh, fresh)
                    else -> resources.getQuantityString(R.plurals.neuro_count, all, all)
                }
            }
            neuroDraw()
        }, ui.block(16))
        }
        val birthday = kid { addView(birthdayCard(), ui.block(4)) }
        funcsCare = {
        // she looks after you: the battery (charged, low) and a break now and then (phone-care.js)
        addView(ui.card(Hud.YELLOW).apply {
            val store = Store(this@MainActivity)
            addView(ui.label(getString(R.string.settings_care)))
            addView(ui.text(getString(R.string.settings_battery), 14f, Hud.PAPER, ui.head), ui.block(10))
            addView(ui.text(getString(R.string.settings_battery_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            addView(choice(listOf(R.string.off to false, R.string.on to true),
                { store.configFlag("batteryAlerts", true) }, { a, b -> a == b }) { CapriSettings.set(this@MainActivity, "batteryAlerts", it) }, ui.block(6))
            addView(ui.text(getString(R.string.settings_battery_full), 13f, Hud.PAPER_2, ui.mono), ui.block(10))
            addView(choiceText(listOf(80, 85, 90, 95, 100).map { "$it%" to it.toDouble() },
                { store.configNumber("batteryFull", 100.0) }, { a, b -> Math.abs(a - b) < 0.5 }) { CapriSettings.set(this@MainActivity, "batteryFull", it) }, ui.block(6))
            addView(ui.text(getString(R.string.settings_battery_low), 13f, Hud.PAPER_2, ui.mono), ui.block(10))
            addView(choiceText(listOf(5, 10, 15, 20, 30).map { "$it%" to it.toDouble() },
                { store.configNumber("batteryLow", 15.0) }, { a, b -> Math.abs(a - b) < 0.5 }) { CapriSettings.set(this@MainActivity, "batteryLow", it) }, ui.block(6))
            // (1.10.0) battery saver: below your %, what costs the most battery rests (saver.js)
            addView(ui.text(getString(R.string.settings_saver), 14f, Hud.PAPER, ui.head), ui.block(16))
            addView(ui.text(getString(R.string.settings_saver_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            addView(choiceText(listOf(getString(R.string.off) to 0.0, "15%" to 15.0, "20%" to 20.0, "30%" to 30.0),
                { if (!store.configFlag("saver", false)) 0.0 else store.configNumber("saverAt", 20.0) }, { a, b -> Math.abs(a - b) < 0.5 }) {
                CapriSettings.set(this@MainActivity, "saver", it > 0)
                if (it > 0) CapriSettings.set(this@MainActivity, "saverAt", it.toInt())
            }, ui.block(6))
            addView(ui.text(getString(R.string.settings_breaks), 14f, Hud.PAPER, ui.head), ui.block(16))
            addView(ui.text(getString(R.string.settings_breaks_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            addView(choiceText(listOf(getString(R.string.off) to 0.0, "1 h" to 1.0, "2 h" to 2.0, "3 h" to 3.0),
                { if (!store.configFlag("breaks", true)) 0.0 else store.configNumber("breakHours", 2.0) }, { a, b -> Math.abs(a - b) < 0.1 }) {
                CapriSettings.set(this@MainActivity, "breaks", it > 0)
                if (it > 0) CapriSettings.set(this@MainActivity, "breakHours", it)
            }, ui.block(6))
        }, ui.block(16))
        }
        return page {
            addView(backRow())
            addView(ui.label(getString(R.string.settings_personal)))
            addView(ui.text(getString(R.string.you_intro), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            addView(avatarPicker(), ui.block(14))
        // what she calls you
        addView(ui.card(Hud.YELLOW).apply {
            addView(ui.label(getString(R.string.settings_name)))
            nameField = ui.field(getString(R.string.ob_name_hint)).apply {
                setText(CapriSettings.name(this@MainActivity))
                filters = arrayOf(android.text.InputFilter.LengthFilter(24))
                setOnEditorActionListener { _, _, _ -> saveName(); true }
            }
            addView(nameField, ui.block(6))
            addView(ui.button(getString(R.string.settings_save), Ui.Kind.GREEN) { saveName() }, ui.block(12))
        }, ui.block(16))

            addView(entry("person", Hud.CYAN, R.string.account_title, R.string.p_account_how) { openSub(account) }, ui.block(10))
            addView(entry("eye", Hud.MANE, R.string.settings_memory, R.string.p_memory_how) { openSub(memoryPage) }, ui.block(10))
            addView(entry("chat", Hud.GREEN, R.string.you_chatlog_title, R.string.p_chatlog_how) { PageActivity.open(this@MainActivity, "chatlog") }, ui.block(10))
            addView(entry("spark", Hud.MANE, R.string.neuro_title, R.string.p_neuro_how) { openSub(neuroPage) }, ui.block(10))
            addView(entry("spark", Hud.RED, R.string.you_birthday, R.string.p_birthday_how) { openSub(birthday) }, ui.block(10))
        }
    }

    // your birthday: a date (no year: only the day matters to her), or none
    // ---------- Modo sueño (1.11.0) ----------

    private var sleepAlarm = -1 // minutes after midnight for her alarm; -1 none
    private fun sleepCard() = ui.card(Hud.CYAN, Hud.MANE).apply {
        val prefs = getSharedPreferences("sleep", MODE_PRIVATE)
        sleepAlarm = prefs.getInt("lastAlarm", 7 * 60)
        addView(ui.label(getString(R.string.sleep_title), Hud.CYAN))
        addView(ui.text(getString(R.string.sleep_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(6))
        val state = ui.text("", 14f, Hud.PAPER, ui.body).apply { setLineSpacing(0f, 1.15f) }
        addView(state, ui.block(10))
        val alarmBtn = ui.button("", Ui.Kind.LINE) {}
        val goBtn = ui.button(getString(R.string.sleep_go), Ui.Kind.MAIN) {}
        val exactBtn = ui.button(getString(R.string.sleep_exact_btn) + " ›", Ui.Kind.LINE) { Sleep.askExact(this@MainActivity) }
        addView(alarmBtn, ui.block(10))
        addView(goBtn, ui.block(10))
        addView(exactBtn, ui.block(8))
        val time = { m: Int ->
            val cal = java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, m / 60); set(java.util.Calendar.MINUTE, m % 60) }
            android.text.format.DateFormat.getTimeFormat(this@MainActivity).format(cal.time)
        }
        fun draw() {
            val on = Sleep.on(this@MainActivity)
            alarmBtn.visibility = if (on) View.GONE else View.VISIBLE
            exactBtn.visibility = if (!on && sleepAlarm >= 0 && !Sleep.exact(this@MainActivity)) View.VISIBLE else View.GONE
            alarmBtn.text = (if (sleepAlarm >= 0) getString(R.string.sleep_alarm_at, time(sleepAlarm)) else getString(R.string.sleep_alarm_none)).uppercase()
            goBtn.text = getString(if (on) R.string.sleep_woke else R.string.sleep_go).uppercase()
            val nights = Sleep.nights(this@MainActivity)
            state.text = when {
                on -> getString(R.string.sleep_now, android.text.format.DateFormat.getTimeFormat(this@MainActivity).format(java.util.Date(Sleep.startedAt(this@MainActivity))), Sleep.wakes(this@MainActivity).length())
                nights.length() > 0 -> getString(R.string.sleep_last) + "\n" + Sleep.describe(this@MainActivity, nights.getJSONObject(nights.length() - 1))
                else -> ""
            }
            state.visibility = if (state.text.isEmpty()) View.GONE else View.VISIBLE
        }
        alarmBtn.setOnClickListener {
            // a time, or none (Cancel on the clock = no alarm)
            val start = if (sleepAlarm >= 0) sleepAlarm else 7 * 60
            android.app.TimePickerDialog(this@MainActivity, { _, h, m ->
                sleepAlarm = h * 60 + m
                prefs.edit().putInt("lastAlarm", sleepAlarm).apply()
                draw()
            }, start / 60, start % 60, android.text.format.DateFormat.is24HourFormat(this@MainActivity)).apply {
                setButton(android.content.DialogInterface.BUTTON_NEUTRAL, getString(R.string.sleep_alarm_none)) { _, _ ->
                    sleepAlarm = -1
                    prefs.edit().putInt("lastAlarm", -1).apply()
                    draw()
                }
            }.show()
        }
        goBtn.setOnClickListener {
            if (Sleep.on(this@MainActivity)) {
                val night = Sleep.stop(this@MainActivity)
                if (night != null) Toast.makeText(this@MainActivity, Sleep.describe(this@MainActivity, night), Toast.LENGTH_LONG).show()
            } else {
                val at = if (sleepAlarm < 0) 0L else java.util.Calendar.getInstance().run {
                    set(java.util.Calendar.HOUR_OF_DAY, sleepAlarm / 60); set(java.util.Calendar.MINUTE, sleepAlarm % 60)
                    set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis() + 60_000) add(java.util.Calendar.DAY_OF_YEAR, 1)
                    timeInMillis
                }
                // she sleeps on your screen: on, if she wasn't
                if (!CapriService.running && Settings.canDrawOverlays(this@MainActivity)) CapriService.start(this@MainActivity)
                Sleep.start(this@MainActivity, at)
            }
            draw()
        }
        sleepDraw = { draw() }
        draw()
    }
    private var sleepDraw: () -> Unit = {}

    // ---------- (1.12.0) your account (Account.kt) ----------

    private fun accountCard() = ui.card(Hud.CYAN).apply {
        addView(ui.label(getString(R.string.account_title), Hud.CYAN))
        addView(ui.text(getString(R.string.account_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
        val status = ui.text("", 14f, Hud.PAPER, ui.body)
        addView(status, ui.block(8))
        val google = ui.button(getString(R.string.account_google) + " ›", Ui.Kind.GREEN) {
            say(R.string.account_working)
            Account.withGoogle(this@MainActivity) { why -> if (why == null) afterSignIn() else if (why != 0) say(why) }
        }
        val mail = ui.button(getString(R.string.account_email), Ui.Kind.LINE) { emailDialog() }
        val save = ui.button(getString(R.string.account_save), Ui.Kind.GREEN) {
            say(R.string.account_working)
            inBackground({ Account.upload(this@MainActivity) }) { say(R.string.account_ok); accountDraw() }
        }
        val bring = ui.button(getString(R.string.account_bring), Ui.Kind.LINE) { offerCopy(askFirst = true) }
        val out = ui.button(getString(R.string.account_out), Ui.Kind.LINE) {
            android.app.AlertDialog.Builder(this@MainActivity).setMessage(R.string.account_out_sure)
                .setPositiveButton(R.string.account_out) { _, _ -> Account.signOut(this@MainActivity); accountDraw() }
                .setNegativeButton(android.R.string.cancel, null).show()
        }
        listOf(google, mail, save, bring, out).forEach { addView(it, ui.block(8)) }
        accountDraw = {
            val inside = Account.signedIn(this@MainActivity)
            val at = Account.lastSaved(this@MainActivity)
            status.text = if (!inside) getString(R.string.account_none) else getString(R.string.account_as, Account.email(this@MainActivity).ifEmpty { "Google" }) + "\n" +
                (if (at > 0) getString(R.string.account_saved, android.text.format.DateUtils.getRelativeTimeSpanString(at)) else getString(R.string.account_never))
            google.visibility = if (inside) View.GONE else View.VISIBLE
            mail.visibility = google.visibility
            listOf(save, bring, out).forEach { it.visibility = if (inside) View.VISIBLE else View.GONE }
        }
        accountDraw()
        choices.add { accountDraw() }
    }
    private var accountDraw: () -> Unit = {}

    private fun say(what: Int) = Toast.makeText(this, what, Toast.LENGTH_LONG).show()

    /** Runs [work] off the main thread; an Account.Failed shows its reason, anything else "no connection". */
    private fun inBackground(work: () -> Unit, then: () -> Unit) {
        Threads.make {
            val why = try { work(); null } catch (f: Account.Failed) { f.why } catch (_: Exception) { R.string.account_err_other }
            runOnUiThread { if (isFinishing) return@runOnUiThread; if (why == null) then() else say(why); accountDraw() }
        }.start()
    }

    private fun emailDialog() {
        val box = ui.col().apply { setPadding(ui.dpi(20), ui.dpi(8), ui.dpi(20), 0) }
        val email = ui.field(getString(R.string.account_mail_hint)).apply { inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS }
        val pass = ui.field(getString(R.string.account_pass_hint), secret = true)
        box.addView(email, ui.block(4))
        box.addView(pass, ui.block(8))
        val go = { create: Boolean ->
            say(R.string.account_working)
            inBackground({ Account.withEmail(this, email.text.toString(), pass.text.toString(), create) }) { afterSignIn() }
        }
        android.app.AlertDialog.Builder(this).setTitle(R.string.account_email).setView(box)
            .setPositiveButton(R.string.account_enter) { _, _ -> go(false) }
            .setNeutralButton(R.string.account_create) { _, _ -> go(true) }
            .setNegativeButton(R.string.account_forgot) { _, _ ->
                inBackground({ Account.resetPassword(email.text.toString()) }) { say(R.string.account_reset_sent) }
            }.show()
    }

    /** Just signed in: a copy in the account is offered to this phone; with none, this phone's is saved. */
    private fun afterSignIn() {
        accountDraw()
        offerCopy(askFirst = true)
    }

    private fun offerCopy(askFirst: Boolean) {
        say(R.string.account_working)
        var found: Triple<Long, String, org.json.JSONObject>? = null
        inBackground({
            found = Account.download(this)
            if (found == null) Account.upload(this)
        }) {
            val copy = found
            if (copy == null) { say(R.string.account_empty); accountDraw(); return@inBackground }
            val day = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT).format(java.util.Date(copy.first))
            val bring = { restoreCopy(copy.third) }
            if (!askFirst) bring() else android.app.AlertDialog.Builder(this)
                .setMessage(getString(R.string.account_found, day, copy.second.ifEmpty { "?" }))
                .setPositiveButton(R.string.account_found_yes) { _, _ -> bring() }
                .setNegativeButton(R.string.account_found_no) { _, _ -> inBackground({ Account.upload(this) }) { say(R.string.account_ok) } }
                .show()
        }
    }

    /** Her page keeps its own copy of her memory and saves it over the files: she rests while it comes back. */
    private fun restoreCopy(snap: org.json.JSONObject) {
        val wasOn = CapriService.running
        if (wasOn) CapriService.send(this, CapriService.ACTION_QUIT)
        inBackground({
            var waited = 0
            while (CapriService.running && waited < 8000) { Thread.sleep(200); waited += 200 }
            Thread.sleep(400)
            Account.restore(this, snap)
        }) {
            say(R.string.account_restored)
            nameField.setText(CapriSettings.name(this))
            choices.forEach { it() }
            if (wasOn && Settings.canDrawOverlays(this)) CapriService.start(this)
        }
    }
    private var neuroDraw: () -> Unit = {}

    private fun birthdayCard() = ui.card(Hud.RED).apply {
        addView(ui.label(getString(R.string.you_birthday), Hud.RED))
        addView(ui.text(getString(R.string.you_birthday_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(6))
        val shown = ui.text("", 17f, Hud.PAPER, ui.head)
        addView(shown, ui.block(10))
        val clear = ui.button(getString(R.string.you_birthday_clear), Ui.Kind.LINE) {}
        val show = {
            val b = Store(this@MainActivity).configObject("birthday")
            val m = b?.optInt("m") ?: 0
            val d = b?.optInt("d") ?: 0
            shown.text = if (m in 1..12 && d in 1..31) {
                val locale = resources.configuration.locales[0]
                val cal = java.util.Calendar.getInstance().apply { set(2000, m - 1, d) }
                java.text.SimpleDateFormat(android.text.format.DateFormat.getBestDateTimePattern(locale, "dMMMM"), locale).format(cal.time)
            } else getString(R.string.you_birthday_none)
            clear.visibility = if (m in 1..12) View.VISIBLE else View.GONE
        }
        show()
        choices.add(show)
        addView(ui.button(getString(R.string.you_birthday_pick) + " ›", Ui.Kind.GREEN) {
            val b = Store(this@MainActivity).configObject("birthday")
            val now = java.util.Calendar.getInstance()
            val m = b?.optInt("m")?.takeIf { it in 1..12 } ?: (now.get(java.util.Calendar.MONTH) + 1)
            val d = b?.optInt("d")?.takeIf { it in 1..31 } ?: now.get(java.util.Calendar.DAY_OF_MONTH)
            android.app.DatePickerDialog(this@MainActivity, { _, _, month, day ->
                CapriSettings.set(this@MainActivity, "birthday", org.json.JSONObject().put("m", month + 1).put("d", day))
                shown.postDelayed({ show() }, 400) // (her page saves it first, when she's on)
            }, now.get(java.util.Calendar.YEAR), m - 1, d).show()
        }, ui.block(12))
        clear.setOnClickListener {
            CapriSettings.set(this@MainActivity, "birthday", org.json.JSONObject.NULL)
            shown.postDelayed({ show() }, 400)
        }
        addView(clear, ui.block(8))
    }

    /** Your classes (days and time, an hour before: her special notification) and your calendar (optional). */
    private fun agendaCard() = ui.card(Hud.CYAN).apply {
        addView(ui.label(getString(R.string.you_classes), Hud.CYAN))
        addView(ui.text(getString(R.string.you_classes_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(6))
        val list = ui.col()
        addView(list, ui.block(8))
        val locale = resources.configuration.locales[0]
        val names = java.text.DateFormatSymbols(locale).shortWeekdays
        val week = listOf(2, 3, 4, 5, 6, 7, 1) // (Monday first)
        fun draw() {
            list.removeAllViews()
            val lessons = Agenda.lessons(this@MainActivity)
            if (lessons.isEmpty()) list.addView(ui.text(getString(R.string.you_classes_none), 13f, Hud.PAPER_3, ui.mono), ui.block(4))
            lessons.forEachIndexed { i, l ->
                val cal = java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, l.h); set(java.util.Calendar.MINUTE, l.m) }
                val time = android.text.format.DateFormat.getTimeFormat(this@MainActivity).format(cal.time)
                val days = week.filter { it in l.days }.joinToString(", ") { names[it].trimEnd('.') }
                val row = ui.row()
                val what = ui.col().apply {
                    addView(ui.text(l.name.ifBlank { getString(R.string.agenda_class_default) }, 15f, Hud.PAPER, ui.head))
                    addView(ui.text("$days · $time", 12f, Hud.CYAN, ui.mono), ui.block(2))
                }
                // (1.12.0, community: "un botón para editar las clases"): a tap on it, and it's edited in place
                what.isClickable = true
                what.setOnClickListener { addLesson(i) { draw() } }
                row.addView(what, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                row.addView(ui.button(getString(R.string.you_birthday_clear), Ui.Kind.LINE) {
                    Agenda.setLessons(this@MainActivity, Agenda.lessons(this@MainActivity).filterIndexed { j, _ -> j != i })
                    draw()
                })
                list.addView(row, ui.block(if (i == 0) 4 else 10))
            }
        }
        draw()
        addView(ui.text(getString(R.string.you_class_edit_how), 12f, Hud.PAPER_3, ui.body), ui.block(6))
        addView(ui.button(getString(R.string.you_class_add) + " ›", Ui.Kind.GREEN) { addLesson { draw() } }, ui.block(12))

        // your calendar: off until you turn it on (then Android asks)
        addView(ui.text(getString(R.string.you_calendar), 14f, Hud.PAPER, ui.head), ui.block(18))
        addView(ui.text(getString(R.string.you_calendar_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
        addView(choice(listOf(R.string.off to false, R.string.on to true), { Agenda.calendarOn(this@MainActivity) }, { a, b -> a == b }) { on ->
            if (on && !Agenda.calendarAllowed(this@MainActivity)) requestPermissions(arrayOf(android.Manifest.permission.READ_CALENDAR), Permissions.CALENDAR)
            else Agenda.setCalendar(this@MainActivity, on)
        }, ui.block(6))
    }

    /** A new class: what (optional), which days, what time. With [editing], that one, changed in its place. */
    private fun addLesson(editing: Int? = null, done: () -> Unit) {
        val locale = resources.configuration.locales[0]
        val names = java.text.DateFormatSymbols(locale).weekdays
        val week = listOf(2, 3, 4, 5, 6, 7, 1)
        val old = editing?.let { Agenda.lessons(this).getOrNull(it) }
        val field = ui.field(getString(R.string.you_class_name)).apply {
            filters = arrayOf(android.text.InputFilter.LengthFilter(40))
            old?.let { setText(it.name) }
        }
        val box = ui.col().apply { setPadding(ui.dpi(20), ui.dpi(8), ui.dpi(20), 0); addView(field) }
        android.app.AlertDialog.Builder(this).setTitle(if (old != null) R.string.you_class_edit else R.string.you_class_add).setView(box)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = field.text.toString().trim()
                val picked = BooleanArray(7) { old != null && week[it] in old.days }
                android.app.AlertDialog.Builder(this).setTitle(R.string.you_class_days)
                    .setMultiChoiceItems(week.map { names[it].replaceFirstChar { c -> c.titlecase(locale) } }.toTypedArray(), picked) { _, i, on -> picked[i] = on }
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        val days = week.filterIndexed { i, _ -> picked[i] }.toSet()
                        if (days.isEmpty()) return@setPositiveButton
                        android.app.TimePickerDialog(this, { _, h, m ->
                            val now = Agenda.lessons(this)
                            val lesson = Agenda.Lesson(days, h, m, name)
                            Agenda.setLessons(this, if (old != null && editing != null && editing in now.indices) now.mapIndexed { j, l -> if (j == editing) lesson else l } else now + lesson)
                            done()
                        }, old?.h ?: 8, old?.m ?: 0, android.text.format.DateFormat.is24HourFormat(this)).apply { setTitle(R.string.you_class_time) }.show()
                    }.show()
            }.show()
    }

    // ---------- Customize (1.9.0): how she looks, how she behaves, how she sounds ----------

    private fun stylePage() = page {
        addView(ui.label(getString(R.string.tab_style)))
        addView(ui.text(getString(R.string.style_intro), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
        // new in 1.8: her wardrobe, shown off first
        addView(wardrobeCard(), ui.block(16))
        // (1.12.0, community + Emma: "otra pantalla que diga crea tu propia Capri") her template to draw on, and your
        // drawing sent to Emma's channel: it only comes to the app if Emma and the Discord server approve it
        addView(ui.card(Hud.MANE, Hud.CYAN).apply {
            addView(ui.label(getString(R.string.skin_title), Hud.MANE))
            addView(ui.text(getString(R.string.skin_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            addView(ui.button(getString(R.string.skin_get) + " ›", Ui.Kind.GREEN) { skinSteps() }, ui.block(10))
            addView(ui.button(getString(R.string.skin_send) + " ›", Ui.Kind.LINE) { pickSkin() }, ui.block(8))
            addView(ui.text(getString(R.string.skin_approval), 12f, Hud.YELLOW, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(8))
        }, ui.block(16))
        // her on your screen: size, how lively, ghost mode
        addView(ui.card(Hud.YELLOW).apply {
            addView(ui.label(getString(R.string.settings_her)))
            addView(ui.text(getString(R.string.settings_size), 14f, Hud.PAPER, ui.head), ui.block(10))
            addView(choice(listOf(R.string.size_xs to 0.35, R.string.size_s to 0.45, R.string.size_m to 0.6, R.string.size_l to 0.8, R.string.size_xl to 1.0),
                { Store(this@MainActivity).configNumber("scale", 0.6) }, { a, b -> Math.abs(a - b) < 0.02 }) { CapriSettings.set(this@MainActivity, "scale", it) }, ui.block(6))
            addView(ui.text(getString(R.string.settings_lively), 14f, Hud.PAPER, ui.head), ui.block(14))
            addView(choice(listOf(R.string.lively_calm to "calm", R.string.lively_normal to "normal", R.string.lively_restless to "restless"),
                { Store(this@MainActivity).configString("lively") ?: "normal" }, { a, b -> a == b }) { CapriSettings.set(this@MainActivity, "lively", it) }, ui.block(6))
            addView(ui.text(getString(R.string.settings_ghost), 14f, Hud.PAPER, ui.head), ui.block(14))
            addView(ui.text(getString(R.string.settings_ghost_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            addView(choice(listOf(R.string.off to false, R.string.on to true),
                { Store(this@MainActivity).configFlag("ghost", false) }, { a, b -> a == b }) { CapriSettings.set(this@MainActivity, "ghost", it) }, ui.block(6))
            // …and her cards too (notifications, music): only if you want it (1.9.0)
            addView(ui.text(getString(R.string.settings_ghost_cards), 13f, Hud.PAPER, ui.head), ui.block(12))
            addView(ui.text(getString(R.string.settings_ghost_cards_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            addView(choice(listOf(R.string.off to false, R.string.on to true),
                { Store(this@MainActivity).configFlag("ghostCards", false) }, { a, b -> a == b }) { CapriSettings.set(this@MainActivity, "ghostCards", it) }, ui.block(6))
            // without gravity: she stays where you put her, isn't thrown, doesn't wander off
            addView(ui.text(getString(R.string.settings_float), 14f, Hud.PAPER, ui.head), ui.block(14))
            addView(ui.text(getString(R.string.settings_float_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            addView(choice(listOf(R.string.off to false, R.string.on to true),
                { Store(this@MainActivity).configFlag("float", false) }, { a, b -> a == b }) { CapriSettings.set(this@MainActivity, "float", it) }, ui.block(6))
            // (1.10.0) she dresses herself now and then (autodress.js): off, or every 6 h, 12 h, a day
            addView(ui.text(getString(R.string.settings_autodress), 14f, Hud.PAPER, ui.head), ui.block(14))
            addView(ui.text(getString(R.string.settings_autodress_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            addView(choiceText(listOf(getString(R.string.off) to 0.0, "6 h" to 6.0, "12 h" to 12.0, getString(R.string.autodress_day) to 24.0),
                {
                    val store = Store(this@MainActivity)
                    if (!store.configFlag("autoDress", false)) 0.0 else store.configNumber("autoDressHours", 12.0)
                }, { a, b -> Math.abs(a - b) < 0.5 }) {
                if (it > 0) CapriSettings.set(this@MainActivity, "autoDressHours", it.toInt())
                CapriSettings.set(this@MainActivity, "autoDress", it > 0)
            }, ui.block(6))
            // (1.10.0) "quería decirle que abra una canción y la abra en Spotify": where a song you ask for plays
            addView(ui.text(getString(R.string.settings_musicapp), 14f, Hud.PAPER, ui.head), ui.block(14))
            addView(ui.text(getString(R.string.settings_musicapp_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            addView(choiceText(listOf("YouTube" to "youtube", "Spotify" to "spotify"),
                { Store(this@MainActivity).configString("musicApp") ?: "youtube" }, { a, b -> a == b }) { CapriSettings.set(this@MainActivity, "musicApp", it) }, ui.block(6))
            // icon mode: only her mask, wherever you put it, still talking
            addView(ui.text(getString(R.string.settings_icon), 14f, Hud.PAPER, ui.head), ui.block(14))
            addView(ui.text(getString(R.string.settings_icon_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            addView(choice(listOf(R.string.off to false, R.string.on to true),
                { Store(this@MainActivity).configFlag("icon", false) }, { a, b -> a == b }) { CapriSettings.set(this@MainActivity, "icon", it) }, ui.block(6))
            // her music card: always while a song plays, a while after each change, or never
            addView(ui.text(getString(R.string.settings_music), 14f, Hud.PAPER, ui.head), ui.block(14))
            addView(choice(listOf(R.string.music_always to "always", R.string.music_while to "while", R.string.music_off to "off"),
                {
                    val store = Store(this@MainActivity)
                    if (!store.configFlag("musicCard", true)) "off" else store.configString("musicStay") ?: "while"
                }, { a, b -> a == b }) {
                CapriSettings.set(this@MainActivity, "musicCard", it != "off")
                if (it != "off") CapriSettings.set(this@MainActivity, "musicStay", it)
            }, ui.block(6))
        }, ui.block(16))

        // peeking in your notification panel (1.9.0, PeekService.kt): only with Android's Accessibility,
        // off unless you turn it on there (not in the Play version)
        if (BuildConfig.PEEK) addView(ui.card(Hud.RED).apply {
            addView(ui.label(getString(R.string.peek_title), Hud.RED))
            addView(ui.text(getString(R.string.peek_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(6))
            val state = ui.text("", 13f, Hud.PAPER, ui.mono)
            addView(state, ui.block(10))
            val btn = ui.button("", Ui.Kind.LINE) {
                runCatching { startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            }
            addView(btn, ui.block(8))
            val show = {
                val on = PeekService.enabled(this@MainActivity)
                state.text = getString(if (on) R.string.peek_on else R.string.peek_off)
                state.setTextColor(if (on) Hud.GREEN else Hud.PAPER_2)
                btn.text = getString(if (on) R.string.peek_turn_off else R.string.peek_turn_on).uppercase() + " ›"
            }
            show()
            choices.add(show)
        }, ui.block(16))

        // (1.10.0) she uses your phone for you (ControlService.kt): only with Android's Accessibility,
        // off unless you turn it on there; never in bank apps
        addView(ui.card(Hud.GREEN).apply {
            addView(ui.label(getString(R.string.control_title), Hud.GREEN))
            addView(ui.text(getString(R.string.control_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(6))
            addView(ui.text(getString(R.string.control_bank), 13f, Hud.YELLOW, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(8))
            val state = ui.text("", 13f, Hud.PAPER, ui.mono)
            addView(state, ui.block(10))
            val btn = ui.button("", Ui.Kind.LINE) {
                runCatching { startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            }
            addView(btn, ui.block(8))
            val show = {
                val on = ControlService.enabled(this@MainActivity)
                state.text = getString(if (on) R.string.peek_on else R.string.peek_off)
                state.setTextColor(if (on) Hud.GREEN else Hud.PAPER_2)
                btn.text = getString(if (on) R.string.peek_turn_off else R.string.peek_turn_on).uppercase() + " ›"
            }
            show()
            choices.add(show)
        }, ui.block(16))

    }


    // ---------- Ajustes de voz (1.11.0, Emma: "otra pantalla ajustes de voz"): everything about her voice ----------

    // (1.12.0, Emma: "ajustemos en pantallas los ajustes… ajustes de audio, y bajar la voz como botón, luego botones
    // para clave api y dentro pones lo de clave api, luego abajo ajustes avanzados… también un switch antes de esa
    // pantalla que diga VOZ LOCAL O EN LA NUBE") Ajustes de voz is a short menu now: her voice on or off, then a
    // screen for each thing (get her voice, your key, how she sounds, her ears), the cloud-or-local switch and,
    // last, the advanced screen of her local voice.
    private val voiceKids = ArrayList<View>()
    private fun voicePage(): View {
        val back = { ui.text("‹ " + getString(R.string.settings_voice_screen).uppercase(), 13f, Hud.YELLOW, ui.mono).apply {
            letterSpacing = 0.1f
            isClickable = true
            setPadding(0, ui.dpi(4), ui.dpi(16), ui.dpi(14))
            setOnClickListener { closeSub() }
        } }
        fun kid(title: Int, how: Int, build: LinearLayout.() -> Unit) = page {
            addView(back())
            addView(ui.label(getString(title)))
            addView(ui.text(getString(how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            build()
        }.also { voiceKids += it }
        val getPage = kid(R.string.vs_get, R.string.vs_get_how) {
            addView(voiceGetCard().also { voiceSpots["get"] = it }, ui.block(14))
        }
        val cloudPage = kid(R.string.vs_cloud, R.string.vs_cloud_how) {
            addView(fishCard(), ui.block(14))
        }
        val soundPage = kid(R.string.vs_sound, R.string.vs_sound_how) {
            val inner = this
        inner.addView(ui.card(Hud.MANE).apply {
            addView(ui.label(getString(R.string.vs_sound)))
            val store = Store(this@MainActivity)
            addView(ui.text(getString(R.string.voice_pitch), 14f, Hud.PAPER, ui.head), ui.block(10))
            addView(choice(listOf(R.string.voice_low to 0.8, R.string.voice_pace_normal to 1.0, R.string.voice_high to 1.3),
                { store.configNumber("voicePitch", 1.0) }, { a, b -> Math.abs(a - b) < 0.05 }) { CapriSettings.set(this@MainActivity, "voicePitch", it) }, ui.block(6))
            addView(ui.text(getString(R.string.voice_rate), 14f, Hud.PAPER, ui.head), ui.block(14))
            addView(choice(listOf(R.string.voice_slower to 0.85, R.string.voice_pace_normal to 1.0, R.string.voice_faster to 1.2),
                { store.configNumber("voiceRate", 1.0) }, { a, b -> Math.abs(a - b) < 0.05 }) { CapriSettings.set(this@MainActivity, "voiceRate", it) }, ui.block(6))
            // her accent in the language she speaks (1.9.0): Mexico or Spain, the US or Britain, Brazil or Portugal…
            val lang = Lang.current(this@MainActivity)
            Ears.ACCENTS[lang]?.let { accents ->
                addView(ui.text(getString(R.string.voice_accent), 14f, Hud.PAPER, ui.head), ui.block(14))
                addView(ui.text(getString(R.string.voice_accent_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
                for (row in accents.chunked(3)) {
                    addView(choiceText(row.map { (tag, label) -> getString(label) to tag },
                        { store.configString("voiceAccent_$lang") ?: accents[0].first }, { a, b -> a == b }) { CapriSettings.set(this@MainActivity, "voiceAccent_$lang", it) }, ui.block(6))
                }
            }
            // (1.12.0, community: "aparece otra voz de IA diferente") which of the phone's voices she uses when it's the
            // phone's voice: you hear each one and keep the one closest to her
            addView(ui.text(getString(R.string.voice_phone), 14f, Hud.PAPER, ui.head), ui.block(14))
            addView(ui.text(getString(R.string.voice_phone_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            addView(ui.button(getString(R.string.voice_phone_pick) + " ›", Ui.Kind.LINE) { pickPhoneVoice() }, ui.block(8))
        }, ui.block(14))

        }
        val earsPage = kid(R.string.vs_ears, R.string.vs_ears_how) {
        addView(ui.card(Hud.MANE).apply {
            addView(ui.label(getString(R.string.settings_voice)))
            voiceText = ui.text("", 17f, Hud.PAPER, ui.head)
            addView(voiceText, ui.block(6))
            // what her ears are doing right now (listening, what they heard, why not)
            voiceState = ui.text("", 12f, Hud.YELLOW, ui.mono).apply { setLineSpacing(0f, 1.15f) }
            addView(voiceState, ui.block(6))
            addView(ui.text(getString(R.string.settings_voice_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            voiceBtn = ui.button("", Ui.Kind.GREEN) { toggleVoice() }
            addView(voiceBtn, ui.block(12))
            // (1.12.0) her passive ears ("Oye Capri"): their small model, downloaded once per language
            addView(ui.text(getString(R.string.ears_passive), 14f, Hud.PAPER, ui.head), ui.block(16))
            addView(ui.text(getString(R.string.ears_passive_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            passiveState = ui.text("", 12f, Hud.YELLOW, ui.mono)
            addView(passiveState, ui.block(6))
            passiveBtn = ui.button(getString(R.string.ears_passive_get), Ui.Kind.GREEN) {
                WakeWord.Models.fetch(this@MainActivity, Lang.current(this@MainActivity), anyNetwork = true) { CapriService.current?.ears?.update(); showVoice() }
                showVoice()
            }
            addView(passiveBtn, ui.block(8))
            // (1.12.0) only your voice: her name in someone else's voice is ignored (VoiceId.kt, with her passive ears)
            addView(ui.text(getString(R.string.ears_onlyme), 14f, Hud.PAPER, ui.head), ui.block(16))
            addView(ui.text(getString(R.string.ears_onlyme_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            val onlyState = ui.text("", 12f, Hud.YELLOW, ui.mono)
            val showOnly = {
                val n = VoiceId.learned(this@MainActivity)
                onlyState.text = if (!VoiceId.enabled(this@MainActivity)) "" else if (n >= 3) getString(R.string.ears_onlyme_ready) else getString(R.string.ears_onlyme_learning, n, 3)
                onlyState.visibility = if (onlyState.text.isEmpty()) View.GONE else View.VISIBLE
            }
            addView(choiceText(listOf(getString(R.string.on) to true, getString(R.string.off) to false),
                { VoiceId.enabled(this@MainActivity) }, { a, b -> a == b }) { VoiceId.setEnabled(this@MainActivity, it); showOnly() }, ui.block(6))
            addView(onlyState, ui.block(6))
            addView(ui.button(getString(R.string.ears_onlyme_forget), Ui.Kind.LINE) {
                VoiceId.forget(this@MainActivity); showOnly()
                Toast.makeText(this@MainActivity, R.string.ears_onlyme_forgot, Toast.LENGTH_LONG).show()
            }, ui.block(8))
            showOnly()
            choices.add(showOnly)
            // the noise around you, how you talk, how she sounds (Ears.kt reads these each time)
            val store = Store(this@MainActivity)
            addView(ui.text(getString(R.string.voice_filter), 14f, Hud.PAPER, ui.head), ui.block(16))
            addView(ui.text(getString(R.string.voice_filter_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            addView(choice(listOf(R.string.voice_filter_normal to "normal", R.string.voice_filter_strict to "strict", R.string.voice_filter_strictest to "strictest"),
                { store.configString("voiceFilter") ?: "normal" }, { a, b -> a == b }) { CapriSettings.set(this@MainActivity, "voiceFilter", it) }, ui.block(6))
            addView(ui.text(getString(R.string.voice_pace), 14f, Hud.PAPER, ui.head), ui.block(14))
            addView(choice(listOf(R.string.voice_pace_fast to "fast", R.string.voice_pace_normal to "normal", R.string.voice_pace_slow to "slow"),
                { store.configString("voicePace") ?: "normal" }, { a, b -> a == b }) { CapriSettings.set(this@MainActivity, "voicePace", it) }, ui.block(6))
        }, ui.block(16))

        }
        val advPage = kid(R.string.vs_adv, R.string.vs_adv_how) {
        // (1.11.0) Capri Voice V8 (CapriVoice.kt): her recorded voice, phrase by phrase, offline; optional
        // packs per language. Here with the rest of her voice, not on a screen of its own.
        addView(ui.card(Hud.MANE).apply {
            addView(ui.label(getString(R.string.cv_title)))
            val store = Store(this@MainActivity)
            addView(ui.text(getString(R.string.cv_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            val engineState = ui.text(getString(R.string.cv_engine_state), 12f, Hud.PAPER_2, ui.mono)
            addView(engineState, ui.block(8))
            addView(ui.text(getString(R.string.cv_clips), 14f, Hud.PAPER, ui.head), ui.block(12))
            addView(choice(listOf(R.string.off to false, R.string.on to true),
                { store.configFlag("voiceClips", true) }, { a, b -> a == b }) { CapriSettings.set(this@MainActivity, "voiceClips", it) }, ui.block(6))
            // (V8.5) how she trades fidelity for performance. Every mode only picks among engines that really
            // work here; none switches her identity behind your back (an experimental engine is only used if
            // you choose it yourself, in Custom).
            addView(ui.text(getString(R.string.cv_mode), 14f, Hud.PAPER, ui.head), ui.block(14))
            addView(choice(listOf(R.string.cv_mode_auto to "automatic", R.string.cv_mode_light to "light", R.string.cv_mode_balanced to "balanced"),
                { store.configString("voiceMode") ?: "automatic" }, { a, b -> a == b }) { setVoiceMode(it) }, ui.block(6))
            addView(choice(listOf(R.string.cv_mode_fidelity to "fidelity", R.string.cv_mode_custom to "custom"),
                { store.configString("voiceMode") ?: "automatic" }, { a, b -> a == b }) { setVoiceMode(it) }, ui.block(4))
            val why = ui.text("", 12f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }
            addView(why, ui.block(6))
            addView(ui.text(getString(R.string.cv_answers), 14f, Hud.PAPER, ui.head), ui.block(14))
            addView(ui.text(getString(R.string.cv_answers_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            addView(choice(listOf(R.string.cv_engine_system to "system", R.string.cv_engine_neural to CapriNeural.ID, R.string.cv_engine_text to "text"),
                { CapriVoice.engine(this@MainActivity) }, { a, b -> a == b }) { pickEngine(it) }.also { voiceSpots["answers"] = it }, ui.block(6))
            val showWhy = {
                val lang = CapriVoice.voiceLang(Lang.current(this@MainActivity)) ?: "es-MX"
                why.text = getString(when (store.configString("voiceMode") ?: "automatic") {
                    "fidelity" -> R.string.cv_why_fidelity
                    "custom" -> if (CapriVoice.engine(this@MainActivity) == CapriNeural.ID) R.string.cv_why_custom_neural else R.string.cv_why_custom
                    "light" -> R.string.cv_why_light
                    else -> R.string.cv_why_auto
                }) + "\n" + getString(R.string.cv_neural_now, neuralStatus(lang))
            }
            showWhy()
            choices.add(showWhy)
            addView(ui.text(getString(R.string.cv_volume), 14f, Hud.PAPER, ui.head), ui.block(14))
            addView(choice(listOf(R.string.cv_vol_low to 0.5, R.string.cv_vol_mid to 0.8, R.string.cv_vol_high to 1.0),
                { store.configNumber("voiceClipVolume", 1.0) }, { a, b -> Math.abs(a - b) < 0.05 }) { CapriSettings.set(this@MainActivity, "voiceClipVolume", it) }, ui.block(6))
            addView(ui.text(getString(R.string.cv_speed), 14f, Hud.PAPER, ui.head), ui.block(14))
            addView(choice(listOf(R.string.voice_slower to 0.9, R.string.voice_pace_normal to 1.0, R.string.voice_faster to 1.1),
                { store.configNumber("voiceClipSpeed", 1.0) }, { a, b -> Math.abs(a - b) < 0.05 }) { CapriSettings.set(this@MainActivity, "voiceClipSpeed", it) }, ui.block(6))
            addView(ui.text(getString(R.string.cv_signature), 12f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(10))
            addView(ui.button(getString(R.string.cv_test), Ui.Kind.LINE) { testVoice() }.also { voiceSpots["test"] = it }, ui.block(10))

            // (1.11.0) her engines for new answers: each precision added or removed on its own (Emma: "que el usuario
            // pueda eliminar o añadir de nuestras opciones, cada una con mayor o menor fidelidad")
            addView(ui.text(getString(R.string.cv_engines), 14f, Hud.PAPER, ui.head).also { voiceSpots["engines"] = it }, ui.block(16))
            addView(ui.text(getString(R.string.cv_engines_how), 12f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            addView(ui.text("", 13f, Hud.YELLOW, ui.mono).apply { visibility = View.GONE; dlViews += this }, ui.block(6))
            for (v in CapriNeural.VARIANTS) {
                val line = ui.text("", 14f, Hud.PAPER, ui.head)
                val note = ui.text("", 12f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }
                addView(line, ui.block(12)); addView(note, ui.block(2))
                val row = ui.row()
                val lpv = { LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { rightMargin = ui.dpi(4) } }
                val add = ui.button(getString(R.string.cv_var_add), Ui.Kind.LINE) {
                    when {
                        CapriNeural.unsupported() != null -> Toast.makeText(this@MainActivity, neuralStatus("es-MX"), Toast.LENGTH_LONG).show()
                        !CapriNeural.fits(this@MainActivity, v) -> Toast.makeText(this@MainActivity, getString(R.string.cv_var_nofit_ram, v.ramGb), Toast.LENGTH_LONG).show()
                        else -> voiceJob(getString(R.string.dl_engine, getString(when (v.id) { "q4" -> R.string.cv_var_q4; "q8" -> R.string.cv_var_q8; else -> R.string.cv_var_bf16 }))) { d ->
                            // (1.12.0, community: "instalo los archivos de voz pero no los reproduce") the engine alone isn't her
                            // voice: her profiles come right after it, or she kept speaking in the phone's voice
                            CapriNeural.download(this@MainActivity, v) { err ->
                                if (err == null && (!CapriNeural.profileInstalled(this@MainActivity, "es-MX") || !CapriNeural.profileInstalled(this@MainActivity, "en-US")))
                                    CapriNeural.downloadProfiles(this@MainActivity, d) else d(err)
                            }
                        }
                    }
                }
                val use = ui.button(getString(R.string.cv_var_use), Ui.Kind.LINE) {
                    CapriSettings.set(this@MainActivity, "voiceNeuralModel", v.id); CapriNeural.release("variant"); choices.forEach { it() }
                }
                val drop = ui.button(getString(R.string.cv_remove), Ui.Kind.LINE) { voiceJob { CapriNeural.remove(this@MainActivity, v.id, it) } }
                row.addView(add, lpv()); row.addView(use, lpv()); row.addView(drop, lpv())
                addView(row, ui.block(6))
                val showV = {
                    val have = CapriNeural.installed(this@MainActivity, v)
                    val inUse = have && CapriNeural.variant(this@MainActivity)?.id == v.id
                    val fmt = android.text.format.Formatter.formatShortFileSize(this@MainActivity, v.bytes)
                    line.text = getString(R.string.cv_var_line, getString(when (v.id) { "q4" -> R.string.cv_var_q4; "q8" -> R.string.cv_var_q8; else -> R.string.cv_var_bf16 }), fmt)
                    line.setTextColor(if (inUse) Hud.GREEN else if (have) Hud.PAPER else Hud.PAPER_2)
                    note.text = getString(when (v.id) { "q4" -> R.string.cv_fid_light; "q8" -> R.string.cv_fid_high; else -> R.string.cv_fid_max }) + " · " + (when {
                        inUse -> R.string.cv_var_inuse
                        have -> R.string.cv_var_installed
                        else -> 0
                    }).let { if (it != 0) getString(it) else when {
                        CapriNeural.unsupported() != null -> neuralStatus("es-MX")
                        !CapriNeural.fits(this@MainActivity, v) -> getString(R.string.cv_var_nofit_ram, v.ramGb)
                        else -> getString(R.string.cv_var_missing)
                    } }
                    add.visibility = if (have) View.GONE else View.VISIBLE
                    use.visibility = if (have && !inUse) View.VISIBLE else View.GONE
                    drop.visibility = if (have) View.VISIBLE else View.GONE
                }
                showV()
                choices.add(showV)
            }

            // (1.11.3, Emma: "de la voz elimina lo de comparar voces") no A/B here any more: «Probar su voz» is the test
            val lpw = { LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { rightMargin = ui.dpi(4) } }

            // advanced: the neural engine itself, its files and its limits
            val advanced = ui.col().apply { visibility = View.GONE }
            addView(ui.button(getString(R.string.cv_advanced), Ui.Kind.LINE) {
                advanced.visibility = if (advanced.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }, ui.block(14))
            addView(advanced)
            advanced.apply {
                addView(ui.text(getString(R.string.cv_neural_title), 14f, Hud.PAPER, ui.head), ui.block(10))
                addView(ui.text(getString(R.string.cv_neural_how), 12f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
                val nState = ui.text("", 12f, Hud.PAPER, ui.mono)
                addView(nState, ui.block(8))
                val r1 = ui.row(); val r2 = ui.row(); val r3 = ui.row()
                r1.addView(ui.button(getString(R.string.cv_neural_download_profile), Ui.Kind.LINE) { voiceJob(getString(R.string.dl_voice)) { CapriNeural.downloadProfiles(this@MainActivity, it) } }, lpw())
                r1.addView(ui.button(getString(R.string.cv_neural_import), Ui.Kind.LINE) { pickFile(PICK_MODEL) }, lpw())
                r2.addView(ui.button(getString(R.string.cv_neural_profile), Ui.Kind.LINE) { pickFile(PICK_PROFILE) }, lpw())
                r2.addView(ui.button(getString(R.string.cv_verify), Ui.Kind.LINE) { voiceJob { CapriNeural.verify(this@MainActivity, it) } }, lpw())
                r3.addView(ui.button(getString(R.string.cv_neural_cancel), Ui.Kind.LINE) { CapriNeural.cancelDownload() }, lpw())
                r3.addView(ui.button(getString(R.string.cv_neural_rm_profile), Ui.Kind.LINE) { voiceJob { CapriNeural.remove(this@MainActivity, "profile", it) } }, lpw())
                addView(r1, ui.block(6)); addView(r2, ui.block(4)); addView(r3, ui.block(4))
                addView(ui.text(getString(R.string.cv_dsp), 13f, Hud.PAPER, ui.head), ui.block(12))
                addView(choice(listOf(R.string.off to false, R.string.on to true), { store.configFlag("voiceDsp", true) }, { a, b -> a == b }) {
                    CapriSettings.set(this@MainActivity, "voiceDsp", it) }, ui.block(4))
                addView(ui.text(getString(R.string.cv_threads), 13f, Hud.PAPER, ui.head), ui.block(12))
                addView(choiceText(listOf(getString(R.string.cv_auto) to 0.0, "2" to 2.0, "4" to 4.0, "6" to 6.0), { store.configNumber("voiceThreads", 0.0) }, { a, b -> a == b }) {
                    CapriSettings.set(this@MainActivity, "voiceThreads", it); CapriNeural.release("threads") }, ui.block(4))
                addView(ui.text(getString(R.string.cv_unload), 13f, Hud.PAPER, ui.head), ui.block(12))
                addView(choiceText(listOf("2 min" to 2.0, "5 min" to 5.0, "10 min" to 10.0, "15 min" to 15.0), { store.configNumber("voiceUnloadMin", CapriNeural.unloadDefault(this@MainActivity)) }, { a, b -> a == b }) {
                    CapriSettings.set(this@MainActivity, "voiceUnloadMin", it) }, ui.block(4))
                addView(ui.text(getString(R.string.cv_fallback), 13f, Hud.PAPER, ui.head), ui.block(12))
                addView(choice(listOf(R.string.cv_engine_text to "text", R.string.cv_engine_system to "system"), { store.configString("voiceFallback") ?: "text" }, { a, b -> a == b }) {
                    CapriSettings.set(this@MainActivity, "voiceFallback", it) }, ui.block(4))
                val showN = {
                    val h = CapriNeural.hardware(this@MainActivity)
                    val fmt = { b: Long -> android.text.format.Formatter.formatShortFileSize(this@MainActivity, b) }
                    val m = CapriNeural.metrics
                    nState.text = buildString {
                        append("es-MX: ").append(neuralStatus("es-MX")).append('\n')
                        append("en-US: ").append(neuralStatus("en-US")).append('\n')
                        append(getString(R.string.cv_hw, h.optInt("api"), h.optJSONArray("abis")?.optString(0) ?: "?", h.optInt("cores"),
                            fmt(h.optLong("ramFree")), fmt(h.optLong("ramTotal")), fmt(h.optLong("disk"))))
                        if (m.has("generationMs")) append('\n').append(getString(R.string.cv_metrics, m.optString("lang"), m.optLong("loadMs"),
                            m.optLong("firstAudioMs"), m.optDouble("rtf", 0.0), fmt(m.optLong("pssPeakBytes"))))
                        if (CapriNeural.busy.isNotEmpty()) append('\n').append(getString(R.string.cv_busy))
                            .append(CapriNeural.progress.takeIf { it >= 0 }?.let { " ${(it * 100).toInt()}%" } ?: "")
                        append('\n').append(getString(R.string.cv_storage, fmt(CapriNeural.bytes(this@MainActivity))))
                        if (CapriNeural.lastFallback.isNotEmpty()) append('\n').append(getString(R.string.cv_last_fallback, CapriNeural.lastFallback))
                    }
                }
                showN()
                choices.add(showN)
            }

            // her packs, one per language: installed or not, its version, and what to do with it
            addView(ui.text(getString(R.string.cv_private), 12f, Hud.YELLOW, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(14))
            val storage = ui.text("", 12f, Hud.PAPER_2, ui.mono)
            for (pack in CapriVoice.PACKS) {
                val name = getString(if (pack.lang == "es-MX") R.string.cv_lang_es else R.string.cv_lang_en)
                val line = ui.text("", 14f, Hud.PAPER, ui.head)
                addView(line, ui.block(12))
                val row1 = ui.row()
                val row2 = ui.row()
                val lp = { LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { rightMargin = ui.dpi(4) } }
                row1.addView(ui.button(getString(R.string.cv_install), Ui.Kind.LINE) {
                    runCatching {
                        @Suppress("DEPRECATION")
                        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                            .setType("*/*").putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/zip", "application/octet-stream")), PICK_VOICE)
                    }
                }, lp())
                row1.addView(ui.button(getString(R.string.cv_download), Ui.Kind.LINE) {
                    voiceJob(getString(R.string.dl_voice)) { done -> CapriVoice.download(this@MainActivity, pack.lang, done) }
                }, lp())
                row2.addView(ui.button(getString(R.string.cv_verify), Ui.Kind.LINE) { voiceJob { CapriVoice.verify(this@MainActivity, pack.lang, it) } }, lp())
                row2.addView(ui.button(getString(R.string.cv_repair), Ui.Kind.LINE) { voiceJob { CapriVoice.repair(this@MainActivity, pack.lang, it) } }, lp())
                row2.addView(ui.button(getString(R.string.cv_remove), Ui.Kind.LINE) { voiceJob { CapriVoice.remove(this@MainActivity, pack.lang, it) } }, lp())
                addView(row1, ui.block(6))
                addView(row2, ui.block(4))
                val show = {
                    val lib = CapriVoice.installed(pack.lang)
                    line.text = if (lib != null) getString(R.string.cv_installed, name, pack.version, lib.listClips().size) else getString(R.string.cv_missing, name)
                    line.setTextColor(if (lib != null) Hud.GREEN else Hud.PAPER_2)
                    row2.visibility = if (lib != null) View.VISIBLE else View.GONE
                }
                show()
                choices.add(show)
            }
            addView(storage, ui.block(10))
            val showStorage = {
                val busy = CapriVoice.busy
                storage.text = if (busy.isNotEmpty()) getString(R.string.cv_busy) + (CapriVoice.progress.takeIf { it >= 0 }?.let { " ${(it * 100).toInt()}%" } ?: "")
                else getString(R.string.cv_storage, android.text.format.Formatter.formatShortFileSize(this@MainActivity, CapriVoice.bytes(this@MainActivity)))
            }
            showStorage()
            choices.add(showStorage)
        }, ui.block(16))
        }
        for (k in listOf("answers", "test", "engines")) spotPage[k] = advPage
        spotPage["get"] = getPage
        return page {
            addView(backRow())
            addView(ui.label(getString(R.string.settings_voice_screen)))
            addView(ui.text(getString(R.string.settings_voice_screen_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            addView(ui.button(getString(R.string.vt_open), Ui.Kind.LINE) { voiceTutorial() }, ui.block(12))
        // how she sounds: her voice's pitch and speed, her accent (Ears.kt reads these each time)
        addView(ui.card(Hud.MANE).apply {
            addView(ui.label(getString(R.string.settings_her_voice)))
            val store = Store(this@MainActivity)
            // (1.10.0) her voice on or off: off, she answers in writing only
            addView(ui.text(getString(R.string.voice_speak), 14f, Hud.PAPER, ui.head), ui.block(10))
            addView(ui.text(getString(R.string.voice_speak_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            addView(choice(listOf(R.string.off to false, R.string.on to true),
                { store.configFlag("speak", true) }, { a, b -> a == b }) { CapriSettings.set(this@MainActivity, "speak", it) }, ui.block(6))
            // (1.11.7) her chat answers out loud too (chatvoice.js), not only when you talk to her
            addView(ui.text(getString(R.string.voice_speak_chat), 14f, Hud.PAPER, ui.head), ui.block(10))
            addView(ui.text(getString(R.string.voice_speak_chat_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            addView(choice(listOf(R.string.off to false, R.string.on to true),
                { store.configFlag("speakChat", true) }, { a, b -> a == b }) { CapriSettings.set(this@MainActivity, "speakChat", it) }, ui.block(6))
        }, ui.block(16))
            addView(entry("spark", Hud.YELLOW, R.string.vs_get, R.string.vs_get_how) { openSub(getPage) }, ui.block(10))
            addView(entry("globe", Hud.CYAN, R.string.vs_cloud, R.string.vs_cloud_how) { openSub(cloudPage) }, ui.block(10))
            addView(entry("mic", Hud.MANE, R.string.vs_sound, R.string.vs_sound_how) { openSub(soundPage) }, ui.block(10))
            addView(entry("person", Hud.GREEN, R.string.vs_ears, R.string.vs_ears_how) { openSub(earsPage) }, ui.block(10))
            // the switch before the advanced screen: her answers in the cloud (Fish, your key) or here on the phone
            // (Emma: "el de avanzados solo debe verse al seleccionar local")
            val adv = entry("eye", Hud.MANE, R.string.vs_adv, R.string.vs_adv_how) { openSub(advPage) }
            addView(ui.card(Hud.YELLOW).apply {
                addView(ui.label(getString(R.string.vs_where)))
                addView(ui.text(getString(R.string.vs_where_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
                val state = ui.text("", 12f, Hud.YELLOW, ui.mono).apply { setLineSpacing(0f, 1.15f) }
                val draw = {
                    val cloud = Store(this@MainActivity).configFlag("voiceCloud", true)
                    adv.visibility = if (cloud) View.GONE else View.VISIBLE
                    state.text = getString(when {
                        cloud && !FishVoice.hasKey(this@MainActivity) -> R.string.vs_where_nokey
                        cloud -> R.string.vs_where_cloud_on
                        else -> R.string.vs_where_local_on
                    })
                }
                addView(choice(listOf(R.string.vs_local to false, R.string.vs_cloud_short to true),
                    { Store(this@MainActivity).configFlag("voiceCloud", true) }, { a, b -> a == b }) {
                    CapriSettings.set(this@MainActivity, "voiceCloud", it); draw()
                    if (it && !FishVoice.hasKey(this@MainActivity)) openSub(cloudPage)
                }, ui.block(8))
                addView(state, ui.block(6))
                draw()
                choices.add(draw)
            }, ui.block(14))
            addView(adv, ui.block(10))
        }
    }

    // (1.12.0) Fish Audio: her voice in the cloud (FishVoice.kt). Your key stays on this phone, sealed.
    private fun fishCard(): LinearLayout = ui.card(Hud.MANE).apply {
        addView(ui.label(getString(R.string.fish_title)))
        addView(ui.text(getString(R.string.fish_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
        val state = ui.text("", 12f, Hud.PAPER_2, ui.mono).apply { setLineSpacing(0f, 1.15f) }
        addView(state, ui.block(8))
        val buttons = ui.col()
        addView(buttons, ui.block(4))
        fun draw() {
            val lang = Lang.current(this@MainActivity)
            state.text = when {
                !FishVoice.hasKey(this@MainActivity) -> getString(R.string.fish_state_none)
                !FishVoice.on(this@MainActivity) -> getString(R.string.fish_state_off)
                FishVoice.voiceFor(this@MainActivity, lang).isEmpty() -> getString(R.string.fish_state_novoice)
                else -> getString(R.string.fish_state_on) + if (FishVoice.customVoice(this@MainActivity).isNotEmpty()) " · " + getString(R.string.fish_custom_on)
                    else if (!FishVoice.hasCapri(lang)) " · " + getString(R.string.fish_other_lang) else ""
            }
            buttons.removeAllViews()
            if (!FishVoice.hasKey(this@MainActivity)) {
                buttons.addView(ui.button(getString(R.string.fish_key), Ui.Kind.GREEN) { fishKeyDialog { draw() } }, ui.block(4))
                buttons.addView(ui.button(getString(R.string.fish_get_key) + " ↗", Ui.Kind.LINE) {
                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://fish.audio/app/api-keys/"))) }
                }, ui.block(8))
                return
            }
            buttons.addView(choice(listOf(R.string.off to false, R.string.on to true),
                { FishVoice.on(this@MainActivity) }, { a, b -> a == b }) { FishVoice.setOn(this@MainActivity, it); draw() }, ui.block(4))
            // (1.12.0, Emma: "que deje seleccionar con cuál modelo") the free one, or Pro with your credit
            buttons.addView(ui.text(getString(R.string.fish_model), 14f, Hud.PAPER, ui.head), ui.block(10))
            buttons.addView(choice(listOf(R.string.fish_model_free to FishVoice.FREE, R.string.fish_model_pro to "s2.1-pro"),
                { FishVoice.model(this@MainActivity) }, { a, b -> a == b }) { FishVoice.setModel(this@MainActivity, it); draw() }, ui.block(4))
            buttons.addView(ui.button(getString(R.string.fish_test) + " ▶", Ui.Kind.GREEN) {
                val ears = CapriService.current?.ears ?: return@button Toast.makeText(this@MainActivity, R.string.fish_test_off, Toast.LENGTH_LONG).show()
                ears.say(getString(R.string.voice_phone_sample), Lang.current(this@MainActivity), org.json.JSONObject().put("id", "fish-test").put("kind", "test").put("engine", FishVoice.ID))
            }, ui.block(8))
            buttons.addView(ui.button(getString(R.string.fish_voice) + " ›", Ui.Kind.LINE) { fishVoiceDialog { draw() } }, ui.block(8))
            buttons.addView(ui.button(getString(R.string.fish_forget), Ui.Kind.LINE) { FishVoice.forget(this@MainActivity); draw() }, ui.block(8))
        }
        draw()
    }

    private fun fishKeyDialog(done: () -> Unit) {
        val box = ui.col().apply { setPadding(ui.dpi(20), ui.dpi(8), ui.dpi(20), 0) }
        val key = ui.field(getString(R.string.fish_key_hint), secret = true)
        box.addView(ui.withPaste(key), ui.block(4))
        android.app.AlertDialog.Builder(this).setTitle(R.string.fish_key).setView(box)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                Toast.makeText(this, R.string.account_working, Toast.LENGTH_SHORT).show()
                val typed = key.text.toString()
                Threads.make {
                    val why = FishVoice.setKey(this, typed)
                    runOnUiThread {
                        if (isFinishing) return@runOnUiThread
                        Toast.makeText(this, when (why) {
                            null -> getString(R.string.fish_key_ok)
                            "badkey" -> getString(R.string.fish_key_bad)
                            "credit" -> getString(R.string.fish_key_credit)
                            else -> getString(R.string.fish_key_err, why)
                        }, Toast.LENGTH_LONG).show()
                        done()
                    }
                }.start()
            }
            .setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun fishVoiceDialog(done: () -> Unit) {
        val box = ui.col().apply { setPadding(ui.dpi(20), ui.dpi(8), ui.dpi(20), 0) }
        box.addView(ui.text(getString(R.string.fish_voice_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
        val id = ui.field(getString(R.string.fish_voice_hint)).apply { setText(FishVoice.customVoice(this@MainActivity)) }
        box.addView(ui.withPaste(id), ui.block(8))
        android.app.AlertDialog.Builder(this).setTitle(R.string.fish_voice).setView(box)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                // (a link to the voice works too: its id is the last part)
                val v = id.text.toString().trim().substringAfterLast('/').substringBefore('?')
                FishVoice.setCustomVoice(this, if (Regex("^[A-Za-z0-9_-]{6,64}$").matches(v)) v else "")
                done()
            }
            .setNeutralButton(R.string.fish_voice_hers) { _, _ -> FishVoice.setCustomVoice(this, ""); done() }
            .setNegativeButton(android.R.string.cancel, null).show()
    }

    // (1.12.0) the phone's voices for the app's language, to hear and pick (Ears.voice uses the one saved)
    private var probe: android.speech.tts.TextToSpeech? = null

    private fun pickPhoneVoice() {
        Toast.makeText(this, R.string.account_working, Toast.LENGTH_SHORT).show()
        probe?.shutdown()
        var t: android.speech.tts.TextToSpeech? = null
        t = android.speech.tts.TextToSpeech(this) { status ->
            val tts = t ?: return@TextToSpeech
            if (status != android.speech.tts.TextToSpeech.SUCCESS) return@TextToSpeech Toast.makeText(this, R.string.voice_phone_none, Toast.LENGTH_LONG).show()
            val lang = Lang.current(this)
            val key = "phoneVoice_$lang"
            val voices = runCatching { tts.voices.orEmpty().filter { it.locale.language == java.util.Locale(lang).language && "notInstalled" !in it.features } }.getOrDefault(emptyList())
                .sortedWith(compareBy({ it.isNetworkConnectionRequired }, { -it.quality }, { it.locale.country }, { it.name }))
            if (voices.isEmpty()) return@TextToSpeech Toast.makeText(this, R.string.voice_phone_none, Toast.LENGTH_LONG).show()
            val saved = Store(this).configString(key)
            val dialog = android.app.Dialog(this)
            val list = ui.col()
            voices.forEachIndexed { i, v ->
                val label = "${getString(R.string.voice_phone_n, i + 1)} · ${v.locale.country}" + (if (v.isNetworkConnectionRequired) " · ☁" else "")
                val row = ui.row()
                row.addView(ui.button("▶ " + label, if (v.name == saved) Ui.Kind.GREEN else Ui.Kind.LINE) {
                    tts.voice = v
                    tts.speak(getString(R.string.voice_phone_sample), android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "probe")
                }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
                row.addView(ui.button("✓", Ui.Kind.GREEN) {
                    CapriSettings.set(this, key, v.name)
                    dialog.dismiss()
                    Toast.makeText(this, R.string.voice_phone_saved, Toast.LENGTH_SHORT).show()
                }, LinearLayout.LayoutParams(ui.dpi(56), WRAP_CONTENT).apply { leftMargin = ui.dpi(8) })
                list.addView(row, ui.block(8))
            }
            val card = ui.card(Hud.YELLOW, Hud.MANE).apply {
                addView(ui.label(getString(R.string.voice_phone)))
                addView(ScrollView(this@MainActivity).apply { addView(list) }, LinearLayout.LayoutParams(MATCH_PARENT, minOf(ui.dpi(56) * voices.size + ui.dpi(20), (resources.displayMetrics.heightPixels * 0.6).toInt())))
                addView(ui.button(getString(android.R.string.cancel), Ui.Kind.LINE) { dialog.dismiss() }, ui.block(12))
            }
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            dialog.setContentView(FrameLayout(this).apply { setPadding(ui.dpi(4), ui.dpi(4), ui.dpi(10), ui.dpi(10)); addView(card) })
            dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.92).toInt(), WRAP_CONTENT)
            dialog.setOnDismissListener { runCatching { tts.stop(); tts.shutdown() }; probe = null }
            dialog.show()
        }
        probe = t
    }

    // (1.11.0) Capri Voice V8: the pack you picked in your files, its jobs, a test of her voice
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        when (requestCode) {
            PICK_VOICE -> voiceJob { CapriVoice.installFrom(this, uri, it) }
            PICK_MODEL -> voiceJob { CapriNeural.importModel(this, uri, it) }
            PICK_PROFILE -> voiceJob { CapriNeural.importProfiles(this, uri, it) }
            // (1.12.0) a file for her (a PDF, a Word file, a picture, a video, a song): as if shared with her
            PICK_SKIN -> sendSkin(uri)
            PICK_AVATAR -> {
                if (!Profile.setPhoto(this, uri)) Toast.makeText(this, R.string.profile_photo_bad, Toast.LENGTH_LONG).show()
                avatars.forEach { it.reload() }
                refresh()
            }
            SAVE_LOG -> {
                val ok = runCatching { contentResolver.openOutputStream(uri)?.use { it.write(logText.toByteArray(Charsets.UTF_8)) } != null }.getOrDefault(false)
                logText = ""
                Toast.makeText(this, if (ok) "Registro guardado" else "No se pudo guardar", Toast.LENGTH_LONG).show()
            }
            SAVE_TEMPLATE -> {
                val ok = runCatching {
                    contentResolver.openOutputStream(uri)?.use { out -> assets.open("capri/assets/capri/skin/plantilla-capri.png").use { it.copyTo(out) } } != null
                }.getOrDefault(false)
                Toast.makeText(this, if (ok) R.string.skin_saved else R.string.report_bad, Toast.LENGTH_LONG).show()
            }
            PICK_FOR_HER -> runCatching {
                // (1.12.0, Emma) answered here in the Chat tab, where you sent it
                val name = runCatching { contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null } }.getOrNull()
                val id = chat.attached("📎 " + (name ?: getString(R.string.chat_attach)))
                startActivity(Intent(this, ShareActivity::class.java).setAction(Intent.ACTION_SEND)
                    .setType(contentResolver.getType(uri) ?: "application/octet-stream")
                    .putExtra(ShareActivity.EXTRA_APP_CHAT, id)
                    .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
            }
        }
    }

    /** (1.12.0) The Chat tab's 📎: a file to give her. */
    fun pickForHer() = pickFile(PICK_FOR_HER)

    private fun pickFile(code: Int) = runCatching {
        @Suppress("DEPRECATION")
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"), code)
    }

    // (V8.5, 1.11.0) the modes: each one only picks among engines that really work here, each time she speaks
    // (CapriVoice.answerEngine): automatic, balanced and fidelity use her neural voice (her approved identity,
    // the same ICL prompt as PC) once it's installed; light never loads a model.
    private fun setVoiceMode(mode: String) {
        CapriSettings.set(this, "voiceMode", mode)
        if (mode != "custom") CapriSettings.set(this, "voiceClips", true)
        if (mode == "light") CapriNeural.release("mode")
    }

    private fun pickEngine(engine: String) {
        if (engine == CapriNeural.ID) {
            val lang = CapriVoice.voiceLang(Lang.current(this)) ?: "es-MX"
            val st = CapriNeural.status(this, lang)
            if (st != "ready" && st != "loaded") {
                Toast.makeText(this, getString(R.string.cv_neural_cant, neuralStatus(lang)), Toast.LENGTH_LONG).show()
                return
            }
        } else CapriNeural.release("engine")
        CapriSettings.set(this, "voiceEngine", engine)
        CapriSettings.set(this, "voiceMode", "custom")
    }

    /** The neural engine's real state for [lang], in words. */
    private fun neuralStatus(lang: String): String {
        val st = CapriNeural.status(this, lang)
        return when {
            st == "unsupported:api" -> getString(R.string.cv_n_api)
            st == "unsupported:abi" -> getString(R.string.cv_n_abi)
            st.startsWith("unsupported") -> getString(R.string.cv_n_native)
            st == "nomodel" -> getString(R.string.cv_n_nomodel, android.text.format.Formatter.formatShortFileSize(this, CapriNeural.MODEL_BYTES))
            st == "noprofile" -> getString(R.string.cv_n_noprofile)
            st == "loaded" -> getString(R.string.cv_n_loaded)
            else -> getString(R.string.cv_n_ready)
        }
    }

    // (1.11.3) what's downloading right now, with its progress: in a notification and on the voice screen
    private val dlViews = mutableListOf<TextView>()
    private var dlTitle: String? = null
    private val dlTick = object : Runnable {
        override fun run() {
            val title = dlTitle ?: return
            val p = maxOf(CapriVoice.progress, CapriNeural.progress)
            DownloadNote.progress(this@MainActivity, title, p)
            val line = if (p < 0f) getString(R.string.dl_starting) else getString(R.string.dl_progress, (p * 100).toInt().coerceIn(0, 100))
            for (v in dlViews) { v.text = "$title · $line"; v.visibility = View.VISIBLE }
            shell.postDelayed(this, 500)
        }
    }

    private fun voiceJob(title: String? = null, work: ((String?) -> Unit) -> Unit) {
        // (1.11.6, Emma: "al bajar la más fiel dice No se pudo: busy") one download at a time: tapping another while
        // one is still going said «No se pudo: busy», and worse, it took down the bar and notification of the one
        // still downloading, so it looked failed. Now it just says to wait, and the running one keeps its bar.
        if (CapriNeural.busy.isNotEmpty() || CapriVoice.busy.isNotEmpty()) {
            val pct = (if (CapriNeural.busy.isNotEmpty()) CapriNeural.progress else CapriVoice.progress).takeIf { it >= 0f }
            Toast.makeText(this, if (pct != null) getString(R.string.cv_wait_pct, (pct * 100).toInt().coerceIn(0, 100)) else getString(R.string.cv_wait), Toast.LENGTH_LONG).show()
            return
        }
        Toast.makeText(this, title ?: getString(R.string.cv_busy), Toast.LENGTH_SHORT).show()
        choices.forEach { it() }
        if (title != null) { dlTitle = title; shell.removeCallbacks(dlTick); dlTick.run() }
        work { err ->
            if (title != null) {
                dlTitle = null; shell.removeCallbacks(dlTick)
                DownloadNote.done(this, title, err)
                for (v in dlViews) v.visibility = View.GONE
            }
            val msg = when (err) {
                null -> getString(R.string.cv_ok)
                "busy" -> getString(R.string.cv_wait)
                "notV8" -> getString(R.string.cv_err_notv8)
                "needFile" -> getString(R.string.cv_err_needfile)
                "nochannel" -> getString(R.string.cv_nochannel)
                else -> getString(R.string.cv_err, err)
            }
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            choices.forEach { it() }
        }
    }

    // ---------- Her voice in one tap (1.11.0) ----------

    /** Everything small of her voice that's missing: her phrases (es, en) and her profiles, one after another. */
    private fun getVoice(done: (String?) -> Unit) {
        val steps = mutableListOf<((String?) -> Unit) -> Unit>()
        for (pack in CapriVoice.PACKS) if (CapriVoice.installed(pack.lang) == null) steps += { d -> CapriVoice.download(this, pack.lang, d) }
        if (!CapriNeural.profileInstalled(this, "es-MX") || !CapriNeural.profileInstalled(this, "en-US")) steps += { d -> CapriNeural.downloadProfiles(this, d) }
        fun next(i: Int) {
            if (i >= steps.size) return done(null)
            steps[i] { err -> if (err != null) done(err) else next(i + 1) }
        }
        next(0)
    }

    private fun voiceGetCard() = ui.card(Hud.YELLOW, Hud.MANE).apply {
        addView(ui.label(getString(R.string.cv_get_title)))
        val state = ui.text("", 13f, Hud.PAPER, ui.body).apply { setLineSpacing(0f, 1.2f) }
        addView(state, ui.block(6))
        val btn = ui.button(getString(R.string.cv_get), Ui.Kind.MAIN) { voiceJob(getString(R.string.dl_voice)) { getVoice(it) } }
        addView(btn, ui.block(10))
        addView(ui.text("", 13f, Hud.YELLOW, ui.mono).apply { visibility = View.GONE; dlViews += this }, ui.block(6))
        addView(ui.text(getString(R.string.cv_get_how), 12f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(8))
        val show = {
            val phrases = CapriVoice.PACKS.count { CapriVoice.installed(it.lang) != null }
            val profiles = CapriNeural.profileInstalled(this@MainActivity, "es-MX") && CapriNeural.profileInstalled(this@MainActivity, "en-US")
            val v = CapriNeural.variant(this@MainActivity)
            state.text = getString(R.string.cv_get_state, phrases, CapriVoice.PACKS.size,
                getString(if (profiles) R.string.cv_get_yes else R.string.cv_get_no),
                v?.let { getString(when (it.id) { "q4" -> R.string.cv_var_q4; "q8" -> R.string.cv_var_q8; else -> R.string.cv_var_bf16 }) } ?: getString(R.string.cv_get_noengine))
            btn.visibility = if (phrases == CapriVoice.PACKS.size && profiles) View.GONE else View.VISIBLE
        }
        show()
        choices.add(show)
    }

    /**
     * (1.11.0, Emma: "las apps te deben preguntar si quieres bajar los datos de voz de Capri al iniciar la
     * siguiente vez en la actualización"): once, the first time the app opens with this version, if her
     * voice isn't here yet. Nothing is downloaded without a yes. True if it asked.
     */
    /** (1.11.0) Once per tour (its id in tour.json), after an update: "¿Quieres conocer lo nuevo?", then her tour. */
    private fun tourOnce(force: Boolean = false): Boolean {
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        val (id, steps) = Tour.load(this) ?: return false
        if (!force && (wantsUpdate || prefs.getString("tourSeen", null) == id)) return false
        prefs.edit().putString("tourSeen", id).apply()
        val after = { if (!isFinishing) offerVoiceOnce() }
        val dialog = android.app.Dialog(this)
        val card = ui.card(Hud.YELLOW, Hud.MANE).apply {
            addView(ui.label(getString(R.string.tour_ask_title)))
            addView(ui.text(getString(R.string.tour_ask_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.2f) }, ui.block(6))
            addView(ui.button(getString(R.string.tour_yes), Ui.Kind.MAIN) {
                dialog.dismiss()
                tour = Tour(this@MainActivity, ui, shell, steps, ::tourTarget) { tour = null; show(TAB_CHAT); shell.postDelayed({ after() }, 400) }.also { it.start() }
            }, ui.block(12))
            addView(ui.button(getString(R.string.community_later), Ui.Kind.LINE) { dialog.dismiss(); after() }, ui.block(8))
        }
        val pad = FrameLayout(this).apply { setPadding(ui.dpi(4), ui.dpi(4), ui.dpi(10), ui.dpi(10)); addView(card) }
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(pad)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.9).toInt(), WRAP_CONTENT)
        dialog.show()
        return true
    }

    // (1.11.3, Emma: "añade un tutorial específico… no entienden cómo ponerle voz a su Capri") the voice
    // tutorial: the same guided view as the tour, its steps in assets/android/tour-voz.json, from Ajustes de voz,
    // Novedades and the offer to get her voice
    private val voiceSpots = HashMap<String, View>()
    private var tour: Tour? = null // (1.12.1) the guided view on screen, so Back closes it first

    private fun voiceTutorial() {
        val (_, steps) = Tour.load(this, "android/tour-voz.json") ?: return
        if (tour != null) return
        tour = Tour(this, ui, shell, steps, ::tourTarget) { tour = null; if (sub !== voice) openSub(voice) }.also { it.start() }
    }

    /** What a tour step lights up (its "target" in tour.json), with its tab on screen first. */
    private fun tourTarget(key: String): View? {
        val (kind, name) = key.split(":", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
        val tab = listOf("chat", "news", "community", "games", "funcs", "style", "settings")
        return when (kind) {
            "bar" -> pill
            "tab" -> tab.indexOf(if (name == "cmds") "community" else name).takeIf { it >= 0 }?.let { show(it, animate = false); tabs[it].button }
            "settings" -> { show(TAB_SETTINGS, animate = false); entries[name] }
            "voice" -> { val pg = spotPage[name] ?: voice; if (sub !== pg) openSub(pg, animate = false); voiceSpots[name] }
            else -> null
        }
    }

    private fun offerVoiceOnce(): Boolean {
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        if (wantsUpdate || prefs.getBoolean("voiceOffered", false)) return false
        prefs.edit().putBoolean("voiceOffered", true).apply()
        if (CapriVoice.PACKS.all { CapriVoice.installed(it.lang) != null } && CapriNeural.profileInstalled(this, "es-MX")) return false
        val dialog = android.app.Dialog(this)
        val card = ui.card(Hud.YELLOW, Hud.MANE).apply {
            addView(ui.label(getString(R.string.offer_title)))
            addView(ui.text(getString(R.string.offer_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.2f) }, ui.block(6))
            addView(ui.button(getString(R.string.offer_get), Ui.Kind.MAIN) { dialog.dismiss(); voiceJob(getString(R.string.dl_voice)) { getVoice(it) } }, ui.block(12))
            addView(ui.button(getString(R.string.offer_more), Ui.Kind.LINE) { dialog.dismiss(); openSub(voice) }, ui.block(8))
            addView(ui.button(getString(R.string.vt_open), Ui.Kind.LINE) { dialog.dismiss(); voiceTutorial() }, ui.block(8))
            addView(ui.button(getString(R.string.community_later), Ui.Kind.LINE) { dialog.dismiss() }, ui.block(8))
        }
        val pad = FrameLayout(this).apply { setPadding(ui.dpi(4), ui.dpi(4), ui.dpi(10), ui.dpi(10)); addView(card) }
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(pad)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.9).toInt(), WRAP_CONTENT)
        dialog.show()
        return true
    }

    /**
     * «Probar su voz»: one of her phrases, out loud. (1.11.3, Emma: "revisa que la voz sí funcione") It used to need
     * Capri running and said nothing when it couldn't; now it plays here even with Capri off, and says why if not.
     */
    private var preview: io.github.emmanuelquintana.voice.CapriVoiceClipPlayer? = null

    private fun testVoice() {
        val lang = CapriVoice.voiceLang(Lang.current(this)) ?: "es-MX"
        val lib = CapriVoice.installed(lang)
        val clip = lib?.listClips()?.let { all -> all.filter { it.eventId.startsWith("greeting") }.ifEmpty { all } }?.randomOrNull()
        val audio = getSystemService(android.media.AudioManager::class.java)
        val why = when {
            lib == null || clip == null -> R.string.cv_test_noclips
            !CapriVoice.clipsOn(this) -> R.string.cv_test_clipsoff
            !Store(this).configFlag("speak", true) -> R.string.cv_test_speakoff
            audio != null && audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) == 0 -> R.string.cv_test_muted
            else -> 0
        }
        if (why != 0) return run { Toast.makeText(this, why, Toast.LENGTH_LONG).show() }
        lib!!; clip!!
        val ears = CapriService.current?.ears
        if (ears != null) {
            Toast.makeText(this, clip.text, Toast.LENGTH_LONG).show()
            ears.say(clip.text, lang, org.json.JSONObject().put("id", "test").put("clip", clip.id).put("kind", "test"))
            return
        }
        // Capri isn't running: the phrase plays from here
        preview?.let { runCatching { it.stop() } }
        val store = Store(this)
        val player = io.github.emmanuelquintana.voice.CapriVoiceClipPlayer(lib)
        preview = player
        val res = runCatching {
            player.playEvent("test", clip.eventId, clip.language, clip.emotion,
                volume = store.configNumber("voiceClipVolume", 1.0).toFloat().coerceIn(0f, 1f),
                speed = store.configNumber("voiceClipSpeed", 1.0).toFloat().coerceIn(0.75f, 1.25f)) { ev ->
                when (ev.state.name) {
                    "VOICE_STARTED" -> Toast.makeText(this, ev.clip.text, Toast.LENGTH_LONG).show()
                    "VOICE_ERROR" -> Toast.makeText(this, getString(R.string.cv_err, ev.error ?: "?"), Toast.LENGTH_LONG).show()
                }
                if (ev.state.name in setOf("VOICE_FINISHED", "VOICE_CANCELLED", "VOICE_ERROR") && preview === player) preview = null
            }
        }
        res.exceptionOrNull()?.let { Toast.makeText(this, getString(R.string.cv_err, it.message ?: it.javaClass.simpleName), Toast.LENGTH_LONG).show() }
    }

    // ---------- Settings: what's general about her (1.9.0: you and her looks have their own tabs) ----------

    private fun settingsPage() = page {
        addView(ui.label(getString(R.string.tab_settings)))
        // (1.11.0, Emma: "en ajustes debe ser otro menú donde estén los ajustes agrupados en pantallas, clicables"):
        // only the menu here; each group is its own screen, with its way back
        addView(ui.text(getString(R.string.settings_intro), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
        // (1.12.0, Emma: "íconos de perfil, fotos de perfil, nombres… ahí lo presionas en lugar de ajustes personales")
        addView(profileCard().also { entries["personal"] = it }, ui.block(14))
        for (g in groups) if (g.key != "personal") addView(entry(g.icon, g.color, g.title, g.how) { openSub(g.page) }.also { entries[g.key] = it }, ui.block(10))
    }

    // you: your picture (or her icon) and your name, at the top of Ajustes; a tap opens Ajustes personales
    private val avatars = ArrayList<Profile.Avatar>()
    private var profileName: TextView? = null
    private fun profileCard() = ui.card(Hud.YELLOW, Hud.MANE).apply {
        val row = ui.row().apply { gravity = Gravity.CENTER_VERTICAL }
        val face = Profile.Avatar(this@MainActivity).also { avatars += it }
        row.addView(face, LinearLayout.LayoutParams(ui.dpi(64), ui.dpi(64)).apply { rightMargin = ui.dpi(16) })
        val texts = ui.col()
        val name = ui.text("", 20f, Hud.PAPER, ui.head).also { profileName = it }
        texts.addView(name)
        texts.addView(ui.text(getString(R.string.profile_edit) + " ›", 12f, Hud.YELLOW, ui.mono).apply { letterSpacing = 0.08f }, ui.block(4))
        row.addView(texts, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        addView(row)
        isClickable = true
        setOnClickListener { openSub(personal) }
        val draw = { name.text = CapriSettings.name(this@MainActivity).ifBlank { getString(R.string.profile_noname) }; avatars.forEach { it.reload() } }
        draw()
        choices.add(draw)
    }

    // your picture or one of her icons (Ajustes personales, at the top)
    private fun avatarPicker() = ui.card(Hud.YELLOW).apply {
        addView(ui.label(getString(R.string.profile_title)))
        val big = Profile.Avatar(this@MainActivity).also { avatars += it }
        addView(big, LinearLayout.LayoutParams(ui.dpi(96), ui.dpi(96)).apply { topMargin = ui.dpi(8); gravity = Gravity.CENTER_HORIZONTAL })
        addView(ui.button(getString(R.string.profile_photo) + " ›", Ui.Kind.GREEN) {
            runCatching {
                @Suppress("DEPRECATION")
                startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE), PICK_AVATAR)
            }
        }, ui.block(12))
        addView(ui.text(getString(R.string.profile_icons), 13f, Hud.PAPER_2, ui.body), ui.block(12))
        val picks = ArrayList<Profile.Avatar>()
        for (line in Profile.ICONS.indices.chunked(4)) {
            val r = ui.row()
            for (i in line) {
                val a = Profile.Avatar(this@MainActivity, i).apply {
                    isClickable = true
                    setOnClickListener {
                        Profile.setIcon(this@MainActivity, i)
                        picks.forEachIndexed { k, v -> v.picked = k == i }
                        avatars.forEach { it.reload() }
                    }
                }
                picks += a
                r.addView(a, LinearLayout.LayoutParams(0, ui.dpi(56), 1f).apply { rightMargin = ui.dpi(6) })
            }
            addView(r, ui.block(8))
        }
        val mark = { val c = Profile.choice(this@MainActivity).toIntOrNull(); picks.forEachIndexed { k, v -> v.picked = k == c } }
        mark()
        choices.add(mark)
    }

    private fun permsPage() = page {
        addView(backRow())
        addView(ui.label(getString(R.string.g_perms)))
        addView(ui.text(getString(R.string.g_perms_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
        // what she's allowed to do on your phone (1.9.0: here, no longer a tab of its own)
        addView(ui.card(Hud.GREEN).apply {
            addView(ui.label(getString(R.string.tab_perms)))
            // folded to a line (how many she has), open by itself while a needed one is missing
            val perms = Permissions.all(this@MainActivity)
            val summary = ui.text("", 15f, Hud.PAPER, ui.head)
            addView(summary, ui.block(6))
            val box = ui.col()
            var open = false
            val toggle = ui.button("", Ui.Kind.LINE) {}
            val showPerms = {
                val granted = perms.count { it.granted() }
                val missing = perms.any { it.required && !it.granted() }
                summary.text = getString(R.string.perms_summary, granted, perms.size)
                summary.setTextColor(if (missing) Hud.YELLOW else Hud.GREEN)
                box.visibility = if (open || missing) View.VISIBLE else View.GONE
                toggle.visibility = if (missing) View.GONE else View.VISIBLE
                toggle.text = getString(if (open) R.string.perms_hide else R.string.perms_show).uppercase()
            }
            toggle.setOnClickListener { open = !open; showPerms() }
            addView(toggle, ui.block(10))
            box.addView(ui.text(getString(R.string.perms_intro), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(6))
            for ((i, p) in perms.withIndex()) {
                val (view, refresh) = Permissions.row(this@MainActivity, ui, p)
                box.addView(view, ui.block(if (i == 0) 14 else 10))
                permRows.add(refresh)
            }
            // banking apps see her overlay and her notification access as risks: what to do about it
            box.addView(ui.text(getString(R.string.perms_bank_head), 14f, Hud.PAPER, ui.head), ui.block(16))
            box.addView(ui.text(getString(R.string.perms_bank), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            addView(box)
            showPerms()
            choices.add(showPerms)
        }, ui.block(16))

    }

    private fun aiLangPage() = page {
        addView(backRow())
        addView(ui.label(getString(R.string.g_ai)))
        addView(ui.text(getString(R.string.g_ai_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
        // the app's language, and hers (written in both, so it's found whatever the app is in)
        addView(ui.card(Hud.MANE).apply {
            addView(ui.label(getString(R.string.settings_lang)))
            // "Automático", then each language in its own name (a new one: Lang.LANGS + its strings),
            // three to a row so the names fit
            val langs = listOf(getString(R.string.lang_auto) to "auto") + Lang.LANGS.map { Lang.name(it) to it }
            for ((n, row) in langs.chunked(3).withIndex()) {
                addView(choiceText(row, { Lang.get(this@MainActivity) }, { a, b -> a == b }) {
                    if (it == Lang.get(this@MainActivity)) return@choiceText
                    Lang.set(this@MainActivity, it)
                    postDelayed({ recreate() }, 350)
                }, ui.block(if (n == 0) 8 else 4))
            }
        }, ui.block(16))

        // her AI
        addView(ui.card(Hud.GREEN).apply {
            addView(ui.label(getString(R.string.settings_ai)))
            aiText = ui.text("", 17f, Hud.PAPER, ui.head)
            addView(aiText, ui.block(6))
            addView(ui.text(getString(R.string.main_ai_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            // which of them can look at your screen: the question everyone asks
            addView(ui.text(getString(R.string.ai_sees), 13f, Hud.GREEN, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(8))
            addView(ui.button(getString(R.string.main_ai_pick) + " ›", Ui.Kind.GREEN) {
                startActivity(Intent(this@MainActivity, OnboardingActivity::class.java).putExtra(OnboardingActivity.EXTRA_ONLY, OnboardingActivity.PAGE_AI))
            }, ui.block(12))
            addView(ui.button(getString(R.string.settings_ai_panel) + " ›", Ui.Kind.LINE) { pickAi() }, ui.block(8))
        }, ui.block(16))

    }

    // (1.12.0, Emma: "los modos van en otro ajuste, que sea solo modos, y el pantalla solo pantalla")
    private fun screenPage() = page {
        addView(backRow())
        addView(ui.label(getString(R.string.g_screen)))
        addView(ui.text(getString(R.string.g_screen_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
        // her look at your screen
        addView(ui.card(Hud.GREEN).apply {
            addView(ui.label(getString(R.string.settings_look)))
            addView(ui.text(getString(R.string.settings_look_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(6))
            addView(ui.text(getString(R.string.ai_sees), 13f, Hud.GREEN, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(6))
            addView(ui.button(getString(R.string.settings_look_now) + " ›", Ui.Kind.GREEN) {
                if (!CapriSettings.tell(org.json.JSONObject().put("look", true))) Toast.makeText(this@MainActivity, R.string.settings_need_on, Toast.LENGTH_LONG).show()
                else moveTaskToBack(true) // out of the way: it's your screen she looks at
            }, ui.block(12))
            lookBtn = ui.button("", Ui.Kind.LINE) {
                val on = !(CapriService.current?.eyes?.live ?: false)
                if (!CapriSettings.tell(org.json.JSONObject().put("watchScreen", on))) Toast.makeText(this@MainActivity, R.string.settings_need_on, Toast.LENGTH_LONG).show()
                lookBtn.postDelayed({ refresh() }, 1500)
            }
            addView(lookBtn, ui.block(8))
            // (1.12.0, community: "una opción para aumentar el delay… el teclado y el globo de Capri se quedan en medio
            // de la pantalla y el escaneo se toma de inmediato") a wait before she takes the picture (look, translate)
            addView(ui.text(getString(R.string.settings_snap_delay), 14f, Hud.PAPER, ui.head), ui.block(14))
            addView(ui.text(getString(R.string.settings_snap_delay_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            addView(choiceText(listOf("0 s" to 0.0, "2 s" to 2.0, "4 s" to 4.0, "6 s" to 6.0),
                { Store(this@MainActivity).configNumber("snapDelay", 0.0) }, { a, b -> Math.abs(a - b) < 0.5 }) { CapriSettings.set(this@MainActivity, "snapDelay", it) }, ui.block(6))
            // (1.12.0) you shake the phone: she gets dizzy (shake.js)
            addView(ui.text(getString(R.string.settings_shake), 14f, Hud.PAPER, ui.head), ui.block(14))
            addView(ui.text(getString(R.string.settings_shake_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            addView(choiceText(listOf(getString(R.string.on) to true, getString(R.string.off) to false),
                { Store(this@MainActivity).configFlag("shake", true) }, { a, b -> a == b }) { CapriSettings.set(this@MainActivity, "shake", it) }, ui.block(6))
        }, ui.block(16))

        // (1.11.0) her pill by the clock and in Samsung's Now Bar (Android 16+, LiveBar.kt)
        if (LiveBar.available()) addView(ui.card(Hud.CYAN).apply {
            addView(ui.label(getString(R.string.live_title), Hud.CYAN))
            addView(ui.text(getString(R.string.live_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(6))
            addView(choice(listOf(R.string.live_on to true, R.string.live_off to false), { LiveBar.enabled(this@MainActivity) }, { x, y -> x == y }) {
                LiveBar.setEnabled(this@MainActivity, it)
            }, ui.block(10))
            addView(ui.button(getString(R.string.live_allow) + " ›", Ui.Kind.LINE) { LiveBar.settings(this@MainActivity) }, ui.block(10))
        }, ui.block(16))
    }

    private fun modesPage() = page {
        addView(backRow())
        addView(ui.label(getString(R.string.g_modes_only)))
        addView(ui.text(getString(R.string.g_modes_only_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
        // her modes: gamer (by hand or on its own), her tips while you play, streaming
        addView(ui.card(Hud.GREEN).apply {
            addView(ui.label(getString(R.string.settings_modes)))
            // gamer mode: small in a corner, untouchable, no cards, no ears (only while she's on)
            addView(ui.text(getString(R.string.settings_gamer), 14f, Hud.PAPER, ui.head), ui.block(10))
            addView(ui.text(getString(R.string.settings_gamer_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            addView(choice(listOf(R.string.off to false, R.string.on to true),
                { CapriService.gamer }, { a, b -> a == b }) {
                if (!CapriSettings.tell(org.json.JSONObject().put("gamer", it))) Toast.makeText(this@MainActivity, R.string.settings_need_on, Toast.LENGTH_LONG).show()
            }, ui.block(6))
            // …or on its own when a game is in front (needs Android's "Acceso de uso"; off unless you want it)
            addView(ui.text(getString(R.string.settings_gamer_auto), 13f, Hud.PAPER, ui.head), ui.block(12))
            addView(ui.text(getString(R.string.settings_gamer_auto_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            addView(choice(listOf(R.string.off to false, R.string.on to true),
                { GameWatch.enabled(this@MainActivity) && GameWatch.allowed(this@MainActivity) }, { a, b -> a == b }) {
                GameWatch.setEnabled(this@MainActivity, it)
                if (it && !GameWatch.allowed(this@MainActivity)) {
                    Toast.makeText(this@MainActivity, R.string.settings_gamer_auto_grant, Toast.LENGTH_LONG).show()
                    Vendor.usageAccess(this@MainActivity)
                }
            }, ui.block(6))
            // her tips while you play (1.9.0: coach.js)
            addView(ui.text(getString(R.string.settings_game_tips), 13f, Hud.PAPER, ui.head), ui.block(12))
            addView(ui.text(getString(R.string.settings_game_tips_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            addView(choice(listOf(R.string.off to false, R.string.on to true),
                { Store(this@MainActivity).configFlag("gameTips", true) }, { a, b -> a == b }) { CapriSettings.set(this@MainActivity, "gameTips", it) }, ui.block(6))
            // streaming mode: she reads your live's comments off your screen and answers them (1.9.0: stream.js)
            addView(ui.text(getString(R.string.settings_streaming), 14f, Hud.PAPER, ui.head), ui.block(14))
            addView(ui.text(getString(R.string.settings_streaming_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            addView(choice(listOf(R.string.off to false, R.string.on to true),
                { CapriService.running && Store(this@MainActivity).configFlag("streaming", false) }, { a, b -> a == b }) {
                if (!CapriService.running) Toast.makeText(this@MainActivity, R.string.settings_streaming_start, Toast.LENGTH_LONG).show()
                else CapriSettings.set(this@MainActivity, "streaming", it)
            }, ui.block(6))
        }, ui.block(16))

    }

    private fun phonePage() = page {
        addView(backRow())
        addView(ui.label(getString(R.string.g_phone)))
        addView(ui.text(getString(R.string.g_phone_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
        // Rendimiento (1.9.0, Perf.kt): modo ligero for older phones, automatic by default
        addView(ui.card().apply {
            addView(ui.label(getString(R.string.settings_perf)))
            addView(ui.text(getString(R.string.settings_perf_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            val now = ui.text("", 12f, Hud.PAPER_3, ui.mono)
            val showNow = {
                now.text = getString(R.string.settings_perf_now, getString(if (Perf.lite(this@MainActivity)) R.string.perf_is_lite else R.string.perf_is_full))
            }
            showNow()
            addView(choice(listOf(R.string.perf_auto to "auto", R.string.perf_lite to "lite", R.string.perf_full to "full"),
                { Perf.mode(this@MainActivity) }, { a, b -> a == b }) {
                val was = Perf.lite(this@MainActivity)
                CapriSettings.set(this@MainActivity, "perfMode", it)
                // her picture's pixels are set as her page starts: a fresh one, if it changes anything
                // (once her page has saved it: it's what the new one reads)
                if (Perf.lite(this@MainActivity, it) != was) now.postDelayed({ CapriService.current?.overlay?.reload() }, 800)
                now.postDelayed({ showNow() }, 400)
            }, ui.block(8))
            addView(now, ui.block(8))
        }, ui.block(16))

        // (1.11.0) on a Huawei or Honor: what those phones need for her to work well (Vendor.kt)
        if (Vendor.huawei) addView(ui.card(Hud.RED).apply {
            addView(ui.label(getString(R.string.huawei_title), Hud.RED))
            addView(ui.text(getString(R.string.huawei_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(6))
            addView(ui.button(getString(R.string.huawei_background) + " ›", Ui.Kind.GREEN) { Vendor.keepAlive(this@MainActivity) }, ui.block(12))
            addView(ui.text(getString(R.string.huawei_background_how), 12f, Hud.PAPER_3, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(6))
            addView(ui.button(getString(R.string.huawei_battery) + " ›", Ui.Kind.LINE) {
                Vendor.open(this@MainActivity, Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }, ui.block(10))
            addView(ui.button(getString(R.string.huawei_voice) + " ›", Ui.Kind.LINE) { Vendor.voices(this@MainActivity) }, ui.block(10))
            addView(ui.text(getString(R.string.huawei_voice_how), 12f, Hud.PAPER_3, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(6))
        }, ui.block(16))
        // anonymous statistics (1.9.0, Stats.kt): how many use her, nothing about you
        addView(ui.card().apply {
            addView(ui.label(getString(R.string.settings_stats)))
            addView(ui.text(getString(R.string.settings_stats_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            addView(choice(listOf(R.string.off to false, R.string.on to true),
                { Stats.enabled(this@MainActivity) }, { a, b -> a == b }) { Stats.setEnabled(this@MainActivity, it) }, ui.block(8))
        }, ui.block(16))

        addView(ui.button(getString(R.string.settings_intro_again), Ui.Kind.LINE) {
            // only her welcome again: her AI, its key and her settings stay as they are
            android.app.AlertDialog.Builder(this@MainActivity)
                .setTitle(R.string.settings_intro_again)
                .setMessage(R.string.settings_intro_again_how)
                .setPositiveButton(R.string.settings_intro_again_go) { _, _ ->
                    getSharedPreferences("app", MODE_PRIVATE).edit().putBoolean("welcomed", false).apply()
                    startActivity(Intent(this@MainActivity, OnboardingActivity::class.java))
                    finish()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }, ui.block(16))

    }

    private fun reportPage() = page {
        addView(backRow())
        addView(ui.label(getString(R.string.g_report)))
        addView(ui.text(getString(R.string.g_report_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
        addView(reportCard(), ui.block(16))
    }


    private fun reportCard() = ui.card(Hud.RED).apply {
        addView(ui.label(getString(R.string.settings_report), Hud.RED))
        addView(ui.text(getString(R.string.settings_report_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(6))
        val field = ui.field(getString(R.string.settings_report_hint)).apply {
            filters = arrayOf(android.text.InputFilter.LengthFilter(Report.MAX_TEXT))
            minLines = 3
            gravity = Gravity.TOP or Gravity.START
            isSingleLine = false
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        addView(field, ui.block(10))
        val state = ui.text("", 13f, Hud.PAPER_2, ui.mono).apply { visibility = View.GONE }
        lateinit var send: TextView
        send = ui.button(getString(R.string.settings_report_send) + " ›", Ui.Kind.GREEN) {
            val text = field.text.toString().trim()
            if (text.length < 5) return@button Toast.makeText(this@MainActivity, R.string.report_empty, Toast.LENGTH_SHORT).show()
            hideKeyboard()
            send.isEnabled = false
            send.alpha = 0.5f
            state.visibility = View.VISIBLE
            state.setTextColor(Hud.PAPER_2)
            state.text = getString(R.string.report_sending)
            Report.send(this@MainActivity, text) { r ->
                send.isEnabled = true
                send.alpha = 1f
                state.setTextColor(if (r == "ok") Hud.GREEN else Hud.RED)
                state.text = getString(when (r) {
                    "ok" -> R.string.report_ok
                    "rate" -> R.string.report_rate
                    "off" -> R.string.report_off
                    "net" -> R.string.report_net
                    else -> R.string.report_bad
                })
                if (r == "ok") field.setText("")
            }
        }
        addView(send, ui.block(10))
        addView(state, ui.block(8))
    }

    // (1.12.0) her beta phones: the whole log in a file you choose (to send it as it is), or its end to Emma's Discord
    private var logText = ""
    private fun saveLog() {
        Toast.makeText(this, R.string.account_working, Toast.LENGTH_SHORT).show()
        Threads.make {
            logText = Diag.phoneLog(this)
            runOnUiThread {
                val stamp = java.text.SimpleDateFormat("yyyyMMdd-HHmm", java.util.Locale.US).format(java.util.Date())
                runCatching {
                    @Suppress("DEPRECATION")
                    startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("text/plain")
                        .putExtra(Intent.EXTRA_TITLE, "capri-registro-$stamp.txt"), SAVE_LOG)
                }
            }
        }.start()
    }

    private fun sendLog() {
        Toast.makeText(this, R.string.account_working, Toast.LENGTH_SHORT).show()
        Threads.make {
            val log = Diag.phoneLog(this, compact = true)
            Report.send(this, "Registro de la beta", fullLog = log) { r ->
                Toast.makeText(this, if (r == "ok") "Registro enviado a Discord" else "No se pudo enviar ($r)", Toast.LENGTH_LONG).show()
            }
        }.start()
    }

    /** (1.12.0, Emma) before her design downloads: how it works, in a popup. */
    private fun skinSteps() {
        val dialog = android.app.Dialog(this)
        val card = ui.card(Hud.MANE, Hud.CYAN).apply {
            addView(ui.label(getString(R.string.skin_title), Hud.MANE))
            addView(ui.text(getString(R.string.skin_rules), 14f, Hud.PAPER, ui.body).apply { setLineSpacing(0f, 1.25f) }, ui.block(8))
            addView(ui.button(getString(R.string.skin_get) + " ›", Ui.Kind.GREEN) { dialog.dismiss(); saveTemplate() }, ui.block(14))
            addView(ui.button(getString(R.string.community_later), Ui.Kind.LINE) { dialog.dismiss() }, ui.block(8))
        }
        val pad = FrameLayout(this).apply { setPadding(ui.dpi(4), ui.dpi(4), ui.dpi(10), ui.dpi(10)); addView(card) }
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(pad)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.9).toInt(), WRAP_CONTENT)
        dialog.show()
    }

    private fun saveTemplate() = runCatching {
        @Suppress("DEPRECATION")
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/png")
            .putExtra(Intent.EXTRA_TITLE, "plantilla-capri.png"), SAVE_TEMPLATE)
    }

    private fun pickSkin() = runCatching {
        @Suppress("DEPRECATION")
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/png"), PICK_SKIN)
    }

    /** (1.12.0, Emma: "que me envíen el PNG de su traje para refinarlo") any PNG of the outfit; a big one is made
     *  smaller (same look) until it fits what the website takes (1.5 MB). */
    private fun sendSkin(uri: android.net.Uri) {
        var bytes = runCatching { contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
        val size = bytes?.let { b -> android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }.also { android.graphics.BitmapFactory.decodeByteArray(b, 0, b.size, it) } }
        if (bytes == null || size == null || size.outWidth <= 0 || size.outMimeType != "image/png" || bytes.size > 40_000_000) {
            Toast.makeText(this, R.string.skin_bad, Toast.LENGTH_LONG).show(); return
        }
        if (bytes.size > 1_500_000) bytes = runCatching {
            var bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes!!.size)
            var out: ByteArray
            do {
                val w = (bmp.width * 0.8f).toInt().coerceAtLeast(1)
                val h = (bmp.height * 0.8f).toInt().coerceAtLeast(1)
                bmp = android.graphics.Bitmap.createScaledBitmap(bmp, w, h, true)
                out = java.io.ByteArrayOutputStream().also { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
            } while (out.size > 1_500_000 && bmp.width > 200)
            out
        }.getOrNull()
        if (bytes == null || bytes.size > 1_500_000) { Toast.makeText(this, R.string.skin_bad, Toast.LENGTH_LONG).show(); return }
        val name = ui.field(getString(R.string.skin_name_hint)).apply { filters = arrayOf(android.text.InputFilter.LengthFilter(80)) }
        val box = ui.col().apply { setPadding(ui.dpi(20), ui.dpi(8), ui.dpi(20), 0); addView(name) }
        android.app.AlertDialog.Builder(this).setTitle(R.string.skin_send).setView(box)
            .setPositiveButton(R.string.settings_report_send) { _, _ ->
                Toast.makeText(this, R.string.report_sending, Toast.LENGTH_SHORT).show()
                Report.sendSkin(this, bytes, name.text.toString().ifBlank { "skin" }) { r ->
                    Toast.makeText(this, if (r == "ok") R.string.skin_ok else when (r) { "rate" -> R.string.report_rate; "net" -> R.string.report_net; "off" -> R.string.report_off; else -> R.string.report_bad }, Toast.LENGTH_LONG).show()
                }
            }.setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun showVoice() {
        if (!::voiceState.isInitialized) return
        val said = Ears.heardLast.take(80)
        val s = Ears.status
        voiceState.text = when {
            !Ears.enabled(this) -> ""
            !CapriService.running -> getString(R.string.voice_starting)
            s == "listening" -> getString(R.string.voice_listening)
            s == "heard" -> getString(R.string.voice_heard, said)
            s == "other" -> getString(R.string.voice_other, said)
            s == "moved" -> getString(R.string.voice_moved, said)
            s == "far" -> getString(R.string.voice_far)
            s == "busy" -> getString(R.string.voice_busy)
            s == "media" -> getString(R.string.voice_media)
            s == "silenced" -> getString(R.string.voice_silenced)
            s == "gamer" -> getString(R.string.voice_gamer)
            s == "nomic" -> getString(R.string.voice_nomic)
            s == "recmic" -> getString(R.string.voice_recmic)
            s == "screenoff" -> getString(R.string.voice_screenoff)
            s == "norecognizer" -> getString(R.string.voice_norecognizer)
            s.startsWith("error:") -> getString(R.string.voice_error, s.removePrefix("error:"))
            else -> getString(R.string.voice_starting)
        }
        voiceState.visibility = if (voiceState.text.isEmpty()) View.GONE else View.VISIBLE
        if (::passiveState.isInitialized) {
            val lang = Lang.current(this).takeIf { it in WakeWord.Models.NAMES } ?: "en"
            val ready = WakeWord.Models.installed(this, lang)
            val m = WakeWord.Models
            passiveState.text = when {
                ready -> getString(R.string.ears_passive_ready)
                m.busy.isNotEmpty() -> getString(R.string.ears_passive_busy, maxOf(0, m.progress))
                m.lastError.isNotEmpty() -> getString(R.string.ears_passive_failed, m.lastError)
                else -> ""
            }
            passiveState.visibility = if (passiveState.text.isEmpty()) View.GONE else View.VISIBLE
            passiveBtn.visibility = if (ready || m.busy.isNotEmpty()) View.GONE else View.VISIBLE
        }
    }


    private fun saveName() {
        val n = nameField.text.toString().trim()
        hideKeyboard()
        if (n.isEmpty()) return
        CapriSettings.setName(this, n)
        Toast.makeText(this, getString(R.string.settings_name_saved, n), Toast.LENGTH_SHORT).show()
    }

    // ---------- state ----------

    // the things she remembers about you, each with a ✕ to forget it, and "forget everything"
    private fun showMemory(fresh: Boolean = false) {
        val facts = CapriSettings.facts(this)
        if (!fresh && facts == shownFacts) return
        shownFacts = facts
        memoryBox.removeAllViews()
        if (facts.isEmpty()) {
            memoryBox.addView(ui.text(getString(R.string.settings_memory_none), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) })
            return
        }
        val later = { memoryBox.postDelayed({ showMemory(true) }, 500) } // her page saves it first
        for (fact in facts) {
            val row = ui.row()
            row.addView(ui.text("• $fact", 13f, Hud.PAPER, ui.body).apply { setLineSpacing(0f, 1.15f) }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            row.addView(ui.text("✕", 15f, Hud.RED, ui.mono).apply {
                setPadding(ui.dpi(14), ui.dpi(6), ui.dpi(4), ui.dpi(6))
                contentDescription = getString(R.string.settings_memory_forget_one)
                setOnClickListener {
                    CapriSettings.forget(this@MainActivity, fact)
                    row.alpha = 0.3f
                    later()
                }
            })
            memoryBox.addView(row, ui.block(6))
        }
        memoryBox.addView(ui.button(getString(R.string.settings_memory_forget), Ui.Kind.LINE) {
            // (1.12.0, community: "que salga una confirmación al tocar el botón de borrar los recuerdos")
            android.app.AlertDialog.Builder(this).setMessage(R.string.settings_memory_forget_sure)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.settings_memory_forget) { _, _ ->
                    CapriSettings.forget(this, null)
                    Toast.makeText(this, R.string.settings_memory_forgot, Toast.LENGTH_SHORT).show()
                    later()
                }.show()
        }, ui.block(12))
    }

    private fun refresh() {
        if (!::ui.isInitialized) return
        showMemory()
        chat.status()
        sleepDraw()
        neuroDraw()
        val running = CapriService.running
        statusDot.background = Hud.Plate(if (running) Hud.GREEN else Hud.RED, 0, cut = 0f, stroke = 0f, offset = 0f)
        statusWord.text = getString(if (running) R.string.main_status_on else R.string.main_status_off)
        statusWord.setTextColor(if (running) Hud.GREEN else Hud.RED)
        statusText.text = getString(if (running) R.string.main_running else R.string.main_idle)
        startBtn.text = getString(if (running) R.string.main_stop else R.string.main_start).uppercase()
        startBtn.alpha = if (Settings.canDrawOverlays(this)) 1f else 0.5f
        startBtn.setTextColor(if (running) Hud.YELLOW else Hud.INK)
        startBtn.background = if (running) Hud.Plate(Hud.INK, Hud.YELLOW, Hud.RED, cut = ui.dp(14f), stroke = ui.dp(2f), offset = ui.dp(5f))
        else Hud.Plate(Hud.YELLOW, Hud.MANE, Hud.MANE, cut = ui.dp(14f), stroke = 0f, offset = ui.dp(5f))
        permRows.forEach { it() }
        aiText.text = getString(R.string.settings_ai_now, when (val p = CapriSettings.provider(this)) {
            "pollinations" -> getString(R.string.main_ai_free)
            "ollama" -> getString(R.string.main_ai_pc) + " · " + CapriSettings.ollamaHost(this)
            "custom" -> getString(R.string.main_ai_custom)
            else -> CapriSettings.KEYED.firstOrNull { it.id == p }?.name ?: p
        })
        if (!nameField.hasFocus()) nameField.setText(CapriSettings.name(this))
        val voice = Ears.enabled(this)
        voiceText.text = getString(when {
            voice && !Ears.allowed(this) -> R.string.settings_voice_nomic
            voice -> R.string.settings_voice_on
            else -> R.string.settings_voice_off
        })
        voiceText.setTextColor(if (voice && Ears.allowed(this)) Hud.GREEN else Hud.PAPER)
        voiceBtn.text = getString(if (voice) R.string.settings_voice_turn_off else R.string.settings_voice_turn_on).uppercase()
        showVoice()
        choices.forEach { it() }
        lookBtn.text = getString(if (CapriService.current?.eyes?.live == true) R.string.settings_look_live_off else R.string.settings_look_live_on).uppercase()
    }

    // ---------- Community ----------

    // ---------- Games (1.11.0: their own tab) ----------

    private fun gamesPage() = page {
        addView(ui.label(getString(R.string.tab_games)))
        addView(ui.text(getString(R.string.you_games_how), 14f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.18f) }, ui.block(8))
        addView(ui.button(getString(R.string.games_open), Ui.Kind.MAIN) { PageActivity.open(this@MainActivity, "games") }, ui.block(16))
        // each game, straight in
        val games = listOf("chess" to "♞", "mines" to "✹", "cards" to "♠", "ttt" to "✕", "pong" to "◐", "rps" to "✊", "c4" to "◉", "memo" to "▣", "simon" to "◈", "react" to "⚡", "guess" to "#", "hilo" to "⇅")
        val names = resources.getStringArray(R.array.game_names)
        val fresh = setOf("react", "guess", "hilo") // (1.12.0)
        var row: LinearLayout? = null
        for ((i, g) in games.withIndex()) {
            if (i % 3 == 0) row = ui.row().also { addView(it, ui.block(10)) }
            val (id, icon) = g
            val cell = ui.col().apply {
                gravity = Gravity.CENTER
                background = ui.plate(if (id in fresh) Hud.GREEN else Hud.LINE_STRONG)
                setPadding(ui.dpi(6), ui.dpi(14), ui.dpi(6), ui.dpi(12))
                isClickable = true
                setOnClickListener { PageActivity.open(this@MainActivity, "games", id) }
                addView(ui.text(icon, 26f, if (id in fresh) Hud.GREEN else Hud.YELLOW, ui.head).apply { gravity = Gravity.CENTER })
                addView(ui.text(names.getOrElse(i) { id }, 12f, Hud.PAPER, ui.mono).apply { gravity = Gravity.CENTER; maxLines = 2 }, ui.block(6))
                if (id in fresh) addView(ui.text(getString(R.string.games_new), 9f, Hud.GREEN, ui.mono).apply { gravity = Gravity.CENTER; letterSpacing = 0.12f }, ui.block(2))
            }
            row?.addView(cell, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { rightMargin = if (i % 3 < 2) ui.dpi(8) else 0 })
        }
        // (1.11.0, Emma: "cuando juegues en la app debe desaparecer la Capri de encima, como opcional")
        addView(ui.card(Hud.CYAN).apply {
            addView(ui.label(getString(R.string.games_hide_title), Hud.CYAN))
            addView(ui.text(getString(R.string.games_hide_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(6))
            addView(choice(listOf(R.string.games_hide_on to true, R.string.games_hide_off to false),
                { PageActivity.hideWhilePlaying(this@MainActivity) }, { x, y -> x == y }) { PageActivity.setHideWhilePlaying(this@MainActivity, it) }, ui.block(10))
        }, ui.block(18))
    }

    // ---------- Comunidad (a tab again in 1.11.0): Discord, TikTok and supporting Capri ----------

    private fun communityPage() = page {
        cmdsInto(this)
    }

    // (1.12.0, Emma) Funciones: what she does for you every day, out of Ajustes: her sleep mode, your classes and
    // calendar, and her care (battery, breaks)
    private fun funcsPage() = page {
        addView(ui.label(getString(R.string.tab_funcs)))
        addView(ui.text(getString(R.string.funcs_intro), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
        addView(sleepCard(), ui.block(14))
        addView(agendaCard(), ui.block(16))
        funcsCare()
    }

    /** (1.12.0) The voice commands (their own tab), scrolled to. */
    private fun showCmds() {
        show(TAB_CMDS)
        val top = cmdsTop ?: return
        (tabs[TAB_CMDS].page as? ScrollView)?.post { (tabs[TAB_CMDS].page as? ScrollView)?.smoothScrollTo(0, maxOf(0, top.top - ui.dpi(12))) }
    }

    // Emma's TikTok and Discord, as a card (the Comunidad tab, and the one-time invite)
    private fun communityCard(onOpen: () -> Unit = {}) = ui.card(Hud.RED).apply {
        addView(ui.label(getString(R.string.community_title), Hud.RED))
        addView(ui.text(getString(R.string.community_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(6))
        addView(ui.button(getString(R.string.community_discord) + " ↗", Ui.Kind.MAIN) { onOpen(); openLink(DISCORD) }, ui.block(10))
        addView(ui.button(getString(R.string.community_tiktok) + " ↗", Ui.Kind.LINE) { onOpen(); openLink(TIKTOK) }, ui.block(8))
        addView(ui.button(getString(R.string.community_x) + " ↗", Ui.Kind.LINE) { onOpen(); openLink(X) }, ui.block(8))
    }

    private fun openLink(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    // once, ever: "join the community" (and then it lives in the Comunidad tab)
    private fun inviteOnce() {
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        if (wantsUpdate) return
        if (prefs.getBoolean("communityShown", false)) return supportMaybe()
        prefs.edit().putBoolean("communityShown", true).apply()
        val dialog = android.app.Dialog(this)
        val card = communityCard { dialog.dismiss() }
        card.addView(ui.button(getString(R.string.community_later), Ui.Kind.LINE) { dialog.dismiss() }, ui.block(8))
        val pad = FrameLayout(this).apply { setPadding(ui.dpi(4), ui.dpi(4), ui.dpi(10), ui.dpi(10)); addView(card) }
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(pad)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.9).toInt(), WRAP_CONTENT)
        dialog.show()
    }

    // (1.10.1, Emma: "una ventana flotante: ¿quieres apoyar a Capri?, con mi PayPal, que diga en grande «no
    // mostrar otra vez» y que no sea molesto"): never on the first days, at most once every two weeks, never
    // on top of an update, and gone for good with one tap (or once you've opened PayPal).
    private fun supportMaybe() {
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        if (prefs.getBoolean("supportNever", false)) return
        val now = System.currentTimeMillis()
        val first = prefs.getLong("supportFirst", 0L)
        if (first == 0L) return prefs.edit().putLong("supportFirst", now).apply()
        val day = 24 * 60 * 60 * 1000L
        if (now - first < 3 * day || now - prefs.getLong("supportAt", 0L) < 14 * day) return
        prefs.edit().putLong("supportAt", now).apply()
        showSupport(asked = false)
    }

    /** The PayPal window: on its own now and then (supportMaybe), or from DONAR (asked: no «No mostrar otra vez»). */
    private fun showSupport(asked: Boolean) {
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        val never = { prefs.edit().putBoolean("supportNever", true).apply() }
        val dialog = android.app.Dialog(this)
        val card = ui.card(Hud.YELLOW).apply {
            addView(ui.label(getString(R.string.support_title), Hud.YELLOW))
            addView(ui.text(getString(R.string.support_text), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(6))
            addView(ui.button(getString(R.string.support_paypal) + " ↗", Ui.Kind.MAIN) { never(); dialog.dismiss(); openLink(PAYPAL) }, ui.block(12))
            if (!asked) addView(ui.button(getString(R.string.support_never), Ui.Kind.GREEN) { never(); dialog.dismiss() }.apply { textSize = 17f }, ui.block(10)) // (in big letters)
            addView(ui.button(getString(R.string.community_later), Ui.Kind.LINE) { dialog.dismiss() }, ui.block(8))
        }
        val pad = FrameLayout(this).apply { setPadding(ui.dpi(4), ui.dpi(4), ui.dpi(10), ui.dpi(10)); addView(card) }
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(pad)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.9).toInt(), WRAP_CONTENT)
        dialog.show()
    }

    /** Her beta code was just accepted in the chat (ChatTab): the switch shows up and the channel is checked. */
    fun betaChanged() {
        release = null
        checkUpdate()
    }

    private fun showBeta() {
        if (!::betaBox.isInitialized) return
        val unlocked = Beta.unlocked(this)
        betaBox.visibility = if (unlocked) View.VISIBLE else View.GONE
        if (!unlocked) return
        val on = Beta.on(this)
        betaText.text = if (on) "Canal beta: ACTIVO. Te llegan las compilaciones de next antes de publicarlas (y la versión de todos en cuanto salga)."
            else "Canal beta: apagado. Solo la versión de todos."
        betaBtn.text = (if (on) "Volver a la versión de todos" else "Recibir betas").uppercase()
    }

    private fun checkUpdate() {
        showBeta()
        if (!Updater.ENABLED || downloading) return
        if (release == null) updateText.text = getString(R.string.update_checking)
        Threads.make {
            val r = Updater.latest(this)
            runOnUiThread { showUpdate(r) }
        }.start()
    }

    private fun showUpdate(r: Updater.Release?) {
        if (downloading) return
        release = r?.takeIf { Updater.isNewer(it.tag) }
        val mine = Beta.shown(this)
        updateText.text = when {
            r == null -> getString(R.string.update_offline, mine)
            release != null -> getString(R.string.update_new, r.version, mine) + if (r.notes.isNotBlank()) "\n\n" + r.notes else ""
            // a beta stays until the release of its version (or a newer one) is out: Android never goes back a version
            Beta.running && Beta.unlocked(this) && !Beta.on(this) -> getString(R.string.update_latest, mine) +
                "\n\nTienes una beta. Volverás a la versión de todos en cuanto salga la ${mine.substringBefore("-beta.")} o una más nueva (Android no deja bajar de versión sin desinstalar)."
            else -> getString(R.string.update_latest, mine)
        }
        updateText.setTextColor(if (release != null) Hud.YELLOW else Hud.PAPER_2)
        updateBtn.visibility = if (release != null) View.VISIBLE else View.GONE
        updateBtn.text = getString(R.string.update_go).uppercase() + " ›"
        if (wantsUpdate && release != null) {
            wantsUpdate = false
            update()
        }
    }

    companion object {
        const val EXTRA_UPDATE = "update"
        const val EXTRA_TAB = "tab"
        const val TAB_CHAT = 0
        private const val PICK_VOICE = 4711 // (1.11.0) a Capri Voice pack from your files
        private const val PICK_MODEL = 4712 // (V8.5) one of the neural engine's .gguf files
        private const val PICK_PROFILE = 4713 // (V8.5) the private profiles ZIP
        private const val PICK_AVATAR = 4718 // (1.12.0) your profile picture
        private const val SAVE_LOG = 4717 // (1.12.0) her beta phones' log, saved where you choose
        private const val SAVE_TEMPLATE = 4716 // (1.12.0) her skin template, saved where you choose
        private const val PICK_SKIN = 4715 // (1.12.0) a community skin, to Emma
        private const val PICK_FOR_HER = 4714 // (1.12.0) a file for her, from the Chat tab
        const val TAB_NEWS = 1
        const val TAB_COMMUNITY = 2
        const val TAB_GAMES = 3
        const val TAB_CMDS = 2 // (1.12.0: the tab is only her commands now)
        const val TAB_FUNCS = 4 // (1.12.0) Funciones: her sleep mode, your classes, her care
        const val TAB_SETTINGS = 6
        private const val TABS = 7
        const val EXTRA_SUB = "sub"
        private const val TIKTOK = "https://www.tiktok.com/@emma180516_"
        private const val X = "https://x.com/kiritor23"
        private const val DISCORD = "https://discord.gg/hmh783tfn"
        private const val PAYPAL = "https://paypal.me/Joseemmanuelq"
    }

    /** Download it and hand it to Android's installer (after letting Capri install apps, once). */
    private fun update() {
        val r = release ?: return
        if (downloading) return
        if (!packageManager.canRequestPackageInstalls()) {
            Toast.makeText(this, R.string.update_allow, Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
            return
        }
        downloading = true
        updateBtn.isEnabled = false
        Threads.make {
            try {
                var shown = -1
                val title = getString(R.string.dl_update, r.version)
                DownloadNote.progress(this, title, -1f)
                val apk = try {
                    Updater.download(this, r) { k ->
                        val pct = (k * 100).toInt()
                        if (pct != shown) {
                            shown = pct
                            DownloadNote.progress(this, title, k)
                            runOnUiThread { updateBtn.text = getString(R.string.update_downloading, pct) }
                        }
                    }.also { DownloadNote.done(this, title, null) }
                } catch (e: Exception) { DownloadNote.done(this, title, e.message ?: "?"); throw e }
                Updater.install(this, apk)
                runOnUiThread { updateBtn.text = getString(R.string.update_installing) }
            } catch (e: Exception) {
                Diag.add("E", "update: ${e.message}")
                runOnUiThread {
                    // it didn't work from here: the browser, as before
                    Toast.makeText(this, R.string.update_failed, Toast.LENGTH_LONG).show()
                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(r.page)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
            } finally {
                downloading = false
                runOnUiThread { updateBtn.isEnabled = true }
            }
        }.start()
    }

    // 1.8: her wardrobe, shown off. Her looks one after another on a little stage (assets/android/showcase,
    // drawn by her own shader), what's in it, how to open it, and a button that opens it on her.
    private fun wardrobeCard() = ui.card(Hud.YELLOW, Hud.MANE).apply {
        val top = ui.row()
        top.addView(ui.label(getString(R.string.closet_tag), Hud.YELLOW), LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        top.addView(ui.text(getString(R.string.closet_new), 10f, Hud.INK, ui.mono).apply {
            letterSpacing = 0.12f
            background = Hud.Plate(Hud.GREEN, Hud.GREEN, cut = ui.dp(5f), stroke = 0f, offset = 0f)
            setPadding(ui.dpi(8), ui.dpi(4), ui.dpi(10), ui.dpi(4))
        })
        addView(top)
        addView(ui.text(getString(R.string.closet_title), 26f, Hud.PAPER, ui.head).apply { includeFontPadding = false }, ui.block(10))

        // the stage: one look after another, each coming in with her glitch
        val looks = runCatching { assets.list("android/showcase")?.filter { it.endsWith(".webp") }?.sorted() }.getOrNull().orEmpty()
        if (looks.isNotEmpty()) {
            val stage = FrameLayout(this@MainActivity).apply {
                background = Hud.Plate(Hud.INK, Hud.LINE_STRONG, cut = ui.dp(12f), stroke = ui.dp(1f), offset = 0f)
                setPadding(ui.dpi(6), ui.dpi(10), ui.dpi(6), ui.dpi(6))
            }
            val pic = ImageView(this@MainActivity).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
            stage.addView(pic, FrameLayout.LayoutParams(MATCH_PARENT, ui.dpi(250)))
            val count = ui.text("", 10f, Hud.PAPER_3, ui.mono).apply { letterSpacing = 0.12f }
            stage.addView(count, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.BOTTOM or Gravity.END).apply {
                rightMargin = ui.dpi(6); bottomMargin = ui.dpi(2)
            })
            var i = 0
            fun show() {
                val bmp = runCatching { assets.open("android/showcase/" + looks[i]).use { BitmapFactory.decodeStream(it) } }.getOrNull()
                if (bmp != null) pic.setImageBitmap(bmp)
                count.text = String.format("%02d / %02d", i + 1, looks.size)
                ui.glitchIn(pic)
            }
            show()
            val next = object : Runnable {
                override fun run() {
                    if (!pic.isAttachedToWindow) return
                    if (pic.isShown) { i = (i + 1) % looks.size; show() }
                    pic.postDelayed(this, 2200)
                }
            }
            pic.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) { pic.removeCallbacks(next); pic.postDelayed(next, 2200) }
                override fun onViewDetachedFromWindow(v: View) { pic.removeCallbacks(next) }
            })
            stage.isClickable = true
            stage.setOnClickListener { i = (i + 1) % looks.size; show() } // a tap: the next one now
            addView(stage, ui.block(12))
        }

        // what's in it, in three numbers
        val stats = ui.row()
        for ((n, word) in listOf("32" to R.string.closet_outfits, "25" to R.string.closet_things, "30" to R.string.closet_anims)) {
            stats.addView(ui.col().apply {
                gravity = Gravity.CENTER_HORIZONTAL
                background = Hud.Plate(Hud.INK_2, Hud.LINE_STRONG, cut = ui.dp(6f), stroke = ui.dp(1f), offset = 0f)
                setPadding(ui.dpi(4), ui.dpi(8), ui.dpi(4), ui.dpi(8))
                addView(ui.text(n, 24f, Hud.YELLOW, ui.head).apply { gravity = Gravity.CENTER; includeFontPadding = false })
                addView(ui.text(getString(word), 11f, Hud.PAPER_2, ui.mono).apply { gravity = Gravity.CENTER })
            }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { if (stats.childCount > 0) leftMargin = ui.dpi(8) })
        }
        addView(stats, ui.block(12))

        addView(ui.text(getString(R.string.closet_how), 14f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.18f) }, ui.block(12))
        addView(ui.text(getString(R.string.closet_alive), 14f, Hud.GREEN, ui.body).apply { setLineSpacing(0f, 1.18f) }, ui.block(8))
        addView(ui.text(getString(R.string.closet_where), 12f, Hud.YELLOW, ui.mono).apply { setLineSpacing(0f, 1.18f) }, ui.block(10))
        // two ways to dress her (1.8.1): her own screen in the app, or over your screen as she is
        addView(ui.button(getString(R.string.closet_open) + " ›", Ui.Kind.MAIN) { startActivity(android.content.Intent(this@MainActivity, ClosetActivity::class.java)) }, ui.block(14))
        addView(ui.button(getString(R.string.closet_on_screen), Ui.Kind.LINE) { openWardrobe() }, ui.block(8))
    }

    /** Her wardrobe, open on her over whatever is on screen: she starts first if she isn't on yet. */
    private fun openWardrobe() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, R.string.ob_need_overlay, Toast.LENGTH_LONG).show()
            show(TAB_SETTINGS)
            return
        }
        if (CapriService.running) CapriService.send(this, CapriService.ACTION_WARDROBE)
        else CapriService.start(this, CapriService.ACTION_WARDROBE)
        moveTaskToBack(true)
    }

    /** Her setup panel, over whatever is on screen: she starts first if she isn't on yet. */
    private fun pickAi() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, R.string.ob_need_overlay, Toast.LENGTH_LONG).show()
            return
        }
        if (CapriService.running) CapriService.send(this, CapriService.ACTION_SETUP)
        else CapriService.start(this, CapriService.ACTION_SETUP)
        moveTaskToBack(true)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(InputMethodManager::class.java) ?: return
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0); it.clearFocus() }
    }

    /** Her display face for part of a line (TypefaceSpan(Typeface) needs Android 9). */
    private class FaceSpan(private val face: android.graphics.Typeface) : MetricAffectingSpan() {
        override fun updateDrawState(tp: TextPaint) { tp.typeface = face }
        override fun updateMeasureState(tp: TextPaint) { tp.typeface = face }
    }
}
