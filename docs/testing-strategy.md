# Testing Strategy

## Current state
- `./gradlew.bat :app:testDebugUnitTest --console=plain --stacktrace` passed on 2026-03-06.
- `./gradlew.bat :app:lintDebug --console=plain --stacktrace` passed on 2026-03-06.
- `./gradlew.bat :app:assembleDebug --console=plain --stacktrace` passed on 2026-03-06.
- `./gradlew.bat :app:lintRelease --console=plain --stacktrace` passed on 2026-03-06.
- `./gradlew.bat :app:assembleRelease --console=plain --stacktrace` passed on 2026-03-06.
- Parser regression coverage was expanded in `OrderEditorInputTest.kt`.
- `:app:connectedDebugAndroidTest` was not run because no device or emulator was available in this pass.
- The manual licensing checklist in `docs/requirements/licensing-verification-checklist.md` remains the source of truth for P0 auth and entitlement validation.

## Risk-based strategy

### Tier 1: must stay green for every release candidate
- `:app:testDebugUnitTest`
- `:app:lintDebug`
- `:app:assembleDebug`
- `:app:lintRelease`
- `:app:assembleRelease`

### Tier 2: device-backed integration coverage
- `:app:connectedDebugAndroidTest` on a stable emulator or device.
- Focus suites and manual checks:
  - auth and licensing gate flow
  - release-signed Google Sign-In
  - backup or restore
  - payment allocation and history flows
  - overlay and microphone permission flows

### Tier 3: manual acceptance checklist
- Execute every item in `docs/requirements/licensing-verification-checklist.md`.
- Run a realistic backup and restore cycle with representative production-like data.
- Validate critical navigation expectations across bottom-nav and top-level destinations.
- Verify accessibility basics on the core task flows.

## Gaps and actions

### T1. Device-backed licensing and release-signing validation is still open
- Severity: critical
- Affected areas: Firebase auth, entitlement validation, install-UUID device binding, offline grace, release-signed Google Sign-In
- Fix: run the manual checklist on a real device with production signing fingerprints configured.
- Implement: blocked in this environment
- Risk: low to code, high to release if skipped
- Status: open

### T2. Backup and restore still need stronger integration coverage
- Severity: high
- Affected areas: `core/backup/BackupManager.kt` and related restore flows
- Fix: add deterministic fixture-driven device or instrumentation coverage where practical, backed by a manual restore script for final sign-off.
- Implement: later
- Risk: medium
- Status: open

### T3. Startup and licensing fallback paths still deserve direct tests
- Severity: medium
- Affected areas: startup routing, onboarding failure fallback, licensing retry or blocked states
- Fix: add focused tests for deterministic fallback behavior and common failure mapping when a practical harness is available.
- Implement: later
- Risk: low
- Status: open

### T4. CI already covers key release checks, but not connected tests
- Severity: medium
- Affected files: `.github/workflows/android-ci.yml`
- Fix: keep the existing unit, lint-release, and assemble-release workflow, then add a connected-test or nightly device validation lane when infrastructure is available.
- Implement: later
- Risk: medium
- Status: open
