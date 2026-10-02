package com.daymark.app.ui.sheets

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daymark.app.data.CourseEntity
import com.daymark.app.data.CourseModuleEntity
import com.daymark.app.data.DeadlineEntity
import com.daymark.app.data.EventEntity
import com.daymark.app.data.GoalEntity
import com.daymark.app.data.MilestoneEntity
import com.daymark.app.data.NoteEntity
import com.daymark.app.data.ProjectEntity
import com.daymark.app.data.RecurrenceRuleEntity
import com.daymark.app.data.TaskEntity
import com.daymark.app.data.SubTaskEntity
import com.daymark.app.ui.formatDay
import com.daymark.app.ui.formatTime
import com.daymark.app.ui.components.PrimaryAction
import com.daymark.app.ui.components.SoftCard
import com.daymark.app.ui.components.TinyPill
import com.daymark.app.domain.RecurrenceEngine
import com.daymark.app.domain.QuickCaptureParser
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

internal enum class CreateKind(val label: String, val detail: String) {
    TASK("Task", "Something to do"),
    REMINDER("Reminder", "A nudge at the right time"),
    EVENT("Event", "A plan with a time"),
    DEADLINE("Deadline", "A date that matters"),
    PROJECT("Project", "A multi-step plan"),
    GOAL("Goal", "An outcome to move toward"),
    COURSE("Course", "Learning and skill-building"),
    NOTE("Note", "A thought to keep"),
    FITNESS("Gym / fitness", "A session or activity"),
    QUICK_CAPTURE("Quick capture", "Natural typing")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddActionSheet(onDismiss: () -> Unit, onSelect: (CreateKind) -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 26.dp)) {
            Text("Create something", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(5.dp))
            Text("Give it a place in Daymark.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(17.dp))
            CreateKind.entries.chunked(3).forEach { rowItems ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    rowItems.forEach { kind ->
                        CreateActionTile(kind, Modifier.weight(1f)) { onSelect(kind) }
                    }
                    repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(9.dp))
            }
        }
    }
}

