# Finly

A native Android app for recording income and expenses, tracking a monthly budget, and reviewing spending patterns.

## Overview

Finly helps people keep their daily financial records in one place. It is intended for individuals who want to enter transactions manually, understand their spending, and compare expenses with a monthly limit.

The app provides transaction lists, budget progress, charts, and reminders. Records and preferences are stored on the device, so the main finance features work offline. Sign-up and login use a local account stored on the same device.

## Key Features

- **Transaction management:** Add, edit, and delete income or expenses with a title, amount, category, date, and optional note. Filter the full list by income or expense.
- **Financial summaries:** View recent transactions, an annual balance summary, spending by category, and income and expense trends over six months.
- **Monthly budget:** Set a spending limit and view the amount used, percentage used, and remaining budget.
- **Notifications:** Review transaction updates, swipe notifications to mark them as read, and clear notification history. Enable daily reminders and budget alerts at 75% and 100% usage.
- **Profile and settings:** Edit your name and email, sign out, switch between light and dark themes, and choose a preferred currency: USD, EUR, JPY, CAD, or LKR.
- **Transaction backup:** Export and share records as JSON, or import a backup. Import updates records with matching IDs and adds new records. Settings also let you clear transactions and reset the monthly budget.

## Tech Stack

| Area | Technologies |
| --- | --- |
| Language | Kotlin 1.9.24 |
| Android interface | XML layouts, View Binding, Material Components, AppCompat, ConstraintLayout, RecyclerView, CircleImageView |
| State and navigation | AndroidX ViewModel, LiveData, Navigation Component |
| Charts | MPAndroidChart 3.1.0 |
| Local storage | SharedPreferences, Gson 2.10.1 |
| Background reminders | AlarmManager, Android services, BroadcastReceiver, NotificationCompat |
| Build tools | Gradle 8.10.2, Android Gradle Plugin 8.8.2, Kotlin DSL |

## System Architecture

Finly contains one Android application module. Activities handle startup, onboarding, sign-in, and the main navigation. Fragments display screens using XML layouts and View Binding.

Transaction and budget screens use ViewModels and LiveData to load and update screen data. Repositories save transactions, budgets, preferences, and notification history in SharedPreferences. Gson converts transaction and notification lists to and from JSON. Some screens, including profile and settings, also access repositories or preferences directly.

Android services and broadcast receivers handle reminder scheduling and budget notifications. `BackupService` transfers transaction records between local storage and JSON files through Android's file picker and sharing tools.

There is no separate backend or database server. Login compares credentials stored in SharedPreferences as plain text; it is a local demonstration flow.

## Project Structure

```text
.
|-- app/
|   |-- build.gradle.kts          # Android settings and dependencies
|   `-- src/
|       |-- main/
|       |   |-- AndroidManifest.xml
|       |   |-- java/com/example/finly/
|       |   |   |-- MainActivity.kt       # Main navigation and transaction entry
|       |   |   |-- ui/                   # Screens, dialogs, and list adapters
|       |   |   |-- viewmodels/           # Transaction, budget, and category logic
|       |   |   |-- repositories/         # Local data access
|       |   |   |-- models/               # Transaction and notification data types
|       |   |   |-- services/             # Backups, reminders, and budget alerts
|       |   |   `-- utils/                # Amount formatting
|       |   `-- res/                      # Layouts, navigation, themes, and images
|       |-- test/                        # Example unit test
|       `-- androidTest/                 # Example device test
|-- gradle/
|   |-- libs.versions.toml        # Dependency versions
|   `-- wrapper/                 # Gradle wrapper configuration
|-- build.gradle.kts             # Shared plugin declarations
|-- settings.gradle.kts          # Module and dependency repositories
|-- gradlew                      # macOS/Linux wrapper
`-- gradlew.bat                  # Windows wrapper
```

## Getting Started

### Prerequisites

- Android Studio with JDK 21, matching the included IDE configuration. Application bytecode targets Java 11.
- Android SDK Platform 35 and SDK Platform-Tools.
- An emulator or Android device running API 24 or newer.
- Internet access for the initial Gradle and dependency downloads.

### Installation

Download and extract the repository, then open its root folder in Android Studio. Select **Sync Project with Gradle Files**. Gradle downloads dependencies from Google Maven, Maven Central, and JitPack. The included wrapper provides Gradle, so a separate Gradle installation is unnecessary.

### Environment Configuration

The app requires no API keys or backend configuration. Android Studio normally creates `local.properties` with your Android SDK location. For manual configuration, use your own path, for example:

```properties
sdk.dir=C:/Users/YOUR_NAME/AppData/Local/Android/Sdk
```

For terminal commands, set `JAVA_HOME` to your JDK directory.

### Running the Application

Start an emulator or connect a device with USB debugging enabled. Select the `app` configuration and press **Run** in Android Studio, or build and install from the repository root:

Windows PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:installDebug
```

macOS/Linux:

```sh
sh ./gradlew :app:assembleDebug
sh ./gradlew :app:installDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. After installation, open **Finly**, complete or skip onboarding, and create a local account. Allow notifications when prompted to receive reminders and budget alerts.

These commands follow the project configuration. Builds, tests, and device behavior were not verified during this documentation update.
