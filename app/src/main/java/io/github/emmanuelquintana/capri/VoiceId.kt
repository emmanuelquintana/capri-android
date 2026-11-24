package io.github.emmanuelquintana.capri

import android.content.Context
import org.json.JSONArray
import kotlin.math.sqrt

/**
 * (1.12.0, community: "que el reconocimiento de voz solo reconozca a una sola persona e ignore los demás") Only your
 * voice. Her passive ears (WakeWord.kt) also get a voiceprint of what they hear from Vosk's small speaker model
 * (about 13 MB, downloaded once). The first times you call her after turning it on, she learns yours; after that,
 * her name in someone else's voice (the TV, someone next to you) is ignored. Nothing leaves the phone: the voiceprint
 * is a list of numbers kept in her settings here.
 */
object VoiceId {
    const val MODEL = "spk"               // WakeWord.Models' name for it (ears/spk)
    private const val PREFS = "ears"
    private const val LEARN = 3           // times she hears you before she knows your voice
    private const val SAME = 0.48         // how alike two voiceprints must be (cosine), from Vosk's x-vectors

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun enabled(c: Context) = prefs(c).getBoolean("onlyMe", false)
    fun learned(c: Context) = prefs(c).getInt("samples", 0)
    fun ready(c: Context) = learned(c) >= LEARN

    fun setEnabled(c: Context, on: Boolean) {
        prefs(c).edit().putBoolean("onlyMe", on).apply()
        if (on) WakeWord.Models.fetch(c, MODEL, anyNetwork = true) { CapriService.current?.ears?.update() }
        CapriService.current?.ears?.restartPassive()
    }

    fun forget(c: Context) {
        prefs(c).edit().remove("print").putInt("samples", 0).apply()
    }

    /**
     * Your call (with her name) and its voiceprint: true when it's you. While she's still learning, it is (and it's
     * learned); with no voiceprint (too short to tell), it's let through rather than ignoring you.
     */
    fun accept(c: Context, print: JSONArray?): Boolean {
        if (!enabled(c)) return true
        val v = print?.let { a -> DoubleArray(a.length()) { a.optDouble(it) } }?.takeIf { it.size >= 32 } ?: return true
        val p = prefs(c)
        val n = p.getInt("samples", 0)
        val mine = runCatching { JSONArray(p.getString("print", "[]")).let { a -> DoubleArray(a.length()) { a.optDouble(it) } } }.getOrNull()
        if (n < LEARN || mine == null || mine.size != v.size) {
            save(c, mean(mine?.takeIf { it.size == v.size }, n, v), n + 1)
            Diag.add("I", "ears: learning your voice (${n + 1}/$LEARN)")
            return true
        }
        val sim = cosine(mine, v)
        if (sim >= SAME) {
            save(c, mean(mine, minOf(n, 20), v), n + 1) // (it keeps learning, slowly: a cold, another room)
            return true
        }
        Diag.add("I", "ears: not your voice (${"%.2f".format(sim)}), ignored")
        return false
    }

    private fun mean(old: DoubleArray?, n: Int, v: DoubleArray): DoubleArray =
        if (old == null || n == 0) v.copyOf() else DoubleArray(v.size) { (old[it] * n + v[it]) / (n + 1) }

    private fun save(c: Context, v: DoubleArray, n: Int) {
        prefs(c).edit().putString("print", JSONArray(v.map { "%.5f".format(java.util.Locale.ROOT, it).toDouble() }).toString()).putInt("samples", n).apply()
    }

    private fun cosine(a: DoubleArray, b: DoubleArray): Double {
        var dot = 0.0; var na = 0.0; var nb = 0.0
        for (i in a.indices) { dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i] }
        return if (na == 0.0 || nb == 0.0) 0.0 else dot / (sqrt(na) * sqrt(nb))
    }
}
