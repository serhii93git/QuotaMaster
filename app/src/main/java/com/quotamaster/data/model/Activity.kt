package com.quotamaster.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a trackable activity (course, gym, etc.)
 *
 * [goalHoursPerPeriod] — target hours per period (e.g. 20h/week)
 * [goalDaysPerPeriod]  — target days per period (e.g. 4 days/week)
 * [endDate]            — null for ONGOING activities
 * [iconName]           — Material Icon name (e.g. "School", "FitnessCenter")
 * [colorHex]           — hex string without # (e.g. "6650A4")
 */
@Entity(tableName = "activities")
data class Activity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "activity_type")
    val activityType: String,                      // "deadline" or "ongoing"
    val period: String,                            // "daily","weekly","monthly","yearly"
    @ColumnInfo(name = "goal_hours_per_period")
    val goalHoursPerPeriod: Float = 0f,
    @ColumnInfo(name = "goal_days_per_period")
    val goalDaysPerPeriod: Int = 0,
    @ColumnInfo(name = "goal_weeks_per_period")
    val goalWeeksPerPeriod: Int = 0,
    @ColumnInfo(name = "goal_months_per_period")
    val goalMonthsPerPeriod: Int = 0,
    @ColumnInfo(name = "start_date")
    val startDate: String,                         // yyyy-MM-dd
    @ColumnInfo(name = "end_date")
    val endDate: String? = null,                   // yyyy-MM-dd, null for ONGOING
    @ColumnInfo(name = "icon_name")
    val iconName: String = "School",
    @ColumnInfo(name = "color_hex")
    val colorHex: String = "6650A4",
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false
) {
    /** Returns true if at least one goal metric is set. */
    val hasAnyGoal: Boolean get() =
        goalHoursPerPeriod > 0f || goalDaysPerPeriod > 0 ||
        goalWeeksPerPeriod > 0 || goalMonthsPerPeriod > 0

    /** Returns true if quick-log should use timer mode (vs instant log). */
    val usesTimer: Boolean get() = goalHoursPerPeriod > 0f
}