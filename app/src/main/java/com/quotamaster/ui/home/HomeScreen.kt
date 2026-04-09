@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.quotamaster.ui.home

import android.content.Intent
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quotamaster.R
import com.quotamaster.data.model.Activity
import com.quotamaster.ui.util.IconMapper
import com.quotamaster.util.CsvExporter
import com.quotamaster.viewmodel.ActivityCardState
import com.quotamaster.viewmodel.ActivityStatus
import com.quotamaster.viewmodel.HomeViewModel
import com.quotamaster.viewmodel.QuickLogAction
import com.quotamaster.viewmodel.SortMode
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onActivityClick: (Long) -> Unit,
    onCreateClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val cards by viewModel.displayCards.collectAsState()
    val quickLog by viewModel.quickLogState.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current

    val appTitle       = stringResource(R.string.app_name)
    val createText     = stringResource(R.string.btn_create_activity)
    val emptyText      = stringResource(R.string.home_empty)
    val hoursLabel     = stringResource(R.string.label_hours_short)
    val daysLabel      = stringResource(R.string.label_days_short)
    val weeksLabel     = stringResource(R.string.label_weeks_short)
    val monthsLabel    = stringResource(R.string.label_months_short)
    val daysLeftLabel  = stringResource(R.string.label_days_left)
    val statusOnTrack  = stringResource(R.string.status_on_track)
    val statusBehind   = stringResource(R.string.status_behind)
    val statusComplete = stringResource(R.string.status_completed)
    val recSince       = stringResource(R.string.quick_log_since)
    val recStarted     = stringResource(R.string.quick_log_started)
    val recStopped     = stringResource(R.string.quick_log_stopped)
    val instantLogged  = stringResource(R.string.quick_log_instant)
    val conflictTitle  = stringResource(R.string.quick_log_conflict_title)
    val conflictMsg    = stringResource(R.string.quick_log_conflict_message)
    val conflictYes    = stringResource(R.string.quick_log_conflict_yes)
    val conflictNo     = stringResource(R.string.btn_cancel)
    val sortDesc       = stringResource(R.string.content_desc_sort)
    val moreDesc       = stringResource(R.string.content_desc_more)
    val menuArchive    = stringResource(R.string.menu_archive)
    val menuDelete     = stringResource(R.string.menu_delete)
    val deleteTitle    = stringResource(R.string.confirm_delete_title)
    val deleteMessage  = stringResource(R.string.confirm_delete_message)
    val btnDelete      = stringResource(R.string.btn_delete)
    val btnCancel      = stringResource(R.string.btn_cancel)
    val dragDesc       = stringResource(R.string.content_desc_drag)
    val exportDesc     = stringResource(R.string.content_desc_export)
    val exportShare    = stringResource(R.string.export_share_title)
    val exportEmpty    = stringResource(R.string.export_empty)
    val archivedMsg    = stringResource(R.string.archive_done)
    val undoLabel      = stringResource(R.string.undo)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var conflictActivity by remember { mutableStateOf<Activity?>(null) }
    var deleteActivity by remember { mutableStateOf<Activity?>(null) }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        viewModel.moveItem(from.index, to.index)
        view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appTitle) },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            val (activities, sessions) = viewModel.getExportData()
                            if (sessions.isEmpty()) {
                                snackbarHostState.showSnackbar(exportEmpty)
                                return@launch
                            }
                            val uri = CsvExporter.export(context, sessions, activities)
                            if (uri != null) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, exportShare))
                            }
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = exportDesc)
                    }
                    IconButton(onClick = { viewModel.toggleSortMode() }) {
                        Icon(Icons.Default.SwapVert, contentDescription = sortDesc)
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateClick,
                icon    = { Icon(Icons.Default.Add, null) },
                text    = { Text(createText) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { pad ->
        if (cards.isEmpty()) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(pad),
                contentAlignment = Alignment.Center
            ) {
                Text(emptyText, style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                state               = lazyListState,
                modifier            = Modifier.fillMaxSize().padding(pad),
                contentPadding      = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cards, key = { it.activity.id }) { card ->
                    ReorderableItem(reorderableState, key = card.activity.id) { isDragging ->
                        val elevation by animateDpAsState(
                            if (isDragging) 8.dp else 2.dp,
                            label = "dragElevation"
                        )
                        ActivityCard(
                            card           = card,
                            hoursLabel     = hoursLabel,
                            daysLabel      = daysLabel,
                            weeksLabel     = weeksLabel,
                            monthsLabel    = monthsLabel,
                            daysLeftLabel  = daysLeftLabel,
                            recSince       = recSince,
                            statusOnTrack  = statusOnTrack,
                            statusBehind   = statusBehind,
                            statusComplete = statusComplete,
                            moreDesc       = moreDesc,
                            menuArchive    = menuArchive,
                            menuDelete     = menuDelete,
                            dragDesc       = dragDesc,
                            showDragHandle = sortMode == SortMode.MANUAL,
                            elevation      = elevation,
                            dragHandleModifier = if (sortMode == SortMode.MANUAL) {
                                Modifier.draggableHandle(
                                    onDragStarted = {
                                        view.performHapticFeedback(HapticFeedbackConstants.DRAG_START)
                                    },
                                    onDragStopped = {
                                        viewModel.onDragEnd()
                                    }
                                )
                            } else Modifier,
                            onClick        = { onActivityClick(card.activity.id) },
                            onLongClick    = {
                                when (viewModel.onQuickLogPress(card.activity)) {
                                    QuickLogAction.STARTED -> scope.launch {
                                        snackbarHostState.showSnackbar(recStarted, duration = SnackbarDuration.Short)
                                    }
                                    QuickLogAction.STOPPED -> scope.launch {
                                        snackbarHostState.showSnackbar(recStopped, duration = SnackbarDuration.Short)
                                    }
                                    QuickLogAction.CONFLICT -> { conflictActivity = card.activity }
                                    QuickLogAction.INSTANT_LOGGED -> scope.launch {
                                        snackbarHostState.showSnackbar(instantLogged, duration = SnackbarDuration.Short)
                                    }
                                }
                            },
                            onArchive      = {
                                viewModel.archiveActivity(card.activity.id) { name ->
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = archivedMsg.format(name),
                                            actionLabel = undoLabel,
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.unarchiveActivity(card.activity.id)
                                        }
                                    }
                                }
                            },
                            onDelete       = { deleteActivity = card.activity },
                            modifier       = Modifier.animateItemPlacement()
                        )
                    }
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    // ── Conflict dialog ──────────────────────────────────────────────
    conflictActivity?.let { activity ->
        val currentName = quickLog.activityName
        AlertDialog(
            onDismissRequest = { conflictActivity = null },
            title = { Text(conflictTitle) },
            text  = { Text(conflictMsg.format(currentName)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.forceStopAndStart(activity)
                    conflictActivity = null
                    scope.launch {
                        snackbarHostState.showSnackbar(recStarted, duration = SnackbarDuration.Short)
                    }
                }) { Text(conflictYes) }
            },
            dismissButton = {
                TextButton(onClick = { conflictActivity = null }) { Text(conflictNo) }
            }
        )
    }

    // ── Delete confirmation dialog ───────────────────────────────────
    deleteActivity?.let { activity ->
        AlertDialog(
            onDismissRequest = { deleteActivity = null },
            title = { Text(deleteTitle) },
            text  = { Text(deleteMessage.format(activity.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteActivity(activity)
                    deleteActivity = null
                }) { Text(btnDelete, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteActivity = null }) { Text(btnCancel) }
            }
        )
    }
}

