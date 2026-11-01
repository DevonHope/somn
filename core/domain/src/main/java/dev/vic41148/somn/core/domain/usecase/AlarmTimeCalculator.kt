package dev.vic41148.somn.core.domain.usecase

import dev.vic41148.somn.core.domain.model.Alarm
import java.util.Calendar
import java.util.TimeZone

/**
 * Single source of truth for "when does this alarm ring next".
 *
 * The scheduler (which arms AlarmManager) and the tracking service (which watches the smart
 * wake window) used to compute this separately. The tracking service ignored repeat days, so on
 * a weekday-only alarm it could watch for a time the alarm would never ring at. Both now call this.
 */
object AlarmTimeCalculator {

    /**
     * @return epoch millis of the next time [alarm] rings strictly after [nowMillis], or null if
     *   the alarm has repeat days that never match (should not happen with a non-empty set).
     */
    fun nextTriggerMillis(
        alarm: Alarm,
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long? {
        val candidate = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Check today plus the next 7 days, which covers every weekly repeat pattern.
        for (daysAhead in 0..7) {
            if (daysAhead > 0) candidate.add(Calendar.DAY_OF_YEAR, 1)
            if (candidate.timeInMillis <= nowMillis) continue
            if (alarm.repeatDays.isEmpty() ||
                alarm.repeatDays.contains(candidate.get(Calendar.DAY_OF_WEEK))
            ) {
                return candidate.timeInMillis
            }
        }
        return null
    }

    /** Start of the smart wake window for a trigger time, in epoch millis. */
    fun windowStartMillis(triggerMillis: Long, wakeWindowMinutes: Int): Long =
        triggerMillis - wakeWindowMinutes.coerceAtLeast(0) * 60_000L
}
