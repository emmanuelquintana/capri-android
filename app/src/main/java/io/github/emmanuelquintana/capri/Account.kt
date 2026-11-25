package io.github.emmanuelquintana.capri

import android.app.Activity
import android.content.Context
import android.os.CancellationSignal
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * (1.12.0, community: "cuentas y que se guarde todo") Your Capri account: Google or email, and her
 * memory of you, your settings, your classes and your chat saved in the cloud, to come back on a new
 * phone or after reinstalling. Firebase through its plain web APIs (no SDK, so the app stays small and
 * email works on phones without Google too). Each account only reaches its own copy (Firestore rules:
 * users/{uid} and below). Your API keys never leave the phone: they're sealed to this phone's Keystore.
 */
object Account {
    private const val KEY = "AIzaSyApnCohQfyYjhqDBvXxg1TE34uH7DZe4MM" // Firebase web key: an id, not a secret
    private const val PROJECT = "project-capricorn-5d029"
    const val WEB_CLIENT = "446203109994-jqh152vt7bdvfe0nr2cie32ck1bvb62b.apps.googleusercontent.com"
    private const val PREFS = "account"
    private const val AUTH = "https://identitytoolkit.googleapis.com/v1/accounts"
    private const val DOC = "https://firestore.googleapis.com/v1/projects/$PROJECT/databases/(default)/documents/users"
    private const val CHATLOG_MAX = 1_500_000 // (gzipped it's a few hundred KB: a cloud copy holds up to 1 MB)
    private const val QUIET_MS = 90_000L

    private val worker = Executors.newSingleThreadExecutor(Threads.factory)
    @Volatile private var pending = false

    /** Why something failed, as a string resource the screen shows. */
    class Failed(val why: Int) : Exception()

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun signedIn(c: Context) = prefs(c).getString("uid", null) != null
    fun email(c: Context): String = prefs(c).getString("email", "") ?: ""
    fun lastSaved(c: Context): Long = prefs(c).getLong("up", 0L)

    // ---------- signing in ----------

    /** Email and password; [create] makes the account first. Blocking: call it off the main thread. */
    fun withEmail(c: Context, email: String, password: String, create: Boolean) {
        val r = post("$AUTH:${if (create) "signUp" else "signInWithPassword"}?key=$KEY",
            JSONObject().put("email", email.trim()).put("password", password).put("returnSecureToken", true))
        keep(c, r)
    }

    /** "Olvidé mi contraseña": Firebase mails a link to set a new one. */
    fun resetPassword(email: String) {
        post("$AUTH:sendOobCode?key=$KEY", JSONObject().put("requestType", "PASSWORD_RESET").put("email", email.trim()))
    }

    /** Google's own account picker (Credential Manager); [done] gets null when it worked. */
    fun withGoogle(a: Activity, done: (Int?) -> Unit) {
        val cm = try {
            androidx.credentials.CredentialManager.create(a)
        } catch (_: Throwable) {
            done(R.string.account_err_google); return
        }
        val option = com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption.Builder(WEB_CLIENT).build()
        val request = androidx.credentials.GetCredentialRequest.Builder().addCredentialOption(option).build()
        cm.getCredentialAsync(a, request, CancellationSignal(), worker,
            object : androidx.credentials.CredentialManagerCallback<androidx.credentials.GetCredentialResponse, androidx.credentials.exceptions.GetCredentialException> {
                override fun onResult(result: androidx.credentials.GetCredentialResponse) {
                    val why = try {
                        val cred = result.credential
                        val token = if (cred is androidx.credentials.CustomCredential &&
                            cred.type == com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                        ) com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(cred.data).idToken else null
                        if (token == null) R.string.account_err_google else {
                            keep(a, post("$AUTH:signInWithIdp?key=$KEY", JSONObject()
                                .put("postBody", "id_token=$token&providerId=google.com")
                                .put("requestUri", "http://localhost")
                                .put("returnSecureToken", true)))
                            null
                        }
                    } catch (f: Failed) {
                        f.why
                    } catch (e: Exception) {
                        Diag.add("W", "account: google ${e.javaClass.simpleName}")
                        R.string.account_err_net
                    }
                    a.runOnUiThread { done(why) }
                }

                override fun onError(e: androidx.credentials.exceptions.GetCredentialException) {
                    Diag.add("I", "account: google ${e.javaClass.simpleName} ${e.message?.take(80)}")
                    val cancelled = e is androidx.credentials.exceptions.GetCredentialCancellationException
                    a.runOnUiThread { done(if (cancelled) 0 else R.string.account_err_google) }
                }
            })
    }

