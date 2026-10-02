package com.daymark.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daymark.app.data.CourseEntity
import com.daymark.app.data.DashboardConfigurationEntity
import com.daymark.app.data.DashboardWidgets
import com.daymark.app.data.DeadlineEntity
import com.daymark.app.data.EventEntity
import com.daymark.app.data.GoalActivityEntity
import com.daymark.app.data.GoalEntity
import com.daymark.app.data.NoteEntity
import com.daymark.app.data.TaskEntity
import com.daymark.app.data.UserPreferencesEntity
import com.daymark.app.ui.formatDay
import com.daymark.app.ui.formatLongDate
import com.daymark.app.ui.formatTime
import com.daymark.app.ui.greetingForHour
import com.daymark.app.ui.remainingLabel
import com.daymark.app.ui.components.DaymarkMark
import com.daymark.app.ui.components.EmptyState
import com.daymark.app.ui.components.ProgressRing
import com.daymark.app.ui.components.ScreenHeader
import com.daymark.app.ui.components.SectionHeading
import com.daymark.app.ui.components.SoftCard
import com.daymark.app.ui.components.TaskRow
import com.daymark.app.ui.components.TinyPill
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private data class NextItem(val title: String, val time: Int?, val type: String, val detail: String)
private data class TaskMetrics(
    val openTasks: List<TaskEntity>,
    val todayOpen: List<TaskEntity>,
    val completedToday: List<TaskEntity>,
    val totalToday: Int,
    val progress: Float,
    val overdueTasks: Int
)

