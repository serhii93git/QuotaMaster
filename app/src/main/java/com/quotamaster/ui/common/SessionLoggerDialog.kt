@file:OptIn(ExperimentalMaterial3Api::class)

package com.quotamaster.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quotamaster.R
import com.quotamaster.util.TimeCalculator
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun SessionLoggerDialog(
    onDismiss: () -> Unit,
    onConfirm: (date: String, startTime: String, endTime: String, note: String) -> Unit,
    existingSession: com.quotamaster.data.model.WorkSession? = null
) {
    val isEdit = existingSession != null
    val dialogTitle    = if (isEdit) stringResource(R.string.edit_session_title)
                         else stringResource(R.string.log_session_title)
    val dateLabel      = stringResource(R.string.label_date)
    val startTimeLabel = stringResource(R.string.label_start_time)
    val endTimeLabel   = stringResource(R.string.label_end_time)
    val noteLabel      = stringResource(R.string.label_note)
    val saveText       = stringResource(R.string.btn_save)
    val cancelText     = stringResource(R.string.btn_cancel)
    val selectLabel    = stringResource(R.string.btn_select)
    val durationPrefix = stringResource(R.string.label_duration_prefix)

    var date      by remember {
        mutableStateOf(
            existingSession?.let { LocalDate.parse(it.date, dateFmt) } ?: LocalDate.now()
        )
    }
    var startTime by remember {
        mutableStateOf(
            existingSession?.let { LocalTime.parse(it.startTime, timeFmt) } ?: LocalTime.of(9, 0)
        )
    }
    var endTime   by remember {
        mutableStateOf(
            existingSession?.let { LocalTime.parse(it.endTime, timeFmt) } ?: LocalTime.of(17, 0)
        )
    }
    var note      by remember { mutableStateOf(existingSession?.note ?: "") }

    var showDatePicker  by remember { mutableStateOf(false) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker   by remember { mutableStateOf(false) }

    val dateStr  = date.format(dateFmt)
    val startStr = startTime.format(timeFmt)
    val endStr   = endTime.format(timeFmt)

    val previewMinutes: Int? = remember(startStr, endStr) {
        runCatching { TimeCalculator.calculateDurationMinutes(startStr, endStr) }.getOrNull()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // ── Date ─────────────────────────────────────────
                OutlinedTextField(
                    value         = dateStr,
                    onValueChange = {},
                    label         = { Text(dateLabel) },
                    readOnly      = true,
                    singleLine    = true,
                    trailingIcon  = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
                )

                // ── Start time ───────────────────────────────────
                OutlinedTextField(
                    value         = startStr,
                    onValueChange = {},
                    label         = { Text(startTimeLabel) },
                    readOnly      = true,
                    singleLine    = true,
                    trailingIcon  = {
                        IconButton(onClick = { showStartPicker = true }) {
                            Icon(Icons.Default.AccessTime, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().clickable { showStartPicker = true }
                )

                // ── End time ─────────────────────────────────────
                OutlinedTextField(
                    value         = endStr,
                    onValueChange = {},
                    label         = { Text(endTimeLabel) },
                    readOnly      = true,
                    singleLine    = true,
                    trailingIcon  = {
                        IconButton(onClick = { showEndPicker = true }) {
                            Icon(Icons.Default.AccessTime, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().clickable { showEndPicker = true }
                )

                // ── Note ─────────────────────────────────────────
                OutlinedTextField(
                    value         = note,
                    onValueChange = { note = it },
                    label         = { Text(noteLabel) },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )

                // ── Duration preview ─────────────────────────────
                if (previewMinutes != null) {
                    val hours = TimeCalculator.minutesToHours(previewMinutes)
                    Text(
                        text  = "%s %.2f h (%d min)".format(durationPrefix, hours, previewMinutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(dateStr, startStr, endStr, note.trim())
            }) { Text(saveText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(cancelText) }
        }
    )

    // ── Pickers ──────────────────────────────────────────────────────
    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneId.of("UTC"))
                .toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC")).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text(selectLabel) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(cancelText) }
            }
        ) { DatePicker(state = state) }
    }

    if (showStartPicker) {
        TimePickerDialog(
            initialHour   = startTime.hour,
            initialMinute = startTime.minute,
            onConfirm     = { h, m -> startTime = LocalTime.of(h, m); showStartPicker = false },
            onDismiss     = { showStartPicker = false },
            selectLabel   = selectLabel,
            cancelLabel   = cancelText
        )
    }

    if (showEndPicker) {
        TimePickerDialog(
            initialHour   = endTime.hour,
            initialMinute = endTime.minute,
            onConfirm     = { h, m -> endTime = LocalTime.of(h, m); showEndPicker = false },
            onDismiss     = { showEndPicker = false },
            selectLabel   = selectLabel,
            cancelLabel   = cancelText
        )
    }
}

@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
    selectLabel: String,
    cancelLabel: String
) {
    val state = rememberTimePickerState(
        initialHour   = initialHour,
        initialMinute = initialMinute,
        is24Hour      = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text(selectLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(cancelLabel) }
        },
        text = { TimePicker(state = state) }
    )
}