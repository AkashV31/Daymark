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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daymark.app.data.CourseEntity
import com.daymark.app.data.DeadlineEntity
import com.daymark.app.data.GoalActivityEntity
import com.daymark.app.data.GoalEntity
import com.daymark.app.data.ProjectEntity
import com.daymark.app.data.TaskCompletionEntity
import com.daymark.app.data.TaskEntity
import com.daymark.app.ui.formatDay
import com.daymark.app.ui.remainingLabel
import com.daymark.app.ui.MoreDestination
import com.daymark.app.ui.components.EmptyState
import com.daymark.app.ui.components.PrimaryAction
import com.daymark.app.ui.components.ScreenHeader
import com.daymark.app.ui.components.SectionHeading
import com.daymark.app.ui.components.SoftCard
import com.daymark.app.ui.components.TinyPill
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.IconButton
import androidx.compose.runtime.remember

@Composable
fun MoreHubScreen(onOpen: (MoreDestination) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(Modifier.padding(bottom = 4.dp)) {
                Text("More of your day", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text("Keep your plans, learning and settings in one quiet place.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { SectionHeading("Plan") }
        item { MoreDestinationCard("Goals", "Outcomes you’re moving toward", Icons.Rounded.Flag, MaterialTheme.colorScheme.primary) { onOpen(MoreDestination.GOALS) } }
        item { MoreDestinationCard("Projects", "Multi-step plans and milestones", Icons.Rounded.WorkOutline, MaterialTheme.colorScheme.secondary) { onOpen(MoreDestination.PROJECTS) } }
        item { SectionHeading("Learn") }
        item { MoreDestinationCard("Learning", "Courses and skill-building", Icons.Rounded.MenuBook, MaterialTheme.colorScheme.tertiary) { onOpen(MoreDestination.COURSES) } }
        item { MoreDestinationCard("Deadlines", "Important dates that stay visible", Icons.Rounded.CalendarMonth, MaterialTheme.colorScheme.error) { onOpen(MoreDestination.DEADLINES) } }
        item { SectionHeading("Review") }
        item { MoreDestinationCard("Insights", "A neutral review of your execution", Icons.Rounded.Insights, MaterialTheme.colorScheme.primary) { onOpen(MoreDestination.INSIGHTS) } }
        item { SectionHeading("App") }
        item { MoreDestinationCard("Settings & backup", "Appearance, reminders and local data", Icons.Rounded.Settings, MaterialTheme.colorScheme.outline) { onOpen(MoreDestination.SETTINGS) } }
        item {
            SoftCard(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.size(11.dp))
                    Column {
                        Text("Private by design", style = MaterialTheme.typography.titleSmall)
                        Text("Your information stays on this device. No account or internet connection needed.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreDestinationCard(title: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, onClick: () -> Unit) {
    SoftCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), onClick = onClick) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(43.dp).clip(RoundedCornerShape(15.dp)).background(tint.copy(alpha = .13f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint)
            }
            Spacer(Modifier.size(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ArrowForward, contentDescription = "Open $title", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
fun GoalsScreen(
    goals: List<GoalEntity>,
    activities: List<GoalActivityEntity>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (GoalEntity) -> Unit,
    onDelete: ((GoalEntity) -> Unit)? = null
) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Goals", "Something worth moving toward", onBack = onBack)
        if (goals.isEmpty()) {
            EmptyState("Define something worth moving toward.", "Goals can be milestones, intentions or longer outcomes—not just numbers.", "Create a goal", onAdd, Modifier.weight(1f), "✧")
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp, 6.dp, 18.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                item { SectionHeading("In motion", "New goal", onAdd) }
                items(goals, key = { it.id }) { goal ->
                    val streak = goalStreak(goal.id, activities)
                    SoftCard(modifier = Modifier.fillMaxWidth().animateItem(), shape = RoundedCornerShape(23.dp), onClick = { onOpen(goal) }) {
                        Column(Modifier.padding(17.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = .11f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
                                }
                                Spacer(Modifier.size(11.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(goal.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(goal.targetEpochDay?.let { "Target ${formatDay(it)}" } ?: "No target date", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("${goal.progress}%", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                if (onDelete != null) {
                                    Spacer(Modifier.width(4.dp))
                                    IconButton(onClick = { onDelete(goal) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete goal", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(13.dp))
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = { goal.progress.coerceIn(0, 100) / 100f },
                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = .11f)
                            )
                            Row(Modifier.padding(top = 11.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TinyPill(if (streak == 0) "Ready when you are" else "$streak active ${if (streak == 1) "day" else "days"}")
                                Text("Tap to adjust progress", modifier = Modifier.align(Alignment.CenterVertically), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectsScreen(
    projects: List<ProjectEntity>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (ProjectEntity) -> Unit,
    onDelete: ((ProjectEntity) -> Unit)? = null
) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Projects", "Plans with more than one step", onBack = onBack)
        if (projects.isEmpty()) {
            EmptyState("Start with the next small step.", "Projects keep related tasks and milestones together, without adding project-management overhead.", "Create a project", onAdd, Modifier.weight(1f), "⌁")
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp, 6.dp, 18.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { SectionHeading("Your projects", "New project", onAdd) }
                items(projects, key = { it.id }) { project ->
                    SoftCard(modifier = Modifier.fillMaxWidth().animateItem(), shape = RoundedCornerShape(21.dp), onClick = { onOpen(project) }) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(42.dp).clip(RoundedCornerShape(15.dp)).background(MaterialTheme.colorScheme.secondary.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.WorkOutline, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            }
                            Spacer(Modifier.size(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(project.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(project.targetEpochDay?.let { "Target ${formatDay(it)}" } ?: project.description.ifBlank { "In progress" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            TinyPill(project.status.lowercase().replaceFirstChar { it.titlecase() })
                            if (onDelete != null) {
                                Spacer(Modifier.width(4.dp))
                                IconButton(onClick = { onDelete(project) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete project", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CoursesScreen(
    courses: List<CourseEntity>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (CourseEntity) -> Unit,
    onDelete: ((CourseEntity) -> Unit)? = null
) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Learning", "Courses, skills and programs", onBack = onBack)
        if (courses.isEmpty()) {
            EmptyState("Your learning space is ready.", "Keep an online course, certification or self-learning plan here. It works just as well offline.", "Add a course", onAdd, Modifier.weight(1f), "⌑")
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp, 6.dp, 18.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { SectionHeading("Learning in progress", "Add course", onAdd) }
                items(courses, key = { it.id }) { course ->
                    SoftCard(modifier = Modifier.fillMaxWidth().animateItem(), shape = RoundedCornerShape(21.dp), onClick = { onOpen(course) }) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(42.dp).clip(RoundedCornerShape(15.dp)).background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                }
                                Spacer(Modifier.size(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(course.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(listOf(course.platform, course.category).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Self-paced learning" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                                Text("${course.progress}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                if (onDelete != null) {
                                    Spacer(Modifier.width(4.dp))
                                    IconButton(onClick = { onDelete(course) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete course", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = { course.progress.coerceIn(0, 100) / 100f },
                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = .12f)
                            )
                            course.targetEpochDay?.let { Text("Target ${formatDay(it)}", modifier = Modifier.padding(top = 9.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeadlinesScreen(
    deadlines: List<DeadlineEntity>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (DeadlineEntity) -> Unit,
    onComplete: (DeadlineEntity) -> Unit,
    onDelete: ((DeadlineEntity) -> Unit)? = null
) {
    val today = LocalDate.now().toEpochDay()
    val active = deadlines.filter { it.status != "COMPLETED" && !it.archived }.sortedBy { it.dueEpochDay }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Deadlines", "Dates that deserve a little room", onBack = onBack)
        if (active.isEmpty()) {
            EmptyState("Nothing pressing right now.", "Add an assignment, application, payment or personal commitment when a date matters.", "Add a deadline", onAdd, Modifier.weight(1f), "⚑")
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp, 6.dp, 18.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { SectionHeading("Open deadlines", "Add deadline", onAdd) }
                val overdue = active.filter { it.dueEpochDay < today }
                val upcoming = active.filter { it.dueEpochDay >= today }
                if (overdue.isNotEmpty()) {
                    item { Text("OVERDUE · ${overdue.size}", modifier = Modifier.padding(top = 3.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, letterSpacing = .8f.sp) }
                    items(overdue, key = { it.id }) { deadline -> DeadlineCard(deadline, true, onOpen, onComplete, onDelete?.let { { it(deadline) } }, modifier = Modifier.animateItem()) }
                }
                if (upcoming.isNotEmpty()) {
                    item { Text("UPCOMING · ${upcoming.size}", modifier = Modifier.padding(top = 7.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = .8f.sp) }
                    items(upcoming, key = { it.id }) { deadline -> DeadlineCard(deadline, false, onOpen, onComplete, onDelete?.let { { it(deadline) } }, modifier = Modifier.animateItem()) }
                }
            }
        }
    }
}

@Composable
private fun DeadlineCard(
    deadline: DeadlineEntity,
    overdue: Boolean,
    onOpen: (DeadlineEntity) -> Unit,
    onComplete: (DeadlineEntity) -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    SoftCard(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), onClick = { onOpen(deadline) }) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(if (overdue) MaterialTheme.colorScheme.error.copy(alpha = .1f) else MaterialTheme.colorScheme.primary.copy(alpha = .1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Flag, contentDescription = null, tint = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.size(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(deadline.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (deadline.category.isBlank()) "Deadline" else deadline.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TinyPill(if (overdue) "${remainingLabel(deadline.dueEpochDay)}" else formatDay(deadline.dueEpochDay), tint = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                if (onDelete != null) {
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete deadline", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f), modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(deadline.description.ifBlank { "Due ${formatDay(deadline.dueEpochDay)}" }, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Mark complete", modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onComplete(deadline) }.padding(horizontal = 9.dp, vertical = 7.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private data class InsightsData(
    val weekCompletions: List<TaskCompletionEntity>,
    val completionRate: Int,
    val daily: List<Pair<LocalDate, Int>>,
    val maxValue: Int,
    val mostProductive: LocalDate?,
    val overdue: Int,
    val avgDelay: Double
)

@Composable
fun InsightsScreen(
    tasks: List<TaskEntity>,
    completions: List<TaskCompletionEntity>,
    goals: List<GoalEntity>,
    activities: List<GoalActivityEntity>,
    onBack: () -> Unit
) {
    val today = LocalDate.now()
    val start = today.minusDays(6)
    val insights = remember(tasks, completions, goals, activities, today) {
        val wc = completions.filter { LocalDate.ofEpochDay(it.occurrenceDateEpochDay) in start..today }
        val wt = tasks.filter { task ->
            task.dueEpochDay?.let { LocalDate.ofEpochDay(it) in start..today } == true
        }
        val planned = (wt.map { it.id } + wc.map { it.taskId }).toSet().size
        val completedCount = wc.map { it.taskId }.toSet().size
        val cr = if (planned == 0) 0 else (completedCount * 100 / planned).coerceAtMost(100)
        val d = (0L..6L).map { offset ->
            val date = today.minusDays(6 - offset)
            date to wc.count { it.occurrenceDateEpochDay == date.toEpochDay() }
        }
        val mv = (d.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
        val mp = d.maxByOrNull { it.second }?.takeIf { it.second > 0 }?.first
        val od = tasks.count { it.status != "COMPLETED" && !it.archived && it.dueEpochDay?.let { day -> day < today.toEpochDay() } == true }
        val ad = tasks.mapNotNull { task ->
            val completedAt = task.completedAtMillis ?: return@mapNotNull null
            val dueDay = task.dueEpochDay ?: return@mapNotNull null
            java.time.temporal.ChronoUnit.DAYS.between(LocalDate.ofEpochDay(dueDay), Instant.ofEpochMilli(completedAt).atZone(ZoneId.systemDefault()).toLocalDate()).coerceAtLeast(0)
        }.average()
        InsightsData(wc, cr, d, mv, mp, od, ad)
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Insights", "Patterns, not judgment", onBack = onBack)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp, 7.dp, 18.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("${insights.weekCompletions.size}", "completed · 7 days", Modifier.weight(1f))
                    StatCard("${insights.completionRate}%", "completion rate", Modifier.weight(1f))
                }
            }
            item {
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(17.dp)) {
                        Text("COMPLETIONS THIS WEEK", style = MaterialTheme.typography.labelSmall, letterSpacing = .8f.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(17.dp))
                        Row(Modifier.fillMaxWidth().height(116.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            insights.daily.forEach { (date, count) ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.weight(1f)) {
                                    Text(if (count == 0) "" else count.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(5.dp))
                                    Box(
                                        Modifier.width(18.dp).height((18 + 63f * count / insights.maxValue).dp).clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = if (date == insights.mostProductive) .95f else .48f))
                                    )
                                    Spacer(Modifier.height(7.dp))
                                    Text(date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("${insights.overdue}", "currently overdue", Modifier.weight(1f))
                    StatCard(insights.avgDelay.takeIf { !it.isNaN() }?.let { "${"%.1f".format(Locale.getDefault(), it)} d" } ?: "—", "avg. delay", Modifier.weight(1f))
                }
            }
            item {
                SoftCard(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(Modifier.padding(17.dp)) {
                        Text("Goal activity", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        if (goals.isEmpty()) {
                            Text("Create a goal and link meaningful tasks to see active-day patterns here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            goals.take(3).forEach { goal ->
                                val days = activities.filter { it.goalId == goal.id }.map { it.activityEpochDay }.toSet()
                                val recentDays = days.count { it >= today.minusDays(6).toEpochDay() && it <= today.toEpochDay() }
                                Row(Modifier.fillMaxWidth().padding(top = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(goal.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    TinyPill("$recentDays active days")
                                }
                            }
                        }
                    }
                }
            }
            item {
                Text("Statistics use locally stored completion history. No activity is sent anywhere.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    SoftCard(modifier = modifier, shape = RoundedCornerShape(21.dp)) {
        Column(Modifier.padding(15.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(5.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun goalStreak(goalId: String, activities: List<GoalActivityEntity>): Int {
    val today = LocalDate.now().toEpochDay()
    val days = activities.filter { it.goalId == goalId }.map { it.activityEpochDay }.toSet()
    var cursor = if (today in days) today else today - 1
    var streak = 0
    while (cursor in days) { streak++; cursor-- }
    return streak
}
