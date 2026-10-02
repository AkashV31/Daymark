# Daymark 🧭

> **A calm, offline-first personal organization system for Android.**  
> Effortlessly unify your tasks, recurring schedules, deadlines, notes, courses, and long-term goals — without cloud lock-in, tracking, or subscription fatigue.

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Android Min SDK](https://img.shields.io/badge/Android-10.0%2B%20(API%2029)-brightgreen.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4.svg)](https://developer.android.com/jetpack/compose)

---

## 🌟 Overview

Most productivity apps today are cluttered with subscriptions, ads, and forced cloud syncing that compromises your privacy. **Daymark** is designed from the ground up to be calm, intentional, and entirely self-contained on your device.

Whether you're managing complex course syllabi, tracking multi-step projects, scheduling recurring routines, or capturing thoughts on the fly, Daymark gives you a coherent, single view of your day.

---

## ✨ Features

### 📋 Intelligent Tasks & Subtasks
* **Priority Levels:** Organize by `Low`, `Normal`, `High`, or `Critical` with subtle visual indicators.
* **Interactive Checklist / Subtasks:** Break down larger tasks into step-by-step checklists with live progress tracking.
* **Flexible Organization:** Associate tasks with Projects, Goals, Courses, and custom Categories.
* **Time Estimates & Notes:** Add estimated durations and private notes to any task.

### 🔁 Powerful Recurrence Engine
* **Flexible Schedules:** Daily, Weekdays, Weekly (with multi-day weekday selection masks), Monthly, and Custom intervals.
* **Smart End Conditions:** Repeat indefinitely, end on a specific date, or stop after an exact occurrence count.
* **Granular Edits:** Choose whether edits apply to *This occurrence*, *This and future*, or the *Entire series*.
* **Virtual Projection:** Fast, low-memory virtual occurrence projection across schedule views.

### 📅 Calendar & Schedule
* **Dynamic Views:** Fluid switching between **Agenda**, **Day**, **3-Day**, and **Week** views.
* **Unified Timeline:** See your tasks, calendar events, and looming deadlines in a single, conflict-free chronological schedule.

### ⏰ Bulletproof Background Notifications
* **Survives Process Termination:** Built on Android's native `AlarmManager.setExactAndAllowWhileIdle()` (`RTC_WAKEUP`). Reminders fire punctually even when the app is cleared from Recents or your device is in deep Doze sleep.
* **Reboot & Timezone Resilience:** Listens for `BOOT_COMPLETED`, `TIME_SET`, and `TIMEZONE_CHANGED` system broadcasts to restore all alarms automatically.
* **Actionable Alerts:** Complete or snooze (10m) tasks straight from the notification shade without launching the app.
* **Custom Sounds:** Includes 6 gentle built-in notification chimes, plus support for importing your own audio tones.

### 🎯 Goals & Milestone Tracking
* Long-term ambition tracking divided into tangible, achievable milestones.
* Progress bars that reflect both milestone completions and linked task activity.

### 📚 Course & Learning Modules
* Dedicated space for students and lifelong learners.
* Track individual courses, credit hours, instructors, and module checklists.

### 📝 Pinned & Searchable Notes
* Fast markdown-ready note editor.
* Pin important notes to the top; search across titles and body text with instant query matching.

### ⚡ Natural Language Quick Capture
* Offline NLP parser for instant input:
  ```
  Submit research draft tomorrow at 5pm !high #college
  ```
  Automatically recognizes relative dates (`tomorrow`, `next friday`), times (`5pm`, `14:30`), priorities (`!critical`, `!high`), and categories (`#tag`).

### 🎨 Material 3 & Curated Aesthetics
* 8 curated color themes: **Daymark Light**, **Daymark Dark**, **Lavender**, **Ocean**, **Sage**, **Rose**, **Monochrome**, and **System Default**.
* Customizable dashboard widgets on the Home screen that you can reorder to suit your workflow.

### 🔒 100% Offline & Private
* **Zero telemetry, zero ads, zero remote analytics.**
* All data is stored in a sandboxed local SQLite database.
* **Backup & Restore:** Full one-tap JSON export and import for seamless migrations between devices.

---

## 🏗️ Architecture & Tech Stack

Daymark is built following modern Android Architecture Components (MVVM) and Clean Architecture principles:

```
com.daymark.app/
├── data/                  # Room Entities, DAOs, Database migrations, BackupManager
├── domain/                # Business logic: RecurrenceEngine, ReminderPlanner, QuickCaptureParser
├── notifications/         # AlarmManager adapters, BroadcastReceivers, NotificationChannels
└── ui/                    # Jetpack Compose UI
    ├── components/        # Reusable design system tokens, cards, and inputs
    ├── screens/           # Home, Schedule, Tasks, Notes, Goals, Courses, Insights, Settings
    ├── sheets/            # Modal bottom sheets for creation, editing, and search
    ├── theme/             # Material 3 dynamic themes, colors, and typography
    └── DaymarkViewModel.kt# Centralized reactive state management
```

* **Language:** Kotlin 2.0+
* **UI Toolkit:** Jetpack Compose with Material Design 3 (BOM `2024.12.01`)
* **Persistence:** AndroidX Room `2.6.1` (SQLite with Foreign Keys & Indices)
* **Concurrency:** Kotlin Coroutines & `StateFlow`
* **Serialization:** `kotlinx.serialization` (JSON)
* **Desugaring:** `desugar_jdk_libs:2.1.4` (Full `java.time` API support across all Android versions)
* **Min SDK:** Android 10 (API 29)
* **Target SDK:** Android 15 (API 35)

---

## 🚀 Building & Running

### Prerequisites
* **Android Studio Ladybug** or newer (or command-line Android SDK)
* **JDK 17** (e.g. Microsoft OpenJDK 17 or Eclipse Temurin 17)
* Android SDK Platform 35 & Build-Tools `35.0.0`

### 1. Clone the repository
```bash
git clone https://github.com/AkashV31/Daymark.git
cd Daymark
```

### 2. Build Debug APK
```bash
./gradlew assembleDebug
```
The output APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### 3. Run Unit Tests
```bash
./gradlew testDebugUnitTest
```

### 4. Build Release APK (Optional)
To sign your own release build, generate a keystore and pass credentials via environment variables:
```bash
keytool -genkeypair -v -keystore release.jks -alias daymark -keyalg RSA -keysize 2048 -validity 10000
./gradlew assembleRelease
```

---

## 📱 Permissions

Daymark requests minimal permissions strictly needed for its functionality:

| Permission | Purpose |
|---|---|
| `POST_NOTIFICATIONS` | Delivers reminder alerts on Android 13+ (API 33+) |
| `SCHEDULE_EXACT_ALARM` | Ensures reminders trigger at the exact minute requested |
| `RECEIVE_BOOT_COMPLETED` | Restores scheduled reminders after device restart |
| `VIBRATE` | Haptic alert feedback when notifications arrive |

*Daymark requires **NO internet permission** — your data never leaves your device.*

---

## 📄 License

```
Copyright 2026 Akash Vishwakarma

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
