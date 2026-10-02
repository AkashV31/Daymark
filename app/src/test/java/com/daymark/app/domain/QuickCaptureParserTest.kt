package com.daymark.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class QuickCaptureParserTest {

    private val baseDate = LocalDate.of(2026, 10, 2) // Friday

    @Test
    fun parsesFullExpressionWithDayAndTime() {
        val input = "Submit database assignment Friday 7 PM"
        val parsed = QuickCaptureParser.parse(input, baseDate)
        assertEquals("Submit database assignment", parsed.title)
        assertEquals(LocalDate.of(2026, 10, 9).toEpochDay(), parsed.dueEpochDay) // next Friday
        assertEquals(19 * 60, parsed.dueMinuteOfDay)
        assertEquals("NORMAL", parsed.priority)
    }

    @Test
    fun parsesTomorrowWithTimeAndPriority() {
        val input = "Gym workout tomorrow 8:30 AM !high #fitness"
        val parsed = QuickCaptureParser.parse(input, baseDate)
        assertEquals("Gym workout", parsed.title)
        assertEquals(baseDate.plusDays(1).toEpochDay(), parsed.dueEpochDay)
        assertEquals(8 * 60 + 30, parsed.dueMinuteOfDay)
        assertEquals("HIGH", parsed.priority)
        assertEquals("Fitness", parsed.category)
    }

    @Test
    fun parsesTonightWithNamedTime() {
        val input = "Review tomorrow plans tonight"
        val parsed = QuickCaptureParser.parse(input, baseDate)
        assertEquals("Review tomorrow plans", parsed.title)
        assertEquals(baseDate.toEpochDay(), parsed.dueEpochDay)
        assertEquals(21 * 60, parsed.dueMinuteOfDay)
    }

    @Test
    fun parsesExplicitMonthAndDay() {
        val input = "Pay electricity bill 15 Oct 11:59 PM"
        val parsed = QuickCaptureParser.parse(input, baseDate)
        assertEquals("Pay electricity bill", parsed.title)
        assertEquals(LocalDate.of(2026, 10, 15).toEpochDay(), parsed.dueEpochDay)
        assertEquals(23 * 60 + 59, parsed.dueMinuteOfDay)
    }
}
