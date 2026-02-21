# Title: Backup Module Line-by-Line Fault Review (Drive + SAF Reliability)

## Status
Resolved in code (2026-02-20), pending device QA on Oppo/ColorOS picker behavior

## Scope Reviewed
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupAttentionNotifier.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupModels.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupPreferences.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupScheduler.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/DailyBackupWorker.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/ManualBackupWorker.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/RestoreWorker.kt`

## Goal
Fix backup module faults that block or degrade Google Drive backup UX/reliability ("picker opens but no Drive", unstable cloud writes, restore safety gaps).

## Findings (ordered by severity)

### P0-1: Cloud folder selection can be rejected even when URI permission is granted
- File refs:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt:154`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt:158`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:380`
- Problem:
  - After picking a tree URI, the flow immediately calls `isSafTargetAccessible()` and blocks selection if `DocumentFile.exists()` is false.
  - Some cloud providers (or transient network state) can return false/unknown immediately after grant.
- Impact:
  - User selects a valid Drive folder but app rejects it as unavailable.
  - Feels like "Drive not working" even when provider is valid.
- Fix direction:
  - Accept and persist URI immediately after permission grant.
  - Downgrade immediate accessibility test to non-blocking warning.
  - Use storage probe/manual backup as the authoritative write validation.

### P0-2: `SafFile` backup path is non-atomic and can corrupt the destination backup
- File refs:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:699`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:705`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:710`
- Problem:
  - Backup writes directly to final file URI (`"w"`) with no temp + finalize phase.
  - Interruption/crash/provider failure can leave a partial file at the same URI.
- Impact:
  - Last backup may be silently replaced with unreadable/corrupt archive.
- Fix direction:
  - Implement two-phase write for file mode as well:
    1. Write temp document.
    2. Verify archive/manifest/checksum.
    3. Replace final only after verification.

### P0-3: Backup snapshot is not transactionally consistent
- File refs:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:558`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:564`
- Problem:
  - Backup reads each table via separate DAO calls without a single read transaction/snapshot boundary.
- Impact:
  - Concurrent writes can produce cross-table inconsistency (orders/items/payments mismatch).
  - Restore may succeed structurally but restore logically inconsistent business data.
- Fix direction:
  - Perform export reads under a consistent snapshot transaction (or pause writes during export window).

### P1-1: Folder picker launch contract is generic and OEM behavior is inconsistent
- File refs:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt:131`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt:175`
- Problem:
  - Uses `StartActivityForResult` + manual intent for tree picker instead of `ActivityResultContracts.OpenDocumentTree()`.
- Impact:
  - On some OEM ROMs the flow can behave like local-first picker UX and hide/obscure providers.
- Fix direction:
  - Switch to `OpenDocumentTree()` contract directly.
  - Keep launch input null for change-folder action to avoid app-side pinning.

### P1-2: Persisted URI grant uses hardcoded flags, not returned flags
- File refs:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt:137`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt:143`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt:203`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt:209`
- Problem:
  - `takePersistableUriPermission()` is called with fixed read/write flags instead of masking `result.data?.flags` grant bits.
- Impact:
  - Some providers/devices can reject persist grant unexpectedly.
- Fix direction:
  - Use granted flags mask:
    - `(intentFlags and (FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_WRITE_URI_PERMISSION))`

### P1-3: `runStorageProbe()` for `SafFile` touches the real backup file
- File refs:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:282`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:287`
- Problem:
  - Probe opens selected backup in `"wa"` mode.
- Impact:
  - Probe may alter metadata/content semantics of the actual backup target.
- Fix direction:
  - Probe against a temporary sibling file (or create/delete dedicated probe URI), never the production backup file.

### P1-4: Restore reads entire ZIP into memory with no entry/size guard
- File refs:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:942`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:957`
- Problem:
  - `readZipEntries()` buffers every entry into memory unbounded.
- Impact:
  - Large/hostile archives can trigger OOM or app kill.
- Fix direction:
  - Add per-entry and total-uncompressed byte limits.
  - Reject zip bombs / oversized archives early.

### P1-5: Force-key mismatch between scheduler and daily worker
- File refs:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupScheduler.kt:20`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/DailyBackupWorker.kt:52`
- Problem:
  - Scheduler key is `"force_backup"`; daily worker key is `"force"`.
- Impact:
  - Any future path expecting shared force key will silently fail.
- Fix direction:
  - Use `BackupScheduler.KEY_FORCE` everywhere.

### P2-1: Initial health in preferences is optimistic for SAF targets
- File refs:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupPreferences.kt:107`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupPreferences.kt:113`
- Problem:
  - Non-empty SAF URI is marked `Healthy` without permission/access checks.
- Impact:
  - UI can briefly display misleading health until re-evaluated by manager.
- Fix direction:
  - Either store `Unknown` state in prefs model or resolve with manager checks before UI render.

### P2-2: Switching to app-private storage clears existing SAF selection
- File refs:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt:477`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt:478`
- Problem:
  - Choosing App storage clears folder/file URI metadata immediately.
- Impact:
  - User loses previously configured cloud target and must re-link later.
- Fix direction:
  - Preserve last SAF selection across mode switches; only clear on explicit "Forget location" action.

## Google Drive-specific diagnosis for current user symptom

### Observed symptom
- Picker opens in local phone folder view (e.g., Oppo storage), Drive not visible.

### Code-level contributors
- Generic picker contract path (`StartActivityForResult`) instead of `OpenDocumentTree` contract in `BackupSettingsScreen.kt:131`.
- Immediate hard accessibility rejection can make valid cloud picks appear broken (`BackupSettingsScreen.kt:154-160`).

### Platform constraint (important)
- Even with correct SAF tree intent, OEM DocumentsUI implementations can still default local-first or hide providers.
- App cannot force Drive provider visibility when OEM picker is limited.

## Required implementation plan
1. Replace folder picker launcher with `ActivityResultContracts.OpenDocumentTree()` and persist only granted flags.
2. Remove blocking `isSafTargetAccessible` gate at selection time; make it warning-only.
3. Make `SafFile` backup atomic (temp -> verify -> finalize).
4. Move backup export reads into a consistent snapshot transaction.
5. Add zip size guards and fail-fast messages for oversized/corrupt archives.
6. Unify force-key constants and add regression tests.
7. Preserve SAF target across location mode switches.

## Test plan updates required
- Add instrumentation tests for:
  - Tree URI selection persistence with returned flags mask.
  - Selection accepted even when immediate provider `exists()` returns false.
  - Atomic saf-file write interruption behavior.
- Add unit tests for:
  - Zip entry size guard logic.
  - Force-key wiring consistency.
  - Health evaluation state transitions (NeedsRelink/Unavailable/Healthy).

## Definition of done for this issue
- User can select/retain cloud target without false negative rejection.
- Backups in SAF folder/file modes are crash-safe and verifiable.
- Restore rejects oversized/invalid archives safely.
- Picker flow is best-effort OEM-compatible and no longer app-side brittle.
