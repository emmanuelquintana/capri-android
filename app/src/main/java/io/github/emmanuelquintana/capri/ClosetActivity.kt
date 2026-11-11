package io.github.emmanuelquintana.capri

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import org.json.JSONObject

/**
 * Her Vestuario as a screen of its own, inside the app (1.8.1): the same sheet as over your screen
 * (assets/android/closet.html + wardrobe.js), with her on a little stage above it. What you pick is
 * saved as her page saves it (config.json's `look`) and, if she's on, she changes at once. From here
 * you can also dress her the other way, over your screen.
 */
class ClosetActivity : Activity() {
    private var web: WebView? = null

    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(Lang.wrap(newBase))

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Hud.INK
        window.navigationBarColor = Hud.INK
        val view = WebView(this).apply {
            setBackgroundColor(Hud.INK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                    if (request.url.host != HOST) return null
                    return serve(request.url.path ?: "/")
                }

                // nothing but her own page here
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = true
            }
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                    Log.d("capri-closet", "${m.message()} (${m.sourceId().substringAfterLast('/')}:${m.lineNumber()})")
                    return true
                }
            }
            addJavascriptInterface(Bridge(), "CapriCloset")
        }
        web = view
        setContentView(view)
        view.loadUrl("https://$HOST/android/closet.html")
    }

    override fun onDestroy() {
        web?.destroy()
        web = null
        super.onDestroy()
    }

    /** Only her page's own files (android/, capri/), as Overlay serves them. */
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
        /** Her saved look and secrets (what the wedding unlocked), and the app's language. */
        @JavascriptInterface
        fun config(): String {
            val cfg = runCatching { JSONObject(Store(this@ClosetActivity).read(Store.CONFIG) ?: "{}") }.getOrElse { JSONObject() }
            val out = JSONObject().put("appLang", Lang.current(this@ClosetActivity))
            cfg.optJSONObject("look")?.let { out.put("look", it) }
            cfg.optJSONArray("unlocked")?.let { out.put("unlocked", it) }
            return out.toString()
        }

        /** What you picked: on her now if she's on, and saved either way. */
        @JavascriptInterface
        fun save(look: String) {
            val value = runCatching { JSONObject(look) }.getOrNull() ?: return
            CapriSettings.set(this@ClosetActivity, "look", value)
        }

        /** Her wardrobe over your screen instead (the other way to dress her). */
        @JavascriptInterface
        fun onScreen() = runOnUiThread {
            if (!Settings.canDrawOverlays(this@ClosetActivity)) {
                Toast.makeText(this@ClosetActivity, R.string.ob_need_overlay, Toast.LENGTH_LONG).show()
                return@runOnUiThread
            }
            if (CapriService.running) CapriService.send(this@ClosetActivity, CapriService.ACTION_WARDROBE)
            else CapriService.start(this@ClosetActivity, CapriService.ACTION_WARDROBE)
            finish()
            moveTaskToBack(true)
        }

        @JavascriptInterface
        fun done() = runOnUiThread { finish() }
    }

    companion object {
        private const val HOST = "appassets.androidplatform.net"
    }
}
