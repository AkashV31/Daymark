package com.daymark.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import com.daymark.app.data.DeadlineEntity
import com.daymark.app.data.EventEntity
import com.daymark.app.data.RecurrenceRuleEntity
import com.daymark.app.data.TaskEntity
import com.daymark.app.data.UserPreferencesEntity
import com.daymark.app.domain.RecurrenceEngine
import com.daymark.app.ui.formatDay
import com.daymark.app.ui.formatLongDate
import com.daymark.app.ui.formatMonth
import com.daymark.app.ui.formatTime
import com.daymark.app.ui.components.EmptyState
import com.daymark.app.ui.components.ScreenHeader
import com.daymark.app.ui.components.SoftCard
import com.daymark.app.ui.components.TinyPill
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

private enum class ScheduleView(val label: String) { DAY("Day"), THREE_DAYS("3 days"), WEEK("Week"), MONTH("Month"), AGENDA("Agenda") }
private data class ScheduleRow(val id: String, val title: String, val detail: String, val day: Long, val minute: Int?, val type: String, val complete: Boolean)

@Composable
fun ScheduleScreen(
    tasks: List<TaskEntity>,
    events: List<EventEntity>,
    deadlines: List<DeadlineEntity>,
    preferences: UserPreferencesEntity,
    recurrenceRules: List<RecurrenceRuleEntity> = emptyList(),
    onSearch: () -> Unit,
    onOpenTask: (TaskEntity) -> Unit,
    onOpenEvent: (EventEntity) -> Unit,
    onOpenDeadline: (DeadlineEntity) -> Unit
) {
    var viewName by rememberSaveable { mutableStateOf(ScheduleView.DAY.name) }
    val view = ScheduleView.entries.firstOrNull { it.name == viewName } ?: ScheduleView.DAY
    var selectedEpochDay by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)
    val today = LocalDate.now()
    val start = when (view) {
        ScheduleView.DAY -> selectedDate
        ScheduleView.THREE_DAYS -> selectedDate
        ScheduleView.WEEK -> selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        ScheduleView.MONTH -> selectedDate.withDayOfMonth(1)
        ScheduleView.AGENDA -> today
    }
    val end = when (view) {
        ScheduleView.DAY -> start.plusDays(1)
        ScheduleView.THREE_DAYS -> selectedDate.plusDays(3)
        ScheduleView.WEEK -> start.plusDays(7)
        ScheduleView.MONTH -> start.withDayOfMonth(1).plusMonths(1)
        ScheduleView.AGENDA -> today.plusDays(30)
    }

    val rulesMap = remember(recurrenceRules) { recurrenceRules.associateBy { it.id } }

    val visibleTasks = remember(tasks, rulesMap, start, end) {
        val list = mutableListOf<ScheduleRow>()
        val startDay = start.toEpochDay()
        val endDay = end.toEpochDay()

        tasks.filter { !it.archived && it.status != "SKIPPED" && it.dueEpochDay != null }.forEach { task ->
            val dueDay = task.dueEpochDay!!
            if (dueDay in startDay until endDay) {
                list.add(ScheduleRow(task.id, task.title, task.category.ifBlank { "Task" }, dueDay, task.dueMinuteOfDay, "TASK", task.status == "COMPLETED"))
            }
            val rule = task.recurrenceRuleId?.let { rulesMap[it] }
            if (rule != null) {
                var cursor = LocalDate.ofEpochDay(dueDay)
                var idx = task.occurrenceIndex
                while (true) {
                    val next = RecurrenceEngine.nextOccurrence(cursor, rule, currentOccurrenceIndex = idx) ?: break
                    if (next.toEpochDay() >= endDay) break
                    if (next.toEpochDay() >= startDay && next.toEpochDay() != dueDay) {
                        list.add(ScheduleRow(task.id, task.title, task.category.ifBlank { "Task" }, next.toEpochDay(), task.dueMinuteOfDay, "TASK", false))
                    }
                    cursor = next
                    idx++
                }
            }
        }
        list
    }

    val visibleEvents = events.filter {
        !it.archived && it.startEpochDay >= start.toEpochDay() && it.startEpochDay < end.toEpochDay()
    }
    val visibleDeadlines = deadlines.filter {
        !it.archived && it.status != "COMPLETED" && it.dueEpochDay >= start.toEpochDay() && it.dueEpochDay < end.toEpochDay()
    }
    val rows = buildList {
        addAll(visibleTasks)
        visibleEvents.forEach { add(ScheduleRow(it.id, it.title, it.category.ifBlank { "Event" }, it.startEpochDay, if (it.isAllDay) null else it.startMinuteOfDay, "EVENT", false)) }
        visibleDeadlines.forEach { add(ScheduleRow(it.id, it.title, it.category.ifBlank { "Deadline" }, it.dueEpochDay, it.dueMinuteOfDay, "DEADLINE", false)) }
    }.sortedWith(compareBy<ScheduleRow> { it.day }.thenBy { it.minute == null }.thenBy { it.minute ?: Int.MAX_VALUE })

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Schedule", if (view == ScheduleView.AGENDA) "The next 30 days" else formatLongDate(selectedEpochDay), onSearch = onSearch)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            ScheduleView.entries.forEach { option ->
                FilterChip(selected = view == option, onClick = { viewName = option.name }, label = { Text(option.label) })
            }
        }
        if (view == ScheduleView.MONTH) {
            MonthCalendar(
                month = selectedDate,
                selected = selectedDate,
                onSelect = { selectedEpochDay = it.toEpochDay() },
                onPrevious = { selectedEpochDay = selectedDate.minusMonths(1).toEpochDay() },
                onNext = { selectedEpochDay = selectedDate.plusMonths(1).toEpochDay() },
                eventDays = visibleEvents.map { it.startEpochDay }.toSet() + visibleTasks.map { it.day }.toSet() + visibleDeadlines.map { it.dueEpochDay }.toSet()
            )
        } else if (view != ScheduleView.AGENDA) {
            val stripStart = when (view) {
                ScheduleView.DAY -> selectedDate.minusDays(3)
                ScheduleView.THREE_DAYS -> selectedDate
                ScheduleView.WEEK -> selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                else -> selectedDate
            }
            DateStrip(stripStart, selectedEpochDay) { selectedEpochDay = it.toEpochDay() }
        }

        if (rows.isEmpty()) {
            EmptyState(
                title = when (view) {
                    ScheduleView.AGENDA -> "Your upcoming schedule is open."
                    else -> "Nothing on this part of the calendar."
                },
                message = "Tasks, plans, events and deadlines appear here when they have a date.",
                modifier = Modifier.weight(1f).padding(bottom = 20.dp),
                symbol = "◷"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var priorDay: Long? = null
                rows.forEach { row ->
                    if (view != ScheduleView.DAY && row.day != priorDay) {
                        item("day-${row.day}") {
                            Text(
                                formatLongDate(row.day),
                                modifier = Modifier.padding(top = 9.dp, bottom = 2.dp),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    item("${row.type}-${row.id}") {
                        ScheduleTimelineRow(
                            row = row,
                            use24HourClock = preferences.use24HourClock,
                            onClick = {
                                when (row.type) {
                                    "TASK" -> tasks.firstOrNull { it.id == row.id }?.let(onOpenTask)
                                    "EVENT" -> events.firstOrNull { it.id == row.id }?.let(onOpenEvent)
                                    "DEADLINE" -> deadlines.firstOrNull { it.id == row.id }?.let(onOpenDeadline)
                                }
                            }
                        )
                    }
                    priorDay = row.day
                }
            }
        }
    }
}

@Composable
private fun DateStrip(start: LocalDate, selectedEpochDay: Long, onSelect: (LocalDate) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        (0..6).forEach { offset ->
            val date = start.plusDays(offset.toLong())
            val selected = date.toEpochDay() == selectedEpochDay
            Column(
                modifier = Modifier.width(47.dp).clip(RoundedCornerShape(17.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                    .clickable { onSelect(date) }
                    .padding(vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(date.dayOfWeek.name.take(1), style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = .8f) else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.titleSmall, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun MonthCalendar(
    month: LocalDate,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    eventDays: Set<Long>
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(formatMonth(month), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            androidx.compose.material3.IconButton(onClick = onPrevious) { Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous month") }
            androidx.compose.material3.IconButton(onClick = onNext) { Icon(Icons.Rounded.ChevronRight, contentDescription = "Next month") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                Text(day, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
        val first = month.withDayOfMonth(1)
        val mondayOffset = first.dayOfWeek.value - 1
        val daysInMonth = month.lengthOfMonth()
        val cells = ((mondayOffset + daysInMonth + 6) / 7) * 7
        (0 until cells / 7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                (0..6).forEach { weekday ->
                    val dayNumber = week * 7 + weekday - mondayOffset + 1
                    if (dayNumber in 1..daysInMonth) {
                        val date = month.withDayOfMonth(dayNumber)
                        val isSelected = date == selected
                        Column(
                            modifier = Modifier.weight(1f).height(42.dp).clickable { onSelect(date) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                Modifier.size(29.dp).clip(CircleShape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(dayNumber.toString(), color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
                            }
                            if (date.toEpochDay() in eventDays) Box(Modifier.padding(top = 1.dp).size(3.dp).clip(CircleShape).background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary))
                        }
                    } else {
                        Spacer(Modifier.weight(1f).height(42.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleTimelineRow(row: ScheduleRow, use24HourClock: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.width(62.dp).padding(top = 11.dp), horizontalAlignment = Alignment.End) {
            Text(
                if (row.minute == null) "All day" else formatTime(row.minute, use24HourClock),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                maxLines = 1
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(10.dp)) {
            Box(Modifier.padding(top = 14.dp).size(8.dp).clip(CircleShape).background(typeColor(row.type)))
            Box(Modifier.padding(top = 3.dp).width(1.dp).height(49.dp).background(MaterialTheme.colorScheme.outlineVariant))
        }
        Spacer(Modifier.width(8.dp))
        SoftCard(modifier = Modifier.weight(1f).padding(bottom = 5.dp), shape = RoundedCornerShape(17.dp), onClick = onClick) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(row.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(3.dp))
                    Text(row.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                TinyPill(
                    when (row.type) { "TASK" -> "Task"; "EVENT" -> "Event"; else -> "Deadline" },
                    tint = typeColor(row.type)
                )
            }
        }
    }
}

@Composable
private fun typeColor(type: String): androidx.compose.ui.graphics.Color = when (type) {
    "EVENT" -> MaterialTheme.colorScheme.secondary
    "DEADLINE" -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.primary
}
