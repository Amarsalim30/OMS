# Architecture Review

## Current architecture snapshot
- Single Android app module with feature-first packages: `order`, `customer`, `accounting`, and `core`.
- Single-activity Compose app with an AuthGate and licensing validation before the main navigation shell.
- Navigation is already split across:
  - `MainAppContent.kt`
  - `MainAppHostScaffold.kt`
  - `AppFeatureNavHost.kt`
  - `AppNavigationHelpers.kt`
  - `core/navigation/graphs/*`
- Room and DAO persistence remain the main data backbone.
- Background scheduling exists for notifications, contacts sync, and backup scheduling.

## Strengths
- Package boundaries broadly follow product domains and are still understandable for a single-module app.
- The app shell is not a pure monolith anymore; navigation graphs and scaffold concerns have already been split out.
- Licensing and entitlement validation are isolated into `core/licensing`, which is a healthier boundary than mixing them into UI code.
- ADR and requirements documentation already exists for several higher-risk flows.

## Architectural concerns

### A1. `BackupManager.kt` is still the highest-risk mixed-responsibility class
- Severity: high
- Files: `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt`
- Production impact: backup archive creation, crypto, manifest handling, restore mapping, and error policy remain tightly coupled, which makes regressions hard to isolate.
- Recommendation: extract collaborators in this order:
  1. archive and metadata I/O
  2. crypto codec and key material handling
  3. restore mapping and validation
  4. orchestration and user-facing result mapping
- Implement timing: later
- Change risk: medium-high

### A2. `HelperOverlayService.kt` still combines multiple lifecycle-sensitive responsibilities
- Severity: high
- Files: `app/src/main/java/com/zeynbakers/order_management_system/core/helper/HelperOverlayService.kt`
- Production impact: overlay lifecycle, microphone or audio handling, and command parsing are coupled into one long-lived service, which is a bad place for subtle regressions.
- Recommendation: extract command parsing, permission state checks, and overlay view coordination into explicit collaborators before adding new helper features.
- Implement timing: later
- Change risk: medium-high

### A3. `MainAppContent.kt` remains large, but the original March audit overstated the problem
- Severity: medium
- Files:
  - `app/src/main/java/com/zeynbakers/order_management_system/MainAppContent.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/MainAppHostScaffold.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/AppFeatureNavHost.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/graphs/*`
- Production impact: `MainAppContent.kt` still owns a wide state surface and several side effects, but navigation shell responsibilities have already been partially decomposed.
- Recommendation: continue incremental extraction around side-effect handlers, permission coordinators, and shared-intent routing rather than rewriting the whole app shell.
- Implement timing: later
- Change risk: medium

### A4. Startup resilience is in a better state than the initial audit suggested
- Severity: resolved in this pass
- Files: `app/src/main/java/com/zeynbakers/order_management_system/MainActivity.kt`
- Production impact: startup now has a safer fallback if onboarding-state reads fail.
- Recommendation: keep the fail-safe startup path and add direct tests when a practical harness is available.
- Implement timing: already implemented
- Change risk: low

## Recommended target architecture
1. Keep the single-module structure for now. The current risk is inside oversized classes, not in the module graph.
2. Continue extracting high-risk collaborators from backup and helper subsystems before touching lower-risk screens.
3. Preserve the current navigation graph split and shrink `MainAppContent.kt` by moving side-effect handling behind narrower helpers.
4. Add tests before each high-risk extraction so the refactor remains production-safe.

## Status after this pass
- Completed:
  - Startup fallback hardening in `MainActivity.kt`.
  - Additional parser tests and resource-access lint cleanup.
  - Removal of dead legacy order UI files.
- Still deferred:
  - `BackupManager.kt` modularization.
  - `HelperOverlayService.kt` modularization.
  - Further `MainAppContent.kt` state-surface reduction.

## Decision
No big rewrite is justified in this repository today. The correct path remains incremental refactoring with verification after each extraction.
