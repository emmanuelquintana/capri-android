// Capri Voice V8 (capri-voice-packs 207cc6747e40ff007c2b3ef70b2f1ea3661eb5e1, android-sdk/qwen-voice), sin cambios.
// Solo la parte de frases (sin motor nativo): funciona desde Android 8, sin modelos ni bibliotecas nativas.
package io.github.emmanuelquintana.voice

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.util.zip.ZipFile

private const val maxClipBytes = 1024 * 1024L
private const val maxPackBytes = 75 * 1024 * 1024L
private val clipToken = Regex("[a-z0-9_]+(?:[.-][a-z0-9_]+)*")
private val checksum = Regex("[a-f0-9]{64}")
private val packId = Regex("capri-clips-(es-MX|en-US)-(demo|production|private-pilot|private-production)")

data class VoiceClip(
    val id: String,
    val eventId: String,
    val language: String,
    val emotion: String,
    val animationHint: String,
    val durationMs: Int,
    internal val asset: String,
    internal val bytes: Long,
    internal val sha: String,
    val text: String = "",
    val preset: CyberPreset = CyberPreset.OFF,
)

class VoiceClipLibrary(directory: File, allowPrivate: Boolean = false) {
    private val root = directory.canonicalFile
    private val manifest = json(File(root, "manifest.json"))
    val language = manifest.getString("language")
    val scope = manifest.getString("scope")
    val id = manifest.getString("id")
    private val clips: List<VoiceClip>

    init {
        require(!scope.startsWith("private-") || allowPrivate)
        require(manifest.getInt("schemaVersion") == 1 && packId.matches(id) &&
            id == "capri-clips-$language-$scope" && clipToken.matches(manifest.getString("version")))
        val entries = manifest.getJSONArray("clips")
        require(entries.length() in 1..900)
        clips = List(entries.length()) { index ->
            val row = entries.getJSONObject(index)
            val clip = VoiceClip(row.getString("id"), row.getString("eventId"), row.getString("language"),
                row.getString("emotion"), row.getString("animationHint"), row.getInt("durationMs"),
                row.getString("asset"), row.getLong("bytes"), row.getString("sha256"), row.optString("text"),
                CyberPreset.valueOf(row.optJSONObject("processing")?.optString("preset", "OFF") ?: "OFF"))
            val category = row.getString("eventType")
            require(listOf(clip.id, clip.eventId, clip.emotion, category).all(clipToken::matches) &&
                clip.id == "${clip.eventId}.${clip.emotion}" && clip.language == language &&
                clip.asset == "$language/$category/${clip.id}.opus" &&
                checksum.matches(clip.sha) && clip.bytes in 1..maxClipBytes &&
                clip.durationMs in 1..40000 &&
                (if (scope.startsWith("private-")) clip.text.isNotBlank() && row.getString("status") == "technical-pass"
                 else row.getString("status") == "approved"))
            clip
        }
        require(clips.distinctBy { it.id }.size == clips.size && clips.distinctBy { it.sha }.size == clips.size &&
            clips.sumOf { it.bytes } <= maxPackBytes)
    }

    fun listClips(): List<VoiceClip> = clips.toList()

    fun listEvents(): List<String> = clips.map { it.eventId }.distinct()

    fun preload(clip: VoiceClip) { asset(clip) }

    fun getEventVariants(eventId: String, language: String): List<VoiceClip> =
        clips.filter { it.eventId == eventId && it.language == language }

    fun findClip(eventId: String, language: String, emotion: String, allowedVariants: List<String> = emptyList()): VoiceClip? {
        val variants = getEventVariants(eventId, language).associateBy { it.emotion }
        return (listOf(emotion) + allowedVariants).firstNotNullOfOrNull(variants::get)
    }

    internal fun asset(clip: VoiceClip): File {
        val file = File(root, clip.asset).canonicalFile
        require(file.toPath().startsWith(root.toPath()) && file.isFile &&
            file.length() == clip.bytes && sha256(file) == clip.sha) { "Invalid clip asset: ${clip.id}" }
        return file
    }

    fun verify() {
        clips.forEach(::asset)
    }
}

/** Use an app-private directory and invoke pack operations on a worker thread. */
class VoicePackManager(directory: File, private val allowPrivate: Boolean = false) {
    private val root = directory.canonicalFile

    private fun installed(id: String): File {
        require(packId.matches(id))
        require(allowPrivate || "-private-" !in id)
        val path = File(root, id)
        require(path.canonicalFile == path)
        return path
    }

    @Synchronized
    fun listPacks(): List<String> = root.listFiles().orEmpty().map { it.name }
        .filter { packId.matches(it) && (allowPrivate || "-private-" !in it) &&
            File(installed(it), "manifest.json").isFile }.sorted()

    @Synchronized
    fun verify(id: String): VoiceClipLibrary = VoiceClipLibrary(installed(id), allowPrivate).also { it.verify() }

    @Synchronized
    fun install(archive: File, expectedSha256: String): VoiceClipLibrary =
        extract(archive, expectedSha256, replace = false)

    @Synchronized
    fun repair(archive: File, expectedSha256: String): VoiceClipLibrary =
        extract(archive, expectedSha256, replace = true)

    @Synchronized
    fun uninstall(id: String) {
        check(installed(id).deleteRecursively()) { "Unable to remove pack" }
    }

