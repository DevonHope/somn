package dev.vic41148.somn.core.audio.sensor

import kotlin.math.abs

/**
 * Tracks the resting acceleration magnitude the phone actually reports, so movement is measured
 * against that instead of a hard-coded 9.81 m/s².
 *
 * Phone accelerometers commonly read 9.6 to 10.0 m/s² flat on a bed. Against a fixed 9.81 that
 * offset alone is 0.05 to 0.2, the same size as the sleep-stage thresholds, so a perfectly still
 * phone could be classified as light sleep or awake all night (or, with a lucky sensor, deep all
 * night). A slow moving average follows the sensor's own bias, and quick movements stand out.
 *
 * @param alpha smoothing weight per sample. 0.02 at 10 Hz is about a 5 second time constant.
 */
class GravityBaseline(private val alpha: Float = 0.02f) {

    private var baseline = Float.NaN

    /** @return how far this sample is from the resting magnitude, in m/s², always >= 0. */
    fun deviation(magnitude: Float): Float {
        if (baseline.isNaN()) baseline = magnitude
        val deviation = abs(magnitude - baseline)
        baseline += alpha * (magnitude - baseline)
        return deviation
    }

    fun reset() {
        baseline = Float.NaN
    }
}
