package com.quotamaster.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.quotamaster.data.model.Activity
import com.quotamaster.data.model.Period
import com.quotamaster.data.model.WorkSession
import com.quotamaster.data.repository.ActivityRepository
import com.quotamaster.data.repository.WorkSessionRepository
import com.quotamaster.util.MotivationProvider
import com.quotamaster.util.TimeCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// ── Detail UI state ──────────────────────────────────────────────────────────

data class ActivityDetailState(
    val activity: Activity? = null,
    val totalHours: Float = 0f,
    val uniqueDays: Int = 0,
    val uniqueWeeks: Int = 0,
    val uniqueMonths: Int = 0,
    val sessionCount: Int = 0,
    val goalHours: Float = 0f,
    val goalDays: Int = 0,
    val goalWeeks: Int = 0,
    val goalMonths: Int = 0,
    val hoursFraction: Float = 0f,
    val daysFraction: Float = 0f,
    val weeksFraction: Float = 0f,
    val monthsFraction: Float = 0f,
    val hoursOverflow: Float = 0f,
    val daysOverflow: Float = 0f,
    val weeksOverflow: Float = 0f,
    val monthsOverflow: Float = 0f,
    val motivationMessage: String = "",
    val daysUntilEnd: Int? = null,
    val activeDates: List<String> = emptyList(),
    val sessions: List<WorkSession> = emptyList(),
    val streak: Int = 0,
    val averageMinutesPerSession: Int = 0,
    val chartData: List<Pair<String, Float>> = emptyList()
)

// ── ViewModel ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityDetailViewModel(
    private val activityId: Long,
    private val activityRepo: ActivityRepository,
    private val sessionRepo: WorkSessionRepository
) : ViewModel() {

    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    val detailState: StateFlow<ActivityDetailState> = activityRepo.getActivityById(activityId)
        .filterNotNull()
        .flatMapLatest { activity ->
            val period = Period.entries.firstOrNull { it.key == activity.period } ?: Period.WEEKLY
            val tag = TimeCalculator.getPeriodTag(TimeCalculator.todayString(), period)

            combine(
                combine(
                    sessionRepo.getTotalMinutesForActivityPeriod(activityId, tag),
                    sessionRepo.getUniqueDaysForActivityPeriod(activityId, tag),
                    sessionRepo.getUniqueWeeksForActivityPeriod(activityId, tag),
                ) { totalMin, days, weeks -> Triple(totalMin, days, weeks) },
                combine(
                    sessionRepo.getUniqueMonthsForActivityPeriod(activityId, tag),
                    sessionRepo.getSessionsForActivityPeriod(activityId, tag),
                    sessionRepo.getActiveDatesForActivityPeriod(activityId, tag),
                ) { months, sessions, dates -> Triple(months, sessions, dates) }
            ) { (totalMin, days, weeks), (months, sessions, activeDates) ->
                val minutes = totalMin ?: 0
                val uniqueDays = days ?: 0
                val uniqueWeeks = weeks ?: 0
                val uniqueMonths = months ?: 0
                val hours = TimeCalculator.minutesToHours(minutes)

                val goalH = activity.goalHoursPerPeriod
                val goalD = activity.goalDaysPerPeriod
                val goalW = activity.goalWeeksPerPeriod
                val goalM = activity.goalMonthsPerPeriod

                val rawH = if (goalH > 0f) hours / goalH else 0f
                val rawD = if (goalD > 0) uniqueDays.toFloat() / goalD else 0f
                val rawW = if (goalW > 0) uniqueWeeks.toFloat() / goalW else 0f
                val rawM = if (goalM > 0) uniqueMonths.toFloat() / goalM else 0f

                val daysUntilEnd = activity.endDate?.let {
                    val end = LocalDate.parse(it, dateFmt)
                    ChronoUnit.DAYS.between(LocalDate.now(), end).toInt().coerceAtLeast(0)
                }

                val sessionsWithTime = sessions.filter { it.durationMinutes > 0 }
               val avg = if (sessionsWithTime.isNotEmpty())
                    sessionsWithTime.sumOf { it.durationMinutes } / sessionsWithTime.size else 0

                // Chart: hours per date, sorted chronologically
                val chartData = sessions
                    .groupBy { it.date }
                    .map { (date, daySessions) ->
                        val dayHours = TimeCalculator.minutesToHours(
                            daySessions.sumOf { it.durationMinutes }
                        )
                        date to dayHours
                    }
                    .sortedBy { it.first }
                    .takeLast(14) // Max 14 bars for readability

                ActivityDetailState(
                    activity      = activity,
                    totalHours    = hours,
                    uniqueDays    = uniqueDays,
                    uniqueWeeks   = uniqueWeeks,
                    uniqueMonths  = uniqueMonths,
                    sessionCount  = sessions.size,
                    goalHours     = goalH,
                    goalDays      = goalD,
                    goalWeeks     = goalW,
                    goalMonths    = goalM,
                    hoursFraction  = rawH.coerceAtMost(1f),
                    daysFraction   = rawD.coerceAtMost(1f),
                    weeksFraction  = rawW.coerceAtMost(1f),
                    monthsFraction = rawM.coerceAtMost(1f),
                    hoursOverflow  = (rawH - 1f).coerceIn(0f, 0.25f),
                    daysOverflow   = (rawD - 1f).coerceIn(0f, 0.25f),
                    weeksOverflow  = (rawW - 1f).coerceIn(0f, 0.25f),
                    monthsOverflow = (rawM - 1f).coerceIn(0f, 0.25f),
                    motivationMessage = MotivationProvider.getHoursMessage(hours, goalH),
                    daysUntilEnd  = daysUntilEnd,
                    activeDates   = activeDates,
                    sessions      = sessions,
                    streak        = calculateStreak(activeDates),
                    averageMinutesPerSession = avg,
                    chartData     = chartData
                )
            }
        }
        .catch { e ->
            Log.e("ActivityDetailVM", "Detail flow error", e)
            emit(ActivityDetailState())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ActivityDetailState()
        )

    // ── Public API ───────────────────────────────────────────────────────

    fun addSession(date: String, startTime: String, endTime: String, note: String) {
        val activity = detailState.value.activity ?: return
        val period = Period.entries.firstOrNull { it.key == activity.period } ?: Period.WEEKLY
        viewModelScope.launch {
            sessionRepo.insertSession(
                activityId = activityId,
                date       = date,
                startTime  = startTime,
                endTime    = endTime,
                note       = note,
                period     = period
            )
        }
    }

    fun deleteSession(session: WorkSession) {
        viewModelScope.launch { sessionRepo.deleteSession(session) }
    }

    fun restoreSession(session: WorkSession) {
        viewModelScope.launch { sessionRepo.restoreSession(session) }
    }

    fun editSession(original: WorkSession, date: String, startTime: String, endTime: String, note: String) {
        val activity = detailState.value.activity ?: return
        val period = Period.entries.firstOrNull { it.key == activity.period } ?: Period.WEEKLY
        viewModelScope.launch {
            sessionRepo.updateSession(
                original.copy(
                    date            = date,
                    startTime       = startTime,
                    endTime         = endTime,
                    durationMinutes = com.quotamaster.util.TimeCalculator.calculateDurationMinutes(startTime, endTime),
                    periodTag       = com.quotamaster.util.TimeCalculator.getPeriodTag(date, period),
                    note            = note
                )
            )
        }
    }

    // ── Streak calculation ───────────────────────────────────────────────

    private fun calculateStreak(dates: List<String>): Int {
        if (dates.isEmpty()) return 0
        val sorted = dates.map { LocalDate.parse(it, dateFmt) }.sortedDescending()
        var streak = 1
        for (i in 0 until sorted.size - 1) {
            if (sorted[i].minusDays(1) == sorted[i + 1]) streak++ else break
        }
        return streak
    }

    // ── Factory ──────────────────────────────────────────────────────────

    class Factory(
        private val activityId: Long,
        private val activityRepo: ActivityRepository,
        private val sessionRepo: WorkSessionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ActivityDetailViewModel(activityId, activityRepo, sessionRepo) as T
    }
}