    fun signOut(c: Context) {
        prefs(c).edit().clear().apply()
    }

    /** (Google Play: an account made in the app can be deleted from the app) Erases the cloud copy and the
     *  account itself, then signs out. Blocking. Google may ask to sign in again first (account_err_again). */
    fun delete(c: Context) {
        val id = token(c)
        val conn = URL(docUrl(c)).openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer $id")
        try {
            send(conn, "DELETE", null, null)
        } catch (f: Failed) {
            if (f.why != R.string.account_err_none) throw f
        }
        try {
            post("$AUTH:delete?key=$KEY", JSONObject().put("idToken", id))
        } catch (f: Failed) {
            // (Firebase wants a recent sign-in to delete: the copy is already gone, sign in again to finish)
            signOut(c); throw f
        }
        signOut(c)
        Diag.add("I", "account: deleted")
    }

    private fun keep(c: Context, r: JSONObject) {
        val uid = r.optString("localId").ifEmpty { throw Failed(R.string.account_err_other) }
        prefs(c).edit()
            .putString("uid", uid)
            .putString("email", r.optString("email"))
            .putString("refresh", Store(c).seal(r.optString("refreshToken")))
            .putString("id", r.optString("idToken"))
            .putLong("idUntil", System.currentTimeMillis() + (r.optString("expiresIn").toLongOrNull() ?: 3600L) * 1000 - 120_000)
            .apply()
        Diag.add("I", "account: signed in")
    }

    /** A fresh id token (they last an hour), from the saved refresh token. */
    private fun token(c: Context): String {
        val p = prefs(c)
        val id = p.getString("id", null)
        if (id != null && System.currentTimeMillis() < p.getLong("idUntil", 0)) return id
        val refresh = Store(c).open(p.getString("refresh", "") ?: "").ifEmpty { throw Failed(R.string.account_err_again) }
        val conn = URL("https://securetoken.googleapis.com/v1/token?key=$KEY").openConnection() as HttpURLConnection
        val r = send(conn, "POST", "application/x-www-form-urlencoded", "grant_type=refresh_token&refresh_token=$refresh")
        val fresh = r.optString("id_token").ifEmpty { throw Failed(R.string.account_err_again) }
        p.edit().putString("id", fresh)
            .putLong("idUntil", System.currentTimeMillis() + (r.optString("expires_in").toLongOrNull() ?: 3600L) * 1000 - 120_000)
            .apply()
        return fresh
    }

    // ---------- what's saved ----------

    /** Her memory, your settings (without your keys), classes, games and chat, as one JSON. */
    fun snapshot(c: Context): JSONObject {
        val store = Store(c)
        val out = JSONObject().put("v", 1).put("at", System.currentTimeMillis()).put("app", BuildConfig.VERSION_NAME)
        store.read(Store.CONFIG)?.let { text ->
            runCatching {
                val cfg = JSONObject(text)
                // your keys (and which AI they are for) stay on this phone only: sealed to its Keystore
                cfg.keys().asSequence().toList().forEach { k -> if (k == "apiKeys" || k == "provider" || cfg.opt(k).toString().contains("enc:") || local(k)) cfg.remove(k) }
                out.put("config", cfg)
            }
        }
        store.memory()?.let { out.put("memory", it) }
        // (1.12.0, community: "guardar todas sus conversaciones") all of it; a very long one keeps its newest part
        runCatching {
            val all = ChatLog.all(c)
            var from = all.length()
            var size = 0
            while (from > 0) {
                val one = all.opt(from - 1)?.toString()?.length ?: 0
                if (size + one > CHATLOG_MAX) break
                size += one
                from--
            }
            out.put("chatlog", JSONArray().apply { for (i in from until all.length()) put(all.get(i)) })
        }
        // her neurodanzas too (1.12.0)
        for (name in listOf("agenda", "games", "sleep", "neuro")) {
            val all = c.getSharedPreferences(name, Context.MODE_PRIVATE).all
            if (all.isEmpty()) continue
            val o = JSONObject()
            all.forEach { (k, v) -> if (v is String || v is Number || v is Boolean) o.put(k, JSONArray().put(type(v)).put(v)) }
            out.put("prefs.$name", o)
        }
        // (1.12.0, Emma) your picture or icon goes with you to another phone
        out.put("profile", Profile.export(c))
        return out
    }

    /** (1.12.2, Emma: "se empieza a desconfigurar la app… seleccionó el inglés") what belongs to each device and never
     *  travels with your account: its language (Ajustes › Idioma), her size and place on this screen, its voice engine,
     *  its performance and its modes. A copy from the PC (or another phone) brought those over and changed this one. */
    private val LOCAL = setOf(
        "lang", "scale", "displayId", "iconAt", "floatAt", "float", "perfMode", "render", "motion", "motionAsked",
        "ollamaHost", "customUrl", "claudeCwd", "startOnBoot", "lan", "gamer", "watchScreen", "ghost", "ghostCards", "icon",
        "visible", "streaming", "saver", "voiceEngine", "voiceMode", "voiceNeuralModel", "voiceThreads", "voiceUnloadMin",
        "voiceDsp", "voiceFallback", "welcomed", "hooksCopied", "models",
    )
    private fun local(key: String) = key in LOCAL || key.startsWith("phoneVoice_")

    private fun type(v: Any) = when (v) { is String -> "s"; is Int -> "i"; is Long -> "l"; is Float -> "f"; is Boolean -> "b"; else -> "s" }

    /** Puts a snapshot back on this phone. Her page must be closed (it keeps its own copy and saves over files). */
    fun restore(c: Context, snap: JSONObject) {
        val store = Store(c)
        snap.optJSONObject("config")?.let { cloud ->
            val local = runCatching { JSONObject(store.read(Store.CONFIG) ?: "{}") }.getOrDefault(JSONObject())
            // your sealed keys stay as they are here, and so does what belongs to this device only (1.12.2)
            cloud.keys().forEach { k -> if (!local(k)) local.put(k, cloud.get(k)) }
            store.write(Store.CONFIG, local.toString())
        }
        snap.optJSONObject("memory")?.let { store.write(Store.MEMORY, it.toString()) }
        // (1.12.0) joined with this phone's chat, not over it: nothing you said here is lost
        snap.optJSONArray("chatlog")?.let { ChatLog.merge(c, it) }
        snap.keys().forEach { k ->
            if (!k.startsWith("prefs.")) return@forEach
            val o = snap.optJSONObject(k) ?: return@forEach
            val e = c.getSharedPreferences(k.removePrefix("prefs."), Context.MODE_PRIVATE).edit()
            o.keys().forEach { key ->
                val pair = o.optJSONArray(key) ?: return@forEach
                when (pair.optString(0)) {
                    "i" -> e.putInt(key, pair.optInt(1)); "l" -> e.putLong(key, pair.optLong(1))
                    "f" -> e.putFloat(key, pair.optDouble(1).toFloat()); "b" -> e.putBoolean(key, pair.optBoolean(1))
                    else -> e.putString(key, pair.optString(1))
                }
            }
            e.commit()
        }
        snap.optJSONObject("profile")?.let { Profile.import(c, it) }
        runCatching { Agenda.plan(c) }
        prefs(c).edit().putLong("up", maxOf(lastSaved(c), snap.optLong("at"))).apply() // (not offered again)
        Diag.add("I", "account: restored from ${snap.optString("app")}")
    }

    // ---------- the cloud copy ----------

    private fun docUrl(c: Context) = "$DOC/${prefs(c).getString("uid", "")}/data/backup"

