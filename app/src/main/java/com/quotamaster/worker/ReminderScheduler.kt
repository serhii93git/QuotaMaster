package com.quotamaster.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Schedules / cancels the daily reminder worker.
 */
object ReminderScheduler {

    private const val WORK_NAME = "daily_reminder"

    /**
     * Schedules a daily reminder at [hour]:00.
     * Uses initial delay to target the correct time today or tomorrow.
     */
    fun schedule(context: Context, hour: Int) {
        val now = LocalDateTime.now()
        val targetToday = now.toLocalDate().atTime(LocalTime.of(hour, 0))
        val target = if (now.isBefore(targetToday)) targetToday
                     else targetToday.plusDays(1)

        val initialDelay = Duration.between(now, target)

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(initialDelay.toMinutes(), TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
    }

    /**
     * Cancels the daily reminder.
     */
    fun cancel(context: Context) {
        WorkManager.getInstance(context)
            .cancelUniqueWork(WORK_NAME)
    }
}
