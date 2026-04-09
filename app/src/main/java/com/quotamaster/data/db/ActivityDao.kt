package com.quotamaster.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.quotamaster.data.model.Activity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {

    @Query("SELECT * FROM activities WHERE is_archived = 0 ORDER BY sort_order ASC, name ASC")
    fun getActiveActivities(): Flow<List<Activity>>

    @Query("SELECT * FROM activities WHERE is_archived = 1 ORDER BY sort_order ASC, name ASC")
    fun getArchivedActivities(): Flow<List<Activity>>

    @Query("UPDATE activities SET sort_order = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM activities")
    suspend fun getMaxSortOrder(): Int

    @Query("SELECT * FROM activities WHERE id = :id")
    fun getActivityById(id: Long): Flow<Activity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activity: Activity): Long

    @Update
    suspend fun update(activity: Activity)

    @Delete
    suspend fun delete(activity: Activity)

    @Query("UPDATE activities SET is_archived = 1 WHERE id = :id")
    suspend fun archive(id: Long)

    @Query("UPDATE activities SET is_archived = 0 WHERE id = :id")
    suspend fun unarchive(id: Long)

    /** One-shot: all activities for backup. */
    @Query("SELECT * FROM activities ORDER BY id ASC")
    suspend fun getAllActivitiesOnce(): List<Activity>

    /** One-shot: single activity by ID (for background receivers). */
    @Query("SELECT * FROM activities WHERE id = :id")
    suspend fun getActivityByIdOnce(id: Long): Activity?

    @Query("DELETE FROM activities")
    suspend fun deleteAll()
}