    private fun extract(archive: File, expectedSha256: String, replace: Boolean): VoiceClipLibrary {
        require(checksum.matches(expectedSha256) && archive.length() in 1..maxPackBytes &&
            sha256(archive) == expectedSha256) { "Invalid archive checksum or size" }
        root.mkdirs()
        val stage = Files.createTempDirectory(root.toPath(), ".install-").toFile()
        try {
            val library = ZipFile(archive).use { zip ->
                val entries = zip.entries().toList()
                val inventory = entries.associateBy { it.name }
                require(inventory.size == entries.size)
                fun extract(name: String, bytes: Long) {
                    val entry = inventory.getValue(name)
                    require(!entry.isDirectory && entry.size == bytes && bytes in 1..maxClipBytes)
                    val target = File(stage, name)
                    target.parentFile?.mkdirs()
                    val payload = ByteArray(bytes.toInt())
                    DataInputStream(zip.getInputStream(entry)).use {
                        it.readFully(payload)
                        require(it.read() == -1)
                    }
                    target.writeBytes(payload)
                }
                extract("manifest.json", inventory.getValue("manifest.json").size)
                val result = VoiceClipLibrary(stage, allowPrivate)
                val clips = result.listClips()
                require(inventory.keys == setOf("manifest.json") + clips.map { it.asset })
                clips.forEach { extract(it.asset, it.bytes) }
                result.verify()
                result
            }
            val destination = installed(library.id)
            if (destination.exists()) {
                check(replace) { "Pack already installed" }
                val backup = File(root, "${library.id}.backup")
                check(!backup.exists())
                Files.move(destination.toPath(), backup.toPath())
                try {
                    Files.move(stage.toPath(), destination.toPath())
                } catch (error: Exception) {
                    Files.move(backup.toPath(), destination.toPath())
                    throw error
                }
                check(backup.deleteRecursively()) { "Pack installed, but its backup could not be removed" }
            } else {
                Files.move(stage.toPath(), destination.toPath())
            }
            return VoiceClipLibrary(destination, allowPrivate)
        } finally {
            stage.deleteRecursively()
        }
    }
}

enum class VoicePlaybackState {
    VOICE_REQUESTED, VOICE_PREPARING, VOICE_STARTED, VOICE_PROGRESS, VOICE_FINISHED,
    VOICE_CANCELLED, VOICE_ERROR, VOICE_UNAVAILABLE,
}

data class VoicePlaybackEvent(
    val requestId: String,
    val state: VoicePlaybackState,
    val clip: VoiceClip,
    val positionMs: Int = 0,
    val source: String = "clip",
    val error: String? = null,
)

enum class ClipPlaybackResult { QUEUED, NEEDS_TTS, BUSY }

/** Main-thread player. A pack and player can be used without constructing QwenVoice. */
class CapriVoiceClipPlayer(private val library: VoiceClipLibrary) : AutoCloseable {
    private val main = Handler(Looper.getMainLooper())
    private var active: MediaPlayer? = null
    private var terminal: ((VoicePlaybackState, Int, String?) -> Unit)? = null
    private var progress: Runnable? = null

    fun playEvent(
        requestId: String, eventId: String, language: String, emotion: String,
        allowedVariants: List<String> = emptyList(), volume: Float = 1f, speed: Float = 1f,
        onEvent: (VoicePlaybackEvent) -> Unit,
    ): ClipPlaybackResult {
        check(Looper.myLooper() == main.looper)
        require(volume in 0f..1f && speed in 0.75f..1.25f)
        val clip = library.findClip(eventId, language, emotion, allowedVariants) ?: return ClipPlaybackResult.NEEDS_TTS
        if (active != null) return ClipPlaybackResult.BUSY
        val player = MediaPlayer()
        active = player
        fun emit(state: VoicePlaybackState, position: Int = 0, error: String? = null) {
            onEvent(VoicePlaybackEvent(requestId, state, clip, position, error = error))
        }
        terminal = { state, position, error ->
            progress?.let(main::removeCallbacks)
            progress = null
            terminal = null
            active = null
            player.release()
            emit(state, position, error)
        }
        emit(VoicePlaybackState.VOICE_REQUESTED)
        if (active !== player) return ClipPlaybackResult.QUEUED
        emit(VoicePlaybackState.VOICE_PREPARING)
        if (active !== player) return ClipPlaybackResult.QUEUED
        try {
            player.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            FileInputStream(library.asset(clip)).use { player.setDataSource(it.fd) }
            player.setOnErrorListener { current, what, extra ->
                if (active === current) terminal?.invoke(VoicePlaybackState.VOICE_ERROR, 0, "MediaPlayer $what/$extra")
                true
            }
            player.setOnCompletionListener {
                if (active === it) terminal?.invoke(VoicePlaybackState.VOICE_FINISHED, clip.durationMs, null)
            }
            player.setOnPreparedListener {
                if (active === it) {
                    try {
                        it.setVolume(volume, volume)
                        if (speed != 1f) it.playbackParams = it.playbackParams.setSpeed(speed)
                        it.start()
                    } catch (error: RuntimeException) {
                        terminal?.invoke(VoicePlaybackState.VOICE_ERROR, 0, error.message)
                        return@setOnPreparedListener
                    }
                    progress = object : Runnable {
                        override fun run() {
                            if (active !== it) return
                            emit(VoicePlaybackState.VOICE_PROGRESS, it.currentPosition)
                            if (active === it) main.postDelayed(this, 100)
                        }
                    }
                    emit(VoicePlaybackState.VOICE_STARTED)
                    if (active === it) progress?.let { tick -> main.postDelayed(tick, 100) }
                }
            }
            player.prepareAsync()
        } catch (error: Exception) {
            terminal?.invoke(VoicePlaybackState.VOICE_ERROR, 0, error.message)
        }
        return ClipPlaybackResult.QUEUED
    }

    fun stop() {
        check(Looper.myLooper() == main.looper)
        terminal?.invoke(VoicePlaybackState.VOICE_CANCELLED, 0, null)
    }

    override fun close() = stop()
}
