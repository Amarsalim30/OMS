# Backup System Complete Reference (UI, UX, Features, Implementation)

## Status
Active reference

## Snapshot Date
2026-02-22

## Purpose
This document is the single clean reference for how backup and restore currently work in the app, including:
- User-facing UX and behavior
- Data coverage and reliability guarantees
- Background scheduling and notifications
- Onboarding integration
- Current limitations and code-level gaps

## Scope (Primary Source Files)
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupScheduler.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/ManualBackupWorker.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/DailyBackupWorker.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/RestoreWorker.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupPreferences.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupAttentionNotifier.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/graphs/OnboardingGraph.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/graphs/SettingsGraph.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/AppIntents.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/MainAppContent.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`
- `app/src/test/java/com/zeynbakers/order_management_system/core/backup/BackupManagerTest.kt`

## What Users Can Do Today
- Choose one backup file via Android system file picker (`CreateDocument`).
- Run manual backup immediately.
- Enable automatic daily backup.
- Run backup location test write/check.
- Restore from a selected backup file or from latest backup source.
- Preview restore metadata before confirming destructive restore.
- Receive persistent backup-attention notification after repeated auto-backup failures.

## End-to-End UX Flow

### 1) Initial Setup (Onboarding)
- On setup checklist, user taps backup action.
- App launches `CreateDocument("application/octet-stream")`.
- If URI permission is persisted, app saves this URI as `SafFile` target, auto-enables backups, and schedules daily work.
- This is currently a file-first flow (not folder-first).

### 2) Backup Settings Screen (`backup` route)
- Storage card:
  - Shows selected target label and provider.
  - Lets user choose/change backup file.
  - Lets user run storage probe.
- Backup now card:
  - Shows last backup, next scheduled time, latest file label.
  - Contains auto daily switch and manual run button.
  - Shows progress when backup worker is running.
- Restore card:
  - Lets user choose restore file.
  - Builds preview first.
  - Shows destructive warning and confirmation dialog.
  - Shows restore progress.

### 3) Notification -> Backup Screen
- Repeated auto-backup failures trigger an ongoing notification.
- Notification opens app with `ACTION_SHOW_BACKUP`.
- `MainAppContent` routes directly to `AppRoutes.Backup`.

## Storage Modes and File Behavior

### AppPrivate
- Stored in app internal directory `files/backups`.
- Archive filename is timestamped (`oms-backup-yyyyMMdd-HHmmss.oms`).
- Retention keeps latest 7 archives.
- Latest pointer metadata file is maintained.

### SafFile (current UI primary mode)
- User chooses a single SAF file URI.
- Every backup overwrites that same URI in-place.
- This keeps cloud storage growth minimal.
- Latest label resolves from selected file display name.

### SafDirectory (supported in core, not directly exposed in current settings UI picker)
- Backup file name is rolling single file: `backup_latest.oms`.
- Write is safe two-phase with partial file and verification.
- After success, old backup archives in that folder are deleted and only the rolling archive is kept.
- Latest pointer file is also maintained for folder mode.

## Backup Data Coverage (Custom Backup Archive)
Included:
- `customers`
- `orders`
- `order_items`
- `account_entries`
- `payments`
- `payment_receipts`
- `payment_allocations`
- `helper_notes`
- metadata and manifest entries

Legacy-compat entry accepted:
- `mpesa_transactions` (legacy import path)

Not included in custom `.oms` payload:
- Most shared preferences (onboarding state, notification settings, etc.)
- UI state and non-DB transient data

## Archive Format
- File extension: `.oms`
- Container: ZIP
- Entry files:
  - `metadata.json`
  - `customers.json`
  - `orders.json`
  - `order_items.json`
  - `account_entries.json`
  - `payments.json`
  - `payment_receipts.json`
  - `payment_allocations.json`
  - `helper_notes.json`
  - optional `mpesa_transactions.json`
  - `manifest.json`

Metadata includes:
- export timestamp
- app version
- DB version
- per-entry row counts (`counts`)

Manifest includes:
- format version
- per-entry SHA-256 checksum
- per-entry size

## Reliability and Data Integrity Guarantees

### Export-time consistency
- Snapshot reads are executed inside a single DB transaction for cross-table consistency.

### Export self-validation
- Produced archive is immediately re-opened and strict-validated before write success path continues.

### Write safety
- App-private and SAF-directory writes use partial-then-finalize strategy.
- SAF-file write verifies by readback fingerprint with retries.
- SAF-file path includes rollback bytes path when write fails before completion.

### Restore preflight checks
- Archive cannot be empty.
- Required entries must exist.
- Required payload entries must parse as JSON arrays.
- Backup DB version must be <= current app DB version.
- Manifest is required in strict mode; checksum and size are verified.
- Metadata `counts` (if present) must match actual payload row counts.
- Cross-table reference graph is validated (missing references are rejected).
- IDs are validated for positivity and uniqueness per entity set.
- ZIP defensive limits:
  - max per entry: 64 MB
  - max total uncompressed: 256 MB

### Restore apply safety
- Restore is transactional and clears all tables then inserts parsed entities.
- Post-restore SQL table counts are verified against expected parsed counts before commit.

## Restore Semantics
- Restore replaces all current app data in DB tables listed above.
- UI shows preview (source name, export time, DB version, app version, selected counts) before final confirm.
- Legacy MPESA backups are mapped into modern receipts/allocations when direct modern entries are absent.
- UI message advises restarting app after restore completion.

## Scheduling and Background Behavior
- WorkManager jobs:
  - `daily_backup` (periodic, 24h)
  - `manual_backup` (one-time)
  - `restore_backup` (one-time)
- Constraints for daily backup:
  - battery not low
  - storage not low
  - no network requirement
- Manual backup is forced.
- `MainAppContent` calls `BackupScheduler.ensureScheduled()` on app launch.

## Auto-Failure Handling and Alerting
- Daily worker increments consecutive failure count when health is bad or backup fails.
- On success, failure count resets.
- At 3 consecutive auto failures, app posts an ongoing backup attention notification.
- Notification channel: `backup_attention`.
- Alert requires `POST_NOTIFICATIONS` permission to be granted.

## Permissions and Platform Requirements
- No broad storage permission is used for custom backup.
- SAF URI permissions are persisted from picker grant.
- Required manifest/runtime aspects:
  - `POST_NOTIFICATIONS` for attention alerts
  - persisted SAF read/write permission for selected backup target
- Backup settings and onboarding file pickers rely on system Documents provider.

## Navigation and Entry Points
- Main route: `AppRoutes.Backup`
- Opened from settings graph.
- Also reachable by notification deep-link action via `ACTION_SHOW_BACKUP`.

## Coexistence With Android System Backup
- App manifest has `allowBackup=true`.
- `backup_rules.xml` and `data_extraction_rules.xml` include DB files and `backup_prefs.xml`.
- This Android system backup path is separate from custom `.oms` export/restore flow.

## Current UX/Implementation Gaps
- Backup settings UI is file-first and does not currently expose a folder picker action, even though core supports `SafDirectory`.
- Some backup-related strings and enums from earlier folder/troubleshoot variants remain in resources/models but are not surfaced in the current screen.
- Restore manifest policy exists in preferences/models, but current screen does not expose policy switching controls.
- Backup settings subtitle is Drive-focused text even when target type is app-private.

## Production QA Checklist
- Verify onboarding backup file selection persists URI permission.
- Verify manual backup success on:
  - AppPrivate target
  - SafFile target
  - SafDirectory target when present in state
- Run two consecutive backups for SafFile and confirm same target file is overwritten.
- Run two consecutive backups for SafDirectory and confirm only rolling file remains (`backup_latest.oms`).
- Verify restore preview shows expected metadata and counts.
- Verify restore on valid archive succeeds and data is replaced.
- Verify corrupted archive fails preflight (manifest/count/reference checks).
- Verify daily auto backup scheduling toggles on/off with switch and health.
- Verify attention notification appears after repeated auto failures and opens backup screen.

## Existing Automated Tests
- `BackupManagerTest` covers:
  - decimal parsing
  - retryable exception mapping
  - ZIP entry size bounds
  - checksum helper stability
  - rolling filename behavior for SAF directory
  - restore preflight rejection for metadata count mismatch
  - restore preflight rejection for broken references

## Summary
Current backup module is reliability-focused and production-safe for data integrity:
- transactional export/import
- archive manifest + checksum verification
- metadata row-count verification
- cross-table reference validation
- post-restore persisted row-count verification

Main product gap is UX consistency: core supports more target modes than the current settings UI exposes. The next UX pass should align visible controls with supported runtime capabilities.
