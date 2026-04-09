package com.quotamaster.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quotamaster.data.model.WorkSession
import kotlinx.coroutines.flow.Flow

data class PeriodStats(
    val totalMinutes: Int = 0,
    val uniqueDays: Int = 0,
    val uniqueWeeks: Int = 0,
    val uniqueMonths: Int = 0
)

@Dao
interface WorkSessionDao {

    @Query("SELECT * FROM work_sessions WHERE activity_id = :activityId AND period_tag = :tag ORDER BY date DESC, start_time DESC")
    fun getSessionsForActivityPeriod(activityId: Long, tag: String): Flow<List<WorkSession>>

    @Query("SELECT * FROM work_sessions WHERE activity_id = :activityId ORDER BY date DESC, start_time DESC")
    fun getSessionsForActivity(activityId: Long): Flow<List<WorkSession>>

    @Query("SELECT SUM(duration_minutes) FROM work_sessions WHERE activity_id = :activityId AND period_tag = :tag")
    fun getTotalMinutesForActivityPeriod(activityId: Long, tag: String): Flow<Int?>

    @Query("SELECT COUNT(DISTINCT date) FROM work_sessions WHERE activity_id = :activityId AND period_tag = :tag")
    fun getUniqueDaysForActivityPeriod(activityId: Long, tag: String): Flow<Int?>

    /** Counts unique ISO weeks (substr extracts 'YYYY-WW' from date 'YYYY-MM-DD'). */
    @Query("SELECT COUNT(DISTINCT (substr(date,1,4) || '-W' || strftime('%W', date))) FROM work_sessions WHERE activity_id = :activityId AND period_tag = :tag")
    fun getUniqueWeeksForActivityPeriod(activityId: Long, tag: String): Flow<Int?>

    /** Counts unique months (substr extracts 'YYYY-MM' from date 'YYYY-MM-DD'). */
    @Query("SELECT COUNT(DISTINCT substr(date,1,7)) FROM work_sessions WHERE activity_id = :activityId AND period_tag = :tag")
    fun getUniqueMonthsForActivityPeriod(activityId: Long, tag: String): Flow<Int?>

    @Query("SELECT DISTINCT date FROM work_sessions WHERE activity_id = :activityId AND period_tag = :tag ORDER BY date ASC")
    fun getActiveDatesForActivityPeriod(activityId: Long, tag: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: WorkSession)

    @androidx.room.Update
    suspend fun update(session: WorkSession)

    @Delete
    suspend fun delete(session: WorkSession)

    /** One-shot count for background workers (not Flow). */
    @Query("SELECT COUNT(*) FROM work_sessions WHERE date = :date")
    suspend fun getSessionCountForDate(date: String): Int

    /** One-shot: all sessions for backup. */
    @Query("SELECT * FROM work_sessions ORDER BY id ASC")
    suspend fun getAllSessionsOnce(): List<WorkSession>

    @Query("DELETE FROM work_sessions")
    suspend fun deleteAll()

    /**
     * Combined stats query — returns all 4 metrics in one DB call.
     * Replaces 4 separate Flow queries per activity.
     */
    @Query("""
        SELECT 
            COALESCE(SUM(duration_minutes), 0) AS totalMinutes,
            COUNT(DISTINCT date) AS uniqueDays,
            COUNT(DISTINCT (substr(date,1,4) || '-W' || strftime('%W', date))) AS uniqueWeeks,
            COUNT(DISTINCT substr(date,1,7)) AS uniqueMonths
        FROM work_sessions 
        WHERE activity_id = :activityId AND period_tag = :tag
    """)
    fun getPeriodStats(activityId: Long, tag: String): Flow<PeriodStats>

}
