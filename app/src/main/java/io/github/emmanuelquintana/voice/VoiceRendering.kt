package io.github.emmanuelquintana.voice

import org.json.JSONObject
import kotlin.math.cos
import kotlin.math.round

internal data class VoiceRendering(val speed: Double = 1.0, val metallicMix: Double = 0.0, val gain: Double = 1.0) {
    init {
        require(speed.isFinite() && speed in 1.0..1.2)
        require(metallicMix.isFinite() && metallicMix in 0.0..0.35)
        require(gain.isFinite() && gain in 0.0..1.0)
    }
    fun apply(samples: FloatArray, rate: Int): FloatArray {
        require(samples.isNotEmpty() && rate in 8000..96000)
        if (speed == 1.0 && metallicMix == 0.0 && gain == 1.0) return samples
        val tempo = speed.toFloat()
        val wet = metallicMix.toFloat()
        val volume = gain.toFloat()
        return FloatArray(maxOf(1, (samples.size / tempo).toInt())) { at ->
            val position = at * tempo.toDouble()
            val left = minOf(position.toInt(), samples.lastIndex)
            val blend = position-left
            var value = samples[left] + (samples[minOf(left+1,samples.lastIndex)]-samples[left])*blend
            if (wet > 0) {
                value *= (1-wet) + wet*cos(2*Math.PI*85*at/rate)
                value = value*.94 + round(value*2048)/2048*.06
            }
            value.toFloat().coerceIn(-1f,1f)*volume
        }
    }
    companion object {
        fun load(data: JSONObject?): VoiceRendering = if (data == null) VoiceRendering() else VoiceRendering(
            data.optDouble("speed", 1.0), data.optDouble("metallic_mix", 0.0), data.optDouble("gain", 1.0))
    }
}
