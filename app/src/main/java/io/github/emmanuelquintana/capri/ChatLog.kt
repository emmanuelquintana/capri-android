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
        Account.changed(context) // (1.12.0) with an account, your chat goes to the cloud too, a little later
    }

    fun clear(context: Context) = synchronized(lock) { file(context).delete(); Account.changed(context) }

    /** (1.12.0) Your chat from another phone joined with this one's (none lost, none twice), the last [MAX] kept. */
    fun merge(context: Context, other: JSONArray) = synchronized(lock) {
        val seen = HashSet<String>()
        val all = ArrayList<JSONObject>()
        val mine = all(context)
        for (list in listOf(mine, other)) for (i in 0 until list.length()) {
            val o = list.optJSONObject(i) ?: continue
            if (seen.add("${o.optLong("at")}|${o.optString("q").take(80)}")) all.add(o)
        }
        all.sortBy { it.optLong("at") }
        val keep = JSONArray()
        for (o in all.takeLast(MAX)) keep.put(o)
        val f = file(context)
        val tmp = File(f.parentFile, "$FILE.tmp")
        tmp.writeText(keep.toString())
        tmp.renameTo(f)
    }

    fun size(context: Context): Int = all(context).length()
}
