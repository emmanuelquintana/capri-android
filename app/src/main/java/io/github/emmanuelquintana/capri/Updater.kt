package io.github.emmanuelquintana.capri

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.widget.Toast
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Updates from the repo's GitHub releases: the latest one, its APK, installed over this one with
 * Android's own installer (it asks you to confirm). Needs every release signed with the same key
 * (CI's CAPRI_KEYSTORE secrets), or Android refuses to install over.
 */
object Updater {
    const val REPO = "emmanuelquintana/capri-android"
    private const val CHANNEL = "updates"
    private const val NOTIFICATION_ID = 2
    private const val PREFS = "updater"
    const val CHECK_EVERY = 3 * 3600 * 1000L

    /** [beta]: one of Emma's early builds (Beta.kt), sealed: download() opens it before installing. */
    data class Release(val tag: String, val apk: String, val size: Long, val page: String, val notes: String, val beta: Boolean = false) {
        val version get() = tag.removePrefix("v")
    }

    /** What a release says it brings (its CHANGELOG.md section), without the install footer, in plain text. */
    private fun plain(body: String): String = body.substringBefore("\n---").lines()
        .map { it.trim().replace(Regex("\\*\\*|`|\\*"), "").replace(Regex("^- "), "• ") }
        .joinToString("\n").replace(Regex("\n(?!•)"), " ").trim()

    /**
     * The latest release, or null when offline or there's none with an APK, with its notes in the
     * language the app speaks (changelog/<lang>.md at that release; Spanish when it has none).
     * Network: not on the main thread.
     */
    fun latest(context: Context): Release? {
        val public = latestSpanish()?.let { r -> localNotes(context, r)?.let { r.copy(notes = it) } ?: r }
        // (Emma's phones only, once unlocked) her beta channel: its build when it's the newest of the two
        val beta = Beta.latest(context) ?: return public
        return if (public == null || isNewer(beta.tag, public.version)) beta else public
    }

    private fun localNotes(context: Context, release: Release): String? = try {
        val lang = Lang.current(context)
        if (lang == "es" || lang !in Lang.LANGS) null
        else {
            val conn = URL("https://raw.githubusercontent.com/$REPO/${release.tag}/changelog/$lang.md").openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Capri-Android")
            conn.connectTimeout = 10000
            conn.readTimeout = 15000
            if (conn.responseCode != 200) null
            else {
                val md = conn.inputStream.bufferedReader().use { it.readText() }
                val section = md.lines().dropWhile { it.trim() != "## ${release.version}" }.drop(1).takeWhile { !it.startsWith("## ") }.joinToString("\n").trim()
                section.ifEmpty { null }?.let { plain(it) }
            }
        }
    } catch (e: Exception) {
        null
    }

    private fun latestSpanish(): Release? = try {
        val conn = URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("User-Agent", "Capri-Android")
        conn.connectTimeout = 10000
        conn.readTimeout = 15000
        if (conn.responseCode != 200) null
        else {
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val assets = json.optJSONArray("assets")
            var found: Release? = null
            for (i in 0 until (assets?.length() ?: 0)) {
                val a = assets!!.getJSONObject(i)
                if (a.optString("name").endsWith(".apk")) {
                    found = Release(json.optString("tag_name"), a.optString("browser_download_url"), a.optLong("size"), json.optString("html_url"), plain(json.optString("body")))
                    break
                }
            }
            found
        }
    } catch (e: Exception) {
        Diag.add("W", "update check: ${e.message}")
        null
    }

    /**
     * Is that tag newer than this build? "v0.5.1-android.4" against "0.5.1-android.3": number by number.
     * A beta ("1.11.8-beta.3", Beta.kt) comes before its own release: 1.11.8-beta.2 < 1.11.8-beta.3 < 1.11.8.
     */
    fun isNewer(tag: String, current: String = BuildConfig.VERSION_NAME): Boolean {
        val (a, betaA) = versionKey(tag)
        val (b, betaB) = versionKey(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return betaA > betaB
    }

    private fun versionKey(v: String): Pair<List<Long>, Long> {
        val m = Regex("^(.*)-beta\\.(\\d+)$").find(v.trim())
        val base = m?.groupValues?.get(1) ?: v
        return Regex("\\d+").findAll(base).map { it.value.toLong() }.toList() to (m?.groupValues?.get(2)?.toLongOrNull() ?: Long.MAX_VALUE)
    }

    private val busy = java.util.concurrent.atomic.AtomicBoolean(false)

    /** (1.12.4) An update is downloading right now (one at a time, whichever screen asked). */
    val downloading: Boolean get() = busy.get()

    /**
     * The APK into the app's cache, with how far along it is (0..1).
     *
     * (1.12.4) "No se instaló: el paquete parece no ser válido": a download cut short by the phone's
     * connection used to reach the installer as if it were whole, and a second download could write
     * over the file the installer was still reading. Now: one download at a time, picked up where it
     * was cut (up to [TRIES] times), its size checked against the release's, opened to check it really
     * is Capri, and only then put where the installer reads it (a rename, never half-written).
     */
    fun download(context: Context, release: Release, progress: (Float) -> Unit): File {
        if (!busy.compareAndSet(false, true)) throw IllegalStateException("already downloading")
        try {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val file = File(dir, "capri.apk")
            val part = File(dir, "capri.part")
            var expected = release.size
            var last: Exception? = null
            for (attempt in 1..TRIES) {
                try {
                    val have = part.length()
                    val conn = open(release.apk, have)
                    val append = have > 0 && conn.responseCode == 206
                    if (conn.responseCode == 416 && have > 0 && have == expected) { conn.disconnect(); break }
                    if (conn.responseCode != 200 && conn.responseCode != 206) throw IllegalStateException("HTTP ${conn.responseCode}")
                    val length = conn.contentLengthLong
                    val whole = if (append) have + length else length
                    if (expected <= 0 && length > 0) expected = whole
                    conn.inputStream.use { input ->
                        java.io.FileOutputStream(part, append).use { out ->
                            val buf = ByteArray(64 * 1024)
                            var done = if (append) have else 0L
                            while (true) {
                                val n = input.read(buf)
                                if (n < 0) break
                                out.write(buf, 0, n)
                                done += n
                                if (expected > 0) progress((done.toFloat() / expected).coerceAtMost(1f))
                            }
                            out.fd.sync()
                        }
                    }
                    if (expected <= 0 || part.length() == expected) break
                    last = java.io.IOException("cut at ${part.length()} of $expected")
                } catch (e: Exception) {
                    last = e
                }
                if (part.length() > expected && expected > 0) part.delete() // not this file: start over
                Diag.add("W", "update download, try $attempt: ${last?.message}")
                if (attempt < TRIES) Thread.sleep(2000L * attempt)
            }
            if (expected > 0 && part.length() != expected) throw last ?: java.io.IOException("incomplete download")
            val ready = File(dir, "capri.tmp")
            // a beta arrives sealed: opened (and checked) into the APK
            if (release.beta) Beta.unseal(context, part, ready) else if (!part.renameTo(ready)) throw java.io.IOException("rename")
            part.delete()
            check(context, ready)
            if (!ready.renameTo(file)) throw java.io.IOException("rename")
            return file
        } finally {
            busy.set(false)
        }
    }

    private const val TRIES = 4

    /** GitHub sends the file from another host: each hop by hand, so the resume range and the timeouts go along. */
    private fun open(url: String, from: Long): HttpURLConnection {
        var next = url
        var hops = 0
        while (true) {
            val conn = URL(next).openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Capri-Android")
            conn.instanceFollowRedirects = false
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            if (from > 0) conn.setRequestProperty("Range", "bytes=$from-")
            if (conn.responseCode !in 300..399 || hops++ >= 5) return conn
            next = conn.getHeaderField("Location") ?: return conn
            conn.disconnect()
        }
    }

    /** The downloaded file really is an APK of this app, whole, before the installer sees it. */
    private fun check(context: Context, apk: File) {
        // a zip whose index (at its very end) reads: a cut file fails here
        java.util.zip.ZipFile(apk).use { zip ->
            if (zip.getEntry("AndroidManifest.xml") == null) throw java.io.IOException("not an APK")
        }
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageArchiveInfo(apk.path, 0) ?: throw java.io.IOException("APK unreadable")
        if (info.packageName != context.packageName) throw java.io.IOException("another app: ${info.packageName}")
    }

    /**
     * Android's own installer screen with the downloaded APK, as when you open it from the browser
     * (some phones, Samsung's among them, refuse an app updating itself any other way). Falls back
     * to an install session when nothing takes it.
     */
    fun install(context: Context, apk: File) {
        val view = Intent(Intent.ACTION_VIEW)
            .setDataAndType(ApkProvider.uri(context), "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(view)
        } catch (e: Exception) {
            Diag.add("W", "installer screen: ${e.message}")
            session(context, apk)
        }
    }

    /** Hands the APK to Android's installer; it asks you, then InstallResult hears how it went. */
    private fun session(context: Context, apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(apk.length())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            session.openWrite("capri.apk", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
            val result = PendingIntent.getBroadcast(context, id, Intent(context, InstallResult::class.java), flags)
            session.commit(result.intentSender)
        }
    }

    /** Every few hours at most, from her service: a notification (and her own word) when there's a newer release. */
    fun checkInBackground(context: Context, force: Boolean = false, done: () -> Unit = {}, onNew: (Release) -> Unit = {}) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        // her service and the job both ask: not twice within half an hour
        if (now - prefs.getLong("checked", 0) < if (force) 30 * 60 * 1000L else CHECK_EVERY) return done()
        Threads.make {
            try {
                val release = latest(context) ?: return@make
                prefs.edit().putLong("checked", now).apply()
                if (!isNewer(release.tag) || prefs.getString("told", null) == release.tag) return@make
                prefs.edit().putString("told", release.tag).apply()
                notifyNew(context, release)
                onNew(release)
            } finally {
                done()
            }
        }.start()
    }

    private fun notifyNew(context: Context, release: Release) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.update_channel), NotificationManager.IMPORTANCE_DEFAULT))
        // a tap: the app, straight into the update
        val open = PendingIntent.getActivity(context, 10, Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_UPDATE, true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(context.getString(R.string.update_title, release.version))
            .setContentText(release.notes.lineSequence().firstOrNull()?.removePrefix("• ") ?: context.getString(R.string.update_text))
            .setStyle(Notification.BigTextStyle().bigText(release.notes.ifBlank { context.getString(R.string.update_text) } + "\n\n" + context.getString(R.string.update_text)))
            .setColor(Hud.YELLOW)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { nm.notify(NOTIFICATION_ID, n) }
    }

    /** How the install went: Android's confirmation screen first, then done or why not. */
    class InstallResult : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
                PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                    @Suppress("DEPRECATION")
                    val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT) ?: return
                    context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                PackageInstaller.STATUS_SUCCESS -> Diag.add("I", "updated")
                else -> {
                    val why = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "status $status"
                    Diag.add("E", "update failed: $why")
                    val msg = if (status == PackageInstaller.STATUS_FAILURE_CONFLICT || status == PackageInstaller.STATUS_FAILURE_INCOMPATIBLE)
                        R.string.update_conflict else R.string.update_failed
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    // its page, to download it there instead
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/$REPO/releases/latest")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }
            }
        }
    }
}