@Composable
private fun CreateActionTile(kind: CreateKind, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val (icon, tint) = when (kind) {
        CreateKind.TASK, CreateKind.REMINDER, CreateKind.FITNESS -> Icons.Rounded.Check to MaterialTheme.colorScheme.primary
        CreateKind.EVENT -> Icons.Rounded.CalendarMonth to MaterialTheme.colorScheme.secondary
        CreateKind.DEADLINE -> Icons.Rounded.Flag to MaterialTheme.colorScheme.error
        CreateKind.PROJECT -> Icons.Rounded.WorkOutline to MaterialTheme.colorScheme.secondary
        CreateKind.GOAL -> Icons.Rounded.Flag to MaterialTheme.colorScheme.primary
        CreateKind.COURSE -> Icons.Rounded.MenuBook to MaterialTheme.colorScheme.tertiary
        CreateKind.NOTE -> Icons.Rounded.Notes to MaterialTheme.colorScheme.primary
        CreateKind.QUICK_CAPTURE -> Icons.Rounded.Bolt to MaterialTheme.colorScheme.tertiary
    }
    SoftCard(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(19.dp), onClick = onClick) {
        Column(Modifier.fillMaxWidth().height(103.dp).padding(11.dp), verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(11.dp)).background(tint.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(7.dp))
            Text(kind.label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(kind.detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TaskEditorSheet(
    existing: TaskEntity?,
    forceReminder: Boolean,
    initialCategory: String,
    use24HourClock: Boolean,
    projects: List<ProjectEntity>,
    goals: List<GoalEntity>,
    courses: List<CourseEntity>,
    onLoadReminders: suspend (String, String) -> Set<Int>,
    onLoadRule: suspend (String) -> RecurrenceRuleEntity?,
    onLoadSubtasks: (suspend (String) -> List<SubTaskEntity>)? = null,
    onDismiss: () -> Unit,
    onSave: (TaskEntity, String, Int, Int, Long?, Int?, Set<Int>, String, List<SubTaskEntity>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by rememberSaveable(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var description by rememberSaveable(existing?.id) { mutableStateOf(existing?.description.orEmpty()) }
    var category by rememberSaveable(existing?.id) { mutableStateOf(existing?.category?.ifBlank { initialCategory } ?: initialCategory) }
    var dueDay by rememberSaveable(existing?.id) { mutableStateOf(existing?.dueEpochDay ?: if (forceReminder) LocalDate.now().toEpochDay() else null) }
    var dueMinute by rememberSaveable(existing?.id) { mutableStateOf(existing?.dueMinuteOfDay ?: if (forceReminder) 9 * 60 else null) }
    var priority by rememberSaveable(existing?.id) { mutableStateOf(existing?.priority ?: "NORMAL") }
    var frequency by rememberSaveable(existing?.id) { mutableStateOf(RecurrenceEngine.NONE) }
    var interval by rememberSaveable(existing?.id) { mutableStateOf(1) }
    var weekdaysMask by rememberSaveable(existing?.id) { mutableStateOf(0) }
    var endType by rememberSaveable(existing?.id) { mutableStateOf("NEVER") }
    var endDay by rememberSaveable(existing?.id) { mutableStateOf<Long?>(null) }
    var occurrenceLimit by rememberSaveable(existing?.id) { mutableStateOf(10) }
    var editScope by rememberSaveable(existing?.id) { mutableStateOf("THIS_OCCURRENCE") }
    var duration by rememberSaveable(existing?.id) { mutableStateOf(existing?.estimatedDurationMinutes?.toString().orEmpty()) }
    var notes by rememberSaveable(existing?.id) { mutableStateOf(existing?.notes.orEmpty()) }
    var projectId by rememberSaveable(existing?.id) { mutableStateOf(existing?.projectId) }
    var goalId by rememberSaveable(existing?.id) { mutableStateOf(existing?.goalId) }
    var courseId by rememberSaveable(existing?.id) { mutableStateOf(existing?.courseId) }
    var reminders by remember(existing?.id) { mutableStateOf<Set<Int>>(if (forceReminder) setOf(0) else emptySet()) }
    var subtasks by remember(existing?.id) { mutableStateOf<List<SubTaskEntity>>(emptyList()) }
    var newSubtaskTitle by rememberSaveable { mutableStateOf("") }
    var loaded by remember(existing?.id) { mutableStateOf(existing == null) }
    var validation by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(existing?.id) {
        if (existing != null) {
            reminders = onLoadReminders("TASK", existing.id)
            if (onLoadSubtasks != null) {
                subtasks = onLoadSubtasks(existing.id)
            }
            existing.recurrenceRuleId?.let { ruleId ->
                onLoadRule(ruleId)?.let { rule ->
                    frequency = rule.frequency
                    interval = rule.interval
                    weekdaysMask = rule.weekdaysMask
                    if (rule.endEpochDay != null) { endType = "DATE"; endDay = rule.endEpochDay }
                    if (rule.occurrenceLimit != null) { endType = "COUNT"; occurrenceLimit = rule.occurrenceLimit }
                }
            }
            loaded = true
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding()
                .padding(horizontal = 20.dp).padding(bottom = 25.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SheetHeading(if (existing == null) if (forceReminder) "New reminder" else "New task" else "Edit task", "Keep it light. Add detail only where it helps.", onDismiss)
            OutlinedTextField(title, { title = it; validation = null }, Modifier.fillMaxWidth(), label = { Text("What needs doing?") }, placeholder = { Text("Task title") }, singleLine = true, shape = RoundedCornerShape(16.dp))
            OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("Details (optional)") }, minLines = 2, maxLines = 4, shape = RoundedCornerShape(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                DateChooser(dueDay, { dueDay = it; validation = null }, "Due date", Modifier.weight(1f))
                TimeChooser(dueMinute, use24HourClock, { dueMinute = it }, "Time", Modifier.weight(1f))
            }
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Priority", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("LOW" to "Low", "NORMAL" to "Normal", "HIGH" to "High", "CRITICAL" to "Critical").forEach { (id, label) ->
                        FilterChip(selected = priority == id, onClick = { priority = id }, label = { Text(label) })
                    }
                }
            }
            OutlinedTextField(category, { category = it }, Modifier.fillMaxWidth(), label = { Text("Category") }, placeholder = { Text("Personal, work, fitness…") }, singleLine = true, shape = RoundedCornerShape(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssociationDropdown("Project", projectId, projects.map { it.id to it.title }, { projectId = it }, Modifier.weight(1f))
                AssociationDropdown("Goal", goalId, goals.map { it.id to it.title }, { goalId = it }, Modifier.weight(1f))
            }
            AssociationDropdown("Course / learning source", courseId, courses.map { it.id to it.title }, { courseId = it }, Modifier.fillMaxWidth())
            OutlinedTextField(
                duration,
                { duration = it.filter(Char::isDigit).take(3) },
                Modifier.fillMaxWidth(),
                label = { Text("Estimated duration in minutes (optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            OutlinedTextField(notes, { notes = it }, Modifier.fillMaxWidth(), label = { Text("Private note (optional)") }, minLines = 1, maxLines = 3, shape = RoundedCornerShape(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Checklist, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        if (subtasks.isEmpty()) "Subtasks / checklist"
                        else "Subtasks (${subtasks.count { it.isCompleted }}/${subtasks.size})",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                subtasks.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f)).padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = item.isCompleted,
                            onCheckedChange = { checked ->
                                subtasks = subtasks.toMutableList().also { list ->
                                    list[index] = item.copy(isCompleted = checked)
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            item.title,
                            modifier = Modifier.weight(1f).padding(start = 4.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = {
                            subtasks = subtasks.toMutableList().also { it.removeAt(index) }
                        }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Remove subtask", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(17.dp))
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newSubtaskTitle,
                        onValueChange = { newSubtaskTitle = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Add a step or subtask") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (newSubtaskTitle.isNotBlank()) {
                                val newId = UUID.randomUUID().toString()
                                val taskId = existing?.id ?: ""
                                subtasks = subtasks + SubTaskEntity(
                                    id = newId,
                                    taskId = taskId,
                                    title = newSubtaskTitle.trim(),
                                    sortOrder = subtasks.size
                                )
                                newSubtaskTitle = ""
                            }
                        }
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add subtask", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Repeat", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(
                        RecurrenceEngine.NONE to "Never",
                        RecurrenceEngine.DAILY to "Daily",
                        RecurrenceEngine.WEEKDAYS to "Weekdays",
                        RecurrenceEngine.WEEKLY to "Weekly",
                        RecurrenceEngine.MONTHLY to "Monthly",
                        RecurrenceEngine.CUSTOM to "Custom"
                    ).forEach { (id, label) -> FilterChip(selected = frequency == id, onClick = { frequency = id }, label = { Text(label) }) }
                }
                if (frequency !in setOf(RecurrenceEngine.NONE, RecurrenceEngine.WEEKDAYS)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Every", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.width(7.dp))
                        TextButton(onClick = { interval = (interval - 1).coerceAtLeast(1) }) { Text("−") }
                        Text(interval.toString(), style = MaterialTheme.typography.titleSmall)
                        TextButton(onClick = { interval = (interval + 1).coerceAtMost(99) }) { Text("+") }
                        Text(if (frequency == RecurrenceEngine.DAILY) "days" else if (frequency == RecurrenceEngine.MONTHLY) "months" else "weeks", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (frequency == RecurrenceEngine.WEEKLY || frequency == RecurrenceEngine.CUSTOM) {
                    WeekdaySelector(weekdaysMask, { weekdaysMask = it })
                }
                if (frequency != RecurrenceEngine.NONE) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        FilterChip(selected = endType == "NEVER", onClick = { endType = "NEVER"; endDay = null }, label = { Text("Never ends") })
                        FilterChip(selected = endType == "DATE", onClick = { endType = "DATE" }, label = { Text("On date") })
                        FilterChip(selected = endType == "COUNT", onClick = { endType = "COUNT" }, label = { Text("After count") })
                    }
                    if (endType == "DATE") DateChooser(endDay, { endDay = it }, "Repeat until", Modifier.fillMaxWidth())
                    if (endType == "COUNT") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Occurrences", style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = { occurrenceLimit = (occurrenceLimit - 1).coerceAtLeast(1) }) { Text("−") }
                            Text(occurrenceLimit.toString(), style = MaterialTheme.typography.titleSmall)
                            TextButton(onClick = { occurrenceLimit = (occurrenceLimit + 1).coerceAtMost(999) }) { Text("+") }
                        }
                    }
                }
                if (existing?.recurrenceRuleId != null) {
                    Text("Apply changes to", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("THIS_OCCURRENCE" to "This occurrence", "THIS_AND_FUTURE" to "This and future", "ENTIRE_SERIES" to "Entire series").forEach { (id, label) ->
                            FilterChip(selected = editScope == id, onClick = { editScope = id }, label = { Text(label) })
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Remind me", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.width(6.dp))
                    TinyPill("${reminders.size} selected")
                }
                Text("Choose one or more reminders before the due time.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ReminderOffsetChoices(reminders) { offset -> reminders = if (offset in reminders) reminders - offset else reminders + offset }
            }
            validation?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            PrimaryAction(
                if (existing == null) "Save task" else "Save changes",
                Modifier.fillMaxWidth(),
                enabled = loaded
            ) {
                val cleanTitle = title.trim()
                when {
                    cleanTitle.isBlank() -> validation = "Add a short title to continue."
                    reminders.isNotEmpty() && dueDay == null -> validation = "Choose a due date before adding reminders."
                    frequency != RecurrenceEngine.NONE && dueDay == null -> validation = "Choose a due date before repeating this task."
                    endType == "DATE" && (endDay == null || (dueDay != null && endDay!! < dueDay!!)) -> validation = "Choose an end date on or after the first occurrence."
                    frequency == RecurrenceEngine.CUSTOM && weekdaysMask == 0 -> validation = "Choose at least one weekday for a custom repeat."
                    else -> {
                        val now = System.currentTimeMillis()
                        val id = existing?.id ?: UUID.randomUUID().toString()
                        val task = (existing ?: TaskEntity(id = id, title = cleanTitle)).copy(
                            id = id,
                            title = cleanTitle,
                            description = description.trim(),
                            dueEpochDay = dueDay,
                            dueMinuteOfDay = if (dueDay == null) null else dueMinute,
                            estimatedDurationMinutes = duration.toIntOrNull()?.coerceAtLeast(1),
                            priority = priority,
                            status = existing?.status ?: "PLANNED",
                            category = category.trim(),
                            courseId = courseId,
                            projectId = projectId,
                            goalId = goalId,
                            recurrenceRuleId = if (frequency == RecurrenceEngine.NONE) null else existing?.recurrenceRuleId,
                            seriesId = existing?.seriesId,
                            notes = notes.trim(),
                            updatedAtMillis = now
                        )
                        val recurrenceEnd = if (endType == "DATE") endDay else null
                        val recurrenceCount = if (endType == "COUNT") occurrenceLimit else null
                        val updatedSubtasks = subtasks.map { it.copy(taskId = id) }
                        onSave(task, frequency, interval, weekdaysMask, recurrenceEnd, recurrenceCount, reminders, editScope, updatedSubtasks)
                        onDismiss()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EventEditorSheet(
    existing: EventEntity?,
    use24HourClock: Boolean,
    onLoadReminders: suspend (String, String) -> Set<Int>,
    onDismiss: () -> Unit,
    onSave: (EventEntity, Set<Int>) -> Unit
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by rememberSaveable(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var description by rememberSaveable(existing?.id) { mutableStateOf(existing?.description.orEmpty()) }
    var category by rememberSaveable(existing?.id) { mutableStateOf(existing?.category.orEmpty()) }
    var day by rememberSaveable(existing?.id) { mutableStateOf(existing?.startEpochDay ?: LocalDate.now().toEpochDay()) }
    var minute by rememberSaveable(existing?.id) { mutableStateOf(existing?.startMinuteOfDay ?: 10 * 60) }
    var allDay by rememberSaveable(existing?.id) { mutableStateOf(existing?.isAllDay ?: false) }
    var reminders by remember(existing?.id) { mutableStateOf(emptySet<Int>()) }
    var loaded by remember(existing?.id) { mutableStateOf(existing == null) }
    var validation by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(existing?.id) {
        if (existing != null) reminders = onLoadReminders("EVENT", existing.id)
        loaded = true
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp).padding(bottom = 25.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SheetHeading(if (existing == null) "New event" else "Edit event", "A plan, appointment or personal commitment.", onDismiss)
            OutlinedTextField(title, { title = it; validation = null }, Modifier.fillMaxWidth(), label = { Text("Event title") }, singleLine = true, shape = RoundedCornerShape(16.dp))
            OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("Details (optional)") }, minLines = 2, maxLines = 4, shape = RoundedCornerShape(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateChooser(day, { if (it != null) day = it }, "Date", Modifier.weight(1f))
                if (!allDay) TimeChooser(minute, use24HourClock, { if (it != null) minute = it }, "Start time", Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Checkbox(allDay, { allDay = it }, modifier = Modifier.size(48.dp))
                Text("All day", style = MaterialTheme.typography.bodyMedium)
            }
            OutlinedTextField(category, { category = it }, Modifier.fillMaxWidth(), label = { Text("Category") }, placeholder = { Text("Personal, work, fitness…") }, singleLine = true, shape = RoundedCornerShape(16.dp))
            ReminderOffsetChoices(reminders) { offset -> reminders = if (offset in reminders) reminders - offset else reminders + offset }
            validation?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            PrimaryAction("Save event", Modifier.fillMaxWidth(), enabled = loaded) {
                if (title.isBlank()) validation = "Add an event title to continue."
                else {
                    onSave(
                        (existing ?: EventEntity(title = title.trim(), startEpochDay = day)).copy(
                            title = title.trim(), description = description.trim(), startEpochDay = day,
                            startMinuteOfDay = if (allDay) null else minute, isAllDay = allDay,
                            category = category.trim(), updatedAtMillis = System.currentTimeMillis()
                        ), reminders
                    )
                    onDismiss()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeadlineEditorSheet(
    existing: DeadlineEntity?,
    use24HourClock: Boolean,
    onLoadReminders: suspend (String, String) -> Set<Int>,
    onDismiss: () -> Unit,
    onSave: (DeadlineEntity, Set<Int>) -> Unit
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by rememberSaveable(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var description by rememberSaveable(existing?.id) { mutableStateOf(existing?.description.orEmpty()) }
    var category by rememberSaveable(existing?.id) { mutableStateOf(existing?.category.orEmpty()) }
    var day by rememberSaveable(existing?.id) { mutableStateOf(existing?.dueEpochDay ?: LocalDate.now().toEpochDay()) }
    var minute by rememberSaveable(existing?.id) { mutableStateOf(existing?.dueMinuteOfDay ?: 23 * 60 + 59) }
    var reminders by remember(existing?.id) { mutableStateOf(emptySet<Int>()) }
    var loaded by remember(existing?.id) { mutableStateOf(existing == null) }
    var validation by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(existing?.id) {
        if (existing != null) reminders = onLoadReminders("DEADLINE", existing.id)
        loaded = true
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp).padding(bottom = 25.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SheetHeading(if (existing == null) "New deadline" else "Edit deadline", "Keep the due date visible and the reminders useful.", onDismiss)
            OutlinedTextField(title, { title = it; validation = null }, Modifier.fillMaxWidth(), label = { Text("What is due?") }, singleLine = true, shape = RoundedCornerShape(16.dp))
            OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("Details (optional)") }, minLines = 2, maxLines = 4, shape = RoundedCornerShape(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateChooser(day, { if (it != null) day = it }, "Due date", Modifier.weight(1f))
                TimeChooser(minute, use24HourClock, { if (it != null) minute = it }, "Due time", Modifier.weight(1f))
            }
            OutlinedTextField(category, { category = it }, Modifier.fillMaxWidth(), label = { Text("Course / project / category") }, singleLine = true, shape = RoundedCornerShape(16.dp))
            Text("Reminder schedule", style = MaterialTheme.typography.labelLarge)
            ReminderOffsetChoices(reminders) { offset -> reminders = if (offset in reminders) reminders - offset else reminders + offset }
            validation?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            PrimaryAction("Save deadline", Modifier.fillMaxWidth(), enabled = loaded) {
                if (title.isBlank()) validation = "Add a deadline title to continue."
                else {
                    onSave(
                        (existing ?: DeadlineEntity(title = title.trim(), dueEpochDay = day)).copy(
                            title = title.trim(), description = description.trim(), dueEpochDay = day,
                            dueMinuteOfDay = minute, category = category.trim(), updatedAtMillis = System.currentTimeMillis()
                        ), reminders
                    )
                    onDismiss()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NoteEditorSheet(existing: NoteEntity?, onDismiss: () -> Unit, onSave: (NoteEntity) -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by rememberSaveable(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var content by rememberSaveable(existing?.id) { mutableStateOf(existing?.content.orEmpty()) }
    var pinned by rememberSaveable(existing?.id) { mutableStateOf(existing?.isPinned ?: false) }
    var validation by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp).padding(bottom = 26.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SheetHeading(if (existing == null) "New note" else "Edit note", "A quick place to keep what matters.", onDismiss)
            OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Title (optional)") }, singleLine = true, shape = RoundedCornerShape(16.dp))
            OutlinedTextField(content, { content = it; validation = null }, Modifier.fillMaxWidth().height(190.dp), label = { Text("Write a note") }, shape = RoundedCornerShape(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Checkbox(pinned, { pinned = it }, modifier = Modifier.size(48.dp))
                Text("Pin this note", style = MaterialTheme.typography.bodyMedium)
            }
            validation?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            PrimaryAction("Save note", Modifier.fillMaxWidth()) {
                if (title.isBlank() && content.isBlank()) validation = "Add a title or a few words first."
                else {
                    val id = existing?.id ?: UUID.randomUUID().toString()
                    val now = System.currentTimeMillis()
                    onSave((existing ?: NoteEntity(id = id)).copy(id = id, title = title.trim(), content = content, isPinned = pinned, updatedAtMillis = now))
                    onDismiss()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GoalEditorSheet(existing: GoalEntity?, onDismiss: () -> Unit, onSave: (GoalEntity) -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by rememberSaveable(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var description by rememberSaveable(existing?.id) { mutableStateOf(existing?.description.orEmpty()) }
    var target by rememberSaveable(existing?.id) { mutableStateOf(existing?.targetEpochDay) }
    var progress by rememberSaveable(existing?.id) { mutableStateOf((existing?.progress ?: 0).toFloat()) }
    var validation by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp).padding(bottom = 25.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SheetHeading(if (existing == null) "New goal" else "Goal progress", "A meaningful outcome can be more than a number.", onDismiss)
            OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Goal title") }, singleLine = true, shape = RoundedCornerShape(16.dp))
            OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("What does this mean to you?") }, minLines = 2, maxLines = 4, shape = RoundedCornerShape(16.dp))
            DateChooser(target, { target = it }, "Target date (optional)", Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Progress", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                Text("${progress.toInt()}%", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            Slider(value = progress, onValueChange = { progress = it }, valueRange = 0f..100f, steps = 19)
            validation?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            PrimaryAction("Save goal", Modifier.fillMaxWidth()) {
                if (title.isBlank()) validation = "Add a goal title to continue."
                else {
                    val now = System.currentTimeMillis()
                    val id = existing?.id ?: UUID.randomUUID().toString()
                    onSave((existing ?: GoalEntity(id = id, title = title.trim(), startEpochDay = LocalDate.now().toEpochDay())).copy(
                        id = id, title = title.trim(), description = description.trim(), targetEpochDay = target,
                        progress = progress.toInt(), updatedAtMillis = now
                    ))
                    onDismiss()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProjectEditorSheet(
    existing: ProjectEntity?,
    goals: List<GoalEntity>,
    onLoadMilestones: (suspend (String) -> List<MilestoneEntity>)? = null,
    onDismiss: () -> Unit,
    onSave: (ProjectEntity, List<MilestoneEntity>) -> Unit
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by rememberSaveable(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var description by rememberSaveable(existing?.id) { mutableStateOf(existing?.description.orEmpty()) }
    var target by rememberSaveable(existing?.id) { mutableStateOf(existing?.targetEpochDay) }
    var goalId by rememberSaveable(existing?.id) { mutableStateOf(existing?.goalId) }
    var milestones by remember(existing?.id) { mutableStateOf<List<MilestoneEntity>>(emptyList()) }
    var newMilestoneTitle by rememberSaveable(existing?.id) { mutableStateOf("") }
    var validation by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(existing?.id) {
        existing?.id?.let { id ->
            onLoadMilestones?.let { milestones = it(id) }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp).padding(bottom = 25.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SheetHeading(if (existing == null) "New project" else "Edit project", "A simple home for a multi-step plan.", onDismiss)
            OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Project title") }, singleLine = true, shape = RoundedCornerShape(16.dp))
            OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("Description (optional)") }, minLines = 2, maxLines = 4, shape = RoundedCornerShape(16.dp))
            DateChooser(target, { target = it }, "Target date (optional)", Modifier.fillMaxWidth())
            AssociationDropdown("Linked goal", goalId, goals.map { it.id to it.title }, { goalId = it }, Modifier.fillMaxWidth())

            Text("Milestones", style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newMilestoneTitle,
                    onValueChange = { newMilestoneTitle = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Add a milestone") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (newMilestoneTitle.isNotBlank()) {
                            milestones = milestones + MilestoneEntity(
                                projectId = existing?.id,
                                title = newMilestoneTitle.trim(),
                                sortOrder = milestones.size
                            )
                            newMilestoneTitle = ""
                        }
                    },
                    modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add milestone", tint = MaterialTheme.colorScheme.primary)
                }
            }
            if (milestones.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    milestones.forEachIndexed { index, m ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)).padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = m.isCompleted,
                                onCheckedChange = { checked ->
                                    milestones = milestones.toMutableList().also {
                                        it[index] = m.copy(isCompleted = checked, completedAtMillis = if (checked) System.currentTimeMillis() else null)
                                    }
                                }
                            )
                            Text(
                                m.title,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (m.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = { milestones = milestones.filterIndexed { i, _ -> i != index } },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            validation?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            PrimaryAction("Save project", Modifier.fillMaxWidth()) {
                if (title.isBlank()) validation = "Add a project title to continue."
                else {
                    val id = existing?.id ?: UUID.randomUUID().toString()
                    val now = System.currentTimeMillis()
                    val project = (existing ?: ProjectEntity(id = id, title = title.trim())).copy(id = id, title = title.trim(), description = description.trim(), targetEpochDay = target, goalId = goalId, updatedAtMillis = now)
                    val updatedMilestones = milestones.mapIndexed { idx, item -> item.copy(projectId = id, sortOrder = idx) }
                    onSave(project, updatedMilestones)
                    onDismiss()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CourseEditorSheet(
    existing: CourseEntity?,
    onLoadModules: (suspend (String) -> List<CourseModuleEntity>)? = null,
    onDismiss: () -> Unit,
    onSave: (CourseEntity, List<CourseModuleEntity>) -> Unit
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by rememberSaveable(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var platform by rememberSaveable(existing?.id) { mutableStateOf(existing?.platform.orEmpty()) }
    var instructor by rememberSaveable(existing?.id) { mutableStateOf(existing?.instructor.orEmpty()) }
    var category by rememberSaveable(existing?.id) { mutableStateOf(existing?.category.orEmpty()) }
    var target by rememberSaveable(existing?.id) { mutableStateOf(existing?.targetEpochDay) }
    var url by rememberSaveable(existing?.id) { mutableStateOf(existing?.referenceUrl.orEmpty()) }
    var progress by rememberSaveable(existing?.id) { mutableStateOf((existing?.progress ?: 0).toFloat()) }
    var modules by remember(existing?.id) { mutableStateOf<List<CourseModuleEntity>>(emptyList()) }
    var newModuleTitle by rememberSaveable(existing?.id) { mutableStateOf("") }
    var validation by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(existing?.id) {
        existing?.id?.let { id ->
            onLoadModules?.let { modules = it(id) }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp).padding(bottom = 25.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SheetHeading(if (existing == null) "Add learning" else "Edit learning", "Online courses, certifications and self-study.", onDismiss)
            OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Course or program") }, singleLine = true, shape = RoundedCornerShape(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(platform, { platform = it }, Modifier.weight(1f), label = { Text("Platform") }, singleLine = true, shape = RoundedCornerShape(16.dp))
                OutlinedTextField(instructor, { instructor = it }, Modifier.weight(1f), label = { Text("Instructor") }, singleLine = true, shape = RoundedCornerShape(16.dp))
            }
            OutlinedTextField(category, { category = it }, Modifier.fillMaxWidth(), label = { Text("Category") }, singleLine = true, shape = RoundedCornerShape(16.dp))
            DateChooser(target, { target = it }, "Target completion (optional)", Modifier.fillMaxWidth())
            OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth(), label = { Text("Reference URL (optional)") }, placeholder = { Text("Stored locally; never opened automatically") }, singleLine = true, shape = RoundedCornerShape(16.dp))

            Text("Syllabus & Modules", style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newModuleTitle,
                    onValueChange = { newModuleTitle = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Add module or topic") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (newModuleTitle.isNotBlank()) {
                            modules = modules + CourseModuleEntity(
                                courseId = existing?.id ?: "",
                                title = newModuleTitle.trim(),
                                sortOrder = modules.size
                            )
                            newModuleTitle = ""
                            if (modules.isNotEmpty()) {
                                progress = ((modules.count { it.isCompleted } * 100) / modules.size).toFloat()
                            }
                        }
                    },
                    modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add module", tint = MaterialTheme.colorScheme.primary)
                }
            }
            if (modules.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    modules.forEachIndexed { index, mod ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)).padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = mod.isCompleted,
                                onCheckedChange = { checked ->
                                    modules = modules.toMutableList().also {
                                        it[index] = mod.copy(isCompleted = checked, completedAtMillis = if (checked) System.currentTimeMillis() else null)
                                    }
                                    val completed = modules.count { it.isCompleted }
                                    progress = ((completed * 100) / modules.size).toFloat()
                                }
                            )
                            Text(
                                mod.title,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (mod.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = {
                                    modules = modules.filterIndexed { i, _ -> i != index }
                                    if (modules.isNotEmpty()) {
                                        progress = ((modules.count { it.isCompleted } * 100) / modules.size).toFloat()
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Progress", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                Text("${progress.toInt()}%", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            Slider(value = progress, onValueChange = { progress = it }, valueRange = 0f..100f, steps = 19)
            validation?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            PrimaryAction("Save course", Modifier.fillMaxWidth()) {
                if (title.isBlank()) validation = "Add a course title to continue."
                else {
                    val id = existing?.id ?: UUID.randomUUID().toString()
                    val now = System.currentTimeMillis()
                    val course = (existing ?: CourseEntity(id = id, title = title.trim())).copy(
                        id = id, title = title.trim(), platform = platform.trim(), instructor = instructor.trim(), category = category.trim(),
                        targetEpochDay = target, referenceUrl = url.trim(), progress = progress.toInt(), updatedAtMillis = now
                    )
                    val updatedModules = modules.mapIndexed { idx, item -> item.copy(courseId = id, sortOrder = idx) }
                    onSave(course, updatedModules)
                    onDismiss()
                }
            }
        }
    }
}

@Composable
private fun SheetHeading(title: String, subtitle: String, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        androidx.compose.material3.IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Close") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateChooser(epochDay: Long?, onChange: (Long?) -> Unit, label: String, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val selectedMillis = epochDay?.let { LocalDate.ofEpochDay(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
    val state = rememberDatePickerState(initialSelectedDateMillis = selectedMillis ?: LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    OutlinedButton(onClick = { open = true }, modifier = modifier.height(52.dp), shape = RoundedCornerShape(15.dp), contentPadding = PaddingValues(horizontal = 11.dp)) {
        Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(6.dp))
        Text(epochDay?.let(::formatDay) ?: label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
    }
    if (open) {
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onChange(date.toEpochDay())
                    }
                    open = false
                }) { Text("Select") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { onChange(null); open = false }) { Text("Clear") }
                    TextButton(onClick = { open = false }) { Text("Cancel") }
                }
            }
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun TimeChooser(minute: Int?, use24HourClock: Boolean, onChange: (Int?) -> Unit, label: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            val initial = minute ?: 9 * 60
            TimePickerDialog(context, { _, hour, min -> onChange(hour * 60 + min) }, initial / 60, initial % 60, use24HourClock).show()
        },
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(15.dp),
        contentPadding = PaddingValues(horizontal = 11.dp)
    ) {
        Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(6.dp))
        Text(minute?.let { formatTime(it, use24HourClock) } ?: label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun AssociationDropdown(label: String, selectedId: String?, options: List<Pair<String, String>>, onSelect: (String?) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp), contentPadding = PaddingValues(horizontal = 11.dp)) {
            Text(options.firstOrNull { it.first == selectedId }?.second ?: label, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            androidx.compose.material3.DropdownMenuItem(text = { Text("None") }, onClick = { onSelect(null); expanded = false })
            options.forEach { (id, title) ->
                androidx.compose.material3.DropdownMenuItem(text = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) }, onClick = { onSelect(id); expanded = false })
            }
        }
    }
}

@Composable
private fun WeekdaySelector(mask: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        DayOfWeek.entries.forEach { day ->
            val bit = 1 shl (day.value - 1)
            val selected = (mask and bit) != 0
            FilterChip(selected = selected, onClick = { onChange(if (selected) mask and bit.inv() else mask or bit) }, label = { Text(day.name.take(2)) })
        }
    }
}

@Composable
private fun ReminderOffsetChoices(selected: Set<Int>, onToggle: (Int) -> Unit) {
    val choices = listOf(0 to "At time", 5 to "5 min", 15 to "15 min", 30 to "30 min", 60 to "1 hour", 120 to "2 hours", 1_440 to "1 day", 4_320 to "3 days", 10_080 to "7 days")
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        choices.chunked(3).forEach { rowChoices ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                rowChoices.forEach { (offset, label) ->
                    FilterChip(selected = offset in selected, onClick = { onToggle(offset) }, label = { Text(label, fontSize = 11.sp) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuickCaptureSheet(
    use24HourClock: Boolean,
    onDismiss: () -> Unit,
    onSaveTask: (TaskEntity) -> Unit
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by rememberSaveable { mutableStateOf("") }
    val parsed = remember(text) { QuickCaptureParser.parse(text) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding()
                .padding(horizontal = 20.dp).padding(bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SheetHeading("Quick capture", "Type naturally. Daymark extracts date and time.", onDismiss)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Submit database assignment Friday 7 PM #study !high") },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(16.dp)
            )

            if (parsed.title.isNotBlank()) {
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("PREVIEW", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                        Text(parsed.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            parsed.dueEpochDay?.let { day ->
                                TinyPill(formatDay(day), tint = MaterialTheme.colorScheme.primary)
                            }
                            parsed.dueMinuteOfDay?.let { minute ->
                                TinyPill(formatTime(minute, use24HourClock), tint = MaterialTheme.colorScheme.secondary)
                            }
                            if (parsed.category.isNotBlank()) {
                                TinyPill(parsed.category, tint = MaterialTheme.colorScheme.tertiary)
                            }
                            if (parsed.priority != "NORMAL") {
                                TinyPill(parsed.priority, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            PrimaryAction("Save to Daymark", Modifier.fillMaxWidth(), enabled = parsed.title.isNotBlank()) {
                val now = System.currentTimeMillis()
                val task = TaskEntity(
                    title = parsed.title,
                    dueEpochDay = parsed.dueEpochDay,
                    dueMinuteOfDay = parsed.dueMinuteOfDay,
                    priority = parsed.priority,
                    category = parsed.category,
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
                onSaveTask(task)
                onDismiss()
            }
        }
    }
}

