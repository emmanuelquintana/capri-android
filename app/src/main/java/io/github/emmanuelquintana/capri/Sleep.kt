package io.github.emmanuelquintana.capri

import android.app.Activity
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import org.json.JSONArray
import org.json.JSONObject

/**
 * Modo sueño (1.11.0, Emma: "un modo siesta o modo sueño donde Capri duerme contigo y supervisa tu hora de
 * sueño, con cuántas veces prendiste la pantalla entre otras cosas, y con una alarma para levantarte, y ella
 * también esté avisando para apagar la alarma").
 * - On (from Tú › Modo sueño, or "Capri, modo sueño / despiértame a las 7"): she falls asleep on your
 *   screen (sleep.js) and keeps the night: when it started, every time the screen came on and for how long.
 * - The screen on in the middle of it: she notices ("¿otra vez despierta?").
 * - Her alarm (optional): a loud notification that repeats until you turn it off, over the lock screen
 *   when Android lets her (WakeActivity), and her, awake, telling you to get up.
 * - Off (her alarm off, or «Ya desperté»): the night's summary, kept for the last 14 nights, on this phone only.
 */
object Sleep {
    private const val PREFS = "sleep"
    private const val CHANNEL = "capri-alarm"
    const val NOTIFICATION_ID = 7
    private const val SNOOZE_MS = 5 * 60 * 1000L

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun on(c: Context) = prefs(c).getBoolean("on", false)
    fun startedAt(c: Context) = prefs(c).getLong("start", 0L)
    fun alarmAt(c: Context) = prefs(c).getLong("alarm", 0L)
    fun wakes(c: Context) = runCatching { JSONArray(prefs(c).getString("wakes", "[]")) }.getOrDefault(JSONArray())
    fun nights(c: Context) = runCatching { JSONArray(prefs(c).getString("nights", "[]")) }.getOrDefault(JSONArray())

    /** Good night: the night starts now; the alarm at `alarm` (ms), or none (0). */
    fun start(c: Context, alarm: Long) {
        prefs(c).edit().putBoolean("on", true).putLong("start", System.currentTimeMillis()).putLong("alarm", alarm)
            .putString("wakes", "[]").putLong("screenOn", 0L).apply()
        if (alarm > 0) schedule(c, alarm) else cancel(c)
        emit("sleep:on", JSONObject().put("alarm", alarm))
        Diag.add("I", "sleep: on" + if (alarm > 0) " (alarm set)" else "")
    }

    /** Morning: the alarm (and its notification) off, the night summed up and kept. Null if she wasn't on. */
    fun stop(c: Context): JSONObject? {
        cancel(c)
        c.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
        if (!on(c)) return null
        val start = startedAt(c)
        val end = System.currentTimeMillis()
        val w = wakes(c)
        var awake = 0L
        var longest = 0L
        for (i in 0 until w.length()) w.optJSONObject(i)?.optLong("ms")?.let { awake += it; longest = maxOf(longest, it) }
        val night = JSONObject().put("start", start).put("end", end).put("times", w.length()).put("awakeMs", awake).put("longestMs", longest)
        val list = nights(c).put(night)
        val keep = JSONArray()
        for (i in maxOf(0, list.length() - 14) until list.length()) keep.put(list.get(i))
        prefs(c).edit().putBoolean("on", false).putLong("alarm", 0L).putString("nights", keep.toString()).apply()
        emit("sleep:report", night)
        Diag.add("I", "sleep: off (${(end - start) / 60000} min, ${w.length()} wakes)")
        return night
    }

    /** The screen came on (CapriService): counted while the night is on, and she notices. */
    fun screenOn(c: Context) {
        if (!on(c)) return
        prefs(c).edit().putLong("screenOn", System.currentTimeMillis()).apply()
        // (not the 5 minutes right after starting, nor once the alarm has rung)
        if (System.currentTimeMillis() - startedAt(c) < 5 * 60 * 1000L) return
        emit("sleep:peek", JSONObject().put("times", wakes(c).length() + 1))
    }

    /** The screen went off: how long it was on goes into the night. */
    fun screenOff(c: Context) {
        if (!on(c)) return
        val p = prefs(c)
        val since = p.getLong("screenOn", 0L)
        if (since <= 0 || since - startedAt(c) < 5 * 60 * 1000L) return
        val w = wakes(c).put(JSONObject().put("at", since).put("ms", System.currentTimeMillis() - since))
        p.edit().putString("wakes", w.toString()).putLong("screenOn", 0L).apply()
    }

    /** After a restart (alarms don't survive it): her alarm set again, if the night is still on. */
    fun resume(c: Context) {
        val at = alarmAt(c)
        if (on(c) && at > System.currentTimeMillis()) schedule(c, at)
    }

    fun snooze(c: Context) {
        c.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
        val at = System.currentTimeMillis() + SNOOZE_MS
        prefs(c).edit().putLong("alarm", at).apply()
        schedule(c, at)
        emit("sleep:snooze", JSONObject())
    }

    /** Whether Android lets her ring at the exact minute (Android 12+: "Alarmas y recordatorios"). */
    fun exact(c: Context): Boolean = Build.VERSION.SDK_INT < 31 || c.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true

    fun askExact(c: Context) {
        if (Build.VERSION.SDK_INT >= 31) Vendor.open(c, Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, android.net.Uri.parse("package:${c.packageName}")))
    }

    private fun ring(c: Context) = PendingIntent.getBroadcast(c, 70, Intent(c, Ring::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun schedule(c: Context, at: Long) {
        val am = c.getSystemService(AlarmManager::class.java) ?: return
        runCatching {
            if (exact(c)) {
                val show = PendingIntent.getActivity(c, 71, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
                am.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), ring(c))
            } else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, ring(c)) // (a few minutes late, at worst)
        }.onFailure { Diag.add("W", "sleep: alarm: ${it.message}") }
    }

    private fun cancel(c: Context) = runCatching { c.getSystemService(AlarmManager::class.java)?.cancel(ring(c)) }

    private fun emit(channel: String, json: JSONObject) {
        CapriService.current?.takeIf { it.pageReady }?.overlay?.emit(channel, json.toString())
    }

    /** Her alarm going off: the loud notification, her over the lock screen, and her awake on your screen. */
    class Ring : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            if (!on(c)) return
            val nm = c.getSystemService(NotificationManager::class.java) ?: return
            val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            if (nm.getNotificationChannel(CHANNEL) == null) {
                nm.createNotificationChannel(NotificationChannel(CHANNEL, c.getString(R.string.sleep_channel), NotificationManager.IMPORTANCE_HIGH).apply {
                    setSound(sound, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 600, 400, 600, 400, 600)
                    setBypassDnd(true)
                })
            }
            val wake = PendingIntent.getActivity(c, 72, Intent(c, WakeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val off = PendingIntent.getBroadcast(c, 73, Intent(c, Act::class.java).setAction("off"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val later = PendingIntent.getBroadcast(c, 74, Intent(c, Act::class.java).setAction("snooze"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val name = CapriSettings.name(c)
            val n = Notification.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_notify)
                .setContentTitle(c.getString(R.string.sleep_ring_title))
                .setContentText(if (name.isNotBlank()) c.getString(R.string.sleep_ring_text_name, name) else c.getString(R.string.sleep_ring_text))
                .setCategory(Notification.CATEGORY_ALARM)
                .setFullScreenIntent(wake, true)
                .setContentIntent(wake)
                .setOngoing(true)
                .addAction(Notification.Action.Builder(null, c.getString(R.string.sleep_off), off).build())
                .addAction(Notification.Action.Builder(null, c.getString(R.string.sleep_snooze), later).build())
                .build()
            n.flags = n.flags or Notification.FLAG_INSISTENT // (the sound again and again, till you turn it off)
            runCatching { nm.notify(NOTIFICATION_ID, n) }
            emit("sleep:alarm", JSONObject())
            if (!CapriService.running) runCatching { CapriService.start(c) }
        }
    }

    /** Its buttons: off, or five more minutes. */
    class Act : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            if (intent.action == "snooze") snooze(c) else stop(c)
        }
    }

    /** Over the lock screen when it rings: her, big buttons, and the night's summary once it's off. */
    class WakeActivity : Activity() {
        override fun attachBaseContext(newBase: Context) = super.attachBaseContext(Lang.wrap(newBase))

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true) }
            window.statusBarColor = Hud.INK
            window.navigationBarColor = Hud.INK
            val ui = Ui(this)
            val col = ui.col().apply { setPadding(ui.dpi(24), ui.dpi(48), ui.dpi(24), ui.dpi(32)); gravity = Gravity.CENTER_HORIZONTAL }
            col.addView(ImageView(this).apply { ui.face("grin")?.let { setImageBitmap(it) } }, LinearLayout.LayoutParams(ui.dpi(120), ui.dpi(120)))
            val name = CapriSettings.name(this)
            col.addView(ui.text(if (name.isNotBlank()) getString(R.string.sleep_ring_text_name, name) else getString(R.string.sleep_ring_text), 26f, Hud.YELLOW, ui.head).apply { gravity = Gravity.CENTER }, ui.block(18))
            val summary = ui.text("", 15f, Hud.PAPER_2, ui.body).apply { gravity = Gravity.CENTER; setLineSpacing(0f, 1.2f) }
            col.addView(summary, ui.block(12))
            val offBtn = ui.button(getString(R.string.sleep_off), Ui.Kind.MAIN) {}
            val laterBtn = ui.button(getString(R.string.sleep_snooze), Ui.Kind.LINE) { snooze(this); finish() }
            offBtn.setOnClickListener {
                val night = stop(this)
                offBtn.visibility = android.view.View.GONE
                laterBtn.visibility = android.view.View.GONE
                summary.text = night?.let { describe(this, it) } ?: ""
                col.postDelayed({ if (!isFinishing) finish() }, 12_000)
            }
            col.addView(offBtn, ui.block(28))
            col.addView(laterBtn, ui.block(12))
            setContentView(ScrollView(this).apply { background = Hud.Backdrop(ui.dp(3f)); isFillViewport = true; addView(col, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)) })
        }
    }

    /** "Dormiste 7 h 12 min · prendiste la pantalla 3 veces (14 min)". */
    fun describe(c: Context, night: JSONObject): String {
        val slept = (night.optLong("end") - night.optLong("start") - night.optLong("awakeMs")).coerceAtLeast(0) / 60000
        val times = night.optInt("times")
        val awake = night.optLong("awakeMs") / 60000
        val base = c.getString(R.string.sleep_summary, slept / 60, slept % 60)
        return base + "\n" + if (times == 0) c.getString(R.string.sleep_summary_none)
        else c.resources.getQuantityString(R.plurals.sleep_summary_times, times, times, awake)
    }
}
