package io.github.emmanuelquintana.capri

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
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
    private lateinit var logText: TextView
    private lateinit var logBox: LinearLayout

    private class Tab(val icon: Ui.Icon, val label: TextView, val button: LinearLayout, val page: View)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (OnboardingActivity.needed(this)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }
        UpdateJob.schedule(this)
        wantsUpdate = intent?.getBooleanExtra(EXTRA_UPDATE, false) == true
        window.statusBarColor = Hud.INK
        window.navigationBarColor = Hud.INK
        ui = Ui(this)

        val root = FrameLayout(this).apply { background = Hud.Backdrop(ui.dp(3f)) }
        content = FrameLayout(this)
        root.addView(content, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        // 1.9.0 (Emma: "necesitamos organizar mejor la UI"): you, her looks and the general settings
        // each in its own tab; the permissions moved into Ajustes
        val pages = listOf(newsPage(), cmdsPage(), youPage(), stylePage(), settingsPage(), communityPage())
        val bar = ui.row().apply {
            background = Ui.Pill(Hud.INK, Hud.MANE, ui.dp(1.5f))
            setPadding(ui.dpi(6), ui.dpi(6), ui.dpi(6), ui.dpi(6))
            elevation = ui.dp(6f)
        }
        val labels = listOf(R.string.tab_news to "spark", R.string.tab_cmds to "mic", R.string.tab_you to "person", R.string.tab_style to "hanger", R.string.tab_settings to "sliders", R.string.tab_community to "people")
        for ((i, pair) in labels.withIndex()) {
            val (label, kind) = pair
            val icon = Ui.Icon(kind, Hud.PAPER_2)
            val btn = ui.row().apply {
                gravity = Gravity.CENTER
                setPadding(ui.dpi(11), ui.dpi(10), ui.dpi(11), ui.dpi(10)) // (six tabs: they fit a 360 dp phone)
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
        val barParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
            bottomMargin = ui.dpi(16)
        }
        root.addView(bar, barParams)
        // Android 15 draws the app under the status and navigation bars: her pages start below the
        // first and the pill bar sits above the second
        ui.underSystemBars(root) { top, bottom ->
            content.setPadding(0, top, 0, 0)
            barParams.bottomMargin = ui.dpi(16) + bottom
            bar.layoutParams = barParams
        }
        setContentView(root)
        show(savedInstanceState?.getInt("tab") ?: tabAsked(intent) ?: 0, animate = false)
        if (savedInstanceState == null && tabAsked(intent) == null) root.postDelayed({ if (!isFinishing) inviteOnce() }, 1500)
    }

    // a tab to open on ("tab", 0..4): the emulator's screenshots ask for one
    private fun tabAsked(intent: Intent?) = intent?.getIntExtra(EXTRA_TAB, -1)?.takeIf { it in 0 until TABS }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("tab", current)
    }

    /** One tab on: its icon and label in ink on a yellow capsule, the others quiet. */
    private fun show(i: Int, animate: Boolean = true) {
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
        if (animate) {
            ui.slideIn(tabs[i].page)
            content.removeCallbacks(refreshLater)
            content.postDelayed(refreshLater, 200)
        } else refresh()
    }
    private val refreshLater = Runnable { if (!isFinishing) refresh() }

    override fun onResume() {
        super.onResume()
        if (!::ui.isInitialized) return
        Diag.onChange = { runOnUiThread { showLog() } }
        // back in the app: her service can take the microphone again if her ears are on (Android
        // only lets it while the app is in front)
        if (Ears.enabled(this)) CapriService.current?.voiceChanged()
        refresh()
        checkUpdate()
        Stats.maybePing(this)
        ticker.post(tick)
    }

    override fun onPause() {
        super.onPause()
        Diag.onChange = null
        ticker.removeCallbacks(tick)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        // the microphone just allowed from "Turn on": her ears go on with it
        if (requestCode == Permissions.MIC && wantVoice && Ears.allowed(this)) Ears.setEnabled(this, true)
        wantVoice = false
        CapriService.current?.voiceChanged()
        refresh()
    }

    private var wantVoice = false
    private var wantsUpdate = false // opened from the "new version" notification: straight into it

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_UPDATE, false)) {
            wantsUpdate = true
            if (::ui.isInitialized) show(0)
        } else tabAsked(intent)?.let { if (::ui.isInitialized) show(it) }
    }

    private fun toggleVoice() {
        if (Ears.enabled(this)) return Ears.setEnabled(this, false).also { refresh() }
        if (!android.speech.SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, R.string.settings_voice_none, Toast.LENGTH_LONG).show()
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
            addView(ui.text("ANDROID // v${BuildConfig.VERSION_NAME}", 10f, Hud.PAPER_3, ui.mono).apply {
                letterSpacing = 0.1f
                // seven taps on the version: the developer's log shows up in Settings (or goes again)
                var taps = 0
                setOnClickListener {
                    if (++taps < 7) return@setOnClickListener
                    taps = 0
                    val on = !devMode()
                    getSharedPreferences("app", MODE_PRIVATE).edit().putBoolean("dev", on).apply()
                    Toast.makeText(this@MainActivity, if (on) R.string.dev_on else R.string.dev_off, Toast.LENGTH_SHORT).show()
                    refresh()
                }
            })
        }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        addView(header)
        // a WebView too old for her page (1.9.0): she can't show up at all until it's updated
        if (Perf.webViewTooOld()) addView(ui.card(Hud.RED).apply {
            addView(ui.label(getString(R.string.webview_old_title), Hud.RED))
            addView(ui.text(getString(R.string.webview_old_how, Perf.webView()), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(6))
            addView(ui.button(getString(R.string.webview_update) + " ↗", Ui.Kind.MAIN) {
                val pkg = Perf.webViewPackage()
                runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))) }
                    .onFailure { openLink("https://play.google.com/store/apps/details?id=$pkg") }
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
        // her Discord, right under her button (1.9.0, the community asked)
        addView(ui.button(getString(R.string.community_discord) + " ↗", Ui.Kind.LINE) { openLink(DISCORD) }, ui.block(10))

        // updates
        updateText = ui.text("", 13f, Hud.PAPER_2, ui.mono).apply { letterSpacing = 0.02f; setLineSpacing(0f, 1.15f) }
        updateBtn = ui.button(getString(R.string.update_go), Ui.Kind.GREEN) { update() }.apply { visibility = View.GONE }
        addView(ui.card().apply {
            addView(ui.label(getString(R.string.update_head)))
            addView(updateText, ui.block(6))
            addView(updateBtn, ui.block(10))
        }, ui.block(22))

        // what each version brought (CHANGELOG.md)
        addView(ui.label(getString(R.string.news_head)), ui.block(26))
        // folded: just the version and how many changes; a tap opens it (and folds it again)
        for ((i, v) in changelog().take(8).withIndex()) {
            val card = ui.card(if (i == 0) Hud.YELLOW else Hud.LINE_STRONG, if (i == 0) Hud.MANE else 0)
            val head = ui.row().apply { gravity = Gravity.CENTER_VERTICAL }
            head.addView(ui.text("v" + v.first, 13f, if (i == 0) Hud.YELLOW else Hud.PAPER_2, ui.mono).apply { letterSpacing = 0.08f })
            head.addView(ui.text(resources.getQuantityString(R.plurals.news_changes, v.second.size, v.second.size), 12f, Hud.PAPER_3, ui.body).apply {
                setPadding(ui.dpi(10), 0, 0, 0)
            }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            val chevron = ui.text("▾", 16f, if (i == 0) Hud.MANE else Hud.PAPER_3, ui.mono)
            head.addView(chevron)
            card.addView(head)
            val items = ui.col().apply { visibility = View.GONE }
            for (item in v.second) items.addView(bullet(item, i == 0), ui.block(8))
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
        return spanish.map { (v, items) -> v to (mine[v]?.takeIf { it.isNotEmpty() } ?: items) }
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

    private fun cmdsPage() = page {
        addView(ui.label(getString(R.string.tab_cmds)))
        addView(ui.text(getString(R.string.cmds_intro), 15f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.2f) }, ui.block(8))
        val colors = listOf(Hud.MANE, Hud.GREEN, Hud.YELLOW)
        var card: LinearLayout? = null
        var n = 0
        for (item in resources.getStringArray(R.array.cmds)) {
            if (item.startsWith("#")) {
                card = ui.card(colors[n++ % colors.size])
                card.addView(ui.label(item.removePrefix("#")))
                addView(card, ui.block(if (n == 1) 18 else 12))
            } else {
                val quoted = item.startsWith("«") || item.startsWith("“")
                card?.addView(ui.text(item, if (quoted) 15f else 14f, if (quoted) Hud.PAPER else Hud.PAPER_2, if (quoted) ui.head else ui.body).apply {
                    setLineSpacing(0f, 1.18f)
                }, ui.block(8))
            }
        }
    }

    // ---------- You (1.9.0): what she calls you, your birthday, what she remembers, how she looks after you ----------

    private fun youPage() = page {
        addView(ui.label(getString(R.string.tab_you)))
        addView(ui.text(getString(R.string.you_intro), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
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

        // your birthday (1.9.0, birthday.js): she celebrates you that day
        addView(birthdayCard(), ui.block(16))
        // what she remembers about you from her chats (memory.js): on or off, and each thing, to forget
        addView(ui.card(Hud.MANE).apply {
            addView(ui.label(getString(R.string.settings_memory)))
            addView(ui.text(getString(R.string.settings_memory_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(6))
            addView(choice(listOf(R.string.off to false, R.string.on to true),
                { Store(this@MainActivity).configFlag("longMemory", true) }, { a, b -> a == b }) { CapriSettings.set(this@MainActivity, "longMemory", it) }, ui.block(8))
            memoryBox = ui.col()
            addView(memoryBox, ui.block(12))
        }, ui.block(16))

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
            addView(ui.text(getString(R.string.settings_breaks), 14f, Hud.PAPER, ui.head), ui.block(16))
            addView(ui.text(getString(R.string.settings_breaks_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
            addView(choiceText(listOf(getString(R.string.off) to 0.0, "1 h" to 1.0, "2 h" to 2.0, "3 h" to 3.0),
                { if (!store.configFlag("breaks", true)) 0.0 else store.configNumber("breakHours", 2.0) }, { a, b -> Math.abs(a - b) < 0.1 }) {
                CapriSettings.set(this@MainActivity, "breaks", it > 0)
                if (it > 0) CapriSettings.set(this@MainActivity, "breakHours", it)
            }, ui.block(6))
        }, ui.block(16))

    }

    // your birthday: a date (no year: only the day matters to her), or none
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

    // ---------- Customize (1.9.0): how she looks, how she behaves, how she sounds ----------

    private fun stylePage() = page {
        addView(ui.label(getString(R.string.tab_style)))
        addView(ui.text(getString(R.string.style_intro), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
        // new in 1.8: her wardrobe, shown off first
        addView(wardrobeCard(), ui.block(16))
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
        // off unless you turn it on there
        addView(ui.card(Hud.RED).apply {
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

        // how she sounds: her voice's pitch and speed, her accent (Ears.kt reads these each time)
        addView(ui.card(Hud.MANE).apply {
            addView(ui.label(getString(R.string.settings_her_voice)))
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
        }, ui.block(16))

    }

    // ---------- Settings: what's general about her (1.9.0: you and her looks have their own tabs) ----------

    private fun settingsPage() = page {
        addView(ui.label(getString(R.string.tab_settings)))
        addView(ui.text(getString(R.string.settings_intro), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(4))
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
        }, ui.block(16))

        // her ears: "Capri, …"
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
                    runCatching { startActivity(Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
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

        // the log: what went wrong lately (the developer's, not for everyone: debug builds, or seven
        // taps on the version)
        logBox = ui.col()
        addView(logBox)
        val logHead = ui.row()
        logHead.addView(ui.label(getString(R.string.main_log)), LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        logHead.addView(ui.button(getString(R.string.main_copy), Ui.Kind.LINE) { copyLog() })
        logBox.addView(logHead, ui.block(26))
        logText = ui.text("", 11f, Hud.PAPER_2, ui.mono).apply {
            setLineSpacing(0f, 1.15f)
            setTextIsSelectable(true)
            background = ui.plate()
            setPadding(ui.dpi(12), ui.dpi(10), ui.dpi(12), ui.dpi(12))
        }
        logBox.addView(logText, ui.block(8))
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
            s == "gamer" -> getString(R.string.voice_gamer)
            s == "nomic" -> getString(R.string.voice_nomic)
            s == "screenoff" -> getString(R.string.voice_screenoff)
            s == "norecognizer" -> getString(R.string.voice_norecognizer)
            s.startsWith("error:") -> getString(R.string.voice_error, s.removePrefix("error:"))
            else -> getString(R.string.voice_starting)
        }
        voiceState.visibility = if (voiceState.text.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun devMode() = BuildConfig.DEBUG || getSharedPreferences("app", MODE_PRIVATE).getBoolean("dev", false)

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
            CapriSettings.forget(this, null)
            Toast.makeText(this, R.string.settings_memory_forgot, Toast.LENGTH_SHORT).show()
            later()
        }, ui.block(12))
    }

    private fun refresh() {
        if (!::ui.isInitialized) return
        showMemory()
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
        showLog()
    }

    // ---------- Community ----------

    // its own tab in the pill bar: Emma's Discord and TikTok now, more of the community later
    private fun communityPage() = page {
        addView(ui.label(getString(R.string.tab_community)))
        addView(communityCard(), ui.block(8))
    }

    // Emma's TikTok and Discord, as a card (the Comunidad tab, and the one-time invite)
    private fun communityCard(onOpen: () -> Unit = {}) = ui.card(Hud.RED).apply {
        addView(ui.label(getString(R.string.community_title), Hud.RED))
        addView(ui.text(getString(R.string.community_how), 13f, Hud.PAPER_2, ui.body).apply { setLineSpacing(0f, 1.15f) }, ui.block(6))
        addView(ui.button(getString(R.string.community_discord) + " ↗", Ui.Kind.MAIN) { onOpen(); openLink(DISCORD) }, ui.block(10))
        addView(ui.button(getString(R.string.community_tiktok) + " ↗", Ui.Kind.LINE) { onOpen(); openLink(TIKTOK) }, ui.block(8))
    }

    private fun openLink(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    // once, ever: "join the community" (and then it lives in the Comunidad tab)
    private fun inviteOnce() {
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        if (prefs.getBoolean("communityShown", false) || wantsUpdate) return
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

    private fun checkUpdate() {
        if (downloading) return
        if (release == null) updateText.text = getString(R.string.update_checking)
        Threads.make {
            val r = Updater.latest(this)
            runOnUiThread { showUpdate(r) }
        }.start()
    }

    private fun showUpdate(r: Updater.Release?) {
        if (downloading) return
        release = r?.takeIf { Updater.isNewer(it.tag) }
        val mine = BuildConfig.VERSION_NAME
        updateText.text = when {
            r == null -> getString(R.string.update_offline, mine)
            release != null -> getString(R.string.update_new, r.version, mine) + if (r.notes.isNotBlank()) "\n\n" + r.notes else ""
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
        const val TAB_SETTINGS = 4
        private const val TABS = 6
        private const val TIKTOK = "https://www.tiktok.com/@emma180516_"
        private const val DISCORD = "https://discord.gg/d8tsTKMhN"
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
                val apk = Updater.download(this, r) { k ->
                    val pct = (k * 100).toInt()
                    if (pct != shown) {
                        shown = pct
                        runOnUiThread { updateBtn.text = getString(R.string.update_downloading, pct) }
                    }
                }
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

    private fun showLog() {
        if (!::logText.isInitialized) return
        logBox.visibility = if (devMode()) View.VISIBLE else View.GONE
        val lines = Diag.all()
        logText.text = if (lines.isEmpty()) getString(R.string.main_log_empty) else lines.takeLast(30).joinToString("\n")
        logText.setTextColor(if (lines.any { it.contains(" E ") }) Hud.PAPER else Hud.PAPER_2)
    }

    private fun copyLog() {
        val all = "Capri ${BuildConfig.VERSION_NAME} / Android ${Build.VERSION.RELEASE} (${Build.MODEL})\n" + Diag.all().joinToString("\n")
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Capri", all))
        Toast.makeText(this, R.string.main_copied, Toast.LENGTH_SHORT).show()
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
