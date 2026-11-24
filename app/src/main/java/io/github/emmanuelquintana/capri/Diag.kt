package io.github.emmanuelquintana.capri

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * What went wrong lately: her page's errors, failed calls, the service starting and stopping, and
 * (1.9.0) the app closing unexpectedly. Internal only (Emma: "guardar logs internamente pero que no lo
 * vean los usuarios"): kept in memory and in a file of the app's own (the last few hundred lines, so it
 * survives a crash), never on screen; it only leaves the phone in a bug report you send (Report.kt),
 * cleaned of keys and e-mails first.
 */
object Diag {
    private const val KEEP = 60        // in memory
    private const val FILE_KEEP = 300  // in the file
    private val lines = ArrayDeque<String>()
    private val time = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US)
    private val disk = Executors.newSingleThreadExecutor { r -> Threads.make { r.run() } }
    @Volatile private var file: File? = null
    var onChange: (() -> Unit)? = null

    /** Once per process (the app's screens and her service call it): the file, and the crash catcher. */
    @Synchronized
    fun init(context: Context) {
        if (file != null) return
        file = File(context.applicationContext.filesDir, "diag.log")
        val before = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            // written right here: the process is about to go
            runCatching {
                val trace = android.util.Log.getStackTraceString(e).lines().take(25).joinToString("\n")
                file?.appendText("${time.format(Date())} E crash in ${t.name}: $trace\n")
            }
            before?.uncaughtException(t, e)
        }
    }

    @Synchronized
    fun add(level: String, text: String) {
        val line = "${time.format(Date())} $level ${text.take(400)}"
        lines.addLast(line)
        android.util.Log.println(if (level == "E") android.util.Log.ERROR else if (level == "W") android.util.Log.WARN else android.util.Log.INFO, "capri", text.take(400))
        while (lines.size > KEEP) lines.removeFirst()
        file?.let { f -> disk.execute { save(f, line) } }
        onChange?.invoke()
    }

    private fun save(f: File, line: String) = runCatching {
        f.appendText(line.replace('\n', ' ') + "\n")
        if (f.length() > 96_000) f.writeText(f.readLines().takeLast(FILE_KEEP).joinToString("\n", postfix = "\n"))
    }

    @Synchronized
    fun all(): List<String> = lines.toList()

    /** The file's lines (both processes' lines and crashes before this run), for a bug report. */
    fun saved(): List<String> = runCatching { file?.readLines()?.takeLast(FILE_KEEP) }.getOrNull() ?: all()

    @Synchronized
    fun clear() = lines.clear()

    /**
     * (1.12.0, Emma: "en las actualizaciones beta … tenga acceso al registro del teléfono para enviártelo y detectar
     * fallos") Everything about this phone for a bug hunt, for her beta phones only (MainActivity's beta box): the
     * versions, her voice settings, her own log, and Android's log of this app (logcat only shows an app its own
     * lines, so nothing of other apps). Keys, e-mails and your name are cleaned out, as in a report.
     */
    fun phoneLog(context: Context, compact: Boolean = false): String {
        val app = context.applicationContext
        val store = Store(app)
        val lang = Lang.current(app)
        val tag = CapriVoice.voiceLang(lang)
        val head = buildString {
            appendLine("Capri ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine("Android ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT}) · WebView ${Perf.webView()} · ${android.os.Build.SUPPORTED_ABIS.joinToString()}")
            val mem = android.app.ActivityManager.MemoryInfo().also { app.getSystemService(android.app.ActivityManager::class.java)?.getMemoryInfo(it) }
            appendLine("RAM ${mem.totalMem / 1_048_576} MB, free ${mem.availMem / 1_048_576} MB · lang $lang · service ${if (CapriService.running) "on" else "off"}")
            appendLine("voice: mode ${store.configString("voiceMode") ?: "automatic"}, picked ${CapriVoice.engine(app)}, answers with ${CapriVoice.answerEngine(app, lang)}")
            appendLine("cloud voice (Fish): ${FishVoice.whyNot(app, lang)}, custom voice ${FishVoice.customVoice(app).ifEmpty { "no" }}, last error ${FishVoice.lastError.ifEmpty { "none" }}")
            appendLine("her pill (Now Bar): ${runCatching { LiveBar.why(app) }.getOrElse { it.toString() }}")
            appendLine("her neural voice: ${tag?.let { CapriNeural.status(app, it) } ?: "no $lang"} · phrases ${CapriVoice.installed(tag ?: "es-MX")?.version ?: "none"}")
        }
        val logcat = runCatching {
            val p = ProcessBuilder("logcat", "-d", "-v", "threadtime", "-t", "3000").redirectErrorStream(true).start()
            val out = p.inputStream.bufferedReader().use { it.readText() }
            p.waitFor()
            out
        }.getOrElse { "(logcat: $it)" }
        // compact (a report has room for 12 000 characters): her log's end, then only Android's warnings and errors
        val mine = saved().joinToString("\n").let { if (compact) it.takeLast(6_000) else it }
        val warned = Regex("""^\S+ \S+\s+\d+\s+\d+ [WEF] """)
        val sys = if (!compact) logcat else logcat.lines().filter { warned.containsMatchIn(it) }
            .joinToString("\n").takeLast((11_500 - head.length - mine.length).coerceAtLeast(500))
        val text = head + "\n--- Capri ---\n" + mine + "\n\n--- Android (logcat) ---\n" + sys
        return Report.clean(app, text)
    }
}
