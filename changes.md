# Daymark — Release Hardening, Delete, Notifications, Brand, Motion & Themes

This release hardens Daymark for offline personal organisation and the shareholder presentation. It eliminates crashes, fixes silent data-correctness bugs and concurrency races, restores full audibility to reminder notifications with an alpha-knockout status bar icon, introduces ubiquitous deletion with Undo snapshots across all records, restructures navigation with predictive BackHandler, splits theme mode and accent colors with Room migration v2 (offering 10 custom palettes with tinted surfaces and dynamic Material You), introduces a unified brand mark with evenOdd negative space, establishes a lightweight 60fps motion system, hardens release builds with R8 shrinking, and opens an on-device SLM assistant seam—all under a strict 100% offline guarantee with zero network permissions.

---

## 1. Crashes Fixed

- **Schedule duplicate-key crash**
  - **Files:** [`app/src/main/java/com/daymark/app/ui/screens/ScheduleScreen.kt`](app/src/main/java/com/daymark/app/ui/screens/ScheduleScreen.kt)
  - **User-visible effect:** Opening the Schedule screen in 3-day, Week, Month, or Agenda views no longer crashes with `IllegalArgumentException: Key "TASK-<id>" was already used` when recurring tasks emit multiple occurrences. Each `ScheduleRow` has a precomputed composite key `"$type-$id-$day"` (`row.key`). Additionally, the recurrence search is capped at 120 steps (`MAX_EXPANSION = 120`) to guard against corrupt recurrence rules.
  - **How to verify:** Create a daily recurring task, open Schedule in 3-day, Week, Month, and Agenda modes. No crash occurs and all occurrences display smoothly.

---

## 2. Correctness Fixed

- **Completion unique-index collision**
  - **Files:** [`app/src/main/java/com/daymark/app/data/DaymarkRepository.kt`](app/src/main/java/com/daymark/app/data/DaymarkRepository.kt)
  - **User-visible effect:** Completing today's and tomorrow's occurrences of a recurring series on the same calendar day now succeeds reliably instead of violating the `(seriesId, occurrenceDateEpochDay)` unique index and rolling back the transaction. The completion record accurately stamps `task.dueEpochDay ?: today`.
  - **How to verify:** Create a daily recurring task, complete today's occurrence, then complete tomorrow's occurrence on the same day. Both completions persist.

- **Atomic saves for parent/child records (Foreign Key race conditions)**
  - **Files:** [`app/src/main/java/com/daymark/app/data/DaymarkRepository.kt`](app/src/main/java/com/daymark/app/data/DaymarkRepository.kt), [`app/src/main/java/com/daymark/app/ui/DaymarkViewModel.kt`](app/src/main/java/com/daymark/app/ui/DaymarkViewModel.kt), [`app/src/main/java/com/daymark/app/ui/DaymarkApp.kt`](app/src/main/java/com/daymark/app/ui/DaymarkApp.kt)
  - **User-visible effect:** Tasks with subtasks, projects with milestones, and courses with modules are now saved atomically within single database transactions (`saveTaskWithSubtasks`, `saveProjectWithMilestones`, `saveCourseWithModules`), eliminating foreign-key CASCADE race conditions and partial saves. Original `createdAtMillis` timestamps are preserved across edits.
  - **How to verify:** Create a task with multiple subtasks; save and reopen to confirm all subtasks persist. Edit a project or course and confirm creation date is preserved.

- **Monthly recurrence drift**
  - **Files:** [`app/src/main/java/com/daymark/app/domain/RecurrenceEngine.kt`](app/src/main/java/com/daymark/app/domain/RecurrenceEngine.kt)
  - **User-visible effect:** Monthly recurring series starting on the 31st (or 29th/30th) anchor on `seriesStart.dayOfMonth` and clamp safely to `lengthOfMonth()` in shorter months (e.g., Feb 28), restoring the original day anchor in subsequent full months (e.g., March 31) rather than permanently drifting.
  - **How to verify:** Unit test `monthlyRecurrenceAcrossJanFebMarPreservesAnchorDay` validates Jan 31 -> Feb 28 -> Mar 31 -> Apr 30 -> May 31.

- **DateChooser reset / clear**
  - **Files:** [`app/src/main/java/com/daymark/app/ui/sheets/CreationSheets.kt`](app/src/main/java/com/daymark/app/ui/sheets/CreationSheets.kt)
  - **User-visible effect:** `DateChooser` no longer has inert buttons. For mandatory-date records (events, deadlines), the reset action resets the date to today. For optional-date tasks, "Clear" removes the date.
  - **How to verify:** Open the event or deadline editor, select a future date, click Reset; the field resets to today.

