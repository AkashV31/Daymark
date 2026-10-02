package com.daymark.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class ParsedQuickCapture(
    val title: String,
    val dueEpochDay: Long? = null,
    val dueMinuteOfDay: Int? = null,
    val priority: String = "NORMAL",
    val category: String = ""
)

/**
 * Lightweight deterministic natural language parser for Quick Capture.
 * Operates purely offline without AI/LLM dependencies or network access.
 */
object QuickCaptureParser {

    private val timePattern = Regex("""\b(?:at\s+)?(\d{1,2})(?::(\d{2}))?\s*(am|pm)\b|\b(?:at\s+)?([01]?\d|2[0-3]):([0-5]\d)\b""", RegexOption.IGNORE_CASE)
    private val namedTimePattern = Regex("""\b(noon|midnight|morning|afternoon|evening|night)\b""", RegexOption.IGNORE_CASE)
    private val relativeDayPattern = Regex("""\b(today|tomorrow|tonight)\b""", RegexOption.IGNORE_CASE)
    private val weekdayPattern = Regex("""\b(?:on\s+)?(?:next\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|wed|thu|fri|sat|sun)\b""", RegexOption.IGNORE_CASE)
    private val monthDayPattern = Regex("""\b(?:on\s+)?(\d{1,2})(?:st|nd|rd|th)?\s+(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\b|\b(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\s+(\d{1,2})(?:st|nd|rd|th)?\b""", RegexOption.IGNORE_CASE)
    private val priorityPattern = Regex("""!(critical|urgent|high|low)\b""", RegexOption.IGNORE_CASE)
    private val categoryPattern = Regex("""#([A-Za-z0-9_-]+)""")

