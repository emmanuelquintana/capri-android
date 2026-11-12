package io.github.emmanuelquintana.capri

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Your chat history with her (1.10.0, chatlog.js): what you said and what she answered, with when,
 * on this phone only (the app's own storage, nothing sent anywhere). The app shows it
 * (ChatLogActivity) and can empty it. The last [MAX] are kept.
 */
object ChatLog {
    private const val MAX = 1500
    private const val FILE = "chatlog.json"
    private val lock = Any()

    private fun file(context: Context) = File(context.filesDir, FILE)

    fun all(context: Context): JSONArray = synchronized(lock) {
        runCatching { JSONArray(file(context).readText()) }.getOrDefault(JSONArray())
    }

    fun add(context: Context, q: String, a: String, at: Long) = synchronized(lock) {
        if (a.isBlank()) return@synchronized
        val list = all(context)
        list.put(JSONObject().put("q", q.take(2000)).put("a", a.take(4000)).put("at", at))
        val keep = JSONArray()
        for (i in maxOf(0, list.length() - MAX) until list.length()) keep.put(list.get(i))
        val f = file(context)
        val tmp = File(f.parentFile, "$FILE.tmp")
        tmp.writeText(keep.toString())
        tmp.renameTo(f)
    }

    fun clear(context: Context) = synchronized(lock) { file(context).delete() }

    fun size(context: Context): Int = all(context).length()
}
