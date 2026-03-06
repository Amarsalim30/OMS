# Performance Review

## Scope
UI responsiveness, recomposition risk, heavy I/O paths, and long-running background or service work.

## Current state
- Debug and release verification now run cleanly in this environment, so performance work is no longer hidden behind a local build blocker.
- The largest performance and maintenance risks are still concentrated in `BackupManager.kt`, `HelperOverlayService.kt`, and the remaining broad state surface in `MainAppContent.kt`.
- This pass added sanitized logs around licensing and backup or restore operations, which helps field diagnosis but is not a substitute for profiling.

## Findings

### P1. `BackupManager.kt` remains the biggest latency and complexity risk
- Severity: high
- Files: `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt`
- Why it matters: backup archive creation, restore validation, and data mapping all sit in one large subsystem, which makes it harder to reason about dispatchers, latency, and cancellation behavior.
- Recommended fix: extract smaller collaborators first, then measure the expensive sections with targeted timing or benchmarks.
- Implement now or later: later
- Risk: medium-high
- Status: open

### P2. `HelperOverlayService.kt` is still a long-lived mixed-responsibility service
- Severity: medium
- Files: `app/src/main/java/com/zeynbakers/order_management_system/core/helper/HelperOverlayService.kt`
- Why it matters: services that mix lifecycle, audio, parsing, and overlay work are harder to optimize and easier to accidentally stall.
- Recommended fix: separate lifecycle management from command parsing and UI overlay coordination before adding more behavior.
- Implement now or later: later
- Risk: medium-high
- Status: open

### P3. `MainAppContent.kt` still owns a broad state surface, but the risk is lower than the initial audit suggested
- Severity: medium
- Files:
  - `app/src/main/java/com/zeynbakers/order_management_system/MainAppContent.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/MainAppHostScaffold.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/AppFeatureNavHost.kt`
- Why it matters: wide state ownership can broaden recomposition scope and make state interactions harder to optimize.
- Recommended fix: continue pulling side-effect-heavy responsibilities behind smaller helpers and keep feature navigation in graph files.
- Implement now or later: later
- Risk: medium
- Status: open

### P4. There is still no profiling or macrobenchmark coverage for the highest-value paths
- Severity: medium
- Files and areas: startup, calendar navigation, payment intake, backup or restore, helper overlay
- Why it matters: without measurement, optimization work risks being speculative.
- Recommended fix: add targeted manual timing captures first, then consider baseline profile or macrobenchmark work once the large refactors stabilize.
- Implement now or later: later
- Risk: low
- Status: open

## Recommended next measurements
1. Startup time from app launch to first interactive frame after licensing validation.
2. Calendar month-switch responsiveness on a representative mid-range device.
3. Backup and restore execution time on realistic datasets.
4. Helper overlay open, listen, and dismiss latency under normal permission conditions.
