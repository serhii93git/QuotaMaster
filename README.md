# QuotaMaster

Track daily/weekly/monthly/yearly work quotas — hours and unique days — with a
motivational coach card and animated 270° gauge progress ring.

## Tech Stack

| Layer        | Library / Pattern                          |
|--------------|--------------------------------------------|
| Language     | Kotlin 1.9.24                              |
| UI           | Jetpack Compose + Material 3 (BOM 2024.06) |
| Architecture | MVVM + Repository                          |
| Database     | Room 2.6.1 (SQLite, persists across reboots)|
| Settings     | SharedPreferences via SettingsRepository    |
| DI           | Manual (`AppContainer` in `Application`)   |
| Navigation   | Navigation-Compose 2.7.7                   |
| Min SDK      | 26 (native `java.time`)                    |
| JVM Target   | 17                                         |

## Quick Start

1. Run `python generate_quotamaster.py`  → creates `QuotaMaster/` folder.
2. Open `QuotaMaster/` in **Android Studio Hedgehog** or later.
3. When prompted, let Studio configure the Gradle Wrapper.
4. **Sync Project** → **Run** on API 26+ device or emulator.

## Project Structure

```
app/src/main/java/com/quotamaster/
├── data/
│   ├── db/          AppDatabase, WorkSessionDao
│   ├── model/       WorkSession, Period, QuotaSettings
│   └── repository/  WorkSessionRepository, SettingsRepository
├── di/              AppContainer
├── ui/
│   ├── dashboard/   DashboardScreen, SessionLoggerDialog, SettingsDialog
│   ├── history/     HistoryScreen (swipe-to-delete + Snackbar undo)
│   ├── navigation/  AppNavigation
│   └── theme/       Color, Type, Theme
├── util/            TimeCalculator, MotivationProvider
├── viewmodel/       MainViewModel, DashboardState
├── MainActivity.kt
└── QuotaMasterApp.kt
```

## Key Improvements over v1

- **durationMinutes (Int)** instead of hoursWorked (Double) — no float rounding bugs
- **note** field on WorkSession for optional session notes
- **DAILY period** added alongside Weekly/Monthly/Yearly
- **Per-period goals** — separate hour targets for each period
- **Settings persistence** — survives app restart via SharedPreferences
- **270° gauge ring** — industry-standard gauge visualization
- **Snackbar undo** — swipe-to-delete shows undo option
- **4-tier motivation** — <80%, 80-99%, 100-119%, 120%+ messages
- **All strings in strings.xml** — ready for localization
- **Semantic date/time validation** — rejects impossible dates like 2024-02-30
- **JVM 17** — modern target for AGP 8.4+ toolchain
- **stateIn(WhileSubscribed)** — auto-cancels Flow when UI inactive
- **Separate allSessions Flow** — History shows all, Dashboard shows current period
