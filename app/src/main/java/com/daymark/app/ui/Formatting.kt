package com.daymark.app.ui

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun dayFormat() = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())
private fun longDateFormat() = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())
private fun monthFormat() = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
private fun time12Format() = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
private fun time24Format() = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

fun formatDay(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(dayFormat())
fun formatLongDate(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(longDateFormat())
fun formatMonth(date: LocalDate): String = date.format(monthFormat())
fun formatTime(minuteOfDay: Int?, use24Hour: Boolean = false): String {
    if (minuteOfDay == null) return "Any time"
    val time = LocalTime.of((minuteOfDay / 60).coerceIn(0, 23), (minuteOfDay % 60).coerceIn(0, 59))
    return time.format(if (use24Hour) time24Format() else time12Format())
}
fun greetingForHour(hour: Int = LocalTime.now().hour): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Hello"
}
fun relativeDateLabel(epochDay: Long, today: LocalDate = LocalDate.now()): String {
    val date = LocalDate.ofEpochDay(epochDay)
    return when (date) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(dayFormat())
    }
}
fun remainingLabel(
    epochDay: Long,
    minuteOfDay: Int? = null,
    use24Hour: Boolean = false,
    nowMillis: Long = System.currentTimeMillis()
): String {
    val zone = ZoneId.systemDefault()
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    val date = LocalDate.ofEpochDay(epochDay)
    val days = java.time.temporal.ChronoUnit.DAYS.between(today, date)
    return when {
        days < -1 -> "${-days} days overdue"
        days == -1L -> "Overdue since yesterday"
        days == 0L && minuteOfDay != null -> "Due ${formatTime(minuteOfDay, use24Hour)}"
        days == 0L -> "Due today"
        days == 1L -> "Due tomorrow"
        else -> "${days} days remaining"
    }
}
