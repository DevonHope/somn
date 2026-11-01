package dev.vic41148.somn.feature.tracking.service

import android.content.Context

/**
 * Remembers on disk which session an alarm ended, and when.
 *
 * The service hands an alarm-ended session to the ViewModel through an in-memory flow. If Android
 * kills the process before the app is opened in the morning (common on /e/OS), that is lost and
 * the night used to be recovered as an "Incomplete Night" ending at its last epoch. With this
 * marker the recovery path can score it as a normal night ending at the alarm.
 */
object AlarmEndedSessionStore {

    private const val PREFS = "somn_alarm_ended_session"
    private const val KEY_SESSION_ID = "session_id"
    private const val KEY_ENDED_AT = "ended_at"
    private const val KEY_SMART_WAKE = "started_by_smart_wake"

    fun save(context: Context, ended: AlarmEndedSession) {
        prefs(context).edit()
            .putLong(KEY_SESSION_ID, ended.sessionId)
            .putLong(KEY_ENDED_AT, ended.endedAtMillis)
            .putBoolean(KEY_SMART_WAKE, ended.startedBySmartWake)
            .commit() // Synchronous: the process may be killed right after the alarm rings.
    }

    /** The saved marker for [sessionId], or null if that session was not ended by an alarm. */
    fun get(context: Context, sessionId: Long): AlarmEndedSession? {
        val p = prefs(context)
        if (p.getLong(KEY_SESSION_ID, -1L) != sessionId) return null
        return AlarmEndedSession(
            sessionId = sessionId,
            startedBySmartWake = p.getBoolean(KEY_SMART_WAKE, false),
            endedAtMillis = p.getLong(KEY_ENDED_AT, System.currentTimeMillis()),
            avgBrpm = null
        )
    }

    fun clear(context: Context, sessionId: Long) {
        val p = prefs(context)
        if (p.getLong(KEY_SESSION_ID, -1L) == sessionId) p.edit().clear().apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
