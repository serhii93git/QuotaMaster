@file:OptIn(ExperimentalMaterial3Api::class)

package com.quotamaster.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quotamaster.R
import com.quotamaster.data.repository.ThemeMode
import com.quotamaster.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val titleText          = stringResource(R.string.settings_title)
    val backDesc           = stringResource(R.string.content_desc_back)
    val appearanceLabel    = stringResource(R.string.settings_appearance)
    val themeSystem        = stringResource(R.string.theme_system)
    val themeLight         = stringResource(R.string.theme_light)
    val themeDark          = stringResource(R.string.theme_dark)
    val remindersLabel     = stringResource(R.string.settings_reminders)
    val reminderToggle     = stringResource(R.string.settings_reminder_toggle)
    val reminderHourLbl    = stringResource(R.string.settings_reminder_hour)
    val dataLabel          = stringResource(R.string.settings_data)
    val exportBtn          = stringResource(R.string.settings_export_backup)
    val importBtn          = stringResource(R.string.settings_import_backup)
    val importConfirmTitle = stringResource(R.string.settings_import_confirm_title)
    val importConfirmMsg   = stringResource(R.string.settings_import_confirm_message)
    val importYes          = stringResource(R.string.settings_import_confirm_yes)
    val cancelLabel        = stringResource(R.string.btn_cancel)
    val importSuccess      = stringResource(R.string.backup_import_success)
    val importError        = stringResource(R.string.backup_import_error)

    var showImportConfirm by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // Local slider state for smooth dragging
    var sliderValue by remember(settings.reminderHour) {
        mutableFloatStateOf(settings.reminderHour.toFloat())
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            showImportConfirm = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleText) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = backDesc)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Appearance ───────────────────────────────────────
            SectionTitle(appearanceLabel)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        val label = when (mode) {
                            ThemeMode.SYSTEM -> themeSystem
                            ThemeMode.LIGHT  -> themeLight
                            ThemeMode.DARK   -> themeDark
                        }
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick  = { viewModel.updateThemeMode(mode) },
                            label    = { Text(label) }
                        )
                    }
                }
            }

            HorizontalDivider()

            // ── Reminders ────────────────────────────────────────
            SectionTitle(remindersLabel)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(reminderToggle, style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = settings.reminderEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.updateReminder(enabled, sliderValue.roundToInt())
                            }
                        )
                    }
                    if (settings.reminderEnabled) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "%s: %02d:00".format(reminderHourLbl, sliderValue.roundToInt()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = sliderValue,
                            onValueChange = { sliderValue = it },
                            onValueChangeFinished = {
                                viewModel.updateReminder(true, sliderValue.roundToInt())
                            },
                            valueRange = 6f..23f,
                            steps = 16,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            HorizontalDivider()

            // ── Data ─────────────────────────────────────────────
            SectionTitle(dataLabel)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.exportBackup { intent ->
                                context.startActivity(Intent.createChooser(intent, null))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(exportBtn) }

                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(importBtn) }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // ── Import confirmation dialog ───────────────────────────────
    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false; pendingImportUri = null },
            title = { Text(importConfirmTitle) },
            text  = { Text(importConfirmMsg) },
            confirmButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    pendingImportUri?.let { uri ->
                        val inputStream = context.contentResolver.openInputStream(uri)
                        if (inputStream != null) {
                            viewModel.importBackup(inputStream) { result ->
                                scope.launch {
                                    result.fold(
                                        onSuccess = { (acts, sess) ->
                                            snackbarHostState.showSnackbar(
                                                importSuccess.format(acts, sess)
                                            )
                                        },
                                        onFailure = {
                                            snackbarHostState.showSnackbar(importError)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    pendingImportUri = null
                }) { Text(importYes, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    pendingImportUri = null
                }) { Text(cancelLabel) }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text       = text,
        style      = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}