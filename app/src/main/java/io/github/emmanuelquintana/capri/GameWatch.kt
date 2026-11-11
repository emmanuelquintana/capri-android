package io.github.emmanuelquintana.capri

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.Process
import org.json.JSONObject

/**
 * Gamer mode on its own: only if you turned it on (Ajustes › Modo gamer › Al abrir un juego) and
 * gave her "Acceso de uso". Every few seconds, with the screen on, she asks Android which app is
 * in front; when it's one its store calls a game, her gamer mode goes on, and off again a little
 * after you leave it. Only the name of the app in front is read, nothing of what's in it, and it
 * never leaves the phone.
 *
 * 1.9.0, her reactions by herself (react.js): every few seconds her page is also told whether
 * something is playing sound (music, a video: AudioManager, no permission needed) and, only with
 * "Acceso de uso" already given, what kind of app is in front (game, video, music, social), never
 * which one.
 */
class GameWatch(private val service: CapriService) {
    private val main = Handler(Looper.getMainLooper())
    private var onByMe = false   // gamer mode is on because of a game (not because you asked)
    private var leftAt = 0L      // when the game stopped being in front
    private var lastApp = ""
    private var dismissed = ""   // the game you turned gamer mode off in
    private val games = HashMap<String, Boolean>()
    private val kinds = HashMap<String, String>()
    private var told = "" // what her page was last told (react.js)

    private val tick = object : Runnable {
        override fun run() {
            runCatching { check() }.onFailure { Diag.add("W", "games: ${it.message}") }
            main.postDelayed(this, EVERY)
        }
    }

    fun start() {
        main.removeCallbacks(tick)
        main.postDelayed(tick, EVERY)
    }

    fun stop() = main.removeCallbacks(tick)

    private fun check() {
        if (!service.getSystemService(PowerManager::class.java)!!.isInteractive) return
        val usage = allowed(service)
        if (usage) {
            val app = front() ?: lastApp
            if (app != lastApp) Diag.add("I", "in front: ${app.ifEmpty { "(home or nothing)" }} (${kind(app).ifEmpty { "app" }})")
            lastApp = app
            gamerCheck(app)
        } else if (onByMe) gamer(false)
        // what's going on on your phone, for her reactions
        val sound = runCatching { service.getSystemService(AudioManager::class.java)!!.isMusicActive }.getOrDefault(false)
        val kind = if (usage && lastApp.isNotEmpty()) kind(lastApp) else ""
        val now = JSONObject().put("sound", sound).put("kind", kind).toString()
        if (!service.pageReady) told = "" // (a fresh page is told again)
        else if (now != told) {
            told = now
            service.overlay?.emit("phone:activity", now)
        }
    }

    private fun gamerCheck(app: String) {
        if (!enabled(service)) {
            if (onByMe) gamer(false)
            return
        }
        gamerSwitch(app)
    }

    private fun gamerSwitch(app: String) {
        val playing = app.isNotEmpty() && isGame(app)
        val now = System.currentTimeMillis()
        // you turned it off yourself during this game: not again until another app comes in front
        if (onByMe && !CapriService.gamer) {
            onByMe = false
            if (playing) dismissed = app
        }
        if (app != dismissed) dismissed = ""
        when {
            playing && app == dismissed -> Unit
            playing && !CapriService.gamer -> {
                Diag.add("I", "games: $app in front, gamer mode on")
                gamer(true)
            }
            playing -> leftAt = 0L
            onByMe && CapriService.gamer -> {
                if (leftAt == 0L) leftAt = now
                // a quick look at a message mid-game doesn't end it
                if (now - leftAt > LEAVE_AFTER) {
                    Diag.add("I", "games: left the game, gamer mode off")
                    gamer(false)
                }
            }
        }
    }

    private fun gamer(on: Boolean) {
        leftAt = 0L
        val ok = CapriSettings.tell(JSONObject().put("gamer", on)) // only while her page is up
        onByMe = on && ok
    }

    // The app in front, from what Android says came to the front or went away in the last minute:
    // null when nothing happened (the same app is still there), "" when the one in front went away
    // and nothing it can name came after (the home screen often says nothing).
    private fun front(): String? {
        val usm = service.getSystemService(UsageStatsManager::class.java) ?: return null
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - 60_000, now)
        val e = UsageEvents.Event()
        var cur: String? = null
        var seen = false
        @Suppress("DEPRECATION")
        val resumed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) UsageEvents.Event.ACTIVITY_RESUMED else UsageEvents.Event.MOVE_TO_FOREGROUND
        @Suppress("DEPRECATION")
        val paused = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) UsageEvents.Event.ACTIVITY_PAUSED else UsageEvents.Event.MOVE_TO_BACKGROUND
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            val pkg = e.packageName ?: continue
            if (pkg in OVERLAYS || pkg == service.packageName) continue // floating panels, not where you are
            when (e.eventType) {
                resumed -> { cur = pkg; seen = true }
                paused -> if (pkg == cur || cur == null) { cur = ""; seen = true }
            }
        }
        return if (seen) cur else null
    }

    // what kind of app: a game, a video one (TikTok, YouTube…), music, social; "" for the rest
    private fun kind(pkg: String): String = kinds.getOrPut(pkg) {
        if (pkg.isEmpty()) return@getOrPut ""
        if (isGame(pkg)) return@getOrPut "game"
        VIDEO.firstOrNull { pkg.startsWith(it) }?.let { return@getOrPut "video" }
        val info = runCatching { service.packageManager.getApplicationInfo(pkg, 0) }.getOrNull() ?: return@getOrPut ""
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return@getOrPut ""
        when (info.category) {
            ApplicationInfo.CATEGORY_VIDEO -> "video"
            ApplicationInfo.CATEGORY_AUDIO -> "music"
            ApplicationInfo.CATEGORY_SOCIAL -> "social"
            else -> ""
        }
    }

    // a game, as its store filed it (or as it says of itself)
    private fun isGame(pkg: String): Boolean = games.getOrPut(pkg) {
        if (pkg == service.packageName || pkg.startsWith("com.samsung.android.game.")) return@getOrPut false // Samsung's game tools aren't the game
        val info = runCatching { service.packageManager.getApplicationInfo(pkg, 0) }.getOrNull() ?: return@getOrPut false
        @Suppress("DEPRECATION")
        val flagged = info.flags and ApplicationInfo.FLAG_IS_GAME != 0
        flagged || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && info.category == ApplicationInfo.CATEGORY_GAME)
    }

    companion object {
        private const val EVERY = 4000L
        private const val LEAVE_AFTER = 10_000L
        // panels that float over the app you're in (the notification shade, Samsung's game tools):
        // their coming and going doesn't mean you left your game
        private val OVERLAYS = setOf("com.android.systemui", "com.samsung.android.game.gametools", "com.samsung.android.game.gos")
        private const val PREF = "autoGamer"
        // short videos and streams, whatever category they filed themselves under
        private val VIDEO = listOf(
            "com.zhiliaoapp.musically", "com.ss.android.ugc", "com.google.android.youtube", "com.netflix.",
            "com.instagram.android", "tv.twitch.", "com.amazon.avod", "com.disney.", "com.kwai.", "com.crunchyroll.",
        )

        fun enabled(context: Context) = context.getSharedPreferences("app", Context.MODE_PRIVATE).getBoolean(PREF, false)

        fun setEnabled(context: Context, on: Boolean) {
            context.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putBoolean(PREF, on).apply()
        }

        /** "Acceso de uso" given to her (Android's special access, in its Settings). */
        fun allowed(context: Context): Boolean {
            val ops = context.getSystemService(AppOpsManager::class.java) ?: return false
            @Suppress("DEPRECATION")
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            else ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            return mode == AppOpsManager.MODE_ALLOWED
        }
    }
}
