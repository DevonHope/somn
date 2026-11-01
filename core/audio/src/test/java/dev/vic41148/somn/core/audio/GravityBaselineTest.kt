package dev.vic41148.somn.core.audio

import dev.vic41148.somn.core.audio.sensor.GravityBaseline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GravityBaselineTest {

    @Test
    fun stillPhone_withBiasedSensor_readsNoMovement() {
        val baseline = GravityBaseline()
        // A sensor that reads 9.65 at rest: 0.16 off 9.81, which used to look like "awake".
        repeat(100) { assertEquals(0f, baseline.deviation(9.65f), 1e-4f) }
    }

    @Test
    fun suddenMovement_standsOut() {
        val baseline = GravityBaseline()
        repeat(100) { baseline.deviation(9.65f) }
        assertTrue(baseline.deviation(10.15f) > 0.45f)
    }

    @Test
    fun reset_relearnsBaseline() {
        val baseline = GravityBaseline()
        repeat(10) { baseline.deviation(9.65f) }
        baseline.reset()
        assertEquals(0f, baseline.deviation(10.0f), 1e-4f)
    }
}
