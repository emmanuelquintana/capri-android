package io.github.emmanuelquintana.capri

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import org.json.JSONObject

/**
 * Capri's process, as main.js is on the desktop: her window (Overlay), the hook server Claude
 * Code reaches (HookServer), and her "tray icon", which on a phone is this service's notification:
 * Menu, Put away / Bring back (Ctrl+Alt+C), Quit.
 */
class CapriService : Service() {
    override fun attachBaseContext(newBase: android.content.Context) = super.attachBaseContext(Lang.wrap(newBase))

    var overlay: Overlay? = null
        private set
    private lateinit var bridge: HostBridge
    private var hooks: HookServer? = null
    private var visible = true
    var ears: Ears? = null
        private set
    private var games: GameWatch? = null // gamer mode on its own, when you open a game (GameWatch.kt)
    val eyes by lazy { eyesMade = true; Eyes(this) } // her look at your screen, only when you ask (Eyes.kt)
    private var eyesMade = false

    private var screenOffAt = 0L
    private val screen = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    screenOffAt = System.currentTimeMillis()
                    overlay?.pause()
                }
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    overlay?.resume()
                    // how long the phone was put down (her breaks: phone-care.js)
                    if (screenOffAt > 0) {
                        val off = System.currentTimeMillis() - screenOffAt
                        screenOffAt = 0L
                        overlay?.emit("screen:on", JSONObject().put("offMs", off).toString())
                    }
                }
            }
            ears?.update() // her ears only with the screen on
        }
    }

    // the battery, as Android tells every app (no permission): her alerts when it's charged or low
    private var lastBattery = ""
    private val battery = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100)
            if (level < 0 || scale <= 0) return
            val status = intent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)
            val plugged = intent.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, 0) != 0
            val percent = Math.round(level * 100f / scale)
            val charging = plugged || status == android.os.BatteryManager.BATTERY_STATUS_CHARGING
            val key = "$percent/$charging"
            if (key == lastBattery) return
            lastBattery = key
            overlay?.emit("battery:update", JSONObject().put("percent", percent).put("charging", charging)
                .put("full", status == android.os.BatteryManager.BATTERY_STATUS_FULL).toString())
        }
    }
    /** The battery right now, for her page when it's (re)loaded. */
    fun batteryNow() {
        lastBattery = ""
        registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.let { battery.onReceive(this, it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // Android running short: her page drops what it can reload (her intro's drawings, her effects'
    // canvas), so Android has no reason to close her
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_RUNNING_LOW) overlay?.trim()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        overlay?.turned()
    }

    override fun onCreate() {
        super.onCreate()
        running = true
        current = this
        bridge = HostBridge(this)
        visible = bridge.store.configFlag("visible", true)
        createChannel()
        checkUpdates()
        startForegroundCompat()
        Diag.add("I", "service started (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})")
        if (!Settings.canDrawOverlays(this)) {
            Diag.add("E", "no permission to show over other apps")
            // no right to be on top: the app asks for it (MainActivity), she starts from there
            stopSelf()
            return
        }
        overlay = try {
            Overlay(this, bridge).also { it.show() }
        } catch (e: Exception) {
            Diag.add("E", "her window could not open: ${e.message}")
            null
        }
        ears = Ears(this).also { it.update() }
        games = GameWatch(this).also { it.start() }
        hooks = HookServer(assets) { evt -> overlay?.emit("claude:event", evt) }.also { it.start() }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) registerReceiver(screen, filter, RECEIVER_NOT_EXPORTED)
        else registerReceiver(screen, filter)
        val power = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) registerReceiver(battery, power, RECEIVER_NOT_EXPORTED)
        else registerReceiver(battery, power)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_MENU -> command("menu")
            ACTION_TOGGLE -> command("toggle")
            ACTION_QUIT -> quit()
            ACTION_CHAT -> command("chat")
            ACTION_SETUP -> if (pageReady) command("setupChat") else queued = "setupChat"
            ACTION_WARDROBE -> if (pageReady) command("wardrobe") else queued = "wardrobe"
            // something shared with her (ShareActivity): her page takes it (share.js)
            ACTION_SHARE -> if (pageReady) shared() else shareQueued = true
            // debug builds only (the service isn't exported otherwise): what she'd hear, for tests
            ACTION_HEAR -> if (BuildConfig.DEBUG) overlay?.emit("capri:heard", JSONObject().put("text", intent.getStringExtra("text") ?: "").toString())
            // (as if your phone were playing something, or that kind of app were in front: react.js)
            ACTION_SENSE -> if (BuildConfig.DEBUG) overlay?.emit("phone:activity", JSONObject().put("sound", intent.getBooleanExtra("sound", false)).put("kind", intent.getStringExtra("kind") ?: "").toString())
        }
        return START_STICKY
    }

    // a command for her page from the app's screen, held until her page is up
    @Volatile var pageReady = false
        private set
    @Volatile private var queued: String? = null

    fun onPageReady() {
        pageReady = true
        // after her intro, so her panel doesn't land on top of her first hello
        queued?.let { queued = null; android.os.Handler(mainLooper).postDelayed({ command(it) }, 6000) }
        if (shareQueued) { shareQueued = false; android.os.Handler(mainLooper).postDelayed({ shared() }, 6000) }
    }

    private var shareQueued = false
    private fun shared() = overlay?.emit("share:in", "{}")

    private fun command(name: String) = overlay?.emit("capri:command", JSONObject().put("name", name).toString())

    /** Her settings changed (host.js saved them): what the notification shows may have too. */
    fun onConfig(cfg: JSONObject) {
        val g = cfg.optBoolean("gamer", false)
        if (g != gamer) {
            gamer = g
            ears?.update() // gamer mode: no ears (the game's sound and your voice chat are yours)
        }
        val v = cfg.optBoolean("visible", true)
        if (v != visible) {
            visible = v
            refreshNotification()
        }
    }

    /** Her ears switched on or off (the app's Settings), or the mic allowed: her service's type too. */
    fun voiceChanged() {
        startForegroundCompat()
        ears?.update()
    }

    fun refreshNotification() {
        getSystemService(NotificationManager::class.java)!!.notify(NOTIFICATION_ID, buildNotification())
    }

    fun quit() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        running = false
        ticker.removeCallbacks(check)
        if (current === this) current = null
        Diag.add("I", "service stopped")
        try { unregisterReceiver(screen) } catch (_: Exception) {}
        try { unregisterReceiver(battery) } catch (_: Exception) {}
        hooks?.stop()
        ears?.destroy()
        ears = null
        games?.stop()
        games = null
        if (eyesMade) eyes.destroy()
        overlay?.destroy()
        overlay = null
        if (::bridge.isInitialized) bridge.shutdown()
        super.onDestroy()
    }

    // ---------- her notification: the tray icon's place ----------

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)!!
        val ch = NotificationChannel(CHANNEL, getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW)
        ch.description = getString(R.string.channel_desc)
        ch.setShowBadge(false)
        nm.createNotificationChannel(ch)
    }

    private fun startForegroundCompat() {
        val n = buildNotification()
        // the microphone, only while her ears are on (and allowed): Android shows it's in use
        val mic = Ears.enabled(this) && Ears.allowed(this)
        // and the screen capture, only while her eyes are open (Eyes.kt)
        val cast = if (Eyes.projecting && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION else 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val special = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or cast
            try {
                startForeground(NOTIFICATION_ID, n, if (mic) special or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else special)
            } catch (e: Exception) {
                Diag.add("W", "microphone not allowed for her service now: ${e.message}")
                startForeground(NOTIFICATION_ID, n, special)
            }
        } else if ((mic || cast != 0) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(NOTIFICATION_ID, n, (if (mic) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0) or cast)
        } else if (cast != 0) {
            startForeground(NOTIFICATION_ID, n, cast)
        } else {
            startForeground(NOTIFICATION_ID, n)
        }
    }

    private fun pending(action: String, code: Int) = PendingIntent.getService(
        this, code, Intent(this, CapriService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    // a newer release: her notification, and she says so on screen too (host.js)
    private val ticker = android.os.Handler(android.os.Looper.getMainLooper())
    private val check = Runnable { checkUpdates() }

    private fun checkUpdates() {
        Updater.checkInBackground(this) { r -> newRelease(r) }
        UpdateJob.schedule(this)
        ticker.removeCallbacks(check)
        ticker.postDelayed(check, Updater.CHECK_EVERY + 60_000)
    }

    /** A newer release just found: she says so on screen too (host.js). */
    fun newRelease(r: Updater.Release) {
        val json = JSONObject().put("version", r.version).put("notes", r.notes).toString()
        ticker.postDelayed({ overlay?.emit("capri:update", json) }, if (pageReady) 0 else 20_000)
    }

    // her last word, for her notification (a wink in your notification shade): what she said, her face
    private var wink: String? = null
    private var winkFace = "smug"
    private var winkAt = 0L
    private val faces = HashMap<String, android.graphics.Bitmap?>()

    fun wink(line: String, face: String) {
        wink = line.take(200)
        winkFace = face
        winkAt = System.currentTimeMillis()
        refreshNotification()
        // an old line doesn't stay there for hours: back to how she is after a while
        ticker.removeCallbacks(winkGone)
        ticker.postDelayed(winkGone, WINK_FOR)
    }

    private val winkGone = Runnable { wink = null; refreshNotification() }

    private fun face(name: String): android.graphics.Bitmap? = faces.getOrPut(name) {
        val file = if (name in FACES) "face-$name.png" else "capri-face.png"
        runCatching { assets.open("capri/assets/capri/$file").use { android.graphics.BitmapFactory.decodeStream(it) } }.getOrNull()
    }

    private fun buildNotification(): Notification {
        val said = wink?.takeIf { visible }
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(if (said != null) "Capri" else getString(R.string.notify_title))
            .setContentText(said ?: if (visible) getString(R.string.notify_text) else getString(R.string.notify_text_hidden))
            .also { b ->
                if (said != null) {
                    b.setStyle(Notification.BigTextStyle().bigText(said))
                    b.setWhen(winkAt).setShowWhen(true)
                } else b.setShowWhen(false)
                face(if (said != null) winkFace else "smug")?.let { b.setLargeIcon(it) }
            }
            // her colours: the shade shows her plate in ink, with her yellow
            .setColor(Hud.INK)
            .setColorized(true)
            .setContentIntent(pending(ACTION_MENU, 1)) // a tap on it is a click on her tray icon: her menu
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(Notification.Action.Builder(null, getString(R.string.action_menu), pending(ACTION_MENU, 2)).build())
            .addAction(Notification.Action.Builder(null, getString(if (visible) R.string.action_hide else R.string.action_show), pending(ACTION_TOGGLE, 3)).build())
            .addAction(Notification.Action.Builder(null, getString(R.string.action_quit), pending(ACTION_QUIT, 4)).build())
            .also { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) it.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE) }
            .build()
    }

    companion object {
        /** Her gamer mode is on (her page's config): small in a corner, no ears. */
        @Volatile
        var gamer = false
            private set
        @Volatile
        var running = false
            private set
        @Volatile
        var current: CapriService? = null
            private set
        private const val CHANNEL = "capri"
        private const val NOTIFICATION_ID = 1
        private const val WINK_FOR = 10 * 60 * 1000L
        const val ACTION_MENU = "capri.MENU"
        const val ACTION_TOGGLE = "capri.TOGGLE"
        const val ACTION_QUIT = "capri.QUIT"
        const val ACTION_SETUP = "capri.SETUP"
        const val ACTION_WARDROBE = "capri.WARDROBE" // the app's Settings: open her wardrobe on her
        const val ACTION_CHAT = "capri.CHAT"
        const val ACTION_HEAR = "capri.HEAR"
        const val ACTION_SENSE = "capri.SENSE"
        const val ACTION_SHARE = "capri.SHARE"
        private val FACES = setOf("aside", "calm", "laugh", "look", "many", "smug", "wide")

        fun start(context: Context, action: String? = null) {
            context.startForegroundService(Intent(context, CapriService::class.java).setAction(action))
        }

        fun send(context: Context, action: String) {
            context.startService(Intent(context, CapriService::class.java).setAction(action))
        }
    }
}
