package com.quotamaster.data.repository

import android.content.Context
import com.quotamaster.util.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists quick-log timer state in SharedPreferences.
 * Only one activity can be recording at a time.
 *
 * Survives app closure — if user forgets to stop,
 * the timer resumes on next launch.
 */
data class QuickLogState(
    val isRecording: Boolean = false,
    val activityId: Long = -1L,
    val activityName: String = "",
    val date: String = "",
    val startTime: String = ""
)

class QuickLogRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())

    val state: StateFlow<QuickLogState> = _state.asStateFlow()

    fun start(activityId: Long, activityName: String, date: String, startTime: String) {
        prefs.edit()
            .putLong(KEY_ACTIVITY_ID, activityId)
            .putString(KEY_ACTIVITY_NAME, activityName)
            .putString(KEY_DATE, date)
            .putString(KEY_START_TIME, startTime)
            .apply()
        _state.value = QuickLogState(
            isRecording  = true,
            activityId   = activityId,
            activityName = activityName,
            date         = date,
            startTime    = startTime
        )
        NotificationHelper.showRecording(context, activityName, date, startTime)
    }

    fun stop(): QuickLogState {
        val snapshot = _state.value
        prefs.edit().clear().apply()
        _state.value = QuickLogState()
        NotificationHelper.cancelRecording(context)
        return snapshot
    }

    private fun load(): QuickLogState {
        val activityId = prefs.getLong(KEY_ACTIVITY_ID, -1L)
        if (activityId == -1L) return QuickLogState()
        val state = QuickLogState(
            isRecording  = true,
            activityId   = activityId,
            activityName = prefs.getString(KEY_ACTIVITY_NAME, "") ?: "",
            date         = prefs.getString(KEY_DATE, "") ?: "",
            startTime    = prefs.getString(KEY_START_TIME, "") ?: ""
        )
        // Restore notification on app restart
        NotificationHelper.showRecording(context, state.activityName, state.date, state.startTime)
        return state
    }

    companion object {
        private const val PREFS_NAME        = "quick_log"
        private const val KEY_ACTIVITY_ID   = "activity_id"
        private const val KEY_ACTIVITY_NAME = "activity_name"
        private const val KEY_DATE          = "date"
        private const val KEY_START_TIME    = "start_time"
    }
}
