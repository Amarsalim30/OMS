# Refactor Roadmap

## Principles
- No big-bang rewrites.
- Preserve behavior and release confidence while reducing risk incrementally.
- Add or expand verification before each high-risk extraction.
- Prioritize the files with the worst maintenance-to-risk ratio first.

## Milestones

### R1. Startup and gating resilience
- Status: completed on 2026-03-06
- Scope: app startup path, onboarding-state fallback, deterministic gating behavior
- Outcome:
  - `MainActivity.kt` now fails into a deterministic route if onboarding-state reads fail.
  - Startup logging baseline exists.
- Follow-up: add direct tests for startup fallback when a practical harness is available.

### R2. Main app shell decomposition
- Status: partially complete
- Scope: reduce the breadth of `MainAppContent.kt` without rewriting navigation
- Current state:
  - `MainAppHostScaffold.kt`, `AppFeatureNavHost.kt`, `AppNavigationHelpers.kt`, and `core/navigation/graphs/*` already hold part of the shell and navigation responsibilities.
  - `MainAppContent.kt` still owns a large state surface and several side effects.
- Next step: continue extracting side-effect-heavy coordination rather than redesigning the shell.

### R3. BackupManager modularization
- Status: deferred and still high priority
- Scope: split archive I/O, crypto, metadata validation, restore mapping, and orchestration
- Outcome target: safer maintenance, easier testing, clearer dispatcher boundaries
- Blocking consideration: this should be paired with stronger device-backed backup or restore verification.

### R4. Helper overlay service hardening
- Status: deferred and still high priority
- Scope: isolate lifecycle management, command parsing, audio handling, and overlay UI coordination
- Outcome target: lower lifecycle regression risk and clearer permission handling
- Blocking consideration: permission and microphone flows should be validated on-device as part of the extraction.

### R5. UX consistency and repository hygiene
- Status: partially complete on 2026-03-06
- Scope: remove stale code, reduce confusion in live flows, and improve correctness in order entry
- Completed this pass:
  - fixed the order-entry customer suffix parser
  - expanded parser regression tests
  - removed dead legacy order UI files
  - removed a stray checked-in log artifact
- Remaining work: continue converging loading, empty, error, and accessibility patterns in critical flows.

### R6. Observability and release hygiene
- Status: partially complete on 2026-03-06
- Scope: add safe operational logging and restore a reliable verification baseline
- Completed this pass:
  - added sanitized logs around licensing validation and backup or restore execution
  - verified debug unit tests, debug lint, debug assembly, release lint, and release assembly locally
- Remaining work:
  - device-backed acceptance validation
  - release-signed Google Sign-In verification
  - central crash or telemetry strategy if the product needs richer operational monitoring

## Recommended execution order from here
1. Clear the remaining device-backed acceptance blockers.
2. Modularize `BackupManager.kt` with tests.
3. Modularize `HelperOverlayService.kt` with device-backed permission validation.
4. Continue shrinking `MainAppContent.kt` opportunistically as adjacent work touches it.

## Deferred items
- Multi-module split. The current problems are still better explained by oversized classes than by a missing module boundary.