@Composable
fun HomeScreen(
    tasks: List<TaskEntity>,
    events: List<EventEntity>,
    deadlines: List<DeadlineEntity>,
    goals: List<GoalEntity>,
    goalActivities: List<GoalActivityEntity>,
    notes: List<NoteEntity>,
    courses: List<CourseEntity>,
    dashboard: List<DashboardConfigurationEntity>,
    preferences: UserPreferencesEntity,
    onSearch: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenCourses: () -> Unit,
    onOpenNotes: () -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onOpenTask: (TaskEntity) -> Unit,
    onOpenEvent: (EventEntity) -> Unit,
    onOpenDeadline: (DeadlineEntity) -> Unit,
    onCreateTask: () -> Unit
) {
    val today = remember { LocalDate.now() }
    val todayEpoch = remember(today) { today.toEpochDay() }
    val metrics = remember(tasks, today, todayEpoch) {
        val open = tasks.filter { !it.archived && it.status != "COMPLETED" && it.status != "SKIPPED" }
        val tOpen = open.filter { it.dueEpochDay == todayEpoch }
        val comp = tasks.filter {
            it.status == "COMPLETED" && it.completedAtMillis?.let { millis ->
                Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate() == today
            } == true
        }
        val tot = tOpen.size + comp.count { completed -> tOpen.none { it.id == completed.id } }
        val prog = if (tot == 0) 0f else comp.size.coerceAtMost(tot).toFloat() / tot
        val overdue = open.count { it.dueEpochDay != null && it.dueEpochDay < todayEpoch }
        TaskMetrics(open, tOpen, comp, tot, prog, overdue)
    }
    val openTasks = metrics.openTasks
    val todayOpen = metrics.todayOpen
    val completedToday = metrics.completedToday
    val totalToday = metrics.totalToday
    val progress = metrics.progress
    val overdueTasks = metrics.overdueTasks

    val todaysEvents = remember(events, todayEpoch) {
        events.filter { !it.archived && it.startEpochDay == todayEpoch }
    }
    val upcomingDeadlines = remember(deadlines, todayEpoch) {
        deadlines.filter { !it.archived && it.status != "COMPLETED" }
            .sortedWith(compareBy<DeadlineEntity> { it.dueEpochDay < todayEpoch }.thenBy { it.dueEpochDay }.thenBy { it.dueMinuteOfDay ?: 1439 })
            .take(3)
    }
    val currentMinute = remember {
        val now = LocalTime.now()
        now.hour * 60 + now.minute
    }
    val nextItem = remember(todayOpen, todaysEvents, currentMinute, todayEpoch) {
        buildList {
            todayOpen.forEach { task ->
                task.dueMinuteOfDay?.let { minute ->
                    if (minute >= currentMinute) {
                        add(NextItem(task.title, minute, task.category.ifBlank { "Task" }, formatDay(todayEpoch)))
                    }
                }
            }
            todaysEvents.forEach { event ->
                val minute = event.startMinuteOfDay
                if (minute == null || minute >= currentMinute) {
                    add(NextItem(event.title, minute, "On your schedule", if (event.isAllDay) "All day" else formatDay(todayEpoch)))
                }
            }
        }.sortedBy { it.time ?: Int.MAX_VALUE }.firstOrNull()
    }

    val widgetList = remember(dashboard) {
        (dashboard.ifEmpty { DashboardWidgets.defaults })
            .sortedBy { it.position }
            .filter { it.isVisible }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {
        item("home-header") {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DaymarkMark(size = 34.dp)
                    Spacer(Modifier.size(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("DAYMARK", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, letterSpacing = 1.15.sp)
                        Text("A little more in hand", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    androidx.compose.material3.IconButton(onClick = onSearch) {
                        Icon(Icons.Rounded.Search, contentDescription = "Search Daymark", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text(formatLongDate(todayEpoch), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(3.dp))
                val greeting = greetingForHour()
                val name = preferences.displayName.trim()
                Text(
                    if (name.isBlank()) "$greeting."
                    else "$greeting, $name.",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text("A calm place to keep the day in view.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        widgetList.forEach { widget ->
            when (widget.widgetKey) {
                DashboardWidgets.TODAY_PROGRESS -> item(widget.widgetKey) {
                    TodayProgressCard(
                        progress = progress,
                        completed = completedToday.size.coerceAtMost(totalToday),
                        total = totalToday,
                        scheduledCount = todaysEvents.size,
                        overdueCount = overdueTasks,
                        onClick = onOpenTasks
                    )
                }
                DashboardWidgets.NEXT_UP -> if (nextItem != null) item(widget.widgetKey) {
                    NextUpCard(nextItem, onClick = onOpenSchedule)
                }
                DashboardWidgets.TODAY_TASKS -> item(widget.widgetKey) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeading("Today’s tasks", "See all", onOpenTasks)
                        if (todayOpen.isEmpty() && completedToday.isEmpty()) {
                            SoftCard(modifier = Modifier.fillMaxWidth()) {
                                EmptyState("Your day is clear.", "Add a task when something needs a place in your day.", "Add a task", onCreateTask, modifier = Modifier.padding(0.dp), symbol = "✓")
                            }
                        } else {
                            val visible = (todayOpen + completedToday.filter { done -> todayOpen.none { it.id == done.id } })
                                .distinctBy { it.id }.take(5)
                            SoftCard(modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                                    visible.forEachIndexed { index, task ->
                                        TaskRow(task, preferences.use24HourClock, onToggle = { onToggleTask(task) }, onOpen = { onOpenTask(task) }, compact = true)
                                        if (index != visible.lastIndex) androidx.compose.material3.HorizontalDivider(
                                            modifier = Modifier.padding(start = 54.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                DashboardWidgets.DEADLINES -> if (upcomingDeadlines.isNotEmpty()) item(widget.widgetKey) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeading("Coming up", "Schedule", onOpenSchedule)
                        SoftCard(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                upcomingDeadlines.forEachIndexed { index, deadline ->
                                    DeadlineRow(deadline, onClick = { onOpenDeadline(deadline) })
                                    if (index != upcomingDeadlines.lastIndex) androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f))
                                }
                            }
                        }
                    }
                }
                DashboardWidgets.GOALS -> if (goals.isNotEmpty()) item(widget.widgetKey) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeading("Goals in motion", "View goals", onOpenGoals)
                        val goal = goals.first()
                        val streak = calculateStreak(goal.id, goalActivities, todayEpoch)
                        SoftCard(modifier = Modifier.fillMaxWidth(), onClick = onOpenGoals) {
                            Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = .1f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.size(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(goal.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(if (streak > 0) "$streak active ${if (streak == 1) "day" else "days"}" else "A step at a time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("${goal.progress}%", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = { goal.progress.coerceIn(0, 100) / 100f },
                                modifier = Modifier.fillMaxWidth().padding(start = 17.dp, end = 17.dp, bottom = 17.dp).height(5.dp).clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = .12f)
                            )
                        }
                    }
                }
                DashboardWidgets.COURSES -> if (courses.isNotEmpty()) item(widget.widgetKey) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeading("Learning", "View courses", onOpenCourses)
                        CourseMiniCard(courses.first(), onClick = onOpenCourses)
                    }
                }
                DashboardWidgets.RECENT_NOTES -> if (notes.isNotEmpty()) item(widget.widgetKey) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeading("Recently captured", "Open notes", onOpenNotes)
                        val note = notes.first()
                        SoftCard(modifier = Modifier.fillMaxWidth(), onClick = onOpenNotes) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.size(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(note.title.ifBlank { "Untitled note" }, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(note.content.ifBlank { "No details yet" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                if (note.isPinned) Icon(Icons.Rounded.PushPin, contentDescription = "Pinned", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                            }
                        }
                    }
                }
                DashboardWidgets.INSIGHTS -> item(widget.widgetKey) {
                    val week = tasks.count { it.completedAtMillis?.let { stamp ->
                        Instant.ofEpochMilli(stamp).atZone(ZoneId.systemDefault()).toLocalDate().isAfter(today.minusDays(7))
                    } == true }
                    SoftCard(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.size(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("A steady week", style = MaterialTheme.typography.titleMedium)
                                Text("$week ${if (week == 1) "task" else "tasks"} completed in the last 7 days", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        if (widgetList.none { it.widgetKey == DashboardWidgets.TODAY_TASKS }) {
            item("quick-capture") {
                SoftCard(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant, onClick = onCreateTask) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Quick capture", style = MaterialTheme.typography.titleMedium)
                            Text("Save a thought before it slips away.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Rounded.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayProgressCard(
    progress: Float,
    completed: Int,
    total: Int,
    scheduledCount: Int,
    overdueCount: Int,
    onClick: () -> Unit
) {
    val tint = MaterialTheme.colorScheme.primary
    val surfaceVar = MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(tint.copy(alpha = .14f), surfaceVar.copy(alpha = .72f))))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 19.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("TODAY, IN VIEW", style = MaterialTheme.typography.labelSmall, color = tint, letterSpacing = 1.1.sp)
                Spacer(Modifier.height(7.dp))
                Text(
                    if (total == 0) "A little space to begin."
                    else "$completed of $total complete",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    TinyPill("$scheduledCount scheduled", tint = MaterialTheme.colorScheme.secondary)
                    if (overdueCount > 0) TinyPill("$overdueCount overdue", tint = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(13.dp))
                Text(
                    if (total == 0) "Capture one thing worth remembering."
                    else "${(progress * 100).toInt()}% of today’s tasks",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ProgressRing(progress, modifier = Modifier.padding(start = 10.dp), label = "${(progress * 100).toInt()}%")
        }
    }
}

@Composable
private fun NextUpCard(item: NextItem, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeading("Next up")
        SoftCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), onClick = onClick) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(47.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = .10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (item.type == "On your schedule") Icons.Rounded.Event else Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(3.dp))
                    Text(item.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatTime(item.time), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(3.dp))
                    Text(item.type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun DeadlineRow(deadline: DeadlineEntity, onClick: () -> Unit) {
    val today = LocalDate.now().toEpochDay()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(35.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = .09f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Flag, contentDescription = null, modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.size(11.dp))
        Column(Modifier.weight(1f)) {
            Text(deadline.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (deadline.dueEpochDay < today) remainingLabel(deadline.dueEpochDay, deadline.dueMinuteOfDay) else formatDay(deadline.dueEpochDay),
                style = MaterialTheme.typography.bodySmall,
                color = if (deadline.dueEpochDay < today) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(formatTime(deadline.dueMinuteOfDay), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CourseMiniCard(course: CourseEntity, onClick: () -> Unit) {
    SoftCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(15.dp)).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(course.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(course.platform.ifBlank { "Learning program" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${course.progress}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun calculateStreak(goalId: String, activities: List<GoalActivityEntity>, todayEpochDay: Long): Int {
    val completedDays = activities.filter { it.goalId == goalId }.map { it.activityEpochDay }.toSet()
    var day = if (todayEpochDay in completedDays) todayEpochDay else todayEpochDay - 1
    var streak = 0
    while (day in completedDays) {
        streak++
        day--
    }
    return streak
}