- **Scheduler reconciliation concurrency**
  - **Files:** [`app/src/main/java/com/daymark/app/notifications/ReminderScheduler.kt`](app/src/main/java/com/daymark/app/notifications/ReminderScheduler.kt)
  - **User-visible effect:** `ReminderScheduler.rescheduleAll()` is guarded by a process-wide `Mutex` companion object, preventing read-modify-write races in shared preferences and eliminating alarm leaks across resume events, boot receivers, and ViewModel updates.
  - **How to verify:** Trigger rapid edits across tasks and settings; alarms reconcile sequentially without dropped intents.

- **Stale notification target consuming**
  - **Files:** [`app/src/main/java/com/daymark/app/MainActivity.kt`](app/src/main/java/com/daymark/app/MainActivity.kt), [`app/src/main/java/com/daymark/app/ui/DaymarkApp.kt`](app/src/main/java/com/daymark/app/ui/DaymarkApp.kt)
  - **User-visible effect:** Launch extras are consumed once via `intent.removeExtra(...)` and `notificationTarget.value = null` is cleared on both branches, preventing the app from re-opening old notification targets when returning from Recents.

---

## 3. Notifications: Loudness, Status-Bar Icon & Bug Cluster

- **Loud reminders on Alarm audio stream**
  - **Files:** [`app/src/main/java/com/daymark/app/notifications/NotificationChannels.kt`](app/src/main/java/com/daymark/app/notifications/NotificationChannels.kt), [`app/src/main/java/com/daymark/app/notifications/ReminderReceiver.kt`](app/src/main/java/com/daymark/app/notifications/ReminderReceiver.kt), [`app/src/main/java/com/daymark/app/ui/screens/SettingsScreen.kt`](app/src/main/java/com/daymark/app/ui/screens/SettingsScreen.kt)
  - **User-visible effect:** Reminders use `USAGE_ALARM` + `CONTENT_TYPE_SONIFICATION` with `IMPORTANCE_HIGH` and `PRIORITY_HIGH`, delivering loud heads-up alerts on the alarm stream that won't be muffled by lowered ring/notification volumes. A user-facing "Loud reminders" switch in Settings allows toggling between alarm stream and notification stream channels.
  - **Channel Versioning:** Channel IDs are versioned (`daymark_reminder_v2_alarm_` and `daymark_reminder_v2_notif_`), ensuring existing installations automatically migrate to the new importance and audio stream on first launch. Stale legacy channels are pruned idempotently.
  - **How to verify:** Schedule a reminder for 1 minute out and lock the phone. The notification appears as a heads-up banner with full alarm volume. Toggle Loud reminders off in Settings and verify subsequent reminders play on the notification stream.

- **Status-bar icon knockout fix**
  - **Files:** [`app/src/main/res/drawable/ic_stat_daymark.xml`](app/src/main/res/drawable/ic_stat_daymark.xml)
  - **User-visible effect:** Redrawn with `android:fillType="evenOdd"` as an alpha-only monochrome glyph. Inner cutouts now subtract cleanly from the outer shape, fixing the solid white rounded rectangle bug on the status bar, shade, and lock screen.
  - **How to verify:** Trigger a reminder; the status-bar glyph displays a crisp, legible "D" checkmark glyph with negative space rather than a solid white blob.

- **Remaining notification fixes**
  - `ReminderReceiver`: Added distinct vector icons `ic_action_snooze` and `ic_action_done`. Added `NotificationCompat.BigTextStyle()` with `setShowWhen(true)` for expanding long descriptions. Wrapped async background operations in `withTimeout(15_000L)`.
  - Notification dismissal: `ReminderScheduler.cancelPosted(ownerType, ownerId)` cancels active shade notifications when a task is completed, archived, or deleted.
  - Repeating reminders roll-forward: `RescheduleReceiver` performs a daily roll-forward pass advancing overdue recurring tasks before rescheduling alarms.
  - Sound importer: Removed pre-Q legacy scanner branch and flagged imported audio with `MediaStore.Audio.Media.IS_NOTIFICATION = 1`.

---

## 4. Delete + Undo Everywhere

