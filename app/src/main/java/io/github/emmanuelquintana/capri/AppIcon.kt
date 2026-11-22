package io.github.emmanuelquintana.capri

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import org.json.JSONObject

/**
 * (1.11.5, Emma: "que en la app puedan elegir el icono y qué tan redondeado lo quieren") Her launcher icon: one of
 * her pictures (assets/android/icons.json, made by tools/make-icons.py) in one of 4 shapes, or the original one.
 * Each choice is an <activity-alias> of Home; exactly one is on at a time. The original is ".MainActivity", the
 * name the launcher always knew. Some launchers take a moment to show the new one, and some drop it from the home
 * screen (it's still in the app list).
 */
object AppIcon {
    val SHAPES = listOf("sys", "round", "rounded", "soft")

    fun faces(c: Context): List<String> = runCatching {
        val o = JSONObject(c.assets.open("android/icons.json").bufferedReader().use { it.readText() })
        o.getJSONArray("faces").let { a -> List(a.length()) { a.getString(it) } }
    }.getOrDefault(emptyList())

    private fun prefs(c: Context) = c.getSharedPreferences("app", Context.MODE_PRIVATE)
    /** The face in use (null: the original icon). */
    fun face(c: Context): String? = prefs(c).getString("iconFace", null)
    fun shape(c: Context): String = prefs(c).getString("iconShape", null)?.takeIf { it in SHAPES } ?: "sys"

    /** Its picture, for the app's own screen. */
    fun preview(c: Context, face: String?, shape: String): Int = if (face == null) R.mipmap.ic_launcher
        else c.resources.getIdentifier(if (shape == "sys") "icf_${face}_full" else "icf_${face}_$shape", "mipmap", c.packageName)

    /** [face] null: back to the original. Only the one chosen stays on. */
    fun apply(c: Context, face: String?, shape: String) {
        val pm = c.packageManager
        val pkg = c.packageName
        val want = if (face == null) "$pkg.MainActivity" else "$pkg.Icon_${face}_$shape"
        val all = listOf("$pkg.MainActivity") + faces(c).flatMap { f -> SHAPES.map { "$pkg.Icon_${f}_$it" } }
        // the new one on first, then the rest off: never a moment with no icon at all
        set(pm, pkg, want, true)
        for (name in all) if (name != want) set(pm, pkg, name, false)
        prefs(c).edit().putString("iconShape", shape).apply {
            if (face == null) remove("iconFace") else putString("iconFace", face)
        }.apply()
    }

    private fun set(pm: PackageManager, pkg: String, name: String, on: Boolean) {
        val state = if (on) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        runCatching {
            val cn = ComponentName(pkg, name)
            if (pm.getComponentEnabledSetting(cn) != state) pm.setComponentEnabledSetting(cn, state, PackageManager.DONT_KILL_APP)
        }.onFailure { Diag.add("W", "icon $name: ${it.message}") }
    }
}
