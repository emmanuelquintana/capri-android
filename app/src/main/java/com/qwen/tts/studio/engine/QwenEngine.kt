package com.qwen.tts.studio.engine

class QwenEngine : AutoCloseable {
    private var nativePtr: Long = 0

    companion object {
        const val BACKEND_AUTO: Int = 0
        const val BACKEND_CPU: Int = 1
        const val BACKEND_GPU: Int = 2
    }

    class NativeParams(
        val languageId: Int = 2050,
        val instruction: String? = null,
        val speaker: String? = null,
        val maxAudioTokens: Int = 320,
    )

    class NativeResult(
        val audio: FloatArray?,
        val sampleRate: Int,
        val success: Boolean,
        val errorMsg: String?,
        val timeMs: Long,
    ) {
        val tokenizeMs: Long = 0L
        val encodeMs: Long = 0L
        val generateMs: Long = 0L
        val decodeMs: Long = 0L
        val decodeFrames: Int = 0
        val decodeSamples: Long = 0L
        val decodeGraphComputeMs: Long = 0L
    }

    class NativeCapabilities(
        val loaded: Boolean,
        val supportsCloning: Boolean,
        val supportsNamedSpeakers: Boolean,
        val supportsInstruction: Boolean,
        val speakerEmbeddingDim: Int,
        val modelKind: Int,
        val speakerCount: Int,
    )

    fun interface ProgressCallback {
        fun onProgress(tokensGenerated: Int, maxTokens: Int)
    }
    fun interface AudioChunkCallback {
        fun onAudio(samples: FloatArray, sampleRate: Int): Boolean
    }

    init {
        android.system.Os.setenv("QWEN3_TTS_LOW_MEM", "1", true)
        CpuFeatures.optimized
        nativePtr = nativeInit()
    }

    fun loadModels(modelDir: String, modelName: String? = null): Boolean =
        nativeLoadModels(nativePtr, modelDir, modelName)

    fun synthesize(
        text: String,
        referenceWav: String? = null,
        speakerEmbeddingPath: String? = null,
        params: NativeParams = NativeParams(),
    ): NativeResult =
        nativeSynthesize(nativePtr, text, referenceWav, speakerEmbeddingPath, params)

    fun getLastError(): String? = nativeGetLastError(nativePtr)
    fun synthesizeStreaming(text: String, speakerEmbeddingPath: String, params: NativeParams = NativeParams(),
        chunkSeconds: Float = .32f, callback: AudioChunkCallback): NativeResult =
        nativeSynthesizeCapriStreaming(nativePtr, text, speakerEmbeddingPath, params, chunkSeconds, callback)
    fun cancel() = nativeCancel(nativePtr)
    fun resetCancel() = nativeResetCancel(nativePtr)

    fun getActiveBackendName(): String? = nativeGetActiveBackendName()

    fun setBackendPreference(preference: Int): Boolean = nativeSetBackendPreference(preference)

    fun getCompiledBackendMask(): Int = nativeGetCompiledBackendMask()

    fun setCpuThreads(nThreads: Int): Boolean = nativeSetCpuThreads(nThreads)

    fun getCpuThreads(): Int = nativeGetCpuThreads()

    fun setProgressCallback(callback: ProgressCallback?): Boolean =
        nativeSetProgressCallback(nativePtr, callback)

    fun getModelCapabilities(): NativeCapabilities? =
        nativeGetModelCapabilities(nativePtr)

    fun extractSpeakerEmbedding(referenceWav: String, outputPath: String): Boolean =
        nativeExtractSpeakerEmbedding(nativePtr, referenceWav, outputPath)

    // (Capri V8.5) ICL, the mode her approved voice uses on PC: her reference sound's codes + its words + her
    // vector. The native library always had it; these are its JNI entry points (checked on the x86_64 build).
    // modelName: the talker's GGUF, like loadModels (its text tokenizer lives there; CI 2026-11-21: passing the
    // speech tokenizer's file gives "tokenizer.ggml.tokens not found in GGUF")
    fun loadIclPromptEncoder(modelDir: String, modelName: String?): Boolean =
        nativeLoadIclPromptEncoder(nativePtr, modelDir, modelName)

    fun extractIclPrompt(referenceWav: String, referenceText: String, outputPath: String): Boolean =
        nativeExtractIclPrompt(nativePtr, referenceWav, referenceText, outputPath)

    fun synthesizeWithIclPrompt(text: String, iclPromptPath: String, params: NativeParams = NativeParams()): NativeResult =
        nativeSynthesizeWithIclPrompt(nativePtr, text, iclPromptPath, params)

    fun getAvailableSpeakers(): List<String> {
        val raw = nativeGetAvailableSpeakers(nativePtr).orEmpty()
        return raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
    }

    override fun close() {
        if (nativePtr != 0L) {
            nativeFree(nativePtr)
            nativePtr = 0
        }
    }

    private external fun nativeInit(): Long
    private external fun nativeCancel(ptr: Long)
    private external fun nativeResetCancel(ptr: Long)
    private external fun nativeFree(ptr: Long)
    private external fun nativeSetBackendPreference(preference: Int): Boolean
    private external fun nativeGetCompiledBackendMask(): Int
    private external fun nativeSetCpuThreads(nThreads: Int): Boolean
    private external fun nativeGetCpuThreads(): Int
    private external fun nativeSetProgressCallback(ptr: Long, callback: ProgressCallback?): Boolean
    private external fun nativeGetActiveBackendName(): String?
    private external fun nativeLoadModels(ptr: Long, modelDir: String, modelName: String?): Boolean
    private external fun nativeSynthesize(
        ptr: Long,
        text: String,
        referenceWav: String?,
        speakerEmbeddingPath: String?,
        params: NativeParams?,
    ): NativeResult
    private external fun nativeGetLastError(ptr: Long): String?
    private external fun nativeSynthesizeCapriStreaming(ptr: Long, text: String, speakerEmbeddingPath: String,
        params: NativeParams, chunkSeconds: Float, callback: AudioChunkCallback): NativeResult
    private external fun nativeGetModelCapabilities(ptr: Long): NativeCapabilities?
    private external fun nativeExtractSpeakerEmbedding(ptr: Long, referenceWav: String, outputPath: String): Boolean
    private external fun nativeGetAvailableSpeakers(ptr: Long): String?
    private external fun nativeLoadIclPromptEncoder(ptr: Long, modelDir: String, tokenizerName: String?): Boolean
    private external fun nativeExtractIclPrompt(ptr: Long, referenceWav: String, referenceText: String, outputPath: String): Boolean
    private external fun nativeSynthesizeWithIclPrompt(ptr: Long, text: String, iclPromptPath: String, params: NativeParams): NativeResult
}
