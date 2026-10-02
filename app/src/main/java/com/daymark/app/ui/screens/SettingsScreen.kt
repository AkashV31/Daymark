package com.daymark.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daymark.app.data.DashboardConfigurationEntity
import com.daymark.app.data.DashboardWidgets
import com.daymark.app.data.NotificationSoundEntity
import com.daymark.app.data.UserPreferencesEntity
import com.daymark.app.notifications.NotificationSoundCatalog
import com.daymark.app.ui.components.PrimaryAction
import com.daymark.app.ui.components.ScreenHeader
import com.daymark.app.ui.components.SectionHeading
import com.daymark.app.ui.components.SoftCard
import com.daymark.app.ui.components.TinyPill

import androidx.compose.foundation.border
import androidx.compose.material.icons.rounded.Check
import com.daymark.app.ui.theme.allAccents

private data class ModeOption(val id: String, val title: String)
private val modeOptions = listOf(
    ModeOption("LIGHT", "Light"),
    ModeOption("DARK", "Dark"),
    ModeOption("SYSTEM", "System default")
)

@Composable
fun SettingsScreen(
    preferences: UserPreferencesEntity,
    dashboard: List<DashboardConfigurationEntity>,
    sounds: List<NotificationSoundEntity>,
    notificationsEnabled: Boolean,
    exactAlarmsAllowed: Boolean,
    onBack: () -> Unit,
    onSaveName: (String) -> Unit,
    onTheme: (String) -> Unit,
    onAccent: (String) -> Unit,
    onToggle24HourClock: (Boolean) -> Unit,
    onToggleLoudReminders: (Boolean) -> Unit = {},
    onSound: (String) -> Unit,
    onTestSound: () -> Unit,
    onImportSound: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onDashboardChange: (DashboardConfigurationEntity) -> Unit,
    onMoveWidget: (String, Int) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onResetData: () -> Unit
) {
    var displayName by rememberSaveable(preferences.displayName) { mutableStateOf(preferences.displayName) }
    var showNotificationRationale by rememberSaveable { mutableStateOf(false) }
    var showResetConfirmation by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Settings", "Make Daymark feel like yours", onBack = onBack)
        SettingsContent(
            preferences = preferences,
            dashboard = dashboard,
            sounds = sounds,
            notificationsEnabled = notificationsEnabled,
            exactAlarmsAllowed = exactAlarmsAllowed,
            displayName = displayName,
            onDisplayNameChange = { displayName = it },
            onSaveName = { onSaveName(displayName.trim()) },
            onTheme = onTheme,
            onAccent = onAccent,
            onToggle24HourClock = onToggle24HourClock,
            onToggleLoudReminders = onToggleLoudReminders,
            onSound = onSound,
            onTestSound = onTestSound,
            onImportSound = onImportSound,
            onRequestPermission = { showNotificationRationale = true },
            onOpenNotificationSettings = onOpenNotificationSettings,
            onOpenExactAlarmSettings = onOpenExactAlarmSettings,
            onDashboardChange = onDashboardChange,
            onMoveWidget = onMoveWidget,
            onExportBackup = onExportBackup,
            onImportBackup = onImportBackup,
            onReset = { showResetConfirmation = true }
        )
    }

    if (showNotificationRationale) {
        AlertDialog(
            onDismissRequest = { showNotificationRationale = false },
            title = { Text("Allow Daymark reminders?") },
            text = { Text("Daymark uses notifications only for reminders you schedule. Your tasks and reminder times stay on this device.") },
            confirmButton = {
                TextButton(onClick = {
                    showNotificationRationale = false
                    if (notificationsEnabled) onOpenNotificationSettings() else onRequestNotificationPermission()
                }) { Text(if (notificationsEnabled) "Notification settings" else "Continue") }
            },
            dismissButton = { TextButton(onClick = { showNotificationRationale = false }) { Text("Not now") } }
        )
    }
    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Reset Daymark data?") },
            text = { Text("This permanently removes local tasks, notes, courses, goals, reminders and settings. Export a backup first if you may need this information.") },
            confirmButton = {
                TextButton(onClick = { showResetConfirmation = false; onResetData() }) { Text("Reset data", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showResetConfirmation = false }) { Text("Keep my data") } }
        )
    }
}

@Composable
private fun SettingsContent(
    preferences: UserPreferencesEntity,
    dashboard: List<DashboardConfigurationEntity>,
    sounds: List<NotificationSoundEntity>,
    notificationsEnabled: Boolean,
    exactAlarmsAllowed: Boolean,
    displayName: String,
    onDisplayNameChange: (String) -> Unit,
    onSaveName: () -> Unit,
    onTheme: (String) -> Unit,
    onAccent: (String) -> Unit,
    onToggle24HourClock: (Boolean) -> Unit,
    onToggleLoudReminders: (Boolean) -> Unit,
    onSound: (String) -> Unit,
    onTestSound: () -> Unit,
    onImportSound: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onDashboardChange: (DashboardConfigurationEntity) -> Unit,
    onMoveWidget: (String, Int) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onReset: () -> Unit
) {
    val defaultDashboard = DashboardWidgets.defaults
    val widgets = (dashboard.ifEmpty { defaultDashboard }).sortedBy { it.position }
    val builtinSelected = NotificationSoundCatalog.builtIns.any { it.id == preferences.notificationSoundId }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 5.dp).padding(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeading("Appearance")
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.size(10.dp))
                    Column {
                        Text("Theme & colors", style = MaterialTheme.typography.titleMedium)
                        Text("Personalize your appearance and accents.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("Mode", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    modeOptions.forEach { mode ->
                        FilterChip(
                            selected = preferences.themeKey == mode.id,
                            onClick = { onTheme(mode.id) },
                            label = { Text(mode.title) }
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("Accent palette", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    allAccents.forEach { opt ->
                        val isSelected = preferences.accentKey == opt.id
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onAccent(opt.id) }
                                .padding(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(opt.previewColor)
                                    .then(
                                        if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                opt.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("24-hour clock", style = MaterialTheme.typography.bodyLarge)
                        Text("Format times in 24-hour format (e.g. 14:30 instead of 2:30 PM)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = preferences.use24HourClock,
                        onCheckedChange = onToggle24HourClock
                    )
                }

                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                OutlinedTextField(
                    value = displayName,
                    onValueChange = onDisplayNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name for your greeting") },
                    placeholder = { Text("Optional") },
                    singleLine = true,
                    shape = RoundedCornerShape(15.dp)
                )
                TextButton(onClick = onSaveName, modifier = Modifier.align(Alignment.End)) { Text("Save name") }
            }
        }

        SectionHeading("Notifications")
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (notificationsEnabled) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff, contentDescription = null, tint = if (notificationsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    Spacer(Modifier.size(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (notificationsEnabled) "Notifications are enabled" else "Notifications are off", style = MaterialTheme.typography.titleSmall)
                        Text(if (notificationsEnabled) "Scheduled reminders can reach you when Daymark is closed." else "Enable notifications to receive the reminders you schedule.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                OutlinedButton(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)) {
                    Text(if (notificationsEnabled) "Manage notification settings" else "Enable notifications")
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Precise alarm access", style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (exactAlarmsAllowed) "Exact reminder times are available on this device."
                            else "Without access, Android may deliver reminders later to protect battery life.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!exactAlarmsAllowed) {
                        TextButton(onClick = onOpenExactAlarmSettings) { Text("Review") }
                    } else {
                        TinyPill("Available", tint = MaterialTheme.colorScheme.secondary)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Loud reminders", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Play reminders at alarm volume so they're heard.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = preferences.loudReminders,
                        onCheckedChange = onToggleLoudReminders
                    )
                }
            }
        }

        SectionHeading("Reminder sounds")
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Choose a sound", style = MaterialTheme.typography.titleMedium)
                Text("Android saves sound preferences on each notification channel. A channel you changed in system settings may keep its own sound.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                NotificationSoundCatalog.builtIns.forEach { sound ->
                    SoundRow(sound.id, sound.label, preferences.notificationSoundId == sound.id) { onSound(sound.id) }
                }
                sounds.filter { !it.isBuiltIn }.forEach { sound ->
                    SoundRow(sound.soundId, sound.name, preferences.notificationSoundId == sound.soundId) { onSound(sound.soundId) }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onImportSound, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Import audio") }
                    Button(onClick = onTestSound, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp)); Text("Test sound")
                    }
                }
                if (!builtinSelected) Text("Your imported audio is copied into local notification storage on this device.", modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        SectionHeading("Home dashboard")
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 15.dp, vertical = 7.dp)) {
                Text("Show and order the sections you use.", modifier = Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                widgets.forEachIndexed { index, item ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Switch(checked = item.isVisible, onCheckedChange = { onDashboardChange(item.copy(isVisible = it)) })
                        Text(widgetLabel(item.widgetKey), modifier = Modifier.weight(1f).padding(start = 9.dp), style = MaterialTheme.typography.bodyMedium)
                        androidx.compose.material3.IconButton(onClick = { onMoveWidget(item.widgetKey, -1) }, enabled = index > 0) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move ${widgetLabel(item.widgetKey)} up", modifier = Modifier.size(18.dp))
                        }
                        androidx.compose.material3.IconButton(onClick = { onMoveWidget(item.widgetKey, 1) }, enabled = index < widgets.lastIndex) {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move ${widgetLabel(item.widgetKey)} down", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        SectionHeading("Backup & restore")
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.size(10.dp))
                    Column {
                        Text("Your data, under your control", style = MaterialTheme.typography.titleSmall)
                        Text("Create a versioned local backup or restore from a file. Nothing is uploaded.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Button(onClick = onExportBackup, modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp)) { Text("Export backup") }
                    OutlinedButton(onClick = onImportBackup, modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp)) { Text("Import backup") }
                }
            }
        }

        SectionHeading("Data")
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Reset application data", style = MaterialTheme.typography.titleSmall)
                    Text("Permanently clear this device’s Daymark data.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onReset) { Text("Reset", color = MaterialTheme.colorScheme.error) }
            }
        }
        Text("DAYMARK · Offline by design · Version 1.0", modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SoundRow(id: String, title: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onSelect).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        if (selected) TinyPill("Selected")
    }
}

private fun widgetLabel(key: String): String = when (key) {
    DashboardWidgets.TODAY_PROGRESS -> "Today’s progress"
    DashboardWidgets.NEXT_UP -> "Next up"
    DashboardWidgets.TODAY_TASKS -> "Today’s tasks"
    DashboardWidgets.DEADLINES -> "Upcoming deadlines"
    DashboardWidgets.GOALS -> "Goals"
    DashboardWidgets.COURSES -> "Learning"
    DashboardWidgets.RECENT_NOTES -> "Recent notes"
    DashboardWidgets.INSIGHTS -> "Execution insights"
    else -> key.lowercase().replace('_', ' ').replaceFirstChar { it.titlecase() }
}
