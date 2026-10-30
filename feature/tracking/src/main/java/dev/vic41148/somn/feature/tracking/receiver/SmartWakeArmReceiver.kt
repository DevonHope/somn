package dev.vic41148.somn.feature.tracking.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import dev.vic41148.somn.core.data.repository.SleepRepository
import dev.vic41148.somn.feature.alarm.receiver.AlarmReceiver
import dev.vic41148.somn.feature.tracking.service.SleepTrackingService
import dev.vic41148.somn.core.domain.model.TrackingMode
import dev.vic41148.somn.feature.tracking.service.TrackingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fires when an alarm's smart wake window opens (scheduled by AlarmSchedulerImpl).
 *
 * Smart wake needs live movement data. Before this receiver existed, if tracking was not running
 * at the window start (the user forgot to tap Start, or the OS killed the service overnight,
 * which aggressive builds like /e/OS do), nothing watched the window and the alarm simply rang at
 * the end of it. Now:
 *  - tracking already running: nothing to do;
 *  - a session from tonight is still open (service was killed): resume tracking into it;
 *  - otherwise: start a short movement-only session just to catch the lightest moment.
 *
 * Movement-only means no microphone: Android 14+ refuses to start a microphone-type foreground
 * service from the background, and the accelerometer is all smart wake needs.
 */
@AndroidEntryPoint
class SmartWakeArmReceiver : BroadcastReceiver() {

    @Inject lateinit var sleepRepository: SleepRepository

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L)
        if (SleepTrackingService.trackingState.value == TrackingState.TRACKING) {
            SleepTrackingService.watchAlarm(alarmId)
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = System.currentTimeMillis()
                val open = sleepRepository.getActiveSession()
                val sessionId = if (open != null && now - open.startTimeMillis < MAX_RESUME_AGE_MS) {
                    open.id
                } else {
                    sleepRepository.createSession(now)
                }
                SleepTrackingService.startTracking(
                    context = context,
                    sessionId = sessionId,
                    mode = TrackingMode.ACCELEROMETER,
                    alarmId = alarmId,
                    startedBySmartWake = true
                )
            } catch (e: Exception) {
                // Never crash the process from a background trigger. The ring alarm is still
                // armed separately, so the worst case is the old behaviour: it rings on time.
                android.util.Log.e("SmartWakeArmReceiver", "Could not start smart wake tracking", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        /** A session older than this is a forgotten one from another night, not tonight's. */
        const val MAX_RESUME_AGE_MS = 16 * 60 * 60 * 1000L
    }
}
