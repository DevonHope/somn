package dev.vic41148.somn.feature.alarm.service

import android.content.Context
import dev.vic41148.somn.core.domain.model.Alarm
import dev.vic41148.somn.core.domain.repository.AlarmScheduler
import dev.vic41148.somn.core.domain.usecase.AlarmTimeCalculator
import dev.vic41148.somn.feature.alarm.receiver.AlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AlarmSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AlarmScheduler {

    override fun schedule(alarm: Alarm) {
        if (!alarm.isEnabled) {
            cancel(alarm.id)
            return
        }

        val triggerMillis = AlarmTimeCalculator.nextTriggerMillis(alarm, System.currentTimeMillis())
            ?: return

        AlarmReceiver.scheduleAlarm(
            context = context,
            alarmId = alarm.id,
            timeInMillis = triggerMillis,
            label = alarm.label,
            vibration = alarm.vibrationEnabled,
            gradualSeconds = alarm.gradualVolumeSeconds,
            captchaType = alarm.captchaType.name,
            wakeWindowMinutes = alarm.wakeWindowMinutes
        )

        // Smart wake only works while movement is being tracked. If the user never started
        // tracking (or the OS killed it overnight), the alarm used to just ring at the end of the
        // window. Arm a second trigger at the window start that makes sure tracking is running.
        if (alarm.wakeWindowMinutes > 0) {
            AlarmReceiver.scheduleSmartWakeArm(
                context = context,
                alarmId = alarm.id,
                timeInMillis = AlarmTimeCalculator.windowStartMillis(triggerMillis, alarm.wakeWindowMinutes)
            )
        } else {
            AlarmReceiver.cancelSmartWakeArm(context, alarm.id)
        }
    }

    override fun cancel(alarmId: Long) {
        AlarmReceiver.cancelAlarm(context, alarmId)
        AlarmReceiver.cancelSmartWakeArm(context, alarmId)
    }
}