- **Ubiquitous delete with reversible Undo snapshots**
  - **Files:** [`app/src/main/java/com/daymark/app/data/DaymarkDatabase.kt`](app/src/main/java/com/daymark/app/data/DaymarkDatabase.kt), [`app/src/main/java/com/daymark/app/data/DaymarkRepository.kt`](app/src/main/java/com/daymark/app/data/DaymarkRepository.kt), [`app/src/main/java/com/daymark/app/ui/DaymarkViewModel.kt`](app/src/main/java/com/daymark/app/ui/DaymarkViewModel.kt), [`app/src/main/java/com/daymark/app/ui/DaymarkApp.kt`](app/src/main/java/com/daymark/app/ui/DaymarkApp.kt), [`app/src/main/java/com/daymark/app/ui/components/DaymarkComponents.kt`](app/src/main/java/com/daymark/app/ui/components/DaymarkComponents.kt), [`app/src/main/java/com/daymark/app/ui/screens/TasksScreen.kt`](app/src/main/java/com/daymark/app/ui/screens/TasksScreen.kt), [`app/src/main/java/com/daymark/app/ui/screens/NotesScreen.kt`](app/src/main/java/com/daymark/app/ui/screens/NotesScreen.kt), [`app/src/main/java/com/daymark/app/ui/screens/MoreScreens.kt`](app/src/main/java/com/daymark/app/ui/screens/MoreScreens.kt), [`app/src/main/java/com/daymark/app/ui/sheets/CreationSheets.kt`](app/src/main/java/com/daymark/app/ui/sheets/CreationSheets.kt)
  - **User-visible effect:**
    - Any record (task, recurring task occurrence/series, event, deadline, note, goal, project, course) can now be deleted.
    - `TaskRow` displays a delete button on task lists; each creation/edit sheet includes a "Delete" button when editing an existing item.
    - Notes, Goals, Projects, Courses, and Deadlines feature delete actions directly on card items and editor sheets.
    - Goals, projects, and courses prompt a confirmation `AlertDialog` explaining cascading deletions.
    - Recurring tasks prompt a 3-way scope chooser: **"Just this occurrence"**, **"This and all future occurrences"**, or **"All occurrences (entire series)"**.
    - All deletions emit an interactive snackbar with **"Undo"**, restoring the entity, its children (subtasks/modules/milestones), and its alarm reminders.
    - The task editor also wires up the previously unreachable "Archive" action.
  - **How to verify:** Delete a task, note, event, or goal; click "Undo" on the snackbar. Item and associated alarms return intact. Delete a recurring task occurrence and test each of the 3 scope choices.

---

## 5. Navigation & UI Restructure

- **Predictive back and hierarchical back-handling**
  - **Files:** [`app/src/main/java/com/daymark/app/ui/DaymarkApp.kt`](app/src/main/java/com/daymark/app/ui/DaymarkApp.kt), [`app/src/main/AndroidManifest.xml`](app/src/main/AndroidManifest.xml)
  - **User-visible effect:** System back follows a predictable stack: close active sheets/dialogs -> pop sub-screens in the More back stack -> return to the Home tab -> exit app. `android:enableOnBackInvokedCallback="true"` is enabled for predictive back transitions.
  - **How to verify:** Navigate to More -> Goals, press Back; navigates to More Hub. Press Back; navigates to Home tab. Press Back; exits app.

- **Grouped More Hub**
  - **Files:** [`app/src/main/java/com/daymark/app/ui/screens/MoreScreens.kt`](app/src/main/java/com/daymark/app/ui/screens/MoreScreens.kt)
  - **User-visible effect:** Destinations on the More hub are structured into logical sections under `SectionHeading`s: **Plan** (Goals, Projects), **Learn** (Courses, Deadlines), **Review** (Insights), and **App** (Settings).
  - **How to verify:** Open the More tab and observe categorized cards.

- **FAB overlap resolution & dead card fix**
  - **Files:** [`app/src/main/java/com/daymark/app/ui/screens/HomeScreen.kt`](app/src/main/java/com/daymark/app/ui/screens/HomeScreen.kt), [`app/src/main/java/com/daymark/app/ui/screens/TasksScreen.kt`](app/src/main/java/com/daymark/app/ui/screens/TasksScreen.kt), [`app/src/main/java/com/daymark/app/ui/screens/NotesScreen.kt`](app/src/main/java/com/daymark/app/ui/screens/NotesScreen.kt), [`app/src/main/java/com/daymark/app/ui/screens/ScheduleScreen.kt`](app/src/main/java/com/daymark/app/ui/screens/ScheduleScreen.kt), [`app/src/main/java/com/daymark/app/ui/screens/SettingsScreen.kt`](app/src/main/java/com/daymark/app/ui/screens/SettingsScreen.kt), [`app/src/main/java/com/daymark/app/ui/screens/MoreScreens.kt`](app/src/main/java/com/daymark/app/ui/screens/MoreScreens.kt)
  - **User-visible effect:** Bottom `contentPadding` has been raised to `96.dp` across list screens, ensuring extended FABs no longer obscure the final card. `TodayProgressCard` on the Home screen is now clickable and routes directly to Tasks.
  - **How to verify:** Tap `TodayProgressCard` on Home; it opens Tasks. Scroll to the bottom of Tasks, Notes, or Schedule; the bottom item is fully visible above the FAB.

- **Schedule month navigation normalization**
  - **Files:** [`app/src/main/java/com/daymark/app/ui/screens/ScheduleScreen.kt`](app/src/main/java/com/daymark/app/ui/screens/ScheduleScreen.kt)
  - **User-visible effect:** Stepping forward or backward months normalizes to the 1st of each month (`withDayOfMonth(1)`), preventing month-end date clamping bugs.
  - **How to verify:** Navigate month-by-month in Schedule starting from March 31; months step cleanly without skipping or sticking.

---

## 6. Theme Split, Creative Palettes & Room Migration v2

- **Theme mode & accent color separation**
  - **Files:** [`app/src/main/java/com/daymark/app/data/Entities.kt`](app/src/main/java/com/daymark/app/data/Entities.kt), [`app/src/main/java/com/daymark/app/data/DaymarkDatabase.kt`](app/src/main/java/com/daymark/app/data/DaymarkDatabase.kt), [`app/schemas/com.daymark.app.data.DaymarkDatabase/2.json`](app/schemas/com.daymark.app.data.DaymarkDatabase/2.json), [`app/src/main/java/com/daymark/app/ui/theme/DaymarkTheme.kt`](app/src/main/java/com/daymark/app/ui/theme/DaymarkTheme.kt), [`app/src/main/java/com/daymark/app/ui/screens/SettingsScreen.kt`](app/src/main/java/com/daymark/app/ui/screens/SettingsScreen.kt)
  - **User-visible effect:**
    - `themeKey` controls appearance mode (**Light**, **Dark**, **System**).
    - `accentKey` controls the accent palette independently across both light and dark modes.
    - 10 distinct, hand-tuned palettes each with dedicated tinted surface ramps: **Indigo**, **Lavender**, **Ocean**, **Sage**, **Rose**, **Monochrome**, **Ember**, **Midnight**, **Mocha**, and **Aurora**, plus **Dynamic (Material You)** wallpaper extraction on Android 12+ (API 31+).
    - Settings features dedicated Appearance selector and horizontal Accent palette picker.
    - Added a **24-hour clock** switch in Settings, wired to `use24HourClock` and respected in `remainingLabel`.
    - Room database version bumped to 2 with migration `MIGRATION_1_2` (`ALTER TABLE user_preferences ADD COLUMN accentKey TEXT NOT NULL DEFAULT 'INDIGO'; ALTER TABLE user_preferences ADD COLUMN loudReminders INTEGER NOT NULL DEFAULT 1`).
    - Fixed dark mode splash flash by introducing `values-night/styles.xml` and `values-night/colors.xml`.
    - Status and navigation bar icon contrast is controlled via `WindowInsetsControllerCompat` based on resolved in-app darkness.
  - **How to verify:** In Settings, toggle Dark mode with Ocean accent; app applies dark surfaces with ocean-tinted backgrounds and readable status bar icons. Cold launch in dark mode displays dark splash with no white flash.

---

## 7. Brand: Unified Mark & Correct Renditions

