package com.quotamaster.data.model

/**
 * DEADLINE — has an end date (e.g. 8-week course)
 * ONGOING  — no end date (e.g. gym membership)
 */
enum class ActivityType(val key: String) {
    DEADLINE("deadline"),
    ONGOING("ongoing")
}