package io.github.emmanuelquintana.capri

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject

/**
 * Her chat inside the app (1.11.0, the Chat tab of MainActivity): what you write goes to her page
 * (assets/android/appchat.js, "app:chat"), which answers with her own brain, and her answer comes
 * back here ("appchat:reply" in HostBridge). If she isn't on yet, she's started and your message
 * waits for her page.
 */
object AppChat {
    class Reply(val id: String, val text: String, val face: String, val ok: Boolean)

    private val main = Handler(Looper.getMainLooper())
    private val pending = mutableListOf<String>()
    @Volatile var listener: ((Reply) -> Unit)? = null

    fun ask(context: Context, text: String): String {
        val id = System.currentTimeMillis().toString()
        val json = JSONObject().put("id", id).put("text", text).toString()
        val svc = CapriService.current
        if (svc != null && svc.pageReady) svc.overlay?.emit("app:chat", json)
        else {
            synchronized(pending) { pending.add(json) }
            if (!CapriService.running) CapriService.start(context.applicationContext)
        }
        return id
    }

    /** Her page is up: what you wrote while she was starting. */
    fun flush(service: CapriService) {
        val list = synchronized(pending) { pending.toList().also { pending.clear() } }
        list.forEachIndexed { i, json -> main.postDelayed({ service.overlay?.emit("app:chat", json) }, 2500L + i * 400) }
    }

    /** (1.11.8) Her answer so far, while her AI writes it: the Chat tab shows it as it comes. */
    @Volatile var partialListener: ((String, String) -> Unit)? = null

    fun partial(json: JSONObject) {
        val id = json.optString("id"); val text = json.optString("text")
        main.post { partialListener?.invoke(id, text) }
    }

    fun deliver(json: JSONObject) {
        val r = Reply(json.optString("id"), json.optString("text"), json.optString("face", "smug"), json.optBoolean("ok", true))
        main.post { listener?.invoke(r) }
    }
}