- **One cohesive geometry with checkmark negative space**
  - **Files:** [`app/src/main/res/drawable/ic_daymark_mark.xml`](app/src/main/res/drawable/ic_daymark_mark.xml), [`app/src/main/res/drawable/ic_launcher_foreground.xml`](app/src/main/res/drawable/ic_launcher_foreground.xml), [`app/src/main/res/drawable/ic_launcher_background.xml`](app/src/main/res/drawable/ic_launcher_background.xml), [`app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`](app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml), [`app/src/main/res/drawable/ic_stat_daymark.xml`](app/src/main/res/drawable/ic_stat_daymark.xml), [`app/src/main/java/com/daymark/app/ui/components/DaymarkComponents.kt`](app/src/main/java/com/daymark/app/ui/components/DaymarkComponents.kt)
  - **User-visible effect:**
    - All marks share a single geometric D design with a checkmark negative-space counter and solar daymark dot using `fillType="evenOdd"`.
    - Adaptive icon foreground centered strictly within the 66x66 safe zone.
    - Subtle brand gradient background replaces the flat plate.
    - Added `<monochrome>` layer for Android 13+ Material You themed icons.
    - `DaymarkMark` composable renders `ic_daymark_mark` directly via `painterResource`.
    - `logo-preview.html` generated in the root directory for inspecting the mark under circular and squircle masks.
  - **How to verify:** Open `logo-preview.html` in any browser to inspect the geometry at 24dp, 48dp, and 108dp. Verify adaptive launcher icon on home screen and themed icon on Android 13+.

---

## 8. Motion: Smooth, Consistent & 60fps Lightweight

- **Lightweight animations across the app**
  - **Files:** [`app/src/main/java/com/daymark/app/ui/theme/Motion.kt`](app/src/main/java/com/daymark/app/ui/theme/Motion.kt), [`app/src/main/java/com/daymark/app/ui/DaymarkApp.kt`](app/src/main/java/com/daymark/app/ui/DaymarkApp.kt), [`app/src/main/java/com/daymark/app/ui/components/DaymarkComponents.kt`](app/src/main/java/com/daymark/app/ui/components/DaymarkComponents.kt), [`app/src/main/java/com/daymark/app/ui/screens/TasksScreen.kt`](app/src/main/java/com/daymark/app/ui/screens/TasksScreen.kt), [`app/src/main/java/com/daymark/app/ui/screens/NotesScreen.kt`](app/src/main/java/com/daymark/app/ui/screens/NotesScreen.kt), [`app/src/main/java/com/daymark/app/ui/screens/MoreScreens.kt`](app/src/main/java/com/daymark/app/ui/screens/MoreScreens.kt)
  - **User-visible effect:**
    - Standardized motion tokens in `Motion.kt`: `Motion.fast` (~120ms), `Motion.standard` (~220ms), and `Motion.spring`.
    - Tab switching upgraded from slow `Crossfade` to a tight `AnimatedContent` fade-through (150ms out / 180ms in) ensuring only one screen composes at a time.
    - List items across Tasks, Notes, Goals, Projects, Courses, and Deadlines use `Modifier.animateItem()` for deliberate animations on delete and reorder.
    - Completing a task in `TaskRow` animates text color transition and strikethrough.
    - `ProgressRing` sweeps smoothly on value changes with `animateFloatAsState`.
    - Theme changes cross-fade smoothly with `animateColorAsState` on top-level color scheme roles.
  - **How to verify:** Toggle a task complete to observe animated strikethrough and color change. Delete a task or note to observe smooth list rearrangement. Switch tabs to experience snappy fade-through transitions.

---

## 9. Performance

- **True lazy rendering on Tasks screen**
  - **Files:** [`app/src/main/java/com/daymark/app/ui/screens/TasksScreen.kt`](app/src/main/java/com/daymark/app/ui/screens/TasksScreen.kt)
  - **User-visible effect:** Replaced the single card wrapping all tasks with distinct `items(filtered, key = { it.id })`. Memory allocation and layout measurements are lazy and smooth on large task lists. Filter and sort calculations memoized via `remember(tasks, filter)`.
- **Memoized calculations (`remember`)**
  - **Files:** [`app/src/main/java/com/daymark/app/ui/screens/HomeScreen.kt`](app/src/main/java/com/daymark/app/ui/screens/HomeScreen.kt), [`app/src/main/java/com/daymark/app/ui/screens/ScheduleScreen.kt`](app/src/main/java/com/daymark/app/ui/screens/ScheduleScreen.kt), [`app/src/main/java/com/daymark/app/ui/screens/NotesScreen.kt`](app/src/main/java/com/daymark/app/ui/screens/NotesScreen.kt), [`app/src/main/java/com/daymark/app/ui/screens/MoreScreens.kt`](app/src/main/java/com/daymark/app/ui/screens/MoreScreens.kt)
  - **User-visible effect:** Eliminated repeated `LocalTime.now()` calls and full-list filtering passes during recomposition.
- **Indexed lookups**
  - **Files:** [`app/src/main/java/com/daymark/app/data/DaymarkDatabase.kt`](app/src/main/java/com/daymark/app/data/DaymarkDatabase.kt), [`app/src/main/java/com/daymark/app/data/DaymarkRepository.kt`](app/src/main/java/com/daymark/app/data/DaymarkRepository.kt)
  - **User-visible effect:** `toggleSubtask`, `toggleMilestone`, and `toggleCourseModule` use indexed SQL queries (`get(id)`) instead of loading entire tables.

---

## 10. Build, Release & Code Hygiene

- **Release Build Verification & Hardened Proguard Rules**
  - **Files:** [`app/build.gradle.kts`](app/build.gradle.kts), [`app/proguard-rules.pro`](app/proguard-rules.pro), [`.github/workflows/build.yml`](.github/workflows/build.yml)
  - **Details:**
    - Hardcoded signing passwords completely removed; release signing only configures when environment variables are supplied.
    - `proguard-rules.pro` hardened with real Room class keep rules (`-keep class com.daymark.app.data.** { *; }`) and canonical `kotlinx.serialization` rules.
    - CI builds both `assembleDebug` and `assembleRelease`, actively exercising R8 shrinking and resource minimization.
- **Removed dead code**
  - **Files:** [`app/src/main/java/com/daymark/app/notifications/NotificationSoundImporter.kt`](app/src/main/java/com/daymark/app/notifications/NotificationSoundImporter.kt), [`app/src/main/java/com/daymark/app/notifications/ReminderScheduler.kt`](app/src/main/java/com/daymark/app/notifications/ReminderScheduler.kt), [`app/src/main/java/com/daymark/app/notifications/NotificationChannels.kt`](app/src/main/java/com/daymark/app/notifications/NotificationChannels.kt)
  - **Details:** Dropped dead pre-Q `MediaScannerConnection` branch, dead `testChannelId()`, unused `val pref`, and renamed `LazySettingsContent` to `SettingsContent`.
- **Locale-reactive formatters & externalized strings**
  - **Files:** [`app/src/main/java/com/daymark/app/ui/Formatting.kt`](app/src/main/java/com/daymark/app/ui/Formatting.kt), [`app/src/main/res/values/strings.xml`](app/src/main/res/values/strings.xml)
  - **Details:** Date and time formatters resolve formatting at call-time using current system locale. Delete/undo/settings strings externalized to `strings.xml`.

---

## 11. On-Device Assistant Seam (SLM / Hermes / "digi paws")

- **Structure & Interface**
  - **Files:** [`app/src/main/java/com/daymark/app/domain/assistant/AssistantContext.kt`](app/src/main/java/com/daymark/app/domain/assistant/AssistantContext.kt), [`app/src/main/java/com/daymark/app/domain/assistant/AssistantProvider.kt`](app/src/main/java/com/daymark/app/domain/assistant/AssistantProvider.kt), [`app/src/main/java/com/daymark/app/DaymarkApplication.kt`](app/src/main/java/com/daymark/app/DaymarkApplication.kt), [`app/src/main/java/com/daymark/app/data/Entities.kt`](app/src/main/java/com/daymark/app/data/Entities.kt)
  - **Architecture:**
    - `AssistantContext`: Immutable data class capturing active metrics (today's tasks, overdue count, free time windows, completion streaks) constructed by `AssistantContextBuilder`.
    - `AssistantProvider`: Standard interface with `isAvailable: Boolean` and `suspend fun suggest(context: AssistantContext): List<AssistantSuggestion>`.
    - `NoopAssistantProvider`: Ships as the active default; returns empty list with zero overhead.
    - Single injection point in `DaymarkApplication.kt`.
    - Dashboard registration includes `DashboardWidgets.ASSISTANT` (hidden by default).
    - **Contract strictly requires all future models to run 100% on-device.**

---

## 12. Offline Guarantee

- **No Network Dependency & No `INTERNET` Permission**
  - `AndroidManifest.xml` only requests:
    - `android.permission.POST_NOTIFICATIONS`
    - `android.permission.SCHEDULE_EXACT_ALARM`
    - `android.permission.RECEIVE_BOOT_COMPLETED`
    - `android.permission.VIBRATE`
  - Zero analytics, zero telemetry, and zero remote network requests. Daymark remains 100% offline-first.
