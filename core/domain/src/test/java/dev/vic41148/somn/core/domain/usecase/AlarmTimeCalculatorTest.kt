package dev.vic41148.somn.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import dev.vic41148.somn.core.domain.model.Alarm
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class AlarmTimeCalculatorTest {

    private val utc = TimeZone.getTimeZone("UTC")

    /** Builds epoch millis for a UTC date/time. */
    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(utc).apply {
            clear()
            set(year, month, day, hour, minute, 0)
        }.timeInMillis

    // 2026-10-29 is a Thursday.
    private val thursday2300 = at(2026, Calendar.OCTOBER, 29, 23, 0)

    @Test
    fun oneShot_laterToday_ringsToday() {
        val now = at(2026, Calendar.OCTOBER, 29, 6, 0)
        val next = AlarmTimeCalculator.nextTriggerMillis(Alarm(hour = 7, minute = 0), now, utc)
        assertThat(next).isEqualTo(at(2026, Calendar.OCTOBER, 29, 7, 0))
    }

    @Test
    fun oneShot_alreadyPassed_ringsTomorrow() {
        val next = AlarmTimeCalculator.nextTriggerMillis(Alarm(hour = 7, minute = 0), thursday2300, utc)
        assertThat(next).isEqualTo(at(2026, Calendar.OCTOBER, 30, 7, 0))
    }

    @Test
    fun weekdayAlarm_onThursdayNight_skipsWeekend() {
        val alarm = Alarm(hour = 7, minute = 0, repeatDays = setOf(Calendar.MONDAY))
        val next = AlarmTimeCalculator.nextTriggerMillis(alarm, thursday2300, utc)
        assertThat(next).isEqualTo(at(2026, Calendar.NOVEMBER, 2, 7, 0))
    }

    @Test
    fun repeating_sameDayLater_ringsToday() {
        val now = at(2026, Calendar.OCTOBER, 29, 6, 0)
        val alarm = Alarm(hour = 7, minute = 0, repeatDays = setOf(Calendar.THURSDAY))
        val next = AlarmTimeCalculator.nextTriggerMillis(alarm, now, utc)
        assertThat(next).isEqualTo(at(2026, Calendar.OCTOBER, 29, 7, 0))
    }

    @Test
    fun repeating_sameDayPassed_ringsNextWeek() {
        val now = at(2026, Calendar.OCTOBER, 29, 8, 0)
        val alarm = Alarm(hour = 7, minute = 0, repeatDays = setOf(Calendar.THURSDAY))
        val next = AlarmTimeCalculator.nextTriggerMillis(alarm, now, utc)
        assertThat(next).isEqualTo(at(2026, Calendar.NOVEMBER, 5, 7, 0))
    }

    @Test
    fun windowStart_subtractsMinutes() {
        assertThat(AlarmTimeCalculator.windowStartMillis(1_800_000L, 30)).isEqualTo(0L)
    }
}