@Composable
private fun ActivityCard(
    card: ActivityCardState,
    hoursLabel: String,
    daysLabel: String,
    weeksLabel: String,
    monthsLabel: String,
    daysLeftLabel: String,
    recSince: String,
    statusOnTrack: String,
    statusBehind: String,
    statusComplete: String,
    moreDesc: String,
    menuArchive: String,
    menuDelete: String,
    dragDesc: String,
    showDragHandle: Boolean,
    elevation: androidx.compose.ui.unit.Dp,
    dragHandleModifier: Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = runCatching {
        Color(android.graphics.Color.parseColor("#${card.activity.colorHex}"))
    }.getOrDefault(MaterialTheme.colorScheme.primary)

    val statusText = when {
        card.isRecording -> recSince.format(card.recordingSince)
        card.status == ActivityStatus.ON_TRACK  -> statusOnTrack
        card.status == ActivityStatus.BEHIND    -> statusBehind
        card.status == ActivityStatus.COMPLETED -> statusComplete
        else -> ""
    }
    val statusColor = when {
        card.isRecording -> MaterialTheme.colorScheme.error
        card.status == ActivityStatus.ON_TRACK  -> MaterialTheme.colorScheme.primary
        card.status == ActivityStatus.BEHIND    -> MaterialTheme.colorScheme.error
        card.status == ActivityStatus.COMPLETED -> Color(0xFF4CAF50)
        else -> MaterialTheme.colorScheme.primary
    }

    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick     = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ── Header: drag handle + icon + name + status + menu ────
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showDragHandle) {
                    Icon(
                        imageVector        = Icons.Default.Menu,
                        contentDescription = dragDesc,
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier           = Modifier
                            .size(28.dp)
                            .padding(2.dp)
                            .then(dragHandleModifier)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Box(
                    modifier       = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = IconMapper.get(card.activity.iconName),
                        contentDescription = null,
                        tint               = accentColor,
                        modifier           = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = card.activity.name,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (card.daysUntilEnd != null && !card.isRecording) {
                        Text(
                            text  = "%d %s".format(card.daysUntilEnd, daysLeftLabel),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (card.isRecording) {
                    RecordingIndicator()
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text       = statusText,
                    style      = MaterialTheme.typography.labelSmall,
                    color      = statusColor,
                    fontWeight = FontWeight.Bold
                )
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = moreDesc)
                    }
                    DropdownMenu(
                        expanded         = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text    = { Text(menuArchive) },
                            onClick = { showMenu = false; onArchive() }
                        )
                        DropdownMenuItem(
                            text    = { Text(menuDelete, color = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Progress bars ────────────────────────────────────────
            val progressItems = buildList {
                if (card.goalHours > 0f)  add(Triple(hoursLabel,  "%.1f/%.0f".format(card.currentHours, card.goalHours), card.hoursFraction))
                if (card.goalDays > 0)    add(Triple(daysLabel,   "%d/%d".format(card.currentDays, card.goalDays), card.daysFraction))
                if (card.goalWeeks > 0)   add(Triple(weeksLabel,  "%d/%d".format(card.currentWeeks, card.goalWeeks), card.weeksFraction))
                if (card.goalMonths > 0)  add(Triple(monthsLabel, "%d/%d".format(card.currentMonths, card.goalMonths), card.monthsFraction))
            }

            progressItems.forEach { (label, valueText, fraction) ->
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text     = label,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(40.dp)
                    )
                    LinearProgressIndicator(
                        progress  = { fraction.coerceIn(0f, 1f) },
                        modifier  = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color     = accentColor,
                        trackColor = accentColor.copy(alpha = 0.15f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text  = valueText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun RecordingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "rec")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recAlpha"
    )
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.error.copy(alpha = alpha))
    )
}