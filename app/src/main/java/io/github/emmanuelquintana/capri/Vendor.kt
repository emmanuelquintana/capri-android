package io.github.emmanuelquintana.capri

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * (1.11.0, Emma: "mejorar la compatibilidad con Huawei, varias de sus funciones no funcionan del todo bien
 * o no funcionan"). Huawei and Honor phones (EMUI, HarmonyOS, MagicOS), many without Google's services:
 * - they close apps in the background unless you let them run ("Inicio de apps" in their Phone Manager):
 *   then Capri disappears after a while, her reminders and her ears stop;
 * - some settings screens Android names aren't there, or are somewhere else: every one we open tries
 *   the next one (and the app's own page last), never nothing;
 * - no Play Store: links to it go to AppGallery;
 * - no Google speech or voice: their screens to add one.
 */
object Vendor {
    val huawei: Boolean by lazy {
        val m = (Build.MANUFACTURER + " " + Build.BRAND).lowercase()
        "huawei" in m || "honor" in m
    }

    /** The first of these that opens; else the app's own page in Settings. */
    fun open(context: Context, vararg intents: Intent): Boolean {
        val flags = if (context is android.app.Activity) 0 else Intent.FLAG_ACTIVITY_NEW_TASK
        for (i in intents) if (runCatching { context.startActivity(Intent(i).addFlags(flags)) }.isSuccess) return true
        return runCatching { context.startActivity(appPage(context).addFlags(flags)) }.isSuccess
    }

    fun appPage(context: Context) = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))

    /** Letting Capri run in the background: Huawei's/Honor's "App launch", else Android's battery list. */
    fun keepAlive(context: Context) = open(
        context,
        component("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
        component("com.hihonor.systemmanager", "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
        component("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"),
        component("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
    )

    /** Drawing over other apps: Android's own page for Capri, else the general list. */
    fun overlay(context: Context) = open(
        context,
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")),
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION),
        component("com.huawei.systemmanager", "com.huawei.permissionmanager.ui.MainActivity"),
    )

    /** Reading notifications (her music card, your messages). */
    fun notificationAccess(context: Context) = open(
        context,
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
        Intent("android.settings.NOTIFICATION_LISTENER_SETTINGS"),
        component("com.android.settings", "com.android.settings.Settings\$NotificationAccessSettingsActivity"),
    )

    fun usageAccess(context: Context) = open(context, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))

    /** Her voice: the phone's text-to-speech settings (to install or pick a voice). */
    fun voices(context: Context) = open(context, Intent("com.android.settings.TTS_SETTINGS"), Intent(Settings.ACTION_SETTINGS))

    /** An app's page in the phone's store: Play, else AppGallery, else the web. */
    fun store(context: Context, pkg: String) = runCatching {
        val flags = if (context is android.app.Activity) 0 else Intent.FLAG_ACTIVITY_NEW_TASK
        val tries = listOf(
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).setPackage("com.android.vending"),
            Intent(Intent.ACTION_VIEW, Uri.parse("appmarket://details?id=$pkg")),
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")),
            Intent(Intent.ACTION_VIEW, Uri.parse(if (huawei) "https://appgallery.huawei.com/search/$pkg" else "https://play.google.com/store/apps/details?id=$pkg")),
        )
        tries.any { i -> runCatching { context.startActivity(i.addFlags(flags)) }.isSuccess }
    }.getOrDefault(false)

    private fun component(pkg: String, cls: String) = Intent().setComponent(ComponentName(pkg, cls))
}
