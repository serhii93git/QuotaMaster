package com.quotamaster.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.quotamaster.data.model.Activity
import com.quotamaster.data.model.Period
import com.quotamaster.data.model.WorkSession
import com.quotamaster.data.repository.ActivityRepository
import com.quotamaster.data.repository.QuickLogRepository
import com.quotamaster.data.repository.QuickLogState
import com.quotamaster.data.repository.WorkSessionRepository
import com.quotamaster.util.TimeCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// ── Card UI state ────────────────────────────────────────────────────────────

data class ActivityCardState(
    val activity: Activity,
    val currentHours: Float = 0f,
    val currentDays: Int = 0,
    val currentWeeks: Int = 0,
    val currentMonths: Int = 0,
    val goalHours: Float = 0f,
    val goalDays: Int = 0,
    val goalWeeks: Int = 0,
    val goalMonths: Int = 0,
    val hoursFraction: Float = 0f,
    val daysFraction: Float = 0f,
    val weeksFraction: Float = 0f,
    val monthsFraction: Float = 0f,
    val status: ActivityStatus = ActivityStatus.ON_TRACK,
    val daysUntilEnd: Int? = null,
    val isRecording: Boolean = false,
    val recordingSince: String = ""
)

enum class ActivityStatus(val key: String) {
    ON_TRACK("on_track"),
    BEHIND("behind"),
    COMPLETED("completed")
}

enum class SortMode { BY_STATUS, MANUAL }

// ── ViewModel ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val activityRepo: ActivityRepository,
    private val sessionRepo: WorkSessionRepository,
    private val quickLogRepo: QuickLogRepository
) : ViewModel() {

    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    val quickLogState: StateFlow<QuickLogState> = quickLogRepo.state

    private val _sortMode = MutableStateFlow(SortMode.MANUAL)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    private val _manualOrder = MutableStateFlow<List<Long>?>(null)

    private val activityCards: StateFlow<List<ActivityCardState>> = activityRepo.getActiveActivities()
        .flatMapLatest { activities ->
            if (activities.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    combine(activities.map { buildCardFlow(it) }) { it.toList() },
                    quickLogRepo.state
                ) { cards, qlState ->
                    cards.map { card ->
                        if (qlState.isRecording && qlState.activityId == card.activity.id) {
                            card.copy(isRecording = true, recordingSince = qlState.startTime)
                        } else {
                            card
                        }
                    }
                }
            }
        }
        .catch { e ->
            Log.e("HomeViewModel", "Cards flow error", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val displayCards: StateFlow<List<ActivityCardState>> = combine(
        activityCards,
        _sortMode,
        _manualOrder
    ) { cards, mode, manual ->
        when {
            manual != null -> {
                val map = cards.associateBy { it.activity.id }
                manual.mapNotNull { map[it] }
            }
            mode == SortMode.BY_STATUS -> cards.sortedBy {
                when (it.status) {
                    ActivityStatus.BEHIND    -> 0
                    ActivityStatus.ON_TRACK  -> 1
                    ActivityStatus.COMPLETED -> 2
                }
            }
            else -> cards
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun toggleSortMode() {
        _sortMode.value = when (_sortMode.value) {
            SortMode.MANUAL    -> SortMode.BY_STATUS
            SortMode.BY_STATUS -> SortMode.MANUAL
        }
    }

    fun moveItem(from: Int, to: Int) {
        val current = _manualOrder.value
            ?: displayCards.value.map { it.activity.id }
        _manualOrder.value = current.toMutableList().apply {
            add(to, removeAt(from))
        }
    }

    fun onDragEnd() {
        val order = _manualOrder.value ?: return
        viewModelScope.launch {
            order.forEachIndexed { index, id ->
                activityRepo.updateSortOrder(id, index)
            }
            _manualOrder.value = null
        }
    }

    suspend fun getExportData(): Pair<List<Activity>, List<WorkSession>> {
        val activities = displayCards.value.map { it.activity }
        val sessions = sessionRepo.getAllSessionsOnce()
        return activities to sessions
    }

    private fun buildCardFlow(activity: Activity): Flow<ActivityCardState> {
        val period = Period.entries.firstOrNull { it.key == activity.period } ?: Period.WEEKLY
        val tag = TimeCalculator.getPeriodTag(TimeCalculator.todayString(), period)

        return combine(
            combine(
                sessionRepo.getTotalMinutesForActivityPeriod(activity.id, tag),
                sessionRepo.getUniqueDaysForActivityPeriod(activity.id, tag),
            ) { a, b -> Pair(a, b) },
            combine(
                sessionRepo.getUniqueWeeksForActivityPeriod(activity.id, tag),
                sessionRepo.getUniqueMonthsForActivityPeriod(activity.id, tag),
            ) { a, b -> Pair(a, b) }
        ) { (totalMin, days), (weeks, months) ->
            val minutes = totalMin ?: 0
            val uniqueDays = days ?: 0
            val uniqueWeeks = weeks ?: 0
            val uniqueMonths = months ?: 0
            val hours = TimeCalculator.minutesToHours(minutes)

            val goalH = activity.goalHoursPerPeriod
            val goalD = activity.goalDaysPerPeriod
            val goalW = activity.goalWeeksPerPeriod
            val goalM = activity.goalMonthsPerPeriod

            val hFrac = if (goalH > 0f) (hours / goalH).coerceAtMost(1f) else 0f
            val dFrac = if (goalD > 0) (uniqueDays.toFloat() / goalD).coerceAtMost(1f) else 0f
            val wFrac = if (goalW > 0) (uniqueWeeks.toFloat() / goalW).coerceAtMost(1f) else 0f
            val mFrac = if (goalM > 0) (uniqueMonths.toFloat() / goalM).coerceAtMost(1f) else 0f

            // Completed = all set goals met; Behind = any set goal < 50%
            val fractions = listOfNotNull(
                if (goalH > 0f) hFrac else null,
                if (goalD > 0) dFrac else null,
                if (goalW > 0) wFrac else null,
                if (goalM > 0) mFrac else null,
            )
            val status = when {
                fractions.isEmpty() -> ActivityStatus.ON_TRACK
                fractions.all { it >= 1f } -> ActivityStatus.COMPLETED
                fractions.any { it < 0.5f } -> ActivityStatus.BEHIND
                else -> ActivityStatus.ON_TRACK
            }

            val daysUntilEnd = activity.endDate?.let {
                val end = LocalDate.parse(it, dateFmt)
                ChronoUnit.DAYS.between(LocalDate.now(), end).toInt().coerceAtLeast(0)
            }

            ActivityCardState(
                activity = activity,
                currentHours = hours, currentDays = uniqueDays,
                currentWeeks = uniqueWeeks, currentMonths = uniqueMonths,
                goalHours = goalH, goalDays = goalD,
                goalWeeks = goalW, goalMonths = goalM,
                hoursFraction = hFrac, daysFraction = dFrac,
                weeksFraction = wFrac, monthsFraction = mFrac,
                status = status, daysUntilEnd = daysUntilEnd
            )
        }
    }

    // ── Quick-log ────────────────────────────────────────────────────────

    /**
     * Long-press handler. If no timer running → start for this activity.
     * If same activity recording → stop and save session.
     * If different activity recording → returns false (UI should confirm).
     */
   fun onQuickLogPress(activity: Activity): QuickLogAction {
        val current = quickLogRepo.state.value

        // Instant log mode (no hours goal — just log a day)
        if (!activity.usesTimer) {
            if (current.isRecording && current.activityId == activity.id) {
                // Stop timer if somehow running
                stopAndSave()
                return QuickLogAction.STOPPED
            }
            val period = Period.entries.firstOrNull { it.key == activity.period } ?: Period.WEEKLY
            viewModelScope.launch {
                sessionRepo.insertInstantLog(
                    activityId = activity.id,
                    date       = TimeCalculator.todayString(),
                    note       = "Quick log",
                    period     = period
                )
            }
            return QuickLogAction.INSTANT_LOGGED
        }

        // Timer mode (has hours goal)
        return when {
            !current.isRecording -> {
                quickLogRepo.start(
                    activityId   = activity.id,
                    activityName = activity.name,
                    date         = TimeCalculator.todayString(),
                    startTime    = TimeCalculator.nowTimeString()
                )
                QuickLogAction.STARTED
            }
            current.activityId == activity.id -> {
                stopAndSave()
                QuickLogAction.STOPPED
            }
            else -> QuickLogAction.CONFLICT
        }
    }

    fun forceStopAndStart(activity: Activity) {
        stopAndSave()
        quickLogRepo.start(
            activityId   = activity.id,
            activityName = activity.name,
            date         = TimeCalculator.todayString(),
            startTime    = TimeCalculator.nowTimeString()
        )
    }

    private fun stopAndSave() {
        val snapshot = quickLogRepo.stop()
        if (snapshot.isRecording && snapshot.activityId > 0) {
            val endTime = TimeCalculator.nowTimeString()
            val period = activityCards.value
                .firstOrNull { it.activity.id == snapshot.activityId }
                ?.activity?.period ?: "weekly"
            val p = Period.entries.firstOrNull { it.key == period } ?: Period.WEEKLY
            viewModelScope.launch {
                sessionRepo.insertSession(
                    activityId = snapshot.activityId,
                    date       = snapshot.date,
                    startTime  = snapshot.startTime,
                    endTime    = endTime,
                    note       = "Quick log",
                    period     = p
                )
            }
        }
    }

    fun deleteActivity(activity: Activity) {
        viewModelScope.launch { activityRepo.delete(activity) }
    }

    fun confirmArchive(id: Long) {
        viewModelScope.launch { activityRepo.archive(id) }
    }

    fun archiveActivity(id: Long) {
        viewModelScope.launch { activityRepo.archive(id) }
    }

    // ── Factory ──────────────────────────────────────────────────────────

    class Factory(
        private val activityRepo: ActivityRepository,
        private val sessionRepo: WorkSessionRepository,
        private val quickLogRepo: QuickLogRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(activityRepo, sessionRepo, quickLogRepo) as T
    }
}

enum class QuickLogAction {
    STARTED, STOPPED, CONFLICT, INSTANT_LOGGED
}