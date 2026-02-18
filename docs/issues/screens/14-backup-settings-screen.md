# Backup Settings Screen UI/UX Issues

Status: Open

## Scope
- Route: `backup`
- Primary file: `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupSettingsScreen.kt`

## Owner goals
- Ensure backup is always active.
- Recover data safely when needed.
- Understand current protection status instantly.

## Open issues

### P1
- Restore actions are high-risk and need stronger irreversible-action messaging.
- Backup destination behavior needs clearer plain-language explanation.
- Progress and completion signals are present but can be more prominent.

### P2
- No simple health score (protected/unprotected) indicator.
- No quick checklist for first-time setup.

## Acceptance criteria
- Owner can confirm protection status in under 2 seconds.
- Restore flow clearly warns and confirms destructive impact.
- Backup location and schedule are unambiguous.
