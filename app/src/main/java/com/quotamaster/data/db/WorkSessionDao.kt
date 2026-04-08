package com.quotamaster.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quotamaster.data.model.WorkSession
import kotlinx.coroutines.flow.Flow

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
}