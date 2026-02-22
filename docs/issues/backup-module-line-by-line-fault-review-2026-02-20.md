# Title: Backup Module Final Fault Review (Refreshed 2026-02-22)

## Status
Final code review completed against current implementation.

- Core backup integrity pipeline is strong.
- Remaining issues are mainly configuration/UX consistency and SAF edge-case robustness.

## Scope Reviewed
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupPreferences.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupScheduler.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/DailyBackupWorker.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/ManualBackupWorker.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/RestoreWorker.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/graphs/OnboardingGraph.kt`
- `docs/issues/backup-system-complete-reference-2026-02-22.md`

## Open Findings (ordered by severity)

### P1-1: Onboarding SAF persistable permission flow is weaker than settings flow
- File refs:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/graphs/OnboardingGraph.kt:163`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/graphs/OnboardingGraph.kt:170`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt:823`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt:833`
- Problem:
  - Onboarding directly persists fixed `READ|WRITE` flags.
  - Settings correctly masks granted flags and retries with required flags.
- Impact:
  - On some providers/OEMs, onboarding can silently fail to persist URI access and backup setup may not stick.
- Fix direction:
  - Extract shared SAF permission persistence helper and use it in both onboarding and settings.

### P1-2: Runtime supports `SafDirectory`, but settings UI exposes only file picker path
- File refs:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupPreferences.kt:59`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt:123`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt:138`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt:771`
- Problem:
  - Core model and manager still support `BackupTargetType.SafDirectory`.
  - Settings only uses `CreateDocument` and always writes `SafFile` via `setFileTargetSelection`.
  - `setTargetSelection(...)` has no active call path.
- Impact:
  - Capability/UX mismatch and maintenance drift.
  - Folder mode reliability improvements exist in core but are not user-configurable from screen.
- Fix direction:
  - Either remove folder mode from runtime model, or re-introduce folder mode controls (`OpenDocumentTree`) and target mode selector.

### P1-3: Restore policy defaults to `LegacyCompatible` with no in-screen control
- File refs:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupPreferences.kt:29`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupPreferences.kt:103`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:246`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:657`
- Problem:
  - Policy is implemented in core, but settings UI does not expose policy choice.
  - Default remains `LegacyCompatible`.
- Impact:
  - Restore posture is weaker than strict-by-default hardening intent.
- Fix direction:
  - Set default to `Strict` for new installs and expose explicit policy UI with warning copy for legacy mode.

### P1-4: `SafFile` health/probe can report healthy from permission fallback even when URI is stale
- File refs:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:354`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:373`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:378`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:400`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:280`
- Problem:
  - `isSafFileWritable()` returns true if persisted read/write permission exists, even when descriptor open fails.
  - `evaluateTargetHealth()` and `runStorageProbe()` rely on that.
- Impact:
  - UI can show healthy/test-passed while actual backup write later fails.
- Fix direction:
  - For health/probe, require at least one concrete I/O capability signal (descriptor or short write/read check) instead of permission fallback alone.

### P2-1: `SafFile` post-write verification failure intentionally skips rollback
- File refs:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:851`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:873`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:885`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:935`
- Problem:
  - Once write is marked complete, rollback path is skipped even if subsequent verification fails.
- Impact:
  - Rare provider inconsistency can leave selected file in uncertain state.
- Fix direction:
  - Where supported, write to temp sibling and only replace on verified success.
  - If not possible, surface stronger user warning and immediate retry guidance.

### P2-2: Output-stream fallback chain is not exception-safe per mode attempt
- File refs:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:894`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:928`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:995`
- Problem:
  - `openSafOutputStreamForWrite()` tries `"wt" ?: "w" ?: default`, but if first call throws, later fallbacks are not attempted in that method.
- Impact:
  - Some providers that reject one mode could fail backup even though a fallback mode would work.
- Fix direction:
  - Attempt each mode in separate `runCatching` blocks and continue to next mode on failure.

### P2-3: Backup settings copy/labels are file-centric across all target types
- File refs:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt:349`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt:354`
  - `app/src/main/res/values/strings.xml:526`
  - `app/src/main/res/values/strings.xml:527`
- Problem:
  - Subtitle and primary label always describe selected file behavior.
  - This is inaccurate when target is `AppPrivate` and ambiguous for dormant `SafDirectory` state.
- Impact:
  - User can misunderstand where backups are saved and how updates happen.
- Fix direction:
  - Render target-specific labels and explanatory text.

### P2-4: App-private mode discoverability/switching is weak in settings UX
- File refs:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt:288`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt:386`
  - `app/src/main/res/values/strings.xml:454`
- Problem:
  - Screen logic supports `AppPrivate`, but primary action is always "choose file" and no explicit mode switch is exposed.
  - String resources for app-storage switch exist but are not surfaced in this screen.
- Impact:
  - Non-technical users may not realize local app storage backup mode is available.
- Fix direction:
  - Add explicit storage mode picker (App storage vs Backup file), with clear mode status and implications.

## Resolved Findings (verified in current code)

### Resolved: transactional export snapshot
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:686`

### Resolved: ZIP size guardrails (entry and total)
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:73`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:1441`

### Resolved: force key mismatch
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupScheduler.kt:21`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/DailyBackupWorker.kt:13`

### Resolved: preflight and restore integrity hardening
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:1212`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:1233`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:1413`

### Resolved: folder backup growth (rolling single-file behavior)
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:63`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:509`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt:625`

## Alignment Check vs `backup-system-complete-reference-2026-02-22.md`
- The reference doc is correct on architecture and major reliability behavior.
- It should keep explicitly calling out these still-open items:
  - onboarding permission persistence parity
  - `SafDirectory` model/UI mismatch
  - restore policy default and missing UI control
  - health/probe false-positive risk for stale SAF file URIs
  - target-specific UX copy cleanup

## Recommended Execution Order
1. Unify SAF permission persistence logic (onboarding + settings).
2. Resolve mode mismatch (`SafDirectory` deprecate or expose).
3. Switch restore default to `Strict` and surface policy control.
4. Fix `isSafFileWritable`/probe false-positive behavior.
5. Improve `writeSafFileBytes` output-stream fallback robustness.
6. Complete target-specific copy and mode switch UX.

## Definition of Done
- Onboarding and settings persist SAF grants with the same provider-safe logic.
- Backup target modes exposed in UI exactly match runtime capabilities.
- Restore strictness is visible and defaults to secure behavior.
- Health/probe status reflects real write capability, not permission-only fallback.
- SAF file write fallback path is resilient across provider mode differences.
- Backup screen wording matches active storage target.
