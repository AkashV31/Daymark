package com.daymark.app.domain

import com.daymark.app.data.RecurrenceRuleEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Pure date arithmetic for recurrence. It never compares formatted date strings. */
object RecurrenceEngine {
    const val NONE = "NONE"
    const val DAILY = "DAILY"
    const val WEEKDAYS = "WEEKDAYS"
    const val WEEKLY = "WEEKLY"
    const val MONTHLY = "MONTHLY"
    const val CUSTOM = "CUSTOM"

    /**
     * Returns the next occurrence after [current], respecting the original series anchor,
     * weekday mask, interval, end date and occurrence limit. [currentOccurrenceIndex] is
     * zero-based (the first occurrence is index 0).
     */
    fun nextOccurrence(
        current: LocalDate,
        rule: RecurrenceRuleEntity,
        seriesStart: LocalDate = current,
        currentOccurrenceIndex: Int = 0
    ): LocalDate? {
        val interval = rule.interval.coerceAtLeast(1)
        if (rule.occurrenceLimit != null && currentOccurrenceIndex + 1 >= rule.occurrenceLimit) return null
        val end = rule.endEpochDay?.let { LocalDate.ofEpochDay(it) }
        if (end != null && !current.isBefore(end)) return null

        val candidate = when (rule.frequency.uppercase()) {
            DAILY -> current.plusDays(interval.toLong())
            WEEKDAYS -> nextWeekday(current, interval)
            WEEKLY -> {
                if (rule.weekdaysMask == 0) {
                    current.plusWeeks(interval.toLong())
                } else {
                    nextMatchingWeekday(current, seriesStart, interval, rule.weekdaysMask)
                }
            }
            MONTHLY -> {
                val targetYearMonth = java.time.YearMonth.from(current).plusMonths(interval.toLong())
                val targetDay = seriesStart.dayOfMonth.coerceAtMost(targetYearMonth.lengthOfMonth())
                targetYearMonth.atDay(targetDay)
            }
            CUSTOM -> {
                if (rule.weekdaysMask == 0) return null
                nextMatchingWeekday(current, seriesStart, interval, rule.weekdaysMask)
            }
            else -> return null
        }

        if (candidate == null || candidate <= current) return null
        if (end != null && candidate.isAfter(end)) return null
        return candidate
    }

    private fun nextWeekday(current: LocalDate, interval: Int): LocalDate {
        var date = current
        var weekdaysAdded = 0
        while (weekdaysAdded < interval) {
            date = date.plusDays(1)
            if (date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY) {
                weekdaysAdded++
            }
        }
        return date
    }

    private fun nextMatchingWeekday(
        current: LocalDate,
        seriesStart: LocalDate,
        intervalWeeks: Int,
        weekdayMask: Int
    ): LocalDate? {
        val anchorMonday = seriesStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        var date = current.plusDays(1)
        // A corrupt rule should never pin the caller in an unbounded loop.
        repeat(366 * 8) {
            val weekMonday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val weeksFromAnchor = ChronoUnit.WEEKS.between(anchorMonday, weekMonday)
            val activeWeek = weeksFromAnchor >= 0 && weeksFromAnchor % intervalWeeks == 0L
            val bit = 1 shl (date.dayOfWeek.value - 1)
            if (activeWeek && (weekdayMask and bit) != 0) return date
            date = date.plusDays(1)
        }
        return null
    }

    fun weekdaysMask(days: Set<DayOfWeek>): Int = days.fold(0) { mask, day ->
        mask or (1 shl (day.value - 1))
    }

    fun selectedWeekdays(mask: Int): Set<DayOfWeek> = DayOfWeek.entries
        .filterTo(linkedSetOf()) { (mask and (1 shl (it.value - 1))) != 0 }
}
