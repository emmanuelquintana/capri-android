package io.github.emmanuelquintana.capri

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.CalendarContract
import android.widget.RemoteViews
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.TimeZone

/**
 * Your classes and your calendar (1.9.0, Emma: "que pueda interactuar con tu calendario, te recuerde
 * fechas que se acercan y los días que tengas clase, que tenga una notificación especial").
 *
 * - Your classes: the days and the time you set in «Tú». An hour before, her special notification:
 *   her face on her ink card with her yellow echo. No permission at all.
 * - Your calendar: only if you turn it on (READ_CALENDAR, read only: she never writes there). What's
 *   coming: the evening before (from 19:00) and an hour before; an all-day one the evening before and
 *   that morning (from 8:00).
 * - Nothing leaves the phone: no AI and no network here, the titles only go to your notification
 *   (and to her bubble, when she's on your screen).
 * - One inexact alarm at the next reminder (no exact-alarm permission); every few hours anyway, to
 *   see what changed in your calendar. Set again on boot, from her update job and from the app.
 */
object Agenda {
    private const val PREFS = "agenda"
    private const val CHANNEL = "agenda"
    private const val HOUR = 3_600_000L
    private const val DAY = 24 * HOUR
    private const val BEFORE = HOUR           // her "in an hour" for classes and events
    private const val EVENING = 19            // the evening before, from 19:00
    private const val MORNING = 8             // an all-day one that morning, from 8:00
    private const val LOOK_AGAIN = 4 * HOUR   // a new look at the calendar at least this often

    class Lesson(val days: Set<Int>, val h: Int, val m: Int, val name: String) {
        fun json(): JSONObject = JSONObject().put("d", JSONArray(days.sorted())).put("h", h).put("m", m).put("n", name)
    }

    private class Due(val key: String, val at: Long, val until: Long, val kind: String, val title: String, val text: String, val event: Long = 0)

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---------- your classes ----------

    fun lessons(c: Context): List<Lesson> = runCatching {
        val a = JSONArray(prefs(c).getString("classes", "[]"))
        (0 until a.length()).mapNotNull { i ->
            val o = a.optJSONObject(i) ?: return@mapNotNull null
            val d = o.optJSONArray("d") ?: return@mapNotNull null
            val days = (0 until d.length()).map { d.optInt(it) }.filter { it in 1..7 }.toSet()
            if (days.isEmpty()) null else Lesson(days, o.optInt("h").coerceIn(0, 23), o.optInt("m").coerceIn(0, 59), o.optString("n").take(40))
        }
    }.getOrDefault(emptyList())

    fun setLessons(c: Context, list: List<Lesson>) {
        prefs(c).edit().putString("classes", JSONArray(list.map { it.json() }).toString()).apply()
        plan(c)
    }

    // ---------- your calendar ----------

    fun calendarAllowed(c: Context) = c.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
    fun calendarOn(c: Context) = prefs(c).getBoolean("calendar", false) && calendarAllowed(c)
    fun setCalendar(c: Context, on: Boolean) {
        prefs(c).edit().putBoolean("calendar", on).apply()
        plan(c)
    }

    // ---------- what's due ----------

