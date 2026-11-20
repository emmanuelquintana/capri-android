// Capri Voice V8 (capri-voice-packs 207cc6747e40ff007c2b3ef70b2f1ea3661eb5e1): las piezas que VoiceClips.kt
// necesita, copiadas tal cual de VoiceProfile.kt (sha256, json) y DynamicFinish.kt (CyberPreset).
// El acabado SIGNATURE ya viene dentro de cada frase en español: aquí no se vuelve a aplicar.
package io.github.emmanuelquintana.voice

import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

enum class CyberPreset(internal val intensity: Float) {
    OFF(0f), LIGHT(.16f), SIGNATURE(.26f)
}

internal fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(1024 * 1024)
        while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

internal fun json(file: File): JSONObject {
    require(file.isFile && file.length() <= 1024 * 1024) { "Missing or excessive JSON" }
    return JSONObject(file.readText(Charsets.UTF_8).removePrefix("﻿"))
}
