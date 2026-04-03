# WorkManager Background Network Bug

Minimal repro for WorkManager 2.10.x starting workers without network access despite a `CONNECTED` constraint.

Related issue: https://issuetracker.google.com/issues/387656869

## The problem

A `CoroutineWorker` with `setRequiredNetworkType(NetworkType.CONNECTED)` starts with `activeNetwork=null` on Android 15+. The same network request succeeds immediately when triggered via `AlarmManager` → `BroadcastReceiver`.

## How to test

1. Install the app and pin the widget to your home screen
2. Tap **"Update via AlarmManager"** or **"Update via WorkManager"** — both schedule an update in 60 seconds and close the activity
3. Wait on the launcher and watch the widget + logcat

Filter logcat by: `tag:NetworkFetch|UpdateWidgetWorker|UpdateWidgetReceiver`

## What to expect

**AlarmManager** — `activeNetwork` is valid, network request succeeds on the first attempt:
```
Alarm — activeNetwork=114, internet=true, validated=true
Alarm attempt #1 — OK (HTTP 204)
```

**WorkManager 2.10.5** — `activeNetwork` is null, first attempt fails. May recover after retrying, or may never recover:
```
Worker — activeNetwork=null, internet=null, validated=null
Worker attempt #1 — FAILED: UnknownHostException
Worker attempt #2 — OK (HTTP 204)
```

**WorkManager 2.9.1** — works correctly (network is available when the worker starts).

## Dependencies

- `androidx.work:work-runtime-ktx:2.10.5`
- No Glance, no other heavyweight dependencies

## Structure

- `NetworkFetch.kt` — shared fetch + retry logic used by both paths
- `UpdateWidgetWorker.kt` — WorkManager path
- `UpdateWidgetReceiver.kt` — AlarmManager path
- `TimestampWidgetProvider.kt` — RemoteViews widget that displays the result