    /** Saves this phone's snapshot. Blocking. */
    fun upload(c: Context) {
        // (1.12.0) the cloud copy first: chats from your other devices (the PC too) joined into this one, and what
        // only they save kept as it is, so a save from here never wipes theirs
        val cloud = runCatching { download(c)?.third }.getOrNull()
        cloud?.optJSONArray("chatlog")?.let { ChatLog.merge(c, it) }
        val snap = snapshot(c)
        cloud?.keys()?.forEach { k -> if (!snap.has(k)) snap.put(k, cloud.get(k)) }
        val gz = ByteArrayOutputStream().also { b -> GZIPOutputStream(b).use { it.write(snap.toString().toByteArray()) } }.toByteArray()
        if (gz.size > 900_000) throw Failed(R.string.account_err_big)
        val body = JSONObject().put("fields", JSONObject()
            .put("data", JSONObject().put("bytesValue", Base64.encodeToString(gz, Base64.NO_WRAP)))
            .put("at", JSONObject().put("integerValue", snap.getLong("at").toString()))
            .put("device", JSONObject().put("stringValue", android.os.Build.MODEL.take(40))))
        val conn = URL(docUrl(c)).openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer ${token(c)}")
        send(conn, "PATCH", "application/json", body.toString())
        prefs(c).edit().putLong("up", System.currentTimeMillis()).apply()
    }

    /** The saved copy (when it was made, on which phone, and the snapshot), or null when there's none yet. */
    fun download(c: Context): Triple<Long, String, JSONObject>? {
        val conn = URL(docUrl(c)).openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer ${token(c)}")
        val r = try {
            send(conn, "GET", null, null)
        } catch (f: Failed) {
            if (f.why == R.string.account_err_none) return null else throw f
        }
        val f = r.optJSONObject("fields") ?: return null
        val raw = Base64.decode(f.optJSONObject("data")?.optString("bytesValue") ?: return null, Base64.DEFAULT)
        val text = GZIPInputStream(ByteArrayInputStream(raw)).use { it.readBytes().toString(Charsets.UTF_8) }
        return Triple(f.optJSONObject("at")?.optString("integerValue")?.toLongOrNull() ?: 0L,
            f.optJSONObject("device")?.optString("stringValue") ?: "", JSONObject(text))
    }

    /** Something of hers changed (Store.write): saved to the cloud a little later, once things are quiet. */
    fun changed(c: Context) {
        if (pending || !signedIn(c)) return
        pending = true
        val app = c.applicationContext
        worker.execute {
            try {
                Thread.sleep(QUIET_MS)
                pending = false
                if (signedIn(app)) upload(app)
            } catch (e: Exception) {
                pending = false
                Diag.add("W", "account: autosave ${(e as? Failed)?.why ?: e.javaClass.simpleName}")
            }
        }
    }

    // ---------- plumbing ----------

    private fun post(url: String, body: JSONObject) = send(URL(url).openConnection() as HttpURLConnection, "POST", "application/json", body.toString())

    private fun send(conn: HttpURLConnection, method: String, type: String?, body: String?): JSONObject {
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            if (method == "PATCH") {
                conn.requestMethod = "POST"
                conn.setRequestProperty("X-HTTP-Method-Override", "PATCH")
            } else conn.requestMethod = method
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "$type; charset=utf-8")
                conn.outputStream.use { it.write(body.toByteArray()) }
            }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
            if (code in 200..299) return runCatching { JSONObject(text) }.getOrDefault(JSONObject())
            val msg = runCatching { JSONObject(text).optJSONObject("error")?.optString("message") }.getOrNull() ?: ""
            Diag.add("W", "account: $code ${msg.take(60)}")
            throw Failed(when {
                code == 404 -> R.string.account_err_none
                msg.startsWith("EMAIL_EXISTS") -> R.string.account_err_exists
                msg.startsWith("WEAK_PASSWORD") -> R.string.account_err_weak
                msg.startsWith("INVALID_EMAIL") || msg.startsWith("MISSING_EMAIL") -> R.string.account_err_email
                msg.startsWith("TOO_MANY") -> R.string.account_err_many
                msg.startsWith("EMAIL_NOT_FOUND") || msg.startsWith("INVALID_PASSWORD") || msg.startsWith("INVALID_LOGIN") || msg.startsWith("MISSING_PASSWORD") -> R.string.account_err_wrong
                msg.startsWith("TOKEN_EXPIRED") || msg.startsWith("CREDENTIAL_TOO_OLD") || msg.startsWith("USER_") || msg.startsWith("INVALID_REFRESH") || code == 401 || code == 403 -> R.string.account_err_again
                else -> R.string.account_err_other
            })
        } catch (f: Failed) {
            throw f
        } catch (e: Exception) {
            throw Failed(R.string.account_err_net)
        } finally {
            conn.disconnect()
        }
    }

}
