package com.daymark.app.domain

import com.daymark.app.data.RecurrenceRuleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class RecurrenceEngineTest {
    @Test fun dailyIntervalAdvancesFromOccurrence() {
        val rule = RecurrenceRuleEntity(frequency = RecurrenceEngine.DAILY, interval = 3)
        assertEquals(LocalDate.of(2026, 10, 5), RecurrenceEngine.nextOccurrence(LocalDate.of(2026, 10, 2), rule))
    }

    @Test fun weekdaysSkipSaturdayAndSunday() {
        val rule = RecurrenceRuleEntity(frequency = RecurrenceEngine.WEEKDAYS)
        assertEquals(LocalDate.of(2026, 10, 5), RecurrenceEngine.nextOccurrence(LocalDate.of(2026, 10, 2), rule))
    }

    @Test fun customWeekdaysChooseNextMatchingDay() {
        val start = LocalDate.of(2026, 10, 5) // Monday
        val rule = RecurrenceRuleEntity(
            frequency = RecurrenceEngine.CUSTOM,
            weekdaysMask = RecurrenceEngine.weekdaysMask(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
        )
        assertEquals(LocalDate.of(2026, 10, 7), RecurrenceEngine.nextOccurrence(start, rule, start))
    }

    @Test fun biweeklyRuleKeepsItsSeriesAnchor() {
        val start = LocalDate.of(2026, 1, 5) // Monday
        val rule = RecurrenceRuleEntity(
            frequency = RecurrenceEngine.WEEKLY,
            interval = 2,
            weekdaysMask = RecurrenceEngine.weekdaysMask(setOf(DayOfWeek.MONDAY))
        )
        assertEquals(
            LocalDate.of(2026, 1, 19),
            RecurrenceEngine.nextOccurrence(LocalDate.of(2026, 1, 7), rule, start)
        )
    }

    @Test fun monthlyDateClampsAtMonthEnd() {
        val rule = RecurrenceRuleEntity(frequency = RecurrenceEngine.MONTHLY)
        assertEquals(LocalDate.of(2026, 2, 28), RecurrenceEngine.nextOccurrence(LocalDate.of(2026, 1, 31), rule))
    }

    @Test fun monthlyRecurrenceAcrossJanFebMarPreservesAnchorDay() {
        val seriesStart = LocalDate.of(2026, 1, 31)
        val rule = RecurrenceRuleEntity(frequency = RecurrenceEngine.MONTHLY, interval = 1)
        val feb = RecurrenceEngine.nextOccurrence(seriesStart, rule, seriesStart)
        assertEquals(LocalDate.of(2026, 2, 28), feb)

        val mar = RecurrenceEngine.nextOccurrence(feb!!, rule, seriesStart)
        assertEquals(LocalDate.of(2026, 3, 31), mar)

        val apr = RecurrenceEngine.nextOccurrence(mar!!, rule, seriesStart)
        assertEquals(LocalDate.of(2026, 4, 30), apr)

        val may = RecurrenceEngine.nextOccurrence(apr!!, rule, seriesStart)
        assertEquals(LocalDate.of(2026, 5, 31), may)
    }

    @Test fun occurrenceLimitStopsAfterConfiguredCount() {
        val rule = RecurrenceRuleEntity(frequency = RecurrenceEngine.DAILY, occurrenceLimit = 2)
        assertEquals(LocalDate.of(2026, 10, 3), RecurrenceEngine.nextOccurrence(LocalDate.of(2026, 10, 2), rule, currentOccurrenceIndex = 0))
        assertNull(RecurrenceEngine.nextOccurrence(LocalDate.of(2026, 10, 3), rule, currentOccurrenceIndex = 1))
    }

    @Test fun occurrenceLimitZeroOrNegativeReturnsNull() {
        val rule = RecurrenceRuleEntity(frequency = RecurrenceEngine.DAILY, occurrenceLimit = 1)
        assertNull(RecurrenceEngine.nextOccurrence(LocalDate.of(2026, 10, 2), rule, currentOccurrenceIndex = 0))
    }

    @Test fun endDateIsInclusiveButNoOccurrenceAfterIt() {
        val rule = RecurrenceRuleEntity(frequency = RecurrenceEngine.DAILY, endEpochDay = LocalDate.of(2026, 10, 4).toEpochDay())
        assertEquals(LocalDate.of(2026, 10, 4), RecurrenceEngine.nextOccurrence(LocalDate.of(2026, 10, 3), rule))
        assertNull(RecurrenceEngine.nextOccurrence(LocalDate.of(2026, 10, 4), rule))
    }

    @Test fun endDateBeforeNextReturnsNull() {
        val rule = RecurrenceRuleEntity(frequency = RecurrenceEngine.DAILY, endEpochDay = LocalDate.of(2026, 10, 2).toEpochDay())
        assertNull(RecurrenceEngine.nextOccurrence(LocalDate.of(2026, 10, 2), rule))
    }
}