    private fun at(day: Calendar, h: Int, m: Int = 0) = (day.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun clock(c: Context, t: Long) = android.text.format.DateFormat.getTimeFormat(c).format(java.util.Date(t))

    private fun upcoming(c: Context, now: Long): List<Due> {
        val out = mutableListOf<Due>()
        val who = CapriSettings.name(c).ifBlank { "choom" }
        // classes: today, tomorrow and the day after
        val today = Calendar.getInstance().apply { timeInMillis = now }
        for (plus in 0..2) {
            val day = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, plus) }
            val dow = day.get(Calendar.DAY_OF_WEEK)
            for (l in lessons(c)) {
                if (dow !in l.days) continue
                val start = at(day, l.h, l.m)
                val name = l.name.ifBlank { c.getString(R.string.agenda_class_default) }
                out += Due("c:$start:${l.name.hashCode()}", start - BEFORE, start, "class",
                    c.getString(R.string.agenda_class_title, who), c.getString(R.string.agenda_class_text, name, clock(c, start)))
            }
        }
        if (calendarOn(c)) out += events(c, now)
        return out
    }

    private fun events(c: Context, now: Long): List<Due> {
        val out = mutableListOf<Due>()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, now - DAY)
            ContentUris.appendId(it, now + 3 * DAY)
        }.build()
        val cols = arrayOf(CalendarContract.Instances.EVENT_ID, CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.ALL_DAY, CalendarContract.Instances.SELF_ATTENDEE_STATUS)
        try {
            c.contentResolver.query(uri, cols, "${CalendarContract.Instances.VISIBLE} = 1", null, null)?.use { q ->
                while (q.moveToNext() && out.size < 60) {
                    val id = q.getLong(0)
                    val title = (q.getString(1) ?: "").trim().take(80).ifBlank { c.getString(R.string.agenda_event_default) }
                    val begin = q.getLong(2)
                    val allDay = q.getInt(3) == 1
                    if (q.getInt(4) == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED) continue // (one you said no to)
                    if (allDay) {
                        // stored at midnight UTC: that date, here
                        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = begin }
                        val day = Calendar.getInstance().apply { clear(); set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH)) }
                        val dayStart = day.timeInMillis
                        val eve = (day.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
                        out += Due("a:$id:$dayStart:eve", at(eve, EVENING), dayStart, "event",
                            c.getString(R.string.agenda_tomorrow_title, title), c.getString(R.string.agenda_tomorrow_allday), id)
                        out += Due("a:$id:$dayStart:day", at(day, MORNING), dayStart + DAY, "event",
                            c.getString(R.string.agenda_today_title, title), c.getString(R.string.agenda_today_text), id)
                    } else {
                        val day = Calendar.getInstance().apply { timeInMillis = begin }
                        val eve = (day.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
                        out += Due("e:$id:$begin:eve", at(eve, EVENING), begin - BEFORE, "event",
                            c.getString(R.string.agenda_tomorrow_title, title), c.getString(R.string.agenda_tomorrow_text, clock(c, begin)), id)
                        out += Due("e:$id:$begin:soon", begin - BEFORE, begin, "event",
                            c.getString(R.string.agenda_soon_title, clock(c, begin), title), c.getString(R.string.agenda_soon_text), id)
                    }
                }
            }
        } catch (e: Exception) {
            Diag.add("W", "agenda: ${e.javaClass.simpleName}") // (no titles in her log)
        }
        return out
    }

    // ---------- the alarm ----------

    /** Sends what's due now (once each), then sets the alarm for the next one. Off the main thread. */
    fun run(c0: Context) {
        val c = Lang.wrap(c0)
        val now = System.currentTimeMillis()
        val list = upcoming(c, now)
        val p = prefs(c)
        // what was already sent, with when it was for: forgotten 4 days later
        val sent = (p.getStringSet("sent", emptySet()) ?: emptySet()).filter { (it.substringAfterLast('@').toLongOrNull() ?: 0) > now - 4 * DAY }.toMutableSet()
        for (d in list.sortedBy { it.at }) {
            if (d.at > now || now >= d.until) continue
            val mark = "${d.key}@${d.until}"
            if (mark in sent) continue
            sent += mark
            notify(c, d)
        }
        p.edit().putStringSet("sent", sent).apply()
        schedule(c, list.map { it.at }.filter { it > now }.minOrNull(), now)
    }

    /** Sets the next alarm (from the app, at boot, from her update job). */
    fun plan(c: Context) = Threads.make { runCatching { run(c) }.onFailure { Diag.add("W", "agenda: ${it.message}") } }.start()

    private fun schedule(c: Context, next: Long?, now: Long) {
        val am = c.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(c, 0, Intent(c, Fire::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        if (lessons(c).isEmpty() && !calendarOn(c)) return am.cancel(pi)
        val at = minOf(next ?: Long.MAX_VALUE, now + LOOK_AGAIN).coerceAtLeast(now + 60_000)
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    class Fire : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val done = goAsync()
            val app = context.applicationContext
            Threads.make {
                try { run(app) } catch (e: Exception) { Diag.add("W", "agenda: ${e.message}") } finally { done.finish() }
            }.start()
        }
    }

    /** Debug builds (CI): her class notification right now, to see it. */
    fun demo(c0: Context) = Threads.make {
        val c = Lang.wrap(c0)
        val start = System.currentTimeMillis() + BEFORE
        notify(c, Due("demo:$start", 0, start, "class", c.getString(R.string.agenda_class_title, CapriSettings.name(c).ifBlank { "choom" }),
            c.getString(R.string.agenda_class_text, c.getString(R.string.agenda_class_default), clock(c, start))))
    }.start()

    // ---------- her notification ----------

    private fun face(c: Context): Bitmap? = runCatching {
        c.assets.open("capri/assets/capri/capri-face.png").use { BitmapFactory.decodeStream(it) }?.let { Bitmap.createScaledBitmap(it, 144, 144, true) }
    }.getOrNull()

    private fun notify(c: Context, d: Due) {
        val nm = c.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(NotificationChannel(CHANNEL, c.getString(R.string.agenda_channel), NotificationManager.IMPORTANCE_HIGH).apply {
            description = c.getString(R.string.agenda_channel_how)
        })
        val open = if (d.event > 0) Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, d.event))
        else Intent(c, MainActivity::class.java)
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val tap = PendingIntent.getActivity(c, d.key.hashCode(), open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val face = face(c)
        val b = Notification.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_notify)
            .setColor(if (d.kind == "class") Hud.YELLOW else Hud.GREEN)
            .setContentTitle(d.title)
            .setContentText(d.text)
            .setStyle(Notification.BigTextStyle().bigText(d.text))
            .setCategory(if (d.kind == "class") Notification.CATEGORY_REMINDER else Notification.CATEGORY_EVENT)
            .setContentIntent(tap)
            .setAutoCancel(true)
            .setTimeoutAfter(maxOf(d.until - System.currentTimeMillis(), 15 * 60_000L) + HOUR)
        if (d.kind == "class") {
            // her special one: her ink card, her yellow echo, her face (on it, so no second face beside it);
            // folded, a line of each so it fits; open, all of it
            fun card(lines: Int) = RemoteViews(c.packageName, R.layout.notify_class).apply {
                if (face != null) setImageViewBitmap(R.id.n_face, face)
                setTextViewText(R.id.n_title, d.title)
                setTextViewText(R.id.n_text, d.text)
                if (lines > 0) {
                    setInt(R.id.n_title, "setMaxLines", 1)
                    setInt(R.id.n_text, "setMaxLines", lines)
                }
            }
            b.setStyle(Notification.DecoratedCustomViewStyle()).setCustomContentView(card(1)).setCustomBigContentView(card(0))
        } else if (face != null) b.setLargeIcon(face)
        runCatching { nm.notify(d.key.hashCode(), b.build()) }.onFailure { Diag.add("W", "agenda notify: ${it.message}") }
        // and she says it herself when she's on your screen
        CapriService.current?.overlay?.emit("agenda:remind", JSONObject().put("kind", d.kind).put("title", d.title).put("text", d.text).toString())
    }
}
