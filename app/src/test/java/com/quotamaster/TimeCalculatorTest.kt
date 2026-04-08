package com.quotamaster

import com.quotamaster.util.TimeCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeCalculatorTest {

    @Test
    fun `normal duration 09-00 to 17-30 returns 510 minutes`() {
        assertEquals(510, TimeCalculator.calculateDurationMinutes("09:00", "17:30"))
    }

    @Test
    fun `midnight crossing 22-30 to 01-15 returns 165 minutes`() {
        assertEquals(165, TimeCalculator.calculateDurationMinutes("22:30", "01:15"))
    }

    @Test
    fun `isValidDate accepts yyyy-MM-dd`() {
        assertTrue(TimeCalculator.isValidDate("2024-06-15"))
    }

    @Test
    fun `isValidDate rejects wrong separator`() {
        assertFalse(TimeCalculator.isValidDate("2024/06/15"))
    }

    @Test
    fun `isValidDate rejects impossible date`() {
        assertFalse(TimeCalculator.isValidDate("2024-02-30"))
    }

    @Test
    fun `isValidTime accepts padded HH-mm`() {
        assertTrue(TimeCalculator.isValidTime("09:30"))
    }

    @Test
    fun `isValidTime rejects unpadded hour`() {
        assertFalse(TimeCalculator.isValidTime("9:30"))
    }

    @Test
    fun `isValidTime rejects out-of-range`() {
        assertFalse(TimeCalculator.isValidTime("25:00"))
    }

    @Test
    fun `minutesToHours converts 90 min to 1_5`() {
        assertEquals(1.5f, TimeCalculator.minutesToHours(90), 0.001f)
    }

    @Test
    fun `week tag format is prefixed ISO week`() {
        val tag = TimeCalculator.getWeekTag("2024-01-08")
        assertTrue("Expected week: prefix, got: $tag", tag.startsWith("week:"))
        assertTrue("Expected ISO week format, got: $tag",
            tag.matches(Regex("week:\\d{4}-W\\d{2}")))
    }

    @Test
    fun `month tag format is month-YYYY-MM`() {
        assertEquals("month:2024-06", TimeCalculator.getMonthTag("2024-06-15"))
    }

    @Test
    fun `year tag format is year-YYYY`() {
        assertEquals("year:2024", TimeCalculator.getYearTag("2024-06-15"))
    }

    @Test
    fun `daily tag format is day-YYYY-MM-DD`() {
        assertEquals("day:2024-06-15", TimeCalculator.getDailyTag("2024-06-15"))
    }
}
