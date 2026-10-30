package dev.vic41148.somn.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import dev.vic41148.somn.core.domain.model.SleepStage
import org.junit.Test

class SmartAlarmUseCaseTest {

    private val useCase = SmartAlarmUseCase()
    private val alarm = 1_000_000_000L
    private val inWindow = alarm - 10 * 60_000L
    private val beforeWindow = alarm - 31 * 60_000L

    @Test
    fun beforeWindow_neverWakes() {
        assertThat(useCase.shouldWakeEarly(beforeWindow, alarm, 30, SleepStage.AWAKE)).isFalse()
    }

    @Test
    fun atAlarmTime_alwaysWakes() {
        assertThat(useCase.shouldWakeEarly(alarm, alarm, 30, SleepStage.DEEP)).isTrue()
    }

    @Test
    fun inWindow_lightSleep_wakes() {
        assertThat(useCase.shouldWakeEarly(inWindow, alarm, 30, SleepStage.LIGHT)).isTrue()
    }

    @Test
    fun inWindow_deepSleep_withoutMovementData_waits() {
        assertThat(useCase.shouldWakeEarly(inWindow, alarm, 30, SleepStage.DEEP)).isFalse()
    }

    @Test
    fun inWindow_deepStage_butMovementBurst_wakes() {
        assertThat(
            useCase.shouldWakeEarly(
                inWindow, alarm, 30, SleepStage.DEEP,
                movementMagnitude = 0.04f, baselineMagnitude = 0.01f, baselineEpochCount = 40
            )
        ).isTrue()
    }

    @Test
    fun movementBurst_needsEnoughBaseline() {
        assertThat(useCase.isMovementBurst(0.04f, 0.01f, baselineEpochCount = 3)).isFalse()
    }

    @Test
    fun movementBurst_ignoresNoiseBelowFloor() {
        // 3x a tiny baseline is still under the noise floor.
        assertThat(useCase.isMovementBurst(0.012f, 0.004f, baselineEpochCount = 40)).isFalse()
    }

    @Test
    fun movementBurst_smallRelativeChange_isNotABurst() {
        assertThat(useCase.isMovementBurst(0.025f, 0.02f, baselineEpochCount = 40)).isFalse()
    }
}
