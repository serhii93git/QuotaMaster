package com.quotamaster.util

/**
 * Generates context-aware motivational strings for the Friendly Coach card.
 *
 * Four-tier logic based on percentage of goal completed:
 *   < 80%     → encouraging "keep going" message
 *   80–99%    → "almost there" message
 *   100–119%  → "goal met" celebration
 *   >= 120%   → "bonus zone" overachiever message
 *
 * Uses String.format() for safe interpolation.
 */
object MotivationProvider {

    fun getHoursMessage(currentHours: Float, goalHours: Float): String {
        if (goalHours <= 0f) return "Set a goal to start tracking!"
        val pct = currentHours / goalHours
        val remaining = goalHours - currentHours
        return when {
            pct < 0.80f ->
                "Keep going! You have done %.1f h — %.1f h to reach your goal."
                    .format(currentHours, remaining)
            pct < 1.00f ->
                "Almost there! Only %.1f h remaining. You can do it!"
                    .format(remaining)
            pct < 1.20f ->
                "Goal reached! %.1f / %.0f h completed. Great work!"
                    .format(currentHours, goalHours)
            else -> {
                val bonus = currentHours - goalHours
                "Bonus zone! +%.1f extra hours beyond your goal. Outstanding!"
                    .format(bonus)
            }
        }
    }

    fun getDaysMessage(currentDays: Int, period: String): String {
        return "Days active this %s: %d".format(period, currentDays)
    }
}
