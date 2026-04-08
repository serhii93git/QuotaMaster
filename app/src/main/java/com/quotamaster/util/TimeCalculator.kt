package com.quotamaster.util

import com.quotamaster.data.model.Period
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields

/**
 * Pure-function helpers for time/date calculations.
 * No Android dependencies — fully unit-testable.
 */
object TimeCalculator {

    private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Returns integer minutes between two HH:mm strings.
     * Handles midnight crossings (e.g. 22:30 → 01:15 = 165 min).
     */
    fun calculateDurationMinutes(startTime: String, endTime: String): Int {
        val s = LocalTime.parse(startTime, TIME_FMT)
        val e = LocalTime.parse(endTime,   TIME_FMT)
        val sMin = s.hour * 60 + s.minute
        val eMin = e.hour * 60 + e.minute
        return if (eMin >= sMin) eMin - sMin else (24 * 60) - sMin + eMin
    }

    /** Converts integer minutes to a Float hours value for display. */
    fun minutesToHours(minutes: Int): Float = minutes / 60f

    // ── Period-tag helpers ────────────────────────────────────────────────

    fun getDailyTag(date: String): String = "day:$date"

    fun getWeekTag(date: String): String {
        val d    = LocalDate.parse(date, DATE_FMT)
        val week = d.get(WeekFields.ISO.weekOfWeekBasedYear())
        val year = d.get(WeekFields.ISO.weekBasedYear())
        return "week:$year-W${week.toString().padStart(2, '0')}"
    }

    fun getMonthTag(date: String): String {
        val d = LocalDate.parse(date, DATE_FMT)
        return "month:${d.year}-${d.monthValue.toString().padStart(2, '0')}"
    }

    fun getYearTag(date: String): String {
        val d = LocalDate.parse(date, DATE_FMT)
        return "year:${d.year}"
    }

    fun getPeriodTag(date: String, period: Period): String = when (period) {
        Period.DAILY   -> getDailyTag(date)
        Period.WEEKLY  -> getWeekTag(date)
        Period.MONTHLY -> getMonthTag(date)
        Period.YEARLY  -> getYearTag(date)
    }

    // ── Convenience ───────────────────────────────────────────────────────

    fun todayString(): String = LocalDate.now().format(DATE_FMT)

    fun nowTimeString(): String = LocalTime.now().format(TIME_FMT)

    /**
     * Validates date format AND semantics.
     * Returns true only for parseable yyyy-MM-dd strings.
     */
    fun isValidDate(s: String): Boolean {
        if (s.length != 10 || s[4] != '-' || s[7] != '-') return false
        if (!s.filterIndexed { i, _ -> i != 4 && i != 7 }.all { it.isDigit() }) return false
        return runCatching { LocalDate.parse(s, DATE_FMT) }.isSuccess
    }

    /**
     * Validates time format AND range (00:00–23:59).
     * Returns true only for parseable HH:mm strings.
     */
    fun isValidTime(s: String): Boolean {
        if (s.length != 5 || s[2] != ':') return false
        if (!s.filterIndexed { i, _ -> i != 2 }.all { it.isDigit() }) return false
        return runCatching { LocalTime.parse(s, TIME_FMT) }.isSuccess
    }
}
