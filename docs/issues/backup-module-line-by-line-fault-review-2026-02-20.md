# Title: Backup Module Line-by-Line Fault Review (Refreshed 2026-02-22)

## Status
Partially resolved. Core backup reliability is strong; several UX/flow consistency issues remain.

## Inputs Reviewed
- Live code:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupPreferences.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupScheduler.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/DailyBackupWorker.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/graphs/OnboardingGraph.kt`
- Reference doc:
  - `docs/issues/backup-system-complete-reference-2026-02-22.md`

## Findings (ordered by severity)

### P1-1: Onboarding backup URI persistence uses hardcoded flags (provider-fragile)
- File refs:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/graphs/OnboardingGraph.kt:163`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/graphs/OnboardingGraph.kt:170`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt:823`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt:833`
- Problem:
  - Onboarding calls `takePersistableUriPermission(uri, READ|WRITE)` directly.
  - Settings flow is more robust and masks granted flags before persisting.
- Impact:
  - Some providers/OEMs may not return both grants; onboarding setup can silently fail to persist backup access.
- Fix direction:
  - Reuse the same granted-flags persistence logic used by Backup Settings (or a shared helper) in onboarding.

### P1-2: `SafDirectory` is supported in core but not configurable from current settings UI
- File refs:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupPreferences.kt:59`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt:123`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt:138`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt:771`
- Problem:
  - Core and preferences still support `BackupTargetType.SafDirectory`.
  - Settings only launches `CreateDocument` and writes `SafFile` via `setFileTargetSelection`.
  - `setTargetSelection(...)` (folder mode setter) has no caller.
- Impact:
  - UX/model mismatch and maintenance risk.
  - Users cannot intentionally configure folder mode from current UI.
- Fix direction:
  - Choose one:chose 1
    1. Fully deprecate/remove `SafDirectory` from runtime model and migrate existing state to `SafFile`, or
    2. Reintroduce folder mode controls (`OpenDocumentTree`) with clear mode selector.

### P1-3: Restore policy defaults to `LegacyCompatible` but no UI control exists
- File refs:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupPreferences.kt:29`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupPreferences.kt:103`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:246`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:657`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt`
- Problem:
  - Policy model exists and manager honors it.
  - UI currently has no policy selector, so installs effectively stay on default `LegacyCompatible`.
- Impact:
  - Default restore posture is less strict than intended for production integrity.
- Fix direction:
  - Set default to `Strict` for new installs (with migration logic for existing users if needed).
  - Add explicit policy UI (with warning text for legacy mode).

### P2-1: Backup settings copy and labels are file-centric even when target is not a file
- File refs:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt:349`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt:354`
  - `app/src/main/res/values/strings.xml:526`
  - `app/src/main/res/values/strings.xml:527`
- Problem:
  - Subtitle and primary label always describe a selected backup file and Drive behavior.
  - This is inaccurate in `AppPrivate` mode and confusing in mixed-mode states.
- Impact:
  - User uncertainty about actual backup destination/behavior.
- Fix direction:
  - Render dynamic copy and labels by target type (`AppPrivate`, `SafFile`, `SafDirectory`).

### P2-2: `SafFile` rollback only occurs before write completion; post-write verification failure can leave bad file
- File refs:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:851`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:873`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:885`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:935`
- Problem:
  - Once write is marked complete, rollback is intentionally skipped even if readback verification fails.
- Impact:
  - Rare provider inconsistencies can leave the selected backup file in uncertain state.
- Fix direction:
  - Where provider allows, write to temp sibling + replace only after verification.
  - If provider does not allow atomic swap, surface explicit warning and suggest retry before leaving screen.

## Resolved Since Original 2026-02-20 Review

### Resolved: transactional export snapshot
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:686`

### Resolved: ZIP limits and zip-bomb guardrails
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:73`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:1441`

### Resolved: force-key mismatch
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/DailyBackupWorker.kt:13`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupScheduler.kt:21`

### Resolved: backup preflight integrity and restore count verification
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:1212`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:1233`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:1413`

### Resolved: folder backup growth (rolling single-file behavior)
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:63`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:509`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:625`

## Alignment Check vs `backup-system-complete-reference-2026-02-22.md`
- The reference doc is broadly accurate for current architecture and reliability behavior.
- Open issues above should be reflected as active gaps in future revisions:
  - onboarding permission persistence robustness
  - `SafDirectory` UX/model mismatch
  - hidden manifest-policy control/default
  - target-type-specific copy cleanup

## Updated Implementation Plan
1. Unify SAF URI persistence logic across onboarding and settings.
2. Decide and implement one target-mode strategy (remove folder mode or expose it fully).
3. Expose restore policy UI and/or switch default to `Strict`.
4. Make storage labels/copy target-aware.
5. Improve `SafFile` post-write failure handling path.
6. Add focused tests for onboarding URI grant handling and mode migration behavior.

## Definition of Done (for remaining faults)
- Onboarding and settings persist SAF permissions with provider-safe logic.
- No dead backup target modes remain in runtime model.
- Restore strictness is visible and intentional in UI defaults.
- Backup destination wording is accurate for every target type.
- `SafFile` failure path cannot silently degrade last known-good backup without clear user signal.
