package io.github.emmanuelquintana.voice

import java.io.File

data class PcmAudio(val samples: FloatArray, val sampleRate: Int, val profileId: String,
                    val generationMs: Long, val processPssBytes: Long?) {
    init {
        require(sampleRate in 8000..96000)
        require(samples.isNotEmpty() && samples.size <= sampleRate * 45)
        require(samples.all { it.isFinite() && it in -1f..1f } && samples.any { it != 0f })
    }
}

/** Blocking operations: call on a worker. cancel() is safe from another thread. */
interface VoiceEngine : AutoCloseable {
    fun initialize()
    fun loadModel(directory: File)
    fun loadVoiceProfile(manifest: File)
    fun synthesize(text: String, language: String = "es-MX", maxAudioTokens: Int = 320): PcmAudio
    fun cancel()
    fun dispose()
    override fun close() = dispose()
}
