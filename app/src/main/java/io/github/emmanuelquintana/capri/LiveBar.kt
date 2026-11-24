package io.github.emmanuelquintana.capri

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Her pill in the status bar and in Samsung's Now Bar (1.11.0, Emma: "en dispositivos Samsung o que tengan
 * islas dinámicas, añade unas interacciones, como en la Now Bar, pero en su píldora; depende del dispositivo
 * y si está disponible"). Android 16 "live updates": her notification asks to be promoted, so the phone can
 * show it as a chip by the clock (her mood and her name) and, on Samsung's One UI 8, in the Now Bar of the
 * lock screen, with its buttons: Chat, Menú, Esconder. Older Android or a phone that doesn't do it: her
 * notification stays as it was. Off in Ajustes › «Su píldora en la barra».
 */
object LiveBar {
    private const val API = 36 // Android 16 (compiled against 35: its calls go by name)

    fun available() = Build.VERSION.SDK_INT >= API

    fun enabled(context: Context) = context.getSharedPreferences("app", Context.MODE_PRIVATE).getBoolean("liveBar", true)

    fun setEnabled(context: Context, on: Boolean) {
        context.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putBoolean("liveBar", on).apply()
        CapriService.current?.refreshNotification()
    }

    fun wanted(context: Context) = available() && enabled(context)

    /** Whether the phone lets her be promoted (the person can turn it off per app). */
    fun allowed(context: Context): Boolean = available() && runCatching {
        val nm = context.getSystemService(NotificationManager::class.java)!!
        nm.javaClass.getMethod("canPostPromotedNotifications").invoke(nm) as Boolean
    }.getOrDefault(false)

    /** Her notification as a live update, with the chip's short text: her mood and her name. */
    fun promote(context: Context, b: Notification.Builder, face: String, visible: Boolean) {
        if (!wanted(context)) return
        runCatching {
            b.javaClass.getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType).invoke(b, true)
            b.javaClass.getMethod("setShortCriticalText", String::class.java).invoke(b, chip(face, visible))
        }.onFailure {
            // (the extras the methods set, should a phone lack the methods)
            b.extras.putBoolean("android.requestPromotedOngoing", true)
            b.extras.putString("android.shortCriticalText", chip(face, visible))
        }
    }

    private fun chip(face: String, visible: Boolean): String = if (!visible) "Capri ·" else (MOOD[face] ?: "◆") + " Capri"

    private val MOOD = mapOf(
        "smug" to "😏", "laugh" to "😆", "love" to "💗", "wink" to "😉", "angry" to "💢", "sad" to "💧",
        "sleepy" to "💤", "surprised" to "❗", "wide" to "❗", "thinking" to "💭", "look" to "👀", "aside" to "🙄",
        "calm" to "🌙", "many" to "✨", "bored" to "😑", "cringe" to "😬", "focus" to "🎯",
    )

    /**
     * (1.12.0, Emma: "tampoco veo que sirva lo de la Now Bar de Samsung") why her pill shows or not, for her log:
     * Android's own answers (allowed for this app, her notification promotable, and promoted for real).
     */
    fun why(context: Context): String {
        if (!available()) return "Android ${Build.VERSION.SDK_INT} < 16"
        if (!enabled(context)) return "off in Ajustes"
        val nm = context.getSystemService(NotificationManager::class.java) ?: return "?"
        val mine = runCatching { nm.activeNotifications.firstOrNull { it.isOngoing } }.getOrNull()
            ?: return "allowed ${allowed(context)}, no ongoing notification"
        val n = mine.notification
        val promotable = runCatching { n.javaClass.getMethod("hasPromotableCharacteristics").invoke(n) as Boolean }.getOrNull()
        val promoted = n.flags and 0x40000 != 0 // Notification.FLAG_PROMOTED_ONGOING
        val ch = runCatching { nm.getNotificationChannel(n.channelId)?.importance }.getOrNull()
        return "allowed ${allowed(context)}, promotable $promotable, promoted $promoted, channel importance $ch, " +
            "colorized ${n.extras.getBoolean(Notification.EXTRA_COLORIZED)}, title ${!n.extras.getCharSequence(Notification.EXTRA_TITLE).isNullOrBlank()}"
    }

    /** "Chat" on her pill: the app, on her chat. */
    fun chat(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 21, Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_TAB, MainActivity.TAB_CHAT)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Android's page where the person allows (or not) live updates for Capri. */
    fun settings(context: Context) = Vendor.open(
        context,
        Intent("android.settings.MANAGE_APP_PROMOTED_NOTIFICATIONS").putExtra("android.provider.extra.APP_PACKAGE", context.packageName),
        Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName),
    )
}
