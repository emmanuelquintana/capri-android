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
}
