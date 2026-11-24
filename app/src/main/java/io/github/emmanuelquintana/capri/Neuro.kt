package io.github.emmanuelquintana.capri

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject

/**
 * Her neurodanzas (1.11.0, assets/android/neuro.js): braindances she records for you after a lot of
 * affection. Kept here (the last 20), told to you with a notification, played in the app (PageActivity
 * "neuro", android/neuro.html), and your rating goes back to her so she can say what she thinks.
 */
object Neuro {
    private const val PREFS = "neuro"
    private const val MAX = 20
    private const val CHANNEL = "capri-neuro"
    private const val NOTIFICATION_ID = 7301

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun all(c: Context): JSONArray = runCatching { JSONArray(prefs(c).getString("list", "[]")) }.getOrDefault(JSONArray())

    fun unseen(c: Context): Int {
        val list = all(c)
        return (0 until list.length()).count { !list.optJSONObject(it).optBoolean("seen") }
    }

    fun add(c: Context, bd: JSONObject): Boolean {
        val list = all(c)
        bd.put("id", System.currentTimeMillis().toString()).put("seen", false).put("rating", 0)
        if (!bd.has("at")) bd.put("at", System.currentTimeMillis())
        val out = JSONArray().put(bd)
        for (i in 0 until minOf(list.length(), MAX - 1)) out.put(list.get(i))
        prefs(c).edit().putString("list", out.toString()).apply(); Account.changed(c)
        notify(c, bd.optString("title"))
        return true
    }

    /** You watched it (and maybe rated it, 1 to 3): saved, and told to her. */
    fun seen(c: Context, id: String, rating: Int) {
        val list = all(c)
        var title = ""
        for (i in 0 until list.length()) {
            val bd = list.optJSONObject(i) ?: continue
            if (bd.optString("id") != id) continue
            bd.put("seen", true)
            if (rating in 1..3) bd.put("rating", rating)
            title = bd.optString("title")
        }
        prefs(c).edit().putString("list", list.toString()).apply(); Account.changed(c)
        c.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
        if (rating in 1..3) CapriService.current?.takeIf { it.pageReady }?.overlay
            ?.emit("neuro:rated", JSONObject().put("title", title).put("rating", rating).toString())
    }

    private fun notify(c: Context, title: String) = runCatching {
        val nm = c.getSystemService(NotificationManager::class.java) ?: return@runCatching
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, c.getString(R.string.neuro_channel), NotificationManager.IMPORTANCE_DEFAULT))
        }
        val open = PendingIntent.getActivity(c, 75,
            Intent(c, PageActivity::class.java).putExtra(PageActivity.EXTRA_PAGE, "neuro").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        nm.notify(NOTIFICATION_ID, Notification.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(c.getString(R.string.neuro_notify_title))
            .setContentText(c.getString(R.string.neuro_notify_text, title))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build())
    }
}
