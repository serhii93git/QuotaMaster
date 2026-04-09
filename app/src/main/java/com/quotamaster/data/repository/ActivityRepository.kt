package com.quotamaster.data.repository

import com.quotamaster.data.db.ActivityDao
import com.quotamaster.data.model.Activity
import kotlinx.coroutines.flow.Flow

class ActivityRepository(private val dao: ActivityDao) {

    fun getActiveActivities(): Flow<List<Activity>> =
        dao.getActiveActivities()

    fun getArchivedActivities(): Flow<List<Activity>> =
        dao.getArchivedActivities()

    fun getActivityById(id: Long): Flow<Activity?> =
        dao.getActivityById(id)

    suspend fun insert(activity: Activity): Long {
        val maxOrder = dao.getMaxSortOrder()
        return dao.insert(activity.copy(sortOrder = maxOrder + 1))
    }

    suspend fun updateSortOrder(id: Long, sortOrder: Int) =
        dao.updateSortOrder(id, sortOrder)

    suspend fun update(activity: Activity) =
        dao.update(activity)

    suspend fun delete(activity: Activity) =
        dao.delete(activity)

    suspend fun archive(id: Long) =
        dao.archive(id)
        
    suspend fun getActivityByIdOnce(id: Long): Activity? =
        dao.getActivityByIdOnce(id)

    }
