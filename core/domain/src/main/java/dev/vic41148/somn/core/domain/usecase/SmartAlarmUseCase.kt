package dev.vic41148.somn.core.domain.usecase

import dev.vic41148.somn.core.domain.model.SleepStage

/**
 * Smart Alarm logic. Determines if an alarm should trigger early
 * based on the user's current sleep stage, to avoid waking
 * them from deep sleep (to minimize sleep inertia).
 */
class SmartAlarmUseCase {

    companion object {
        /** An epoch counts as a movement burst when it is this many times the night's typical movement. */
        const val RELATIVE_MOVEMENT_FACTOR = 3f

        /**
         * Floor for a movement burst, in the bias-free units the accelerometer collector emits.
         * Keeps sensor noise on a perfectly still phone from looking like a burst.
         */
        const val MIN_BURST_MAGNITUDE = 0.02f

        /** Epochs needed before the night's baseline is trusted (5 minutes of 30 s epochs). */
        const val MIN_BASELINE_EPOCHS = 10
    }

    /**
     * @param currentTimeMillis Current epoch time in ms.
     * @param alarmTimeMillis Scheduled alarm time in ms.
     * @param wakeWindowMinutes The window (in minutes) before the alarm where early waking is allowed.
     * @param currentStage The latest classified sleep stage.
     * @param movementMagnitude The latest epoch's movement, if known.
     * @param baselineMagnitude The session's typical (median) epoch movement so far, if known.
     * @param baselineEpochCount How many epochs [baselineMagnitude] was computed from.
     * @return True if the alarm should ring now, False otherwise.
     */
    fun shouldWakeEarly(
        currentTimeMillis: Long,
        alarmTimeMillis: Long,
        wakeWindowMinutes: Int,
        currentStage: SleepStage,
        movementMagnitude: Float? = null,
        baselineMagnitude: Float? = null,
        baselineEpochCount: Int = 0
    ): Boolean {
        // If the exact alarm time is reached or passed, wake up immediately
        if (currentTimeMillis >= alarmTimeMillis) {
            return true
        }

        val windowStartMillis = alarmTimeMillis - (wakeWindowMinutes * 60 * 1000L)

        // If we are outside the wake window, do not wake
        if (currentTimeMillis < windowStartMillis) {
            return false
        }

        // Inside the wake window: wake in LIGHT sleep or AWAKE
        if (currentStage == SleepStage.LIGHT || currentStage == SleepStage.AWAKE) {
            return true
        }

        // Fixed thresholds do not fit every phone and mattress. A movement burst well above
        // this person's own baseline for the night is the classic actigraphy sign of light
        // sleep / a micro-arousal, so treat it as a wake opportunity too.
        return isMovementBurst(movementMagnitude, baselineMagnitude, baselineEpochCount)
    }

    fun isMovementBurst(
        movementMagnitude: Float?,
        baselineMagnitude: Float?,
        baselineEpochCount: Int
    ): Boolean {
        if (movementMagnitude == null || baselineMagnitude == null) return false
        if (baselineEpochCount < MIN_BASELINE_EPOCHS) return false
        val threshold = maxOf(baselineMagnitude * RELATIVE_MOVEMENT_FACTOR, MIN_BURST_MAGNITUDE)
        return movementMagnitude >= threshold
    }
}