    fun parse(rawInput: String, today: LocalDate = LocalDate.now()): ParsedQuickCapture {
        var text = rawInput.trim()
        if (text.isBlank()) return ParsedQuickCapture(title = "")

        var priority = "NORMAL"
        val priorityMatch = priorityPattern.find(text)
        if (priorityMatch != null) {
            val level = priorityMatch.groupValues[1].lowercase(Locale.ROOT)
            priority = when (level) {
                "critical", "urgent" -> "CRITICAL"
                "high" -> "HIGH"
                "low" -> "LOW"
                else -> "NORMAL"
            }
            text = text.removeRange(priorityMatch.range).trim()
        }

        var category = ""
        val categoryMatch = categoryPattern.find(text)
        if (categoryMatch != null) {
            category = categoryMatch.groupValues[1].replaceFirstChar { it.titlecase(Locale.ROOT) }
            text = text.removeRange(categoryMatch.range).trim()
        }

        var dueMinuteOfDay: Int? = null
        val timeMatch = timePattern.find(text)
        if (timeMatch != null) {
            val hourStr = timeMatch.groupValues[1].ifEmpty { timeMatch.groupValues[4] }
            val minStr = timeMatch.groupValues[2].ifEmpty { timeMatch.groupValues[5] }
            val ampm = timeMatch.groupValues[3].lowercase(Locale.ROOT)
            var hour = hourStr.toIntOrNull() ?: 9
            val min = minStr.toIntOrNull() ?: 0

            if (ampm == "pm" && hour < 12) hour += 12
            else if (ampm == "am" && hour == 12) hour = 0

            dueMinuteOfDay = (hour.coerceIn(0, 23) * 60) + min.coerceIn(0, 59)
            text = text.removeRange(timeMatch.range).trim()
        } else {
            val namedTimeMatch = namedTimePattern.find(text)
            if (namedTimeMatch != null) {
                dueMinuteOfDay = when (namedTimeMatch.groupValues[1].lowercase(Locale.ROOT)) {
                    "morning" -> 9 * 60
                    "noon" -> 12 * 60
                    "afternoon" -> 14 * 60
                    "evening" -> 18 * 60
                    "night", "tonight" -> 21 * 60
                    "midnight" -> 23 * 60 + 59
                    else -> null
                }
                text = text.removeRange(namedTimeMatch.range).trim()
            }
        }

        var dueEpochDay: Long? = null
        val relativeMatch = relativeDayPattern.findAll(text).lastOrNull()
        if (relativeMatch != null) {
            val word = relativeMatch.groupValues[1].lowercase(Locale.ROOT)
            dueEpochDay = when (word) {
                "tomorrow" -> today.plusDays(1).toEpochDay()
                "today", "tonight" -> today.toEpochDay()
                else -> null
            }
            if (word == "tonight" && dueMinuteOfDay == null) {
                dueMinuteOfDay = 21 * 60
            }
            text = text.removeRange(relativeMatch.range).trim()
        } else {
            val weekdayMatch = weekdayPattern.find(text)
            if (weekdayMatch != null) {
                val dayName = weekdayMatch.groupValues[1].lowercase(Locale.ROOT)
                val targetDayOfWeek = when {
                    dayName.startsWith("mon") -> DayOfWeek.MONDAY
                    dayName.startsWith("tue") -> DayOfWeek.TUESDAY
                    dayName.startsWith("wed") -> DayOfWeek.WEDNESDAY
                    dayName.startsWith("thu") -> DayOfWeek.THURSDAY
                    dayName.startsWith("fri") -> DayOfWeek.FRIDAY
                    dayName.startsWith("sat") -> DayOfWeek.SATURDAY
                    dayName.startsWith("sun") -> DayOfWeek.SUNDAY
                    else -> null
                }
                if (targetDayOfWeek != null) {
                    val nextDate = if (today.dayOfWeek == targetDayOfWeek) {
                        today.plusWeeks(1)
                    } else {
                        today.with(TemporalAdjusters.next(targetDayOfWeek))
                    }
                    dueEpochDay = nextDate.toEpochDay()
                }
                text = text.removeRange(weekdayMatch.range).trim()
            } else {
                val monthDayMatch = monthDayPattern.find(text)
                if (monthDayMatch != null) {
                    val dayStr = monthDayMatch.groupValues[1].ifEmpty { monthDayMatch.groupValues[4] }
                    val monthStr = monthDayMatch.groupValues[2].ifEmpty { monthDayMatch.groupValues[3] }.lowercase(Locale.ROOT)
                    val month = parseMonth(monthStr)
                    val dayNum = dayStr.toIntOrNull()
                    if (month != null && dayNum != null) {
                        var candidateYear = today.year
                        val maxDays = month.length(java.time.Year.isLeap(candidateYear.toLong()))
                        val safeDay = dayNum.coerceIn(1, maxDays)
                        var candidateDate = LocalDate.of(candidateYear, month, safeDay)
                        if (candidateDate.isBefore(today)) {
                            candidateYear++
                            candidateDate = LocalDate.of(candidateYear, month, safeDay.coerceAtMost(month.length(java.time.Year.isLeap(candidateYear.toLong()))))
                        }
                        dueEpochDay = candidateDate.toEpochDay()
                    }
                    text = text.removeRange(monthDayMatch.range).trim()
                }
            }
        }

        // Clean extra prepositions left over at ends or double spaces
        text = text.replace(Regex("""\s+at$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+on$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+by$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+for$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s{2,}"""), " ")
            .trim()

        val cleanTitle = text.ifBlank { rawInput.trim() }

        return ParsedQuickCapture(
            title = cleanTitle,
            dueEpochDay = dueEpochDay,
            dueMinuteOfDay = dueMinuteOfDay,
            priority = priority,
            category = category
        )
    }

    private fun parseMonth(name: String): Month? = when {
        name.startsWith("jan") -> Month.JANUARY
        name.startsWith("feb") -> Month.FEBRUARY
        name.startsWith("mar") -> Month.MARCH
        name.startsWith("apr") -> Month.APRIL
        name.startsWith("may") -> Month.MAY
        name.startsWith("jun") -> Month.JUNE
        name.startsWith("jul") -> Month.JULY
        name.startsWith("aug") -> Month.AUGUST
        name.startsWith("sep") -> Month.SEPTEMBER
        name.startsWith("oct") -> Month.OCTOBER
        name.startsWith("nov") -> Month.NOVEMBER
        name.startsWith("dec") -> Month.DECEMBER
        else -> null
    }
}
