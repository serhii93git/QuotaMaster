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
import com.quotamaster.viewmodel.SettingsViewModel

/**
 * Manual DI container. One instance lives in [QuotaMasterApp].
 */
class AppContainer(context: Context) {

    private val appContext          = context.applicationContext
    private val database           = AppDatabase.getInstance(appContext)
    val activityRepository         = ActivityRepository(database.activityDao())
    val sessionRepository          = WorkSessionRepository(database.workSessionDao())
    val quickLogRepository         = QuickLogRepository(appContext)
    val settingsRepository         = SettingsRepository(appContext)
    val backupManager              = BackupManager(appContext)

    val homeViewModelFactory: HomeViewModel.Factory =
        HomeViewModel.Factory(activityRepository, sessionRepository, quickLogRepository)

    val settingsViewModelFactory: SettingsViewModel.Factory =
        SettingsViewModel.Factory(settingsRepository, backupManager, appContext)

    fun detailViewModelFactory(activityId: Long): ActivityDetailViewModel.Factory =
        ActivityDetailViewModel.Factory(activityId, activityRepository, sessionRepository)
}