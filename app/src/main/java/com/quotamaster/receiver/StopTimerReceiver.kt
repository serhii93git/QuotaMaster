package com.quotamaster.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.quotamaster.QuotaMasterApp
import com.quotamaster.data.model.Period
import com.quotamaster.util.NotificationHelper
import com.quotamaster.util.TimeCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receives "Stop" action from recording notification.
 * Stops the quick-log timer and saves the session.
 */
class StopTimerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as QuotaMasterApp
                val container = app.container

                val snapshot = container.quickLogRepository.stop()

                if (snapshot.isRecording && snapshot.activityId > 0) {
                    val endTime = TimeCalculator.nowTimeString()

                    // Find activity period
                    val activity = container.activityRepository.getActivityByIdOnce(snapshot.activityId)
                    val periodKey = activity?.period ?: "weekly"
                    val period = Period.entries.firstOrNull { it.key == periodKey } ?: Period.WEEKLY

                    container.sessionRepository.insertSession(
                        activityId = snapshot.activityId,
                        date       = snapshot.date,
                        startTime  = snapshot.startTime,
                        endTime    = endTime,
                        note       = "Quick log",
                        period     = period
                    )
                }

                NotificationHelper.cancelRecording(context)
            } catch (e: Exception) {
                Log.e("StopTimerReceiver", "Error stopping timer", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
