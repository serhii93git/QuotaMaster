package com.quotamaster.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a single work session tied to an [Activity].
 *
 * [periodTag] encodes the reset-period bucket with a prefix:
 *   Daily   → "day:2024-06-15"
 *   Weekly  → "week:2024-W03"
 *   Monthly → "month:2024-06"
 *   Yearly  → "year:2024"
 *
 * [durationMinutes] is computed on insert from [startTime] / [endTime],
 * stored as Int to avoid floating-point rounding issues.
 */
@Entity(
    tableName = "work_sessions",
    foreignKeys = [ForeignKey(
        entity = Activity::class,
        parentColumns = ["id"],
        childColumns = ["activity_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("activity_id")]
)
data class WorkSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "activity_id")
    val activityId: Long,
    val date: String,                              // yyyy-MM-dd
    @ColumnInfo(name = "start_time")
    val startTime: String,                         // HH:mm
    @ColumnInfo(name = "end_time")
    val endTime: String,                           // HH:mm
    @ColumnInfo(name = "duration_minutes")
    val durationMinutes: Int,                      // pre-computed integer minutes
    @ColumnInfo(name = "period_tag")
    val periodTag: String,
    val note: String = ""                          // optional user note
)