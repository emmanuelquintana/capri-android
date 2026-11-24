package io.github.emmanuelquintana.capri

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.BatteryManager
import android.widget.RemoteViews
import java.util.Calendar

/**
 * (1.12.0, community: "gostaria que tivesse mais widgets") Two more widgets, in her colors like her first one
 * (CapriWidget.kt), whose taps they share:
 * - Capri mini (2x2): her face, how she is, and a line of hers that changes every half hour. A tap: talk to her.
 * - Tu día con Capri (4x2): your next class (the ones you gave her in Tú › Clases), your battery, her line,
 *   and talk / look at my screen.
 */
private fun faceBitmap(context: Context, on: Boolean, size: Int): Bitmap? {
    val name = if (on) "capri-face.png" else "face-calm.png"
    return runCatching { context.assets.open("capri/assets/capri/$name").use { BitmapFactory.decodeStream(it) } }.getOrNull()
        ?.let { Bitmap.createScaledBitmap(it, size, size, true) }
}

/** One of her lines for the widgets: a different one each half hour, in the app's language. */
private fun herLine(ctx: Context): String {
    val lines = ctx.resources.getStringArray(R.array.widget_lines)
    if (lines.isEmpty()) return ""
    return lines[((System.currentTimeMillis() / 1_800_000L) % lines.size).toInt()]
}

private fun ids(context: Context, cls: Class<*>): IntArray =
    runCatching { AppWidgetManager.getInstance(context)?.getAppWidgetIds(ComponentName(context, cls)) }.getOrNull() ?: IntArray(0)

class CapriFaceWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = update(context)

    companion object {
        fun update(context: Context) {
            val app = context.applicationContext
            val ids = ids(app, CapriFaceWidget::class.java)
            if (ids.isEmpty()) return
            val ctx = Lang.wrap(app)
            val on = CapriService.running
            val v = RemoteViews(app.packageName, R.layout.widget_face)
            faceBitmap(app, on, 160)?.let { v.setImageViewBitmap(R.id.wf_face, it) }
            v.setTextViewText(R.id.wf_status, ctx.getString(if (on) R.string.widget_on else R.string.widget_off))
            v.setTextColor(R.id.wf_status, if (on) Color.rgb(54, 255, 122) else Color.rgb(255, 32, 82))
            v.setTextViewText(R.id.wf_line, herLine(ctx))
            v.setOnClickPendingIntent(R.id.wf_root, CapriWidget.tap(app, if (on) "chat" else "start", 11))
            runCatching { AppWidgetManager.getInstance(app).updateAppWidget(ids, v) }
        }
    }
}

class CapriDayWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = update(context)

    companion object {
        fun update(context: Context) {
            val app = context.applicationContext
            val ids = ids(app, CapriDayWidget::class.java)
            if (ids.isEmpty()) return
            val ctx = Lang.wrap(app)
            val on = CapriService.running
            val v = RemoteViews(app.packageName, R.layout.widget_day)
            faceBitmap(app, on, 108)?.let { v.setImageViewBitmap(R.id.wd_face, it) }
            val now = Calendar.getInstance()
            val day = java.text.DateFormat.getDateInstance(java.text.DateFormat.FULL, ctx.resources.configuration.locales[0]).format(now.time)
            v.setTextViewText(R.id.wd_title, day.uppercase(ctx.resources.configuration.locales[0]))
            val battery = app.getSystemService(BatteryManager::class.java)?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            v.setTextViewText(R.id.wd_battery, if (battery in 0..100) "⚡ $battery %" else "")
            v.setTextViewText(R.id.wd_next, nextClass(ctx) ?: ctx.getString(R.string.widget_day_free))
            v.setTextViewText(R.id.wd_line, herLine(ctx))
            v.setTextViewText(R.id.wd_talk, ctx.getString(if (on) R.string.widget_talk else R.string.widget_start))
            v.setTextViewText(R.id.wd_look, ctx.getString(R.string.widget_look))
            v.setOnClickPendingIntent(R.id.wd_face, CapriWidget.tap(app, "app", 20))
            v.setOnClickPendingIntent(R.id.wd_next, CapriWidget.tap(app, "app", 20))
            v.setOnClickPendingIntent(R.id.wd_talk, CapriWidget.tap(app, if (on) "chat" else "start", 21))
            v.setOnClickPendingIntent(R.id.wd_look, CapriWidget.tap(app, "look", 22))
            runCatching { AppWidgetManager.getInstance(app).updateAppWidget(ids, v) }
        }

        /** "Próxima clase: Mate · hoy 10:00", the next one in the coming week, or null. */
        private fun nextClass(ctx: Context): String? {
            val lessons = Agenda.lessons(ctx)
            if (lessons.isEmpty()) return null
            val now = Calendar.getInstance()
            for (ahead in 0..7) {
                val d = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, ahead) }
                val dow = d.get(Calendar.DAY_OF_WEEK)
                val todays = lessons.filter { dow in it.days }.sortedBy { it.h * 60 + it.m }
                val next = todays.firstOrNull { ahead > 0 || it.h * 60 + it.m > now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE) } ?: continue
                d.set(Calendar.HOUR_OF_DAY, next.h); d.set(Calendar.MINUTE, next.m)
                val time = android.text.format.DateFormat.getTimeFormat(ctx).format(d.time)
                val whenText = when (ahead) {
                    0 -> ctx.getString(R.string.widget_day_today)
                    1 -> ctx.getString(R.string.widget_day_tomorrow)
                    else -> java.text.DateFormatSymbols(ctx.resources.configuration.locales[0]).weekdays[dow]
                }
                return ctx.getString(R.string.widget_day_next, next.name.ifBlank { ctx.getString(R.string.agenda_class_default) }, "$whenText $time")
            }
            return null
        }
    }
}
