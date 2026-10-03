# FixMate

Production-style Android task management app built with Kotlin and Jetpack Compose.

FixMate is a small "repair job tracker": browse a list of maintenance tasks, search and filter them, open the details, create or edit tasks, mark them done — and keep doing all of that **offline**. Every change is stored locally first and synchronised with the remote API in the background once the device is back online.

> Demo data comes from the public [DummyJSON `/todos` API](https://dummyjson.com/docs/todos). Its `POST`/`PATCH`/`DELETE` endpoints are **simulated** — they return realistic responses but the server never persists anything. Room is therefore the source of truth and the remote API is used to seed and sync demo data.

## Demo

| Task list | Task detail | Editor | Settings |
| --- | --- | --- | --- |
| _screenshot_ | _screenshot_ | _screenshot_ | _screenshot_ |

Offline scenario worth recording as a GIF:

1. Open the app with internet — tasks load from DummyJSON.
2. Turn Wi-Fi off — the list still shows tasks from Room.
3. Create a task — it appears instantly with a **Not synced** badge.
4. Turn Wi-Fi on — WorkManager pushes the change and the badge disappears.

## Features

- Task list with search, `All / Pending / Completed` filters and pull-to-refresh
- Task detail with status, owner, last update and sync state
- Create / edit tasks with validation (title required, min 3 characters)
- Complete / uncomplete tasks from the list or the detail screen
- Delete tasks with confirmation
- **Offline-first**: Room is the single source of truth, the UI only observes the database
- **Background sync** with WorkManager (network constraint + exponential backoff)
- Per-task sync state (`SYNCED`, `PENDING_CREATE`, `PENDING_UPDATE`, `PENDING_DELETE`) surfaced in the UI
- Error handling mapped to three user-facing categories (network / server / unknown) with retry snackbars
- Loading, empty and error states
- Settings: System / Light / Dark theme, last sync time, manual "Sync now"
- Material 3, dynamic colour on Android 12+, edge-to-edge
- Unit tests for the repository and ViewModels, instrumented tests for the DAO

## Tech Stack

| Area | Choice |
| --- | --- |
| Language | Kotlin 2.3 |
| UI | Jetpack Compose, Material 3, Navigation Compose (type-safe routes) |
| Architecture | MVVM + use cases + repository pattern (single module, clean packages) |
| Async | Coroutines, `StateFlow`, `combine` |
| Network | Retrofit 3, OkHttp 5, Gson |
| Persistence | Room 2.8 (KSP, exported schemas), DataStore Preferences |
| DI | Hilt 2.60 (incl. `@HiltWorker`, assisted ViewModels) |
| Background | WorkManager 2.12 |
| Build | AGP 9.2 with built-in Kotlin, Gradle 9.4, compileSdk 37, minSdk 24, JDK 17+ |
| Tests | JUnit 4, kotlinx-coroutines-test, Turbine, AndroidX Test |

## Architecture

```
UI (Compose screens)
        ↓ collects StateFlow
ViewModel
        ↓
Use cases (domain)
        ↓
TaskRepository (interface in domain, impl in data)
      ↙            ↘
Room (DAO)      Retrofit (DummyJSON)
      ↖            ↙
   WorkManager TaskSyncWorker
```

```
com.duychien.fixmate
├── core
│   ├── common      AppError, AppResult, DispatcherProvider, Clock, Constants
│   ├── database    FixMateDatabase, TaskDao, TaskEntity
│   ├── network     FixMateApi, DTOs, NetworkModule
│   └── ui          theme + shared components (sync badge, empty/loading states)
├── data
│   ├── mapper      DTO ⇄ Entity ⇄ Domain
│   ├── preferences DataStore-backed SettingsRepository
│   ├── repository  TaskRepositoryImpl (offline-first logic)
│   └── worker      TaskSyncWorker, SyncScheduler
├── domain
│   ├── model       Task, SyncState, ThemeMode
│   ├── repository  TaskRepository, SettingsRepository
│   └── usecase     GetTasks, GetTask, CreateTask, UpdateTask, ToggleTask, DeleteTask, RefreshTasks, SyncTasks
├── feature
│   ├── tasklist    screen + ViewModel + UiState + TaskFilter
│   ├── taskdetail
│   ├── taskeditor
│   └── settings
└── navigation      Destination (serializable routes) + AppNavGraph
```

## Offline Strategy

**Room is the single source of truth.** Screens never render network responses directly; they observe `Flow`s from the DAO, so the UI is always consistent with what is stored on the device.

Every row carries a `syncState`:

| State | Meaning |
| --- | --- |
| `SYNCED` | Matches what the server knows |
| `PENDING_CREATE` | Created locally, not yet pushed |
| `PENDING_UPDATE` | Edited locally, not yet pushed |
| `PENDING_DELETE` | Deleted locally (hidden from the UI) until the server confirms |

Write path:

```
user action → write to Room (mark pending) → UI updates instantly
           → try the API call right away
                 success → mark SYNCED
                 failure → stay pending, enqueue WorkManager (CONNECTED constraint, backoff)
```

Refresh path (`GET /todos` with `limit`/`skip` paging):

```
fetch pages → map DTO → upsert into Room, skipping rows that are still pending locally
```

Details that make this robust rather than a demo:

- A refresh never overwrites a change the user made offline (pending rows are excluded from the upsert).
- A task is only flipped to `SYNCED` if it wasn't edited again while the request was in flight.
- Deleting a task that never reached the server just removes it locally — no pointless network call.
- `404` on update/delete is treated as "the server doesn't have it": updates fall back to re-creating the task, deletes are considered done.
- Locally created tasks keep a client-generated id because DummyJSON always answers `POST /todos/add` with the same fake id.
- `TaskSyncWorker` retries on `IOException`/5xx and fails fast on everything else.

Known limitation (by design of the demo backend): because DummyJSON never persists writes, a full refresh will bring back deleted tasks and revert synced edits of the seed data. With a real backend the same code path would simply return the updated records.

## API

Base URL: `https://dummyjson.com/`

| Method | Endpoint | Used for |
| --- | --- | --- |
| `GET` | `/todos?limit=&skip=` | Paged refresh |
| `GET` | `/todos/{id}` | Single task |
| `POST` | `/todos/add` | Create (simulated) |
| `PATCH` | `/todos/{id}` | Update (simulated) |
| `DELETE` | `/todos/{id}` | Delete (simulated) |

## Testing

```bash
./gradlew testDebugUnitTest          # JVM tests
./gradlew connectedDebugAndroidTest  # DAO tests on a device/emulator
```

- `TaskRepositoryImplTest` — refresh persists remote data and pages through results, refresh never clobbers pending rows, offline create marks `PENDING_CREATE` and schedules a sync, sync flips rows to `SYNCED`, 404 fallbacks, delete semantics.
- `TaskListViewModelTest` — search filtering, status filters, refresh failure keeps cached data and exposes a network error, initial loading state.
- `TaskEditorViewModelTest` — title validation, create and edit flows.
- `TaskDaoTest` (instrumented) — upsert/observe, ordering, pending-delete visibility, pending-sync queries.

Tests use hand-written fakes (`FakeTaskDao`, `FakeFixMateApi`, `FakeTaskRepository`) rather than mocks, so they read like specifications of the offline behaviour.

## How to run

1. Clone the repository.
2. Open the project in Android Studio (Narwhal or newer, bundled JDK 17+).
3. Let Gradle sync — it will download the Android 37 platform if needed.
4. Run the `app` configuration on a device or emulator (minSdk 24).

Command line:

```bash
./gradlew :app:assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## License

MIT
