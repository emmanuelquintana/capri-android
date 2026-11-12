package io.github.emmanuelquintana.capri

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject

/**
 * A screen of the app drawn by one of her pages (1.10.0), like her Vestuario (ClosetActivity):
 * - "chatlog": your chat history with her (ChatLog.kt), to read again or empty.
 * - "games": games with her (assets/android/games.html): chess, minesweeper, 21, tic-tac-toe.
 * Only her own files are served; nothing here goes on the internet.
 */
class PageActivity : Activity() {
    private var web: WebView? = null

    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(Lang.wrap(newBase))

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val page = intent.getStringExtra(EXTRA_PAGE)?.takeIf { it in PAGES } ?: return finish()
        window.statusBarColor = Hud.INK
        window.navigationBarColor = Hud.INK
        val view = WebView(this).apply {
            setBackgroundColor(Hud.INK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.textZoom = 100
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                    if (request.url.host != HOST) return null
                    return serve(request.url.path ?: "/")
                }

                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = true

                // its page crashed or was killed for memory: this screen closes, not the whole app
                override fun onRenderProcessGone(view: WebView, detail: android.webkit.RenderProcessGoneDetail): Boolean {
                    Diag.add("W", "$page: page gone (crash: ${detail.didCrash()})")
                    if (web === view) web = null
                    (view.parent as? android.view.ViewGroup)?.removeView(view)
                    view.destroy()
                    if (!isFinishing) finish()
                    return true
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                    Log.i("capri-$page", "${m.message()} (${m.sourceId().substringAfterLast('/')}:${m.lineNumber()})")
                    return true
                }
            }
            addJavascriptInterface(Bridge(), "CapriPage")
        }
        web = view
        setContentView(view)
        view.loadUrl("https://$HOST/android/$page.html")
    }

    override fun onDestroy() {
        web?.destroy()
        web = null
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val w = web
        if (w == null) return super.onBackPressed()
        // her page first (a game's menu, a confirmation): it says if it took the Back
        w.evaluateJavascript("window.capriBack ? window.capriBack() : false") { took ->
            if (took != "true") finish()
        }
    }

    private fun serve(path: String): WebResourceResponse = try {
        val rel = path.removePrefix("/")
        if (rel.contains("..") || !(rel.startsWith("capri/") || rel.startsWith("android/"))) throw SecurityException(rel)
        val type = when (rel.substringAfterLast('.', "").lowercase()) {
            "html" -> "text/html"
            "js", "mjs" -> "text/javascript"
            "css" -> "text/css"
            "json" -> "application/json"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "woff2" -> "font/woff2"
            else -> "application/octet-stream"
        }
        WebResourceResponse(type, if (type.startsWith("text/")) "utf-8" else null, assets.open(rel)).apply {
            responseHeaders = mapOf("Cache-Control" to "no-cache")
        }
    } catch (e: Exception) {
        WebResourceResponse("text/plain", "utf-8", 404, "Not Found", emptyMap(), "".byteInputStream())
    }

    private inner class Bridge {
        /** The app's language and what she calls you. */
        @JavascriptInterface
        fun info(): String = JSONObject()
            .put("lang", Lang.current(this@PageActivity))
            .put("name", CapriSettings.name(this@PageActivity))
            .toString()

        /** Your chat history, oldest first. */
        @JavascriptInterface
        fun chatlog(): String = ChatLog.all(this@PageActivity).toString()

        @JavascriptInterface
        fun clearChatlog() = ChatLog.clear(this@PageActivity).let { }

        /** A game's result, so she can bring it up later over your screen (games.js). */
        @JavascriptInterface
        fun played(json: String) {
            val p = getSharedPreferences("games", Context.MODE_PRIVATE)
            p.edit().putString("last", json.take(400)).apply()
        }

        @JavascriptInterface
        fun haptic() = runOnUiThread { web?.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP) }

        @JavascriptInterface
        fun done() = runOnUiThread { finish() }
    }

    companion object {
        private const val HOST = "appassets.androidplatform.net"
        const val EXTRA_PAGE = "page"
        private val PAGES = setOf("chatlog", "games")

        fun open(context: Context, page: String) {
            context.startActivity(Intent(context, PageActivity::class.java).putExtra(EXTRA_PAGE, page)
                .addFlags(if (context is Activity) 0 else Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
