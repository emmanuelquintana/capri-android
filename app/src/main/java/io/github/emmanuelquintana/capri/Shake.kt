package io.github.emmanuelquintana.capri

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * (1.12.0, community: "que pueda sentir cuando agitemos el teléfono, y reaccione") A shake of the phone, while
 * she's on and the screen is on: three hard jolts within a second. Her page hears "phone:shaken" (shake.js).
 * The accelerometer at its slow "UI" rate, and only with the screen on: next to nothing for the battery.
 */
class Shake(private val context: Context, private val onShake: () -> Unit) : SensorEventListener {
    private val sm = context.getSystemService(SensorManager::class.java)
    private var on = false
    private val jolts = ArrayDeque<Long>()
    private var lastShake = 0L

    fun set(want: Boolean) {
        val s = sm ?: return
        if (want == on) return
        on = want
        if (want) s.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { s.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        else { s.unregisterListener(this); jolts.clear() }
    }

    override fun onSensorChanged(e: SensorEvent) {
        val g = Math.sqrt((e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2]).toDouble())
        if (g < SensorManager.GRAVITY_EARTH * 2.3) return
        val now = System.currentTimeMillis()
        if (jolts.isNotEmpty() && now - jolts.last() < 120) return // (the same jolt)
        jolts.addLast(now)
        while (jolts.isNotEmpty() && now - jolts.first() > 1000) jolts.removeFirst()
        if (jolts.size >= 3 && now - lastShake > 3000) {
            lastShake = now
            jolts.clear()
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
