@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.quotamaster.ui.detail

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quotamaster.R
import androidx.compose.foundation.combinedClickable
import com.quotamaster.ui.common.BarChart
import com.quotamaster.ui.common.BarChartEntry
import com.quotamaster.ui.common.SessionLoggerDialog
import com.quotamaster.ui.theme.RingGoalMet
import com.quotamaster.ui.theme.RingOverflow
import com.quotamaster.ui.theme.RingTrack
import com.quotamaster.util.TimeCalculator
import com.quotamaster.viewmodel.ActivityDetailState
import com.quotamaster.viewmodel.ActivityDetailViewModel
import kotlinx.coroutines.launch
import com.quotamaster.data.model.WorkSession
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.min

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivityDetailScreen(
    viewModel: ActivityDetailViewModel,
    onNavigateBack: () -> Unit,
    onEditClick: () -> Unit
) {
    val state by viewModel.detailState.collectAsState()

    var showLogger by remember { mutableStateOf(false) }
    var editingSession by remember { mutableStateOf<WorkSession?>(null) }

    val backDesc       = stringResource(R.string.content_desc_back)
    val editDesc       = stringResource(R.string.content_desc_edit)
    val logText        = stringResource(R.string.btn_log_session)
    val hoursLabel     = stringResource(R.string.label_hours)
    val daysLabel      = stringResource(R.string.label_days)
    val weeksLabel     = stringResource(R.string.label_weeks)
    val monthsLabel    = stringResource(R.string.label_months)
    val sessionsLabel  = stringResource(R.string.label_sessions)
    val streakLabel    = stringResource(R.string.label_streak)
    val avgLabel       = stringResource(R.string.label_avg_duration)
    val minShort       = stringResource(R.string.label_min_short)
    val daysUntilLabel = stringResource(R.string.label_days_until_end)
    val coachTitle     = stringResource(R.string.coach_title)
    val deletedMsg     = stringResource(R.string.session_deleted)
    val undoLabel      = stringResource(R.string.undo)
    val deleteDesc     = stringResource(R.string.content_desc_delete)
    val arrowLabel     = stringResource(R.string.label_arrow)
    val emptyTitle     = stringResource(R.string.detail_empty_title)
    val emptySubtitle  = stringResource(R.string.detail_empty_subtitle)
    val chartTitle     = stringResource(R.string.chart_hours_per_day)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val accentColor = state.activity?.let {
        runCatching { Color(android.graphics.Color.parseColor("#${it.colorHex}")) }.getOrNull()
    } ?: MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.activity?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = backDesc)
                    }
                },
                actions = {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = editDesc)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showLogger = true },
                icon    = { Icon(Icons.Default.Add, null) },
                text    = { Text(logText) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { pad ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(pad),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Progress rings (only for set goals) ──────────────────
            item {
                FlowRow(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalArrangement   = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.goalHours > 0f) {
                        GaugeRing(
                            label = hoursLabel,
                            current = "%.1f".format(state.totalHours),
                            goal = "%.0f".format(state.goalHours),
                            fraction = state.hoursFraction,
                            overflow = state.hoursOverflow,
                            color = accentColor
                        )
                    }
                    if (state.goalDays > 0) {
                        GaugeRing(
                            label = daysLabel,
                            current = state.uniqueDays.toString(),
                            goal = state.goalDays.toString(),
                            fraction = state.daysFraction,
                            overflow = state.daysOverflow,
                            color = accentColor
                        )
                    }
                    if (state.goalWeeks > 0) {
                        GaugeRing(
                            label = weeksLabel,
                            current = state.uniqueWeeks.toString(),
                            goal = state.goalWeeks.toString(),
                            fraction = state.weeksFraction,
                            overflow = state.weeksOverflow,
                            color = accentColor
                        )
                    }
                    if (state.goalMonths > 0) {
                        GaugeRing(
                            label = monthsLabel,
                            current = state.uniqueMonths.toString(),
                            goal = state.goalMonths.toString(),
                            fraction = state.monthsFraction,
                            overflow = state.monthsOverflow,
                            color = accentColor
                        )
                    }
                }
            }

            // ── Countdown ────────────────────────────────────────────
            state.daysUntilEnd?.let { daysLeft ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(daysLeft.toString(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = accentColor)
                            Spacer(Modifier.width(8.dp))
                            Text(daysUntilLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // ── Stats row ────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatChip(label = sessionsLabel, value = state.sessionCount.toString())
                    StatChip(label = streakLabel, value = "%d d".format(state.streak))
                    if (state.goalHours > 0f) {
                        StatChip(label = avgLabel, value = "%d %s".format(state.averageMinutesPerSession, minShort))
                    }
                }
            }

            // ── Week calendar ────────────────────────────────────────
            item { WeekCalendar(activeDates = state.activeDates, accentColor = accentColor) }

            // ── Coach card ───────────────────────────────────────────
            if (state.motivationMessage.isNotBlank() && state.goalHours > 0f) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(coachTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(state.motivationMessage, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // ── Empty state ──────────────────────────────────────────
            if (state.sessions.isEmpty() && state.activity != null) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(emptyTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(emptySubtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ── Sessions list ────────────────────────────────────────
            items(state.sessions, key = { it.id }) { session ->
                SwipeableSessionItem(
                    session = session, deleteDesc = deleteDesc, arrowLabel = arrowLabel,
                    onClick = { editingSession = session },
                    onDelete = {
                        viewModel.deleteSession(session)
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(deletedMsg, undoLabel, duration = SnackbarDuration.Short)
                            if (result == SnackbarResult.ActionPerformed) viewModel.restoreSession(session)
                        }
                    }
                )
            }

            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    if (showLogger) {
        SessionLoggerDialog(
            onDismiss = { showLogger = false },
            onConfirm = { date, start, end, note ->
                viewModel.addSession(date, start, end, note)
                showLogger = false
            }
        )
    }

    editingSession?.let { session ->
        SessionLoggerDialog(
            onDismiss       = { editingSession = null },
            existingSession = session,
            onConfirm       = { date, start, end, note ->
                viewModel.editSession(session, date, start, end, note)
                editingSession = null
            }
        )
    }
}

// ── Gauge Ring ───────────────────────────────────────────────────────────────

@Composable
private fun GaugeRing(label: String, current: String, goal: String, fraction: Float, overflow: Float, color: Color) {
    val animProgress by animateFloatAsState(targetValue = fraction, animationSpec = tween(900, easing = FastOutSlowInEasing), label = "ring_progress")
    val animOverflow by animateFloatAsState(targetValue = overflow, animationSpec = tween(900, easing = FastOutSlowInEasing), label = "ring_overflow")
    val ringColor = if (animProgress >= 1f) RingGoalMet else color

    Card(modifier = Modifier.size(160.dp), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(100.dp)) {
                    val strokeWidth = 12.dp.toPx()
                    val diam = min(size.width, size.height) - strokeWidth
                    val tl = Offset((size.width - diam) / 2f, (size.height - diam) / 2f)
                    val sz = Size(diam, diam)
                    val startAngle = 135f; val totalSweep = 270f
                    drawArc(RingTrack, startAngle, totalSweep, false, tl, sz, style = Stroke(strokeWidth, cap = StrokeCap.Round))
                    drawArc(ringColor, startAngle, totalSweep * animProgress, false, tl, sz, style = Stroke(strokeWidth, cap = StrokeCap.Round))
                    if (animOverflow > 0f) drawArc(RingOverflow, startAngle, totalSweep * animOverflow, false, tl, sz, style = Stroke(strokeWidth * 1.3f, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("%d%%".format((animProgress * 100).toInt()), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ringColor)
                    Text("%s / %s".format(current, goal), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

// ── Stat chip ────────────────────────────────────────────────────────────────

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Week calendar ────────────────────────────────────────────────────────────

@Composable
private fun WeekCalendar(activeDates: List<String>, accentColor: Color) {
    val today = LocalDate.now()
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val activeSet = activeDates.toSet()

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            for (i in 0L..6L) {
                val day = monday.plusDays(i)
                val dayStr = day.format(fmt)
                val isActive = activeSet.contains(dayStr)
                val isToday = day == today
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(
                            when { isActive -> accentColor; isToday -> accentColor.copy(alpha = 0.15f); else -> Color.Transparent }
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.bodySmall, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal, color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

// ── Swipeable session item ───────────────────────────────────────────────────

@Composable
private fun SwipeableSessionItem(session: WorkSession, deleteDesc: String, arrowLabel: String, onClick: () -> Unit, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = { if (it == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false })
    SwipeToDismissBox(state = dismissState, enableDismissFromStartToEnd = false, enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer).padding(end = 20.dp), contentAlignment = Alignment.CenterEnd) {
                Icon(Icons.Default.Delete, contentDescription = deleteDesc, tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            SessionCardContent(session, arrowLabel)
        }
    }
}

@Composable
private fun SessionCardContent(session: WorkSession, arrowLabel: String) {
    val hours = TimeCalculator.minutesToHours(session.durationMinutes)
    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(session.date, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (session.durationMinutes > 0) {
                    Text("%s %s %s".format(session.startTime, arrowLabel, session.endTime), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (session.note.isNotBlank()) {
                    Text(session.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (session.durationMinutes > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("%.1f h".format(hours), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("%d min".format(session.durationMinutes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
            }
        }
    }
}
