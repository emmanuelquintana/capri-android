package io.github.emmanuelquintana.capri

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * (1.11.3, Emma: "añade notificaciones cuando bajas algo de la app… no veo el progreso de cuánto ya se bajó")
 * One notification for whatever the app is downloading (her voice, her engines, an update): its name, a bar with
 * how much is done, then «Listo» or why it couldn't. A tap opens the app. Quiet: it never rings.
 */
object DownloadNote {
    private const val CHANNEL = "downloads"
    private const val ID = 4711
    private var last = -2

    private fun nm(c: Context): NotificationManager? = c.getSystemService(NotificationManager::class.java)?.also {
        if (it.getNotificationChannel(CHANNEL) == null)
            it.createNotificationChannel(NotificationChannel(CHANNEL, c.getString(R.string.dl_channel), NotificationManager.IMPORTANCE_LOW))
    }

    private fun base(c: Context, title: String) = Notification.Builder(c, CHANNEL)
        .setSmallIcon(R.drawable.ic_notify)
        .setColor(Hud.YELLOW)
        .setContentTitle(title)
        .setOnlyAlertOnce(true)
        .setContentIntent(PendingIntent.getActivity(c, 11, Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

    /** [progress] 0..1, or below 0 while it doesn't know yet (a bar that moves on its own). */
    fun progress(c: Context, title: String, progress: Float) {
        val pct = if (progress < 0f) -1 else (progress * 100).toInt().coerceIn(0, 100)
        if (pct == last) return
        last = pct
        val n = base(c, title).setOngoing(true)
            .setContentText(if (pct < 0) c.getString(R.string.dl_starting) else c.getString(R.string.dl_progress, pct))
            .setProgress(100, maxOf(pct, 0), pct < 0)
            .build()
        runCatching { nm(c)?.notify(ID, n) }
    }

    fun done(c: Context, title: String, error: String?) {
        last = -2
        val n = base(c, title).setAutoCancel(true)
            .setContentText(if (error == null) c.getString(R.string.dl_done) else c.getString(R.string.cv_err, error))
            .build()
        runCatching { nm(c)?.notify(ID, n) }
    }
}
