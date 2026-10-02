package com.daymark.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daymark.app.data.TaskEntity
import com.daymark.app.data.UserPreferencesEntity
import com.daymark.app.ui.components.EmptyState
import com.daymark.app.ui.components.ScreenHeader
import com.daymark.app.ui.components.SoftCard
import com.daymark.app.ui.components.TaskRow
import java.time.LocalDate

private enum class TaskFilter(val label: String) { TODAY("Today"), UPCOMING("Upcoming"), OVERDUE("Overdue"), ANYTIME("Anytime"), COMPLETED("Completed"), ALL("All") }

@Composable
fun TasksScreen(
    tasks: List<TaskEntity>,
    preferences: UserPreferencesEntity,
    onSearch: () -> Unit,
    onOpenTask: (TaskEntity) -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onCreateTask: () -> Unit,
    onDeleteTask: ((TaskEntity) -> Unit)? = null
) {
    var selected by rememberSaveable { mutableStateOf(TaskFilter.TODAY.name) }
    val filter = TaskFilter.entries.firstOrNull { it.name == selected } ?: TaskFilter.TODAY
    val today = LocalDate.now().toEpochDay()

    val (open, completed, filtered) = remember(tasks, filter, today) {
        val o = tasks.filter { !it.archived && it.status != "COMPLETED" && it.status != "SKIPPED" }
        val c = tasks.filter { !it.archived && it.status == "COMPLETED" }.sortedByDescending { it.completedAtMillis }
        val f = when (filter) {
            TaskFilter.TODAY -> o.filter { it.dueEpochDay == today }.sortedWith(taskSort())
            TaskFilter.UPCOMING -> o.filter { it.dueEpochDay != null && it.dueEpochDay > today }.sortedWith(taskSort())
            TaskFilter.OVERDUE -> o.filter { it.dueEpochDay != null && it.dueEpochDay < today }.sortedWith(taskSort())
            TaskFilter.ANYTIME -> o.filter { it.dueEpochDay == null }
            TaskFilter.COMPLETED -> c
            TaskFilter.ALL -> (o + c).sortedWith(taskSort())
        }
        Triple(o, c, f)
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Tasks", "A clear view of what’s in motion", onSearch = onSearch)
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TaskFilter.entries.forEach { item ->
                FilterChip(
                    selected = filter == item,
                    onClick = { selected = item.name },
                    label = { Text(item.label) }
                )
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filtered.isEmpty()) {
                item {
                    EmptyState(
                        title = when (filter) {
                            TaskFilter.TODAY -> "Your day is clear."
                            TaskFilter.UPCOMING -> "Nothing scheduled ahead."
                            TaskFilter.OVERDUE -> "No overdue tasks."
                            TaskFilter.ANYTIME -> "No unscheduled tasks."
                            TaskFilter.COMPLETED -> "Completed tasks will live here."
                            TaskFilter.ALL -> "Your task list is ready."
                        },
                        message = when (filter) {
                            TaskFilter.TODAY -> "Give something important a place in today."
                            TaskFilter.UPCOMING -> "Tasks with a future date will appear here."
                            TaskFilter.OVERDUE -> "Anything unfinished stays visible here without judgment."
                            TaskFilter.ANYTIME -> "Keep flexible tasks here until you’re ready to schedule them."
                            TaskFilter.COMPLETED -> "Your completed history stays on this device."
                            TaskFilter.ALL -> "Start with a task, then add a date or reminder when it helps."
                        },
                        actionLabel = if (filter == TaskFilter.COMPLETED || filter == TaskFilter.OVERDUE) null else "Add a task",
                        onAction = if (filter == TaskFilter.COMPLETED || filter == TaskFilter.OVERDUE) null else onCreateTask,
                        modifier = Modifier.padding(top = 25.dp)
                    )
                }
            } else {
                item(key = "header_summary") {
                    Text(
                        when (filter) {
                            TaskFilter.TODAY -> "TODAY · ${filtered.size} ${if (filtered.size == 1) "TASK" else "TASKS"}"
                            TaskFilter.UPCOMING -> "UPCOMING · ${filtered.size}"
                            TaskFilter.OVERDUE -> "OVERDUE · ${filtered.size}"
                            TaskFilter.ANYTIME -> "WITHOUT A DATE · ${filtered.size}"
                            TaskFilter.COMPLETED -> "COMPLETED · ${filtered.size}"
                            TaskFilter.ALL -> "ALL TASKS · ${filtered.size}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (filter == TaskFilter.OVERDUE && filtered.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = .8f.sp
                    )
                }
                items(filtered, key = { it.id }) { task ->
                    SoftCard(modifier = Modifier.fillMaxWidth().animateItem()) {
                        TaskRow(
                            task = task,
                            use24HourClock = preferences.use24HourClock,
                            onToggle = { onToggleTask(task) },
                            onOpen = { onOpenTask(task) },
                            onDelete = onDeleteTask?.let { { it(task) } },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            if (filter != TaskFilter.TODAY && open.any { it.dueEpochDay == today } && filtered.isNotEmpty()) {
                item(key = "today_spacer") { Spacer(Modifier.height(6.dp)) }
            }
        }
    }
}

private fun taskSort(): Comparator<TaskEntity> = compareBy<TaskEntity> { it.dueEpochDay == null }
    .thenBy { it.dueEpochDay ?: Long.MAX_VALUE }
    .thenBy { it.dueMinuteOfDay ?: Int.MAX_VALUE }
    .thenByDescending { it.priorityRank() }

private fun TaskEntity.priorityRank(): Int = when (priority) {
    "CRITICAL" -> 4
    "HIGH" -> 3
    "NORMAL" -> 2
    else -> 1
}
