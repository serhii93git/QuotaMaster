package com.quotamaster.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.quotamaster.data.repository.AppSettings
import com.quotamaster.data.repository.SettingsRepository
import com.quotamaster.data.repository.ThemeMode
import com.quotamaster.util.BackupManager
import com.quotamaster.worker.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream

class SettingsViewModel(
    private val settingsRepo: SettingsRepository,
    private val backupManager: BackupManager,
    private val appContext: Context
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepo.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSettings()
        )

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepo.updateThemeMode(mode) }
    }

    fun updateReminder(enabled: Boolean, hour: Int) {
        viewModelScope.launch {
            settingsRepo.updateReminder(enabled, hour)
            if (enabled) {
                ReminderScheduler.schedule(appContext, hour)
            } else {
                ReminderScheduler.cancel(appContext)
            }
        }
    }

    fun exportBackup(onResult: (android.content.Intent) -> Unit) {
        viewModelScope.launch {
            val intent = backupManager.exportToShareIntent()
            onResult(intent)
        }
    }

    fun importBackup(inputStream: InputStream, onResult: (Result<Pair<Int, Int>>) -> Unit) {
        viewModelScope.launch {
            try {
                val counts = backupManager.importFromStream(inputStream)
                onResult(Result.success(counts))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    class Factory(
        private val settingsRepo: SettingsRepository,
        private val backupManager: BackupManager,
        private val appContext: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(settingsRepo, backupManager, appContext) as T
    }
}