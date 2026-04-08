package com.quotamaster.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.quotamaster.QuotaMasterApp
import com.quotamaster.util.NotificationHelper
import com.quotamaster.util.TimeCalculator

/**
 * Periodic worker that checks if user logged any session today.
 * If not — shows a reminder notification.
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as QuotaMasterApp
            val dao = com.quotamaster.data.db.AppDatabase
                .getInstance(applicationContext)
                .workSessionDao()

            val today = TimeCalculator.todayString()
            val count = dao.getSessionCountForDate(today)

            if (count == 0) {
                NotificationHelper.showReminderNotification(applicationContext)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("ReminderWorker", "Error checking sessions", e)
            Result.retry()
        }
    }
}
