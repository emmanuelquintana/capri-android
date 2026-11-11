package io.github.emmanuelquintana.capri

import android.content.Context
import android.os.Build
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Anonymous statistics (1.9.0, for Emma's private dashboard on the website): once a day, "someone has
 * Capri", so she knows how many people use it. What goes: a random id made up here the first time
 * (never the phone's ANDROID_ID, an account or anything of the phone's), the app's version, its
 * language and Android's version. The website only counts it (it keeps neither the id nor the IP).
 * Off in Ajustes › Estadísticas anónimas. Errors are let go: tomorrow is another day.
 */
object Stats {
    private const val URL_PING = "https://capri-web-sdkn.vercel.app/api/ping"
    private const val PREFS = "app"

    fun enabled(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("stats", true)

    fun setEnabled(context: Context, on: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("stats", on).apply()
    }

    /** Once a day at most (by the phone's date), in the background. */
    fun maybePing(context: Context) {
        if (!enabled(context)) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        if (prefs.getString("statsDay", null) == today) return
        // marked first: a failure isn't tried again and again
        prefs.edit().putString("statsDay", today).apply()
        val id = prefs.getString("statsId", null) ?: UUID.randomUUID().toString().also { prefs.edit().putString("statsId", it).apply() }
        val body = JSONObject()
            .put("id", id)
            .put("v", BuildConfig.VERSION_NAME)
            .put("lang", Lang.current(context).takeIf { it in Lang.LANGS } ?: "en")
            .put("sdk", Build.VERSION.SDK_INT)
            .toString()
        Threads.make {
            runCatching {
                val conn = (URL(URL_PING).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    doOutput = true
                    setRequestProperty("content-type", "application/json")
                }
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                conn.disconnect()
                if (code >= 400) Diag.add("I", "stats: $code")
            }
        }.start()
    }
}
