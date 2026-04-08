package com.quotamaster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.quotamaster.data.repository.AppSettings
import com.quotamaster.ui.navigation.AppNavigation
import com.quotamaster.ui.theme.QuotaMasterTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as QuotaMasterApp

        setContent {
            val settings by app.container.settingsRepository.settings
                .collectAsState(initial = AppSettings())

            QuotaMasterTheme(themeMode = settings.themeMode) {
                AppNavigation(container = app.container)
            }
        }
    }
}