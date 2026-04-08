@file:OptIn(ExperimentalMaterial3Api::class)

package com.quotamaster.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.quotamaster.R
import com.quotamaster.data.model.Activity
import com.quotamaster.di.AppContainer
import com.quotamaster.ui.util.IconMapper
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val PALETTE = listOf(
    "6650A4", "E91E63", "FF9800", "4CAF50",
    "2196F3", "00BCD4", "795548", "607D8B",
    "9C27B0", "F44336", "FFEB3B", "8BC34A"
)

private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateEditActivityScreen(
    activityId: Long,
    container: AppContainer,
    onNavigateBack: () -> Unit
) {
    val isEdit = activityId > 0
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Resolve strings
    val createTitle   = stringResource(R.string.create_activity_title)
    val editTitle     = stringResource(R.string.edit_activity_title)
    val backDesc      = stringResource(R.string.content_desc_back)
    val nameLabel     = stringResource(R.string.label_activity_name)
    val nameError     = stringResource(R.string.error_name_required)
    val typeLabel     = stringResource(R.string.label_activity_type)
    val typeDeadline  = stringResource(R.string.type_deadline)
    val typeOngoing   = stringResource(R.string.type_ongoing)
    val periodLabel   = stringResource(R.string.label_period)
    val goalHoursLbl  = stringResource(R.string.label_goal_hours)
    val goalDaysLbl   = stringResource(R.string.label_goal_days)
    val goalWeeksLbl  = stringResource(R.string.label_goal_weeks)
    val goalMonthsLbl = stringResource(R.string.label_goal_months)
    val startDateLbl  = stringResource(R.string.label_start_date)
    val endDateLbl    = stringResource(R.string.label_end_date)
    val iconLabel     = stringResource(R.string.label_icon)
    val colorLabel    = stringResource(R.string.label_color)
    val saveText      = stringResource(R.string.btn_save)
    val goalError     = stringResource(R.string.error_no_goal)
    val selectLabel   = stringResource(R.string.btn_select)
    val cancelLabel   = stringResource(R.string.btn_cancel)

    val periodLabels = mapOf(
        "daily"   to stringResource(R.string.period_daily),
        "weekly"  to stringResource(R.string.period_weekly),
        "monthly" to stringResource(R.string.period_monthly),
        "yearly"  to stringResource(R.string.period_yearly)
    )

    // Form state
    var name         by remember { mutableStateOf("") }
    var actType      by remember { mutableStateOf("deadline") }
    var period       by remember { mutableStateOf("weekly") }
    var goalHours    by remember { mutableStateOf("") }
    var goalDays     by remember { mutableStateOf("") }
    var goalWeeks    by remember { mutableStateOf("") }
    var goalMonths   by remember { mutableStateOf("") }
    var startDate    by remember { mutableStateOf(LocalDate.now()) }
    var endDate      by remember { mutableStateOf<LocalDate?>(null) }
    var iconName     by remember { mutableStateOf("School") }
    var colorHex     by remember { mutableStateOf("6650A4") }
    var sortOrder    by remember { mutableStateOf(0) }
    var nameErr      by remember { mutableStateOf(false) }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker   by remember { mutableStateOf(false) }

    // Load existing activity for edit
    if (isEdit) {
        val existingActivity by container.activityRepository
            .getActivityById(activityId)
            .collectAsState(initial = null)

        LaunchedEffect(existingActivity) {
            existingActivity?.let { a ->
                name      = a.name
                actType   = a.activityType
                period    = a.period
                goalHours = if (a.goalHoursPerPeriod > 0f) a.goalHoursPerPeriod.toString() else ""
                goalDays  = if (a.goalDaysPerPeriod > 0) a.goalDaysPerPeriod.toString() else ""
                goalWeeks = if (a.goalWeeksPerPeriod > 0) a.goalWeeksPerPeriod.toString() else ""
                goalMonths= if (a.goalMonthsPerPeriod > 0) a.goalMonthsPerPeriod.toString() else ""
                startDate = LocalDate.parse(a.startDate, dateFmt)
                endDate   = a.endDate?.let { LocalDate.parse(it, dateFmt) }
                iconName  = a.iconName
                colorHex  = a.colorHex
                sortOrder = a.sortOrder
            }
        }
    }

    // Determine which goal fields to show based on period
    val showHours  = true // always available
    val showDays   = period != "daily"
    val showWeeks  = period == "monthly" || period == "yearly"
    val showMonths = period == "yearly"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) editTitle else createTitle) },
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

            // ── Name ─────────────────────────────────────────────────
            OutlinedTextField(
                value         = name,
                onValueChange = { name = it; nameErr = false },
                label         = { Text(nameLabel) },
                isError       = nameErr,
                supportingText = if (nameErr) {{ Text(nameError) }} else null,
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )

            // ── Type ─────────────────────────────────────────────────
            Text(typeLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = actType == "deadline",
                    onClick  = { actType = "deadline" },
                    label    = { Text(typeDeadline) }
                )
                FilterChip(
                    selected = actType == "ongoing",
                    onClick  = { actType = "ongoing" },
                    label    = { Text(typeOngoing) }
                )
            }

            // ── Period ───────────────────────────────────────────────
            Text(periodLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                periodLabels.forEach { (key, label) ->
                    FilterChip(
                        selected = period == key,
                        onClick  = {
                            period = key
                            // Clear fields not applicable to new period
                            if (key == "daily") { goalDays = ""; goalWeeks = ""; goalMonths = "" }
                            if (key == "weekly") { goalWeeks = ""; goalMonths = "" }
                            if (key == "monthly") { goalMonths = "" }
                        },
                        label = { Text(label) }
                    )
                }
            }

            // ── Goals (dynamic) ──────────────────────────────────────
            if (showHours) {
                OutlinedTextField(
                    value           = goalHours,
                    onValueChange   = { goalHours = it },
                    label           = { Text(goalHoursLbl) },
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier        = Modifier.fillMaxWidth()
                )
            }
            if (showDays) {
                OutlinedTextField(
                    value           = goalDays,
                    onValueChange   = { goalDays = it },
                    label           = { Text(goalDaysLbl) },
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier        = Modifier.fillMaxWidth()
                )
            }
            if (showWeeks) {
                OutlinedTextField(
                    value           = goalWeeks,
                    onValueChange   = { goalWeeks = it },
                    label           = { Text(goalWeeksLbl) },
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier        = Modifier.fillMaxWidth()
                )
            }
            if (showMonths) {
                OutlinedTextField(
                    value           = goalMonths,
                    onValueChange   = { goalMonths = it },
                    label           = { Text(goalMonthsLbl) },
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier        = Modifier.fillMaxWidth()
                )
            }

            // ── Start date (DatePicker) ──────────────────────────────
            OutlinedTextField(
                value         = startDate.format(dateFmt),
                onValueChange = {},
                label         = { Text(startDateLbl) },
                readOnly      = true,
                singleLine    = true,
                trailingIcon  = {
                    IconButton(onClick = { showStartPicker = true }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null)
                    }
                },
                modifier = Modifier.fillMaxWidth().clickable { showStartPicker = true }
            )

            // ── End date (DatePicker, only for deadline) ─────────────
            if (actType == "deadline") {
                OutlinedTextField(
                    value         = endDate?.format(dateFmt) ?: "",
                    onValueChange = {},
                    label         = { Text(endDateLbl) },
                    readOnly      = true,
                    singleLine    = true,
                    trailingIcon  = {
                        IconButton(onClick = { showEndPicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().clickable { showEndPicker = true }
                )
            }

            // ── Icon picker ──────────────────────────────────────────
            Text(iconLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp)
            ) {
                IconMapper.allNames().forEach { iName ->
                    val isSelected = iName == iconName
                    val tint = if (isSelected) {
                        runCatching { Color(android.graphics.Color.parseColor("#$colorHex")) }
                            .getOrDefault(MaterialTheme.colorScheme.primary)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) tint.copy(alpha = 0.15f) else Color.Transparent)
                            .then(if (isSelected) Modifier.border(2.dp, tint, CircleShape) else Modifier)
                            .clickable { iconName = iName },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(IconMapper.get(iName), null, tint = tint, modifier = Modifier.size(24.dp))
                    }
                }
            }

            // ── Color picker ─────────────────────────────────────────
            Text(colorLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp)
            ) {
                PALETTE.forEach { hex ->
                    val color = runCatching { Color(android.graphics.Color.parseColor("#$hex")) }.getOrDefault(Color.Gray)
                    val isSelected = hex == colorHex
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                            .clickable { colorHex = hex }
                    )
                }
            }

            // ── Save button ──────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    nameErr = name.isBlank()

                    val h = goalHours.toFloatOrNull() ?: 0f
                    val d = goalDays.toIntOrNull() ?: 0
                    val w = goalWeeks.toIntOrNull() ?: 0
                    val m = goalMonths.toIntOrNull() ?: 0
                    val hasGoal = h > 0f || d > 0 || w > 0 || m > 0

                    if (!hasGoal && !nameErr) {
                        scope.launch { snackbarHostState.showSnackbar(goalError) }
                        return@Button
                    }

                    if (!nameErr) {
                        val activity = Activity(
                            id                  = if (isEdit) activityId else 0,
                            name                = name.trim(),
                            activityType        = actType,
                            period              = period,
                            goalHoursPerPeriod   = h,
                            goalDaysPerPeriod    = d,
                            goalWeeksPerPeriod   = w,
                            goalMonthsPerPeriod  = m,
                            startDate           = startDate.format(dateFmt),
                            endDate             = if (actType == "deadline") endDate?.format(dateFmt) else null,
                            iconName            = iconName,
                            colorHex            = colorHex,
                            sortOrder           = sortOrder
                        )
                        scope.launch {
                            if (isEdit) container.activityRepository.update(activity)
                            else container.activityRepository.insert(activity)
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(saveText) }

            Spacer(Modifier.height(24.dp))
        }
    }

    // ── Date Pickers ─────────────────────────────────────────────────
    if (showStartPicker) {
        QuotaDatePicker(
            initialDate = startDate,
            onConfirm   = { startDate = it; showStartPicker = false },
            onDismiss   = { showStartPicker = false },
            selectLabel = selectLabel,
            cancelLabel = cancelLabel
        )
    }
    if (showEndPicker) {
        QuotaDatePicker(
            initialDate = endDate ?: LocalDate.now().plusWeeks(8),
            onConfirm   = { endDate = it; showEndPicker = false },
            onDismiss   = { showEndPicker = false },
            selectLabel = selectLabel,
            cancelLabel = cancelLabel
        )
    }
}

@Composable
private fun QuotaDatePicker(
    initialDate: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    selectLabel: String,
    cancelLabel: String
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneId.of("UTC"))
            .toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    val date = Instant.ofEpochMilli(millis)
                        .atZone(ZoneId.of("UTC")).toLocalDate()
                    onConfirm(date)
                }
            }) { Text(selectLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(cancelLabel) }
        }
    ) {
        DatePicker(state = state)
    }
}