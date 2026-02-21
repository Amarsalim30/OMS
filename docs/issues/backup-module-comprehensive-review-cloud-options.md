# Title: Backup Module + UI Re-Audit (Option B Cloud via SAF)

## Status
In Progress (implementation applied on 2026-02-20)

Update on 2026-02-20:
- `Change backup folder` now launches root-style `OpenDocumentTree` with no initial URI.
- Removed all app-side `EXTRA_INITIAL_URI` usage.
- Added separate `Browse backups` action that lists ZIP files from saved backup folder in-app (no picker re-launch).
- Added package visibility queries in manifest for Drive/DocumentsProvider detection.
- Added fallback target mode `Backup file` (SAF single-file via `CreateDocument`) for devices where folder tree picker does not expose Drive.

## Goal
- Primary goal: user can back up to Google Drive from Backup settings.
- Delivery model: Option B (Android Storage Access Framework folder picker), not direct Google Drive REST API integration.

## New Blocking Issue: Picker opens but Drive is missing

### Observed behavior
- Tapping backup folder pick/change opens picker UI but stays in local device path (example: `Oppo Reno/Download/OrderBookBackup`).
- Google Drive is not visible in picker provider list on affected devices.

### Impact
- Goal "save backup to Google Drive" fails for users on affected picker/ROM behavior.
- User assumes cloud backup is not implemented.

### Likely root causes
1. Picker launch is pinned to a previously saved folder (`EXTRA_INITIAL_URI` or equivalent flow).
2. Wrong picker contract (`OpenDocument`/`GetContent`) instead of `OpenDocumentTree`.
3. OEM picker behavior (ColorOS/Oppo) hides providers unless opened from true provider root.
4. Drive provider unavailable (Drive app not installed/signed in) or hidden by system restrictions.

### Required behavior
1. `Change backup folder` must launch `OpenDocumentTree` with no initial URI to start at provider root.
2. App must separate actions:
- `Change backup folder`: provider/root chooser flow.
- `Browse backups`: open current selected folder content view.
 - `Backup file` mode: select one backup ZIP file destination via SAF file picker when folder picker is limited.
3. App must not auto-force picker into last selected folder for the change-folder action.
4. Troubleshoot UI must explain that Drive visibility depends on system picker/provider availability.

### Platform constraint
- App cannot force Drive to appear if OEM picker/provider integration is limited.
- App can only use SAF correctly and guide fallback/testing.

### Reproduction
1. Open Backup settings.
2. Tap `Change backup folder`.
3. Picker opens directly in local folder content view.
4. Drive provider is absent from visible options.

### Acceptance criteria
1. `Change backup folder` opens provider/root chooser via `OpenDocumentTree` and does not pre-pin local path.
2. On compatible devices, Drive appears under Browse/providers.
3. On devices where Drive is still hidden, app shows actionable troubleshooting (Drive installed/signed in, DocumentsUI enabled, OEM limitation note).
4. `Backup now` continues writing to saved SAF URI without reopening picker.

## Option B Clarification
- The app does not authenticate against Google Drive API directly.
- User selects a folder via `OpenDocumentTree` and backup files are written through a SAF `content://` URI.
- Google Drive works when the Drive app exposes a DocumentsProvider and the user selects Drive in system picker Browse.

## Implemented in this pass

### Backup safety and restore hardening
- Added strict restore preflight with manifest/checksum policy support (`Strict` vs `LegacyCompatible`).
- Added manifest generation/checksums for produced archives.
- Added latest-pointer strategy for selected target.
- Added safer SAF write/verify/finalize flow and backup storage probe.

### SAF target reliability and diagnostics
- Added target health model: `Healthy`, `NeedsRelink`, `Unavailable`.
- Added persisted URI read/write permission checks and SAF accessibility checks.
- Added Drive troubleshooting report:
  - Drive installed/enabled check
  - DocumentsUI enabled check
  - selected URI authority
  - persisted permission validity
  - managed profile signal
  - guided next steps list

### Automatic backup resilience
- Daily scheduling now depends on healthy target.
- Repeated auto-backup failures are counted and persisted.
- Added persistent backup-attention notification channel + notification with direct action to Backup screen.
- Success path resets failure count and clears backup-attention notification.

### Backup UI changes
- Folder picker flow now guards `takePersistableUriPermission` and validates selected folder before saving target.
- Backup Location now shows:
  - folder label
  - provider authority
  - target health
  - Drive picker guidance text
- Added warning card when SAF target needs re-link/unavailable.
- Added `Troubleshoot Drive` dialog with diagnostics + actions.
- Added restore manifest policy controls in UI (`Strict` / `Legacy compatible`) with warning/info messaging.
- Manual backup now blocks SAF run when target is unhealthy and redirects user to re-pick folder.

### Navigation and notifications plumbing
- Added `ACTION_SHOW_BACKUP` app intent handling to route from notification to backup screen.
- Added `backup_attention` notification channel.

## Google Drive "picker shows local only" handling now covered
- In-app guidance added before/near picker usage:
  - install/open/sign in to Drive app
  - use Browse in picker to find Drive
- Troubleshoot flow surfaces likely causes:
  - Drive app missing/disabled
  - DocumentsUI disabled
  - lost URI permission
  - work-profile policy restrictions

## Known scope boundary (still true)
- This is still SAF-folder backup, not provider API integration with account-level cloud sync/history.
- Reliability remains dependent on provider DocumentsProvider behavior.

## QA Acceptance Checks
1. Select Drive folder via `OpenDocumentTree`, run test write, and run manual backup successfully.
2. Verify provider authority and health are visible in Backup UI.
3. Revoke URI permission / clear app data and confirm health becomes `Needs re-link`.
4. Force repeated auto failures and confirm persistent backup-attention notification appears.
5. Switch to strict manifest policy and verify restore rejects archives without manifest.
6. Open `Troubleshoot Drive` and verify diagnostics/guidance render correctly.

## Files touched
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupModels.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupPreferences.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupScheduler.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/DailyBackupWorker.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/ManualBackupWorker.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupAttentionNotifier.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/AppIntents.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/MainAppContent.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/notifications/NotificationChannels.kt`
- `app/src/main/res/values/strings.xml`
