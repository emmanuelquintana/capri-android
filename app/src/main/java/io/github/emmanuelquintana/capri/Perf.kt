package io.github.emmanuelquintana.capri

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.webkit.WebView

/**
 * Old and modest phones (1.9.0). "Modo ligero": her picture drawn at fewer pixels (the GPU's biggest
 * job, the screen-sized one for her effects included) and no blur behind her menus. It's on by
 * itself on a phone with little memory or an old Android, and can be set by hand in Ajustes.
 * And her page needs a WebView recent enough: an old one gets told how to update it.
 */
object Perf {
    /** Below this, her page can't run at all (the language her page is written in is newer). */
    const val MIN_WEBVIEW = 85
    private const val LOW_RAM = 3_300_000_000L // ~3 GB phones and less

    /** "auto" (default), "lite" or "full". */
    fun mode(context: Context): String = Store(context).configString("perfMode") ?: "auto"

    fun lite(context: Context, mode: String = mode(context)): Boolean = when (mode) {
        "lite" -> true
        "full" -> false
        else -> modest(context)
    }

    /** A phone that does better with less: little memory, or Android 8. */
    fun modest(context: Context): Boolean = modest ?: run {
        val am = context.getSystemService(ActivityManager::class.java)
        val mem = ActivityManager.MemoryInfo().also { am?.getMemoryInfo(it) }
        val low = am?.isLowRamDevice == true || (mem.totalMem in 1 until LOW_RAM) || Build.VERSION.SDK_INT < Build.VERSION_CODES.P
        modest = low
        low
    }
    @Volatile private var modest: Boolean? = null

    // ---------- her picture without WebGL (1.9.1) ----------
    // Community reports on 1.9.0 (Redmi Note 11, Honor 8X, realme…): her WebGL picture lost, then the
    // whole app closing or the screen going white, again and again. On those phones the GPU driver
    // goes down with it (in the app's own process). Drawing her with a plain 2D canvas has no WebGL
    // to lose: in modo ligero always, and by itself on any phone where it was lost twice lately.

    private const val LOST_WINDOW = 2 * 3_600_000L
    private const val PREFS = "perf"

    /** Her page reported her WebGL picture lost: kept (on disk at once: the process may be about to go). */
    fun gpuLost(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val recent = (p.getString("lost", "") ?: "").split(',').mapNotNull { it.toLongOrNull() }.filter { now - it < LOST_WINDOW } + now
        p.edit().putString("lost", recent.takeLast(6).joinToString(",")).commit()
        if (recent.size == 2) Diag.add("W", "her picture was lost twice: from now on she's drawn without WebGL")
    }

    /** Lost twice in the last hours (it stays so until the phone has gone a while without it). */
    fun gpuShaky(context: Context): Boolean {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (p.getLong("flatSince", 0) > now - 3 * 86_400_000L) return true
        val n = (p.getString("lost", "") ?: "").split(',').mapNotNull { it.toLongOrNull() }.count { now - it < LOST_WINDOW }
        if (n >= 2) p.edit().putLong("flatSince", now).apply()
        return n >= 2
    }

    /** Her picture in 2D, no WebGL: modo ligero, or a phone whose GPU kept losing it ("full" never). */
    fun flat(context: Context, mode: String = mode(context)): Boolean = mode != "full" && (lite(context, mode) || gpuShaky(context))

    /** The WebView's major version (Chrome's), 0 if Android won't say. */
    fun webView(): Int = runCatching {
        WebView.getCurrentWebViewPackage()?.versionName?.substringBefore('.')?.toIntOrNull() ?: 0
    }.getOrDefault(0)

    fun webViewPackage(): String = runCatching { WebView.getCurrentWebViewPackage()?.packageName }.getOrNull() ?: "com.google.android.webview"

    /** Too old for her page: it has to be updated from the Play Store. */
    fun webViewTooOld(): Boolean = webView().let { it in 1 until MIN_WEBVIEW }
}
