# Production Readiness Completion Plan

Date: 2026-03-06
Scope: Complete the remaining realistic production-readiness work without broad rewrites or stack churn.

## Goals
- Restore a green local verification baseline for debug and release builds.
- Fix correctness issues in high-frequency order-entry flows.
- Remove stale code and stray artifacts that no longer belong in the live app.
- Add minimal sanitized observability around licensing and backup or restore operations.
- Leave the roadmap and audit documents aligned with the actual repository state.

## Non-goals
- No framework migration or multi-module rewrite.
- No broad redesign of backup, overlay, or payment flows.
- No fake verification claims for device-only or release-signing scenarios.

## Ordered milestones

### Milestone 1: Green baseline first
- Status: completed on 2026-03-06.
- Completed work:
  - Tightened `extractCustomerQueryFromNotes` so customer typeahead only activates from a true trailing customer suffix.
  - Expanded `OrderEditorInputTest` coverage for amount-only tails and trailing customer suffix removal.
  - Replaced configuration-unsafe resource access in `MainAppContent.kt` and `core/navigation/graphs/CustomersGraph.kt` with configuration-aware resource reads.
- Acceptance criteria: met.
- Validation commands:
  - `./gradlew.bat --stop`
  - `./gradlew.bat :app:testDebugUnitTest --console=plain --stacktrace` -> passed
  - `./gradlew.bat :app:lintDebug --console=plain --stacktrace` -> passed
  - `./gradlew.bat :app:assembleDebug --console=plain --stacktrace` -> passed
- Rollback or risk notes:
  - Low risk. The parser change narrows behavior only for false-positive customer suffix detection.
  - The lint fixes do not change product behavior; they align Compose resource access with Android lint requirements.

### Milestone 2: Dead code and repo hygiene
- Status: completed on 2026-03-06.
- Completed work:
  - Removed unreferenced legacy order UI files `order/ui/OrderEditor.kt` and `order/ui/OrderCard.kt`.
  - Deleted the checked-in raw log artifact at `app/src/main/java/com/zeynbakers/order_management_system/accounting/logs`.
  - Kept the live order-entry flow in `OrderEditorInput.kt` and `OrderEditorSheet.kt` untouched apart from the parser fix above.
- Acceptance criteria: met.
- Validation commands:
  - `./gradlew.bat :app:testDebugUnitTest --console=plain --stacktrace` -> passed
  - `./gradlew.bat :app:lintDebug --console=plain --stacktrace` -> passed
  - `./gradlew.bat :app:assembleDebug --console=plain --stacktrace` -> passed
- Rollback or risk notes:
  - Low risk. Removed files were not part of the live navigation flow.

### Milestone 3: Targeted observability and release hardening
- Status: completed on 2026-03-06.
- Completed work:
  - Added sanitized licensing validation logs in `core/licensing/LicensingRepository.kt` for start, success, blocked reason, retryable failure, and fatal failure.
  - Added sanitized backup and restore logs in `core/backup/BackupManager.kt` for start, blocked reason, success, retryable failure, and fatal failure.
  - Kept logging limited to stable reason codes and operation state. No shared text, tokens, phone numbers, emails, passphrases, or raw Firestore payloads are logged.
- Acceptance criteria: met.
- Validation commands:
  - `./gradlew.bat :app:testDebugUnitTest --console=plain --stacktrace` -> passed
  - `./gradlew.bat :app:lintRelease --console=plain --stacktrace` -> passed
  - `./gradlew.bat :app:assembleRelease --console=plain --stacktrace` -> passed
- Rollback or risk notes:
  - Low risk. Logging is additive and deliberately sanitized.

### Milestone 4: Documentation reconciliation
- Status: completed on 2026-03-06.
- Completed work:
  - Rewrote `plans.md`, `implementation-log.md`, and the production-readiness review documents to match the implemented state.
  - Removed stale claims that debug lint and release verification were blocked by the earlier `25.0.1` toolchain issue.
  - Recorded the remaining blockers and unverifiable items explicitly instead of leaving them implied.
- Acceptance criteria: met.
- Validation commands:
  - `git status --short`
- Rollback or risk notes:
  - No runtime risk. Documentation only.

## Acceptance summary
- All realistic milestones from this pass are complete.
- Local debug verification is green.
- Local release lint and release assembly are green.
- Remaining work is now limited to environment-dependent validation and larger deferred refactors.

## remaining blockers
- No device or emulator was available in this pass, so `:app:connectedDebugAndroidTest` was not run.
- The manual licensing P0 checklist in `docs/requirements/licensing-verification-checklist.md` is still open, including release-signed Google Sign-In and offline grace validation.
- `BackupManager.kt` and `HelperOverlayService.kt` remain the two largest maintenance hotspots and still need staged decomposition before major future feature work.
- Firebase operational controls such as API key restrictions, signing fingerprint governance, and rules deployment discipline depend on project configuration outside this repository and were not verifiable from code alone.
