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

    fun lite(context: Context): Boolean = when (mode(context)) {
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

    /** The WebView's major version (Chrome's), 0 if Android won't say. */
    fun webView(): Int = runCatching {
        WebView.getCurrentWebViewPackage()?.versionName?.substringBefore('.')?.toIntOrNull() ?: 0
    }.getOrDefault(0)

    fun webViewPackage(): String = runCatching { WebView.getCurrentWebViewPackage()?.packageName }.getOrNull() ?: "com.google.android.webview"

    /** Too old for her page: it has to be updated from the Play Store. */
    fun webViewTooOld(): Boolean = webView().let { it in 1 until MIN_WEBVIEW }
}
