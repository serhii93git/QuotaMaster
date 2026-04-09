package com.quotamaster.data.repository

import com.quotamaster.data.db.WorkSessionDao
import com.quotamaster.data.model.Period
import com.quotamaster.data.db.PeriodStats
import com.quotamaster.data.model.WorkSession
import com.quotamaster.util.TimeCalculator
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for work session data.
 * Encapsulates duration calculation and period tagging on insert.
 */
class WorkSessionRepository(private val dao: WorkSessionDao) {

    fun getSessionsForActivityPeriod(activityId: Long, tag: String): Flow<List<WorkSession>> =
        dao.getSessionsForActivityPeriod(activityId, tag)

    fun getSessionsForActivity(activityId: Long): Flow<List<WorkSession>> =
        dao.getSessionsForActivity(activityId)

    fun getTotalMinutesForActivityPeriod(activityId: Long, tag: String): Flow<Int?> =
        dao.getTotalMinutesForActivityPeriod(activityId, tag)

    fun getUniqueDaysForActivityPeriod(activityId: Long, tag: String): Flow<Int?> =
        dao.getUniqueDaysForActivityPeriod(activityId, tag)

    fun getUniqueWeeksForActivityPeriod(activityId: Long, tag: String): Flow<Int?> =
        dao.getUniqueWeeksForActivityPeriod(activityId, tag)

    fun getUniqueMonthsForActivityPeriod(activityId: Long, tag: String): Flow<Int?> =
        dao.getUniqueMonthsForActivityPeriod(activityId, tag)

    fun getActiveDatesForActivityPeriod(activityId: Long, tag: String): Flow<List<String>> =
        dao.getActiveDatesForActivityPeriod(activityId, tag)

    suspend fun insertSession(
        activityId: Long,
        date: String,
        startTime: String,
        endTime: String,
        note: String,
        period: Period
    ) {
        val session = WorkSession(
            activityId      = activityId,
            date            = date,
            startTime       = startTime,
            endTime         = endTime,
            durationMinutes = TimeCalculator.calculateDurationMinutes(startTime, endTime),
            periodTag       = TimeCalculator.getPeriodTag(date, period),
            note            = note
        )
        dao.insert(session)
    }

    /** Instant log — records a session with 0 duration (for day/week/month tracking). */
    suspend fun insertInstantLog(
        activityId: Long,
        date: String,
        note: String,
        period: Period
    ) {
        val time = TimeCalculator.nowTimeString()
        val session = WorkSession(
            activityId      = activityId,
            date            = date,
            startTime       = time,
            endTime         = time,
            durationMinutes = 0,
            periodTag       = TimeCalculator.getPeriodTag(date, period),
            note            = note
        )
        dao.insert(session)
    }

    /** Re-inserts a previously deleted session (for undo). */
    suspend fun restoreSession(session: WorkSession) {
        dao.insert(session)
    }

    suspend fun updateSession(session: WorkSession) = dao.update(session)

    suspend fun deleteSession(session: WorkSession) = dao.delete(session)

    suspend fun getAllSessionsOnce(): List<WorkSession> = dao.getAllSessionsOnce()

    fun getPeriodStats(activityId: Long, tag: String) =
        dao.getPeriodStats(activityId, tag)
}