package io.github.emmanuelquintana.capri

import android.content.Context
import android.os.Build
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * «Reportar un fallo» (1.9.0, Emma: "que puedan reportar fallos y envíen mensaje al canal de Discord").
 * What you wrote, the app's version, Android's version, the phone's model and her internal log go to
 * the website (/api/report), which passes it on to Emma's Discord channel: no Discord webhook in the
 * app (anyone could pull it out of the APK). The same body as the PC app's.
 *
 * Before it goes, the log is cleaned: API keys, tokens, e-mails, and your name if it shows up. Only
 * when you press Enviar; nothing is sent by itself.
 */
object Report {
    private const val URL_REPORT = "https://capri-web-sdkn.vercel.app/api/report"
    const val MAX_TEXT = 1000
    private const val MAX_LOG = 12_000

    private val SECRETS = listOf(
        Regex("""\b(?:sk|gsk|xai|nvapi|hf|pk|rk)[-_][A-Za-z0-9_\-]{8,}"""),  // OpenAI, Groq, xAI, NVIDIA, HF…
        Regex("""\bAIza[0-9A-Za-z_\-]{20,}"""),                              // Google
        Regex("""(?i)\b(bearer|authorization|api[-_]?key|token|key)\s*[:=]\s*\S+"""),
        Regex("""(?i)([?&](?:key|token|api_key|access_token)=)[^&\s]+"""),
    )
    private val EMAIL = Regex("""[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}""")

    fun clean(context: Context, text: String): String {
        var t = text
        SECRETS.forEach { re -> t = re.replace(t) { m -> if (m.groupValues.size > 1 && m.groupValues[1].isNotEmpty()) "${m.groupValues[1]}[clave]" else "[clave]" } }
        t = EMAIL.replace(t, "[correo]")
        val name = CapriSettings.name(context).trim()
        if (name.length >= 3) t = t.replace(name, "[nombre]", ignoreCase = true)
        return t
    }

    /** Sends it in the background; [done] gets "ok", "rate", "off", "bad" or "net", on the main thread. */
    fun send(context: Context, text: String, done: (String) -> Unit) {
        val app = context.applicationContext
        val main = android.os.Handler(android.os.Looper.getMainLooper())
        Threads.make {
            val result = runCatching {
                val log = clean(app, Diag.saved().joinToString("\n")).takeLast(MAX_LOG)
                val body = JSONObject()
                    .put("app", "android")
                    .put("v", BuildConfig.VERSION_NAME)
                    .put("os", "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}) · WebView ${Perf.webView()}")
                    .put("device", "${Build.MANUFACTURER} ${Build.MODEL}".take(80))
                    .put("lang", Lang.current(app).takeIf { it in Lang.LANGS } ?: "en")
                    .put("text", clean(app, text.trim()).take(MAX_TEXT))
                    .put("log", log)
                app.getSharedPreferences("app", Context.MODE_PRIVATE).getString("statsId", null)?.let { body.put("id", it) }
                // her last unexpected close, if there was one: shown apart in the report
                Diag.saved().lastOrNull { " crash in " in it }?.let { body.put("stack", clean(app, it).take(1800)) }
                val conn = (URL(URL_REPORT).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15_000
                    readTimeout = 20_000
                    doOutput = true
                    setRequestProperty("content-type", "application/json")
                }
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val answer = runCatching { (if (code < 400) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() } }.getOrNull().orEmpty()
                conn.disconnect()
                val error = runCatching { JSONObject(answer).optString("error") }.getOrDefault("")
                when {
                    code in 200..299 -> "ok"
                    error in setOf("rate", "off", "bad") -> error
                    error == "send" -> "off" // (the website couldn't pass it on: later)
                    code == 429 -> "rate"
                    code == 503 || code == 404 -> "off"
                    else -> "bad"
                }
            }.getOrElse { "net" }
            Diag.add("I", "report: $result")
            main.post { done(result) }
        }.start()
    }
}
