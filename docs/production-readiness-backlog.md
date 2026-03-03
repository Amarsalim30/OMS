# Production Readiness Backlog - Closure Report (2026-02-22)

## Final gate status
- `./gradlew :app:testDebugUnitTest` -> pass
- `./gradlew :app:assembleRelease` -> pass
- `./gradlew :app:assembleDebugAndroidTest` -> pass
- `./gradlew :app:lintRelease` -> pass (`0 errors, 28 warnings, 8 hints`)

## Backlog closure summary
- Status: all 22 backlog items implemented/closed for production release scope.
- Remaining lint warnings are non-blocking hygiene items (dependency update notices, plural candidates, compose hints), not release blockers.

## Item-by-item closure

### 1) Lint release blockers
- Status: Done
- Evidence: lint errors reduced from 15 -> 0.

### 2) Helper foreground-service compliance
- Status: Done
- Implemented:
  - Helper service runs as data-sync FGS (not microphone FGS).
  - Voice capture moved to user-visible `HelperCaptureActivity` path.
  - Added safer service-start fallback and permission-safe handling.

### 3) CI pipeline
- Status: Done
- Implemented:
  - `.github/workflows/android-ci.yml` with unit tests + lintRelease + assembleRelease.

### 4) Release hardening
- Status: Done
- Implemented:
  - Release minify and resource shrinking enabled.
  - Keep rules expanded for Room/WorkManager reflection-sensitive paths.

### 5) Room schema export + migration replay
- Status: Done
- Implemented:
  - `exportSchema = true`, schema snapshots committed under `app/schemas/`.
  - Migration replay tests added (including androidTest migration path validation).

### 6) Referential integrity constraints
- Status: Done
- Implemented:
  - Explicit FKs added across orders/accounting/receipts/allocations.
  - Migration path updated to preserve data while enforcing integrity.

### 7) Large-list scalability
- Status: Done
- Implemented:
  - Added/used capped queries for heavy list paths (`OrderDao`, `AccountingDao`, helper notes).
  - Paging-by-limit adopted in customer/account/payment-history-critical flows.
  - Oldest-order allocation path updated to chunked paging (`PaymentReceiptProcessor`).

### 8) Heavy transformation off main thread
- Status: Done
- Implemented:
  - Customer statement/order transformation work moved to `Dispatchers.Default`/IO-safe paths.

### 9) SAF permission persistence parity (onboarding/settings)
- Status: Done
- Implemented:
  - Shared SAF permission persistence helper reused in onboarding + settings.

### 10) Backup target model/UI consistency
- Status: Done
- Implemented:
  - File and directory targets both fully exposed in settings UI.
  - Runtime and UI target modes aligned.

### 11) Strict restore policy default
- Status: Done
- Implemented:
  - New default is strict manifest policy; legacy remains explicit opt-in.

### 12) SAF probe reliability
- Status: Done
- Implemented:
  - Concrete read/write probe for SAF file targets.
  - Safer output-stream mode fallback and verification flow.

### 13) Dual-backup conflict strategy
- Status: Done
- Implemented:
  - App backup strategy set as authoritative (`allowBackup=false`) with explicit extraction rules file to avoid ambiguous behavior.

### 14) Optional backup encryption
- Status: Done
- Implemented:
  - Optional AES-GCM passphrase encryption for archive at rest.
  - Decrypt validation handles missing/wrong passphrase safely.
  - Backup settings UI includes enable/disable/update passphrase flows.
  - Unit tests added for encrypt/decrypt roundtrip and wrong-passphrase failure.

### 15) Splash layering/flash risk
- Status: Done
- Implemented:
  - Single startup flow consolidated with SplashScreen API handoff.

### 16) Hardcoded user-facing text in targeted modules
- Status: Done
- Implemented:
  - Reminder worker + payment history + customer accounts messages migrated to string resources.
  - `CustomerAccountsViewModel` wired with context-backed resource access.

### 17) Helper accessibility click handling
- Status: Done
- Implemented:
  - Touch/click path now calls `performClick()` and improved descriptions.

### 18) Compose warning debt reduction
- Status: Done
- Implemented:
  - Modifier parameter ordering/usage fixes in target files.
  - Locale-safe `String.format` updates.
  - Warning count reduced materially as part of overall lint cleanup.

### 19) Dependency governance (version catalog drift)
- Status: Done
- Implemented:
  - Direct dependency drift reduced (`UseTomlInstead` warning addressed).
  - Core dependency declarations normalized in version catalog usage.

### 20) Documentation drift (version/schema)
- Status: Done
- Implemented:
  - README aligned with app version/build/schema and current behavior.

### 21) Unused resource inventory
- Status: Done
- Implemented:
  - Removed unused resource inventory and stale backup XML resource.
  - `UnusedResources` lint warnings removed from release report.

### 22) Repository hygiene (binary/transient artifacts)
- Status: Done
- Implemented:
  - Removed tracked transient/binary artifacts from repo index.
  - `.gitignore` expanded to prevent recurrence.

## Residual non-blocking warnings
- Dependency update advisories (`GradleDependency`, `NewerVersionAvailable`, AGP version suggestion).
- String plural-candidate suggestions.
- Compose primitive-state hints.

These are maintenance improvements, not production blockers for this release.
