package com.daymark.app.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daymark.app.data.DashboardWidgets
import com.daymark.app.data.DeadlineEntity
import com.daymark.app.data.EventEntity
import com.daymark.app.data.GoalEntity
import com.daymark.app.data.NoteEntity
import com.daymark.app.data.ProjectEntity
import com.daymark.app.data.SearchResult
import com.daymark.app.data.TaskEntity
import com.daymark.app.data.UserPreferencesEntity
import com.daymark.app.data.CourseEntity
import com.daymark.app.ui.MoreDestination
import com.daymark.app.ui.screens.CoursesScreen
import com.daymark.app.ui.screens.DeadlinesScreen
import com.daymark.app.ui.screens.GoalsScreen
import com.daymark.app.ui.screens.HomeScreen
import com.daymark.app.ui.screens.InsightsScreen
import com.daymark.app.ui.screens.MoreHubScreen
import com.daymark.app.ui.screens.NotesScreen
import com.daymark.app.ui.screens.ProjectsScreen
import com.daymark.app.ui.screens.ScheduleScreen
import com.daymark.app.ui.screens.SettingsScreen
import com.daymark.app.ui.screens.TasksScreen
import com.daymark.app.ui.sheets.AddActionSheet
import com.daymark.app.ui.sheets.CreateKind
import com.daymark.app.ui.sheets.CourseEditorSheet
import com.daymark.app.ui.sheets.DeadlineEditorSheet
import com.daymark.app.ui.sheets.EventEditorSheet
import com.daymark.app.ui.sheets.GoalEditorSheet
import com.daymark.app.ui.sheets.NoteEditorSheet
import com.daymark.app.ui.sheets.ProjectEditorSheet
import com.daymark.app.ui.sheets.QuickCaptureSheet
import com.daymark.app.ui.sheets.SearchSheet
import com.daymark.app.ui.sheets.TaskEditorSheet
import com.daymark.app.ui.theme.DaymarkTheme
import kotlinx.coroutines.flow.MutableStateFlow

private enum class MainTab(val label: String) { HOME("Home"), SCHEDULE("Schedule"), TASKS("Tasks"), NOTES("Notes"), MORE("More") }

@Composable
fun DaymarkApp(viewModel: DaymarkViewModel, notificationTarget: MutableStateFlow<Pair<String, String>?>) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val deadlines by viewModel.deadlines.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val completions by viewModel.completions.collectAsStateWithLifecycle()
    val goalActivities by viewModel.goalActivities.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val sounds by viewModel.sounds.collectAsStateWithLifecycle()
    val recurrenceRules by viewModel.recurrenceRules.collectAsStateWithLifecycle()
    val target by notificationTarget.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTabName by rememberSaveable { mutableStateOf(MainTab.HOME.name) }
    var moreDestinationName by rememberSaveable { mutableStateOf(MoreDestination.HUB.name) }
    var addSheetOpen by remember { mutableStateOf(false) }
    var searchSheetOpen by remember { mutableStateOf(false) }
    var editorKind by remember { mutableStateOf<CreateKind?>(null) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var editingEvent by remember { mutableStateOf<EventEntity?>(null) }
    var editingDeadline by remember { mutableStateOf<DeadlineEntity?>(null) }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var editingGoal by remember { mutableStateOf<GoalEntity?>(null) }
    var editingProject by remember { mutableStateOf<ProjectEntity?>(null) }
    var editingCourse by remember { mutableStateOf<CourseEntity?>(null) }
    val selectedTab = MainTab.entries.firstOrNull { it.name == selectedTabName } ?: MainTab.HOME
    val moreDestination = MoreDestination.entries.firstOrNull { it.name == moreDestinationName } ?: MoreDestination.HUB

    var notificationEnabled by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    var exactAlarmsAllowed by remember { mutableStateOf(exactAlarmPermission(context)) }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
                exactAlarmsAllowed = exactAlarmPermission(context)
                viewModel.refreshReminders()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message -> snackbarHostState.showSnackbar(message) }
    }
    LaunchedEffect(target) {
        target?.first?.let { type ->
            selectedTabName = when (type) {
                "TASK" -> MainTab.TASKS.name
                "EVENT" -> MainTab.SCHEDULE.name
                "DEADLINE" -> MainTab.MORE.name.also { moreDestinationName = MoreDestination.DEADLINES.name }
                else -> MainTab.HOME.name
            }
        }
    }
    LaunchedEffect(target, tasks, events, deadlines) {
        val request = target ?: return@LaunchedEffect
        when (request.first) {
            "TASK" -> tasks.firstOrNull { it.id == request.second }?.let { editingTask = it; editorKind = CreateKind.TASK; notificationTarget.value = null }
            "EVENT" -> events.firstOrNull { it.id == request.second }?.let { editingEvent = it; editorKind = CreateKind.EVENT; notificationTarget.value = null }
            "DEADLINE" -> deadlines.firstOrNull { it.id == request.second }?.let { editingDeadline = it; editorKind = CreateKind.DEADLINE; notificationTarget.value = null }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationEnabled = granted || NotificationManagerCompat.from(context).areNotificationsEnabled()
        viewModel.refreshReminders()
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) viewModel.exportBackup(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) viewModel.restoreBackup(uri)
    }
    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) viewModel.importSound(uri)
    }

    DaymarkTheme(preferences.themeKey) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { addSheetOpen = true },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("Add") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(17.dp)
                )
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    MainTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = {
                                selectedTabName = tab.name
                                if (tab == MainTab.MORE) moreDestinationName = MoreDestination.HUB.name
                            },
                            icon = {
                                val icon = when (tab) {
                                    MainTab.HOME -> Icons.Rounded.Home
                                    MainTab.SCHEDULE -> Icons.Rounded.CalendarMonth
                                    MainTab.TASKS -> Icons.Rounded.Checklist
                                    MainTab.NOTES -> Icons.Rounded.Notes
                                    MainTab.MORE -> Icons.Rounded.MoreHoriz
                                }
                                Icon(icon, contentDescription = tab.label)
                            },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = .10f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Crossfade(targetState = selectedTab, modifier = Modifier.fillMaxSize().padding(padding), label = "Daymark section") { tab ->
                when (tab) {
                    MainTab.HOME -> HomeScreen(
                        tasks = tasks, events = events, deadlines = deadlines, goals = goals,
                        goalActivities = goalActivities, notes = notes, courses = courses,
                        dashboard = dashboard, preferences = preferences,
                        onSearch = { searchSheetOpen = true },
                        onOpenTasks = { selectedTabName = MainTab.TASKS.name },
                        onOpenSchedule = { selectedTabName = MainTab.SCHEDULE.name },
                        onOpenGoals = { selectedTabName = MainTab.MORE.name; moreDestinationName = MoreDestination.GOALS.name },
                        onOpenCourses = { selectedTabName = MainTab.MORE.name; moreDestinationName = MoreDestination.COURSES.name },
                        onOpenNotes = { selectedTabName = MainTab.NOTES.name },
                        onToggleTask = { task -> if (task.status == "COMPLETED") viewModel.uncompleteTask(task.id) else viewModel.completeTask(task.id) },
                        onOpenTask = { editingTask = it; editorKind = CreateKind.TASK },
                        onOpenEvent = { editingEvent = it; editorKind = CreateKind.EVENT },
                        onOpenDeadline = { selectedTabName = MainTab.MORE.name; moreDestinationName = MoreDestination.DEADLINES.name; editingDeadline = it; editorKind = CreateKind.DEADLINE },
                        onCreateTask = { editorKind = CreateKind.TASK }
                    )
                    MainTab.SCHEDULE -> ScheduleScreen(
                        tasks = tasks, events = events, deadlines = deadlines, preferences = preferences,
                        recurrenceRules = recurrenceRules,
                        onSearch = { searchSheetOpen = true },
                        onOpenTask = { editingTask = it; editorKind = CreateKind.TASK },
                        onOpenEvent = { editingEvent = it; editorKind = CreateKind.EVENT },
                        onOpenDeadline = { selectedTabName = MainTab.MORE.name; moreDestinationName = MoreDestination.DEADLINES.name; editingDeadline = it; editorKind = CreateKind.DEADLINE }
                    )
                    MainTab.TASKS -> TasksScreen(
                        tasks = tasks, preferences = preferences,
                        onSearch = { searchSheetOpen = true },
                        onOpenTask = { editingTask = it; editorKind = CreateKind.TASK },
                        onToggleTask = { task -> if (task.status == "COMPLETED") viewModel.uncompleteTask(task.id) else viewModel.completeTask(task.id) },
                        onCreateTask = { editorKind = CreateKind.TASK }
                    )
                    MainTab.NOTES -> NotesScreen(
                        notes = notes,
                        onSearch = { searchSheetOpen = true },
                        onCreate = { editorKind = CreateKind.NOTE },
                        onOpen = { editingNote = it; editorKind = CreateKind.NOTE }
                    )
                    MainTab.MORE -> when (moreDestination) {
                        MoreDestination.HUB -> MoreHubScreen { moreDestinationName = it.name }
                        MoreDestination.GOALS -> GoalsScreen(
                            goals, goalActivities,
                            onBack = { moreDestinationName = MoreDestination.HUB.name },
                            onAdd = { editingGoal = null; editorKind = CreateKind.GOAL },
                            onOpen = { editingGoal = it; editorKind = CreateKind.GOAL }
                        )
                        MoreDestination.PROJECTS -> ProjectsScreen(
                            projects,
                            onBack = { moreDestinationName = MoreDestination.HUB.name },
                            onAdd = { editingProject = null; editorKind = CreateKind.PROJECT },
                            onOpen = { editingProject = it; editorKind = CreateKind.PROJECT }
                        )
                        MoreDestination.COURSES -> CoursesScreen(
                            courses,
                            onBack = { moreDestinationName = MoreDestination.HUB.name },
                            onAdd = { editingCourse = null; editorKind = CreateKind.COURSE },
                            onOpen = { editingCourse = it; editorKind = CreateKind.COURSE }
                        )
                        MoreDestination.DEADLINES -> DeadlinesScreen(
                            deadlines,
                            onBack = { moreDestinationName = MoreDestination.HUB.name },
                            onAdd = { editingDeadline = null; editorKind = CreateKind.DEADLINE },
                            onOpen = { editingDeadline = it; editorKind = CreateKind.DEADLINE },
                            onComplete = { viewModel.completeDeadline(it.id) }
                        )
                        MoreDestination.INSIGHTS -> InsightsScreen(tasks, completions, goals, goalActivities) { moreDestinationName = MoreDestination.HUB.name }
                        MoreDestination.SETTINGS -> SettingsScreen(
                            preferences = preferences,
                            dashboard = dashboard,
                            sounds = sounds,
                            notificationsEnabled = notificationEnabled,
                            exactAlarmsAllowed = exactAlarmsAllowed,
                            onBack = { moreDestinationName = MoreDestination.HUB.name },
                            onSaveName = { viewModel.savePreferences(preferences.copy(displayName = it)) },
                            onTheme = { viewModel.savePreferences(preferences.copy(themeKey = it)) },
                            onSound = { viewModel.selectSound(it) },
                            onTestSound = { viewModel.sendTestNotification() },
                            onImportSound = { audioLauncher.launch(arrayOf("audio/*")) },
                            onRequestNotificationPermission = {
                                if (Build.VERSION.SDK_INT >= 33) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                else openAppNotificationSettings(context)
                            },
                            onOpenNotificationSettings = { openAppNotificationSettings(context) },
                            onOpenExactAlarmSettings = { openExactAlarmSettings(context) },
                            onDashboardChange = { viewModel.updateDashboard(it) },
                            onMoveWidget = { key, delta -> viewModel.moveDashboardWidget(key, delta) },
                            onExportBackup = { exportLauncher.launch("Daymark-backup-v1.json") },
                            onImportBackup = { importLauncher.launch(arrayOf("application/json", "text/json", "application/octet-stream")) },
                            onResetData = { viewModel.resetApplicationData() }
                        )
                    }
                }
            }
        }
    }

    if (addSheetOpen) {
        AddActionSheet(onDismiss = { addSheetOpen = false }) { kind ->
            addSheetOpen = false
            editingTask = null
            editingEvent = null
            editingDeadline = null
            editingNote = null
            editingGoal = null
            editingProject = null
            editingCourse = null
            editorKind = kind
        }
    }

    val closeEditor = {
        editorKind = null
        editingTask = null
        editingEvent = null
        editingDeadline = null
        editingNote = null
        editingGoal = null
        editingProject = null
        editingCourse = null
    }
    when (editorKind) {
        CreateKind.TASK, CreateKind.REMINDER, CreateKind.FITNESS -> TaskEditorSheet(
            existing = editingTask,
            forceReminder = editorKind == CreateKind.REMINDER,
            initialCategory = if (editorKind == CreateKind.FITNESS) "Fitness" else "",
            use24HourClock = preferences.use24HourClock,
            projects = projects, goals = goals, courses = courses,
            onLoadReminders = viewModel::reminderOffsets,
            onLoadRule = viewModel::recurrenceRule,
            onLoadSubtasks = viewModel::getSubtasks,
            onDismiss = closeEditor,
            onSave = { task, frequency, interval, mask, endDay, count, offsets, scope, subtasks ->
                viewModel.saveTask(task, frequency, interval, mask, endDay, count, offsets, scope)
                viewModel.saveSubtasksForTask(task.id, subtasks)
            }
        )
        CreateKind.QUICK_CAPTURE -> QuickCaptureSheet(
            use24HourClock = preferences.use24HourClock,
            onDismiss = closeEditor,
            onSaveTask = { task ->
                viewModel.saveTask(
                    task = task,
                    frequency = "NONE",
                    interval = 1,
                    weekdaysMask = 0,
                    endEpochDay = null,
                    occurrenceLimit = null,
                    reminderOffsets = emptySet(),
                    editScope = "THIS_OCCURRENCE"
                )
            }
        )
        CreateKind.EVENT -> EventEditorSheet(
            existing = editingEvent,
            use24HourClock = preferences.use24HourClock,
            onLoadReminders = viewModel::reminderOffsets,
            onDismiss = closeEditor,
            onSave = { event, reminders -> viewModel.saveEvent(event, reminders) }
        )
        CreateKind.DEADLINE -> DeadlineEditorSheet(
            existing = editingDeadline,
            use24HourClock = preferences.use24HourClock,
            onLoadReminders = viewModel::reminderOffsets,
            onDismiss = closeEditor,
            onSave = { deadline, reminders -> viewModel.saveDeadline(deadline, reminders) }
        )
        CreateKind.NOTE -> NoteEditorSheet(existing = editingNote, onDismiss = closeEditor, onSave = { viewModel.saveNote(it) })
        CreateKind.GOAL -> GoalEditorSheet(existing = editingGoal, onDismiss = closeEditor, onSave = { viewModel.saveGoal(it) })
        CreateKind.PROJECT -> ProjectEditorSheet(
            existing = editingProject,
            goals = goals,
            onLoadMilestones = viewModel::getMilestonesForProject,
            onDismiss = closeEditor,
            onSave = { project, milestones ->
                viewModel.saveProject(project)
                viewModel.saveMilestonesForProject(project.id, milestones)
            }
        )
        CreateKind.COURSE -> CourseEditorSheet(
            existing = editingCourse,
            onLoadModules = viewModel::getCourseModules,
            onDismiss = closeEditor,
            onSave = { course, modules ->
                viewModel.saveCourse(course)
                viewModel.saveCourseModulesForCourse(course.id, modules)
            }
        )
        null -> Unit
    }

    if (searchSheetOpen) {
        SearchSheet(
            onDismiss = { searchSheetOpen = false },
            onSearch = viewModel::search,
            onOpenResult = { result ->
                searchSheetOpen = false
                routeSearchResult(result, tasks, events, deadlines, notes, goals, projects, courses) { tab, page, kind, record ->
                    selectedTabName = tab.name
                    moreDestinationName = page.name
                    when (kind) {
                        CreateKind.TASK -> editingTask = record as? TaskEntity
                        CreateKind.EVENT -> editingEvent = record as? EventEntity
                        CreateKind.DEADLINE -> editingDeadline = record as? DeadlineEntity
                        CreateKind.NOTE -> editingNote = record as? NoteEntity
                        CreateKind.GOAL -> editingGoal = record as? GoalEntity
                        CreateKind.PROJECT -> editingProject = record as? ProjectEntity
                        CreateKind.COURSE -> editingCourse = record as? CourseEntity
                        else -> Unit
                    }
                    editorKind = kind.takeIf { record != null }
                }
            }
        )
    }
}

private fun routeSearchResult(
    result: SearchResult,
    tasks: List<TaskEntity>,
    events: List<EventEntity>,
    deadlines: List<DeadlineEntity>,
    notes: List<NoteEntity>,
    goals: List<GoalEntity>,
    projects: List<ProjectEntity>,
    courses: List<CourseEntity>,
    open: (MainTab, MoreDestination, CreateKind, Any?) -> Unit
) {
    when (result.type) {
        "TASK" -> open(MainTab.TASKS, MoreDestination.HUB, CreateKind.TASK, tasks.firstOrNull { it.id == result.id })
        "EVENT" -> open(MainTab.SCHEDULE, MoreDestination.HUB, CreateKind.EVENT, events.firstOrNull { it.id == result.id })
        "DEADLINE" -> open(MainTab.MORE, MoreDestination.DEADLINES, CreateKind.DEADLINE, deadlines.firstOrNull { it.id == result.id })
        "NOTE" -> open(MainTab.NOTES, MoreDestination.HUB, CreateKind.NOTE, notes.firstOrNull { it.id == result.id })
        "GOAL" -> open(MainTab.MORE, MoreDestination.GOALS, CreateKind.GOAL, goals.firstOrNull { it.id == result.id })
        "PROJECT" -> open(MainTab.MORE, MoreDestination.PROJECTS, CreateKind.PROJECT, projects.firstOrNull { it.id == result.id })
        "COURSE" -> open(MainTab.MORE, MoreDestination.COURSES, CreateKind.COURSE, courses.firstOrNull { it.id == result.id })
    }
}

private fun exactAlarmPermission(context: android.content.Context): Boolean = runCatching {
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S || (context.getSystemService(android.content.Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
}.getOrDefault(false)

private fun openAppNotificationSettings(context: android.content.Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}

private fun openExactAlarmSettings(context: android.content.Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}
