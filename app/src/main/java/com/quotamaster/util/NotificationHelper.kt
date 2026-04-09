package com.quotamaster.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.quotamaster.MainActivity
import com.quotamaster.R
import com.quotamaster.receiver.StopTimerReceiver
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Centralised notification helper.
 * - Recording channel: ongoing notification with chronometer + Stop action
 * - Reminder channel: daily reminder if no sessions logged
 */
object NotificationHelper {

    private const val CHANNEL_RECORDING         = "recording_channel"
    private const val CHANNEL_REMINDER          = "reminder_channel"
    private const val NOTIFICATION_ID_RECORDING = 1001
    private const val NOTIFICATION_ID_REMINDER  = 2001

    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * Creates all notification channels. Call once from Application.onCreate().
     */
    fun createChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val recording = NotificationChannel(
            CHANNEL_RECORDING,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notification_channel_desc)
        }

        val reminder = NotificationChannel(
            CHANNEL_REMINDER,
            context.getString(R.string.notification_channel_reminder),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_reminder_desc)
        }

        manager.createNotificationChannels(listOf(recording, reminder))
    }

    // ── Recording notification with chronometer + Stop ───────────

    fun showRecording(context: Context, activityName: String, date: String, startTime: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val tapPending = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, StopTimerReceiver::class.java)
        val stopPending = PendingIntent.getBroadcast(
            context, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Parse date+startTime to epoch millis for chronometer
        val startMillis = parseToEpochMillis(date, startTime)

        val notification = NotificationCompat.Builder(context, CHANNEL_RECORDING)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_recording_title))
            .setContentText(activityName)
            .setOngoing(true)
            .setUsesChronometer(true)
            .setWhen(startMillis)
            .setContentIntent(tapPending)
            .addAction(0, context.getString(R.string.notification_stop), stopPending)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        manager.notify(NOTIFICATION_ID_RECORDING, notification)
    }

    fun showRecording(context: Context, activityName: String) {
        // Fallback without chronometer (for resume on app restart)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val tapPending = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, StopTimerReceiver::class.java)
        val stopPending = PendingIntent.getBroadcast(
            context, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_RECORDING)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_recording_title))
            .setContentText(activityName)
            .setOngoing(true)
            .setContentIntent(tapPending)
            .addAction(0, context.getString(R.string.notification_stop), stopPending)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        manager.notify(NOTIFICATION_ID_RECORDING, notification)
    }

    fun cancelRecording(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID_RECORDING)
    }

    // ── Reminder notification ────────────────────────────────────

    fun showReminderNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.reminder_notification_title))
            .setContentText(context.getString(R.string.reminder_notification_text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID_REMINDER, notification)
    }

    // ── Helpers ──────────────────────────────────────────────────

    private fun parseToEpochMillis(date: String, time: String): Long {
        return try {
            val localDate = LocalDate.parse(date, dateFmt)
            val localTime = LocalTime.parse(time, timeFmt)
            localDate.atTime(localTime)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
