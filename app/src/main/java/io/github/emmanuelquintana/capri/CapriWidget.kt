package io.github.emmanuelquintana.capri

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.provider.Settings
import android.widget.RemoteViews

/**
 * Her widget on your home screen (1.9.0, Emma: "un widget… de cosas rápidas de Capri, con su
 * personalidad y colores"): her face (it changes with how she is), her name in yellow on ink, and four
 * quick things: start her / talk, look at my screen, dress her, put her away / bring her out.
 * The taps go to [Tap], a receiver only this app can reach.
 */
class CapriWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = update(context)

    /** A tap on the widget, from its PendingIntents only (not exported). */
    class Tap : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val what = intent.getStringExtra(EXTRA_DO) ?: return
            if (what == "app" || !Settings.canDrawOverlays(context)) return openApp(context)
            val action = when (what) {
                "start" -> null
                "chat" -> CapriService.ACTION_CHAT
                "look" -> CapriService.ACTION_LOOK
                "dress" -> CapriService.ACTION_WARDROBE
                "toggle" -> CapriService.ACTION_TOGGLE
                else -> return
            }
            runCatching {
                if (CapriService.running) action?.let { CapriService.send(context, it) }
                else CapriService.start(context, action)
            }.onFailure {
                Diag.add("W", "widget: ${it.message}")
                openApp(context)
            }
        }
    }

    companion object {
        private const val EXTRA_DO = "do"

        private fun openApp(context: Context) {
            context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }

        private fun tap(context: Context, what: String, code: Int) = PendingIntent.getBroadcast(
            context, 700 + code,
            Intent(context, Tap::class.java).putExtra(EXTRA_DO, what),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        /** Redrawn as she is now: on or off, out or put away. */
        fun update(context: Context, visible: Boolean = true) {
            val app = context.applicationContext
            val manager = AppWidgetManager.getInstance(app) ?: return
            val ids = runCatching { manager.getAppWidgetIds(ComponentName(app, CapriWidget::class.java)) }.getOrNull() ?: return
            if (ids.isEmpty()) return
            val ctx = Lang.wrap(app)
            val on = CapriService.running
            val v = RemoteViews(app.packageName, R.layout.widget_capri)
            val face = when {
                !on -> "face-calm.png"
                !visible -> "face-aside.png"
                else -> "capri-face.png"
            }
            runCatching { app.assets.open("capri/assets/capri/$face").use { BitmapFactory.decodeStream(it) } }.getOrNull()
                ?.let { v.setImageViewBitmap(R.id.w_face, android.graphics.Bitmap.createScaledBitmap(it, 144, 144, true)) }
            v.setTextViewText(R.id.w_status, ctx.getString(when {
                !on -> R.string.widget_off
                !visible -> R.string.widget_hidden
                else -> R.string.widget_on
            }))
            v.setTextColor(R.id.w_status, when {
                !on -> Color.rgb(255, 32, 82)
                !visible -> Color.rgb(246, 236, 0)
                else -> Color.rgb(54, 255, 122)
            })
            v.setTextViewText(R.id.w_main, ctx.getString(if (on) R.string.widget_talk else R.string.widget_start))
            v.setTextViewText(R.id.w_look, ctx.getString(R.string.widget_look))
            v.setTextViewText(R.id.w_dress, ctx.getString(R.string.widget_dress))
            v.setTextViewText(R.id.w_toggle, ctx.getString(when {
                !on -> R.string.widget_app
                visible -> R.string.widget_hide
                else -> R.string.widget_show
            }))
            v.setOnClickPendingIntent(R.id.w_face, tap(app, "app", 0))
            v.setOnClickPendingIntent(R.id.w_name, tap(app, "app", 0))
            v.setOnClickPendingIntent(R.id.w_main, tap(app, if (on) "chat" else "start", 1))
            v.setOnClickPendingIntent(R.id.w_look, tap(app, "look", 2))
            v.setOnClickPendingIntent(R.id.w_dress, tap(app, "dress", 3))
            v.setOnClickPendingIntent(R.id.w_toggle, tap(app, if (on) "toggle" else "app", 4))
            runCatching { manager.updateAppWidget(ids, v) }
        }
    }
}
