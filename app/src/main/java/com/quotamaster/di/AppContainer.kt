package com.quotamaster.di

import android.content.Context
import com.quotamaster.data.db.AppDatabase
import com.quotamaster.data.repository.ActivityRepository
import com.quotamaster.data.repository.QuickLogRepository
import com.quotamaster.data.repository.SettingsRepository
import com.quotamaster.data.repository.WorkSessionRepository
import com.quotamaster.util.BackupManager
import com.quotamaster.viewmodel.ActivityDetailViewModel
import com.quotamaster.viewmodel.HomeViewModel

/**
 * Manual DI container. One instance lives in [QuotaMasterApp].
 */
class AppContainer(context: Context) {

    private val database           = AppDatabase.getInstance(context)
    val activityRepository         = ActivityRepository(database.activityDao())
    val sessionRepository          = WorkSessionRepository(database.workSessionDao())
    val quickLogRepository         = QuickLogRepository(context)
    val settingsRepository         = SettingsRepository(context)
    val backupManager              = BackupManager(context)

    val homeViewModelFactory: HomeViewModel.Factory =
        HomeViewModel.Factory(activityRepository, sessionRepository, quickLogRepository)

    fun detailViewModelFactory(activityId: Long): ActivityDetailViewModel.Factory =
        ActivityDetailViewModel.Factory(activityId, activityRepository, sessionRepository)
}
