# Architecture Review

Date: 2026-03-06 (refresh)

## Current architecture
- Single-module Android app (`app`) using Kotlin + Jetpack Compose.
- Feature-first packaging across `order`, `customer`, `accounting`, with shared platform concerns in `core`.
- Navigation already partially decomposed via graph files and app-shell helpers.

## Strengths
- Domain boundaries are mostly understandable.
- Licensing/auth gate exists and is isolated under `core/licensing`.
- Room schema history and migration tests exist.

## Key architecture issues

### A1. Oversized backup orchestration
- Severity: high
- Files: `core/backup/BackupManager.kt`
- Production impact: mixed responsibilities (I/O, crypto, restore mapping, policy) increase blast radius.
- Fix: split into smaller collaborators with clear contracts; keep manager as orchestrator only.
- Timing: later
- Change risk: medium-high

### A2. Oversized helper overlay service
- Severity: high
- Files: `core/helper/HelperOverlayService.kt`
- Production impact: lifecycle + UI + voice/mic + command coordination in one class complicates reliability.
- Fix: extract permission gate, parser, and overlay coordinator classes.
- Timing: later
- Change risk: medium-high

### A3. Main app shell still broad but trending better
- Severity: medium
- Files: `MainAppContent.kt`, `MainAppHostScaffold.kt`, `core/navigation/graphs/*`
- Production impact: broad state surface increases coupling.
- Fix: incremental extraction of side-effect coordinators rather than rewrite.
- Timing: later
- Change risk: medium

## Implement-now decision in this pass
- No large architecture rewrite performed.
- Focus kept on security hygiene + documentation/plan hardening.

## Recommended next architectural milestone
1. Introduce backup subcomponents behind interfaces and test seams.
2. Reduce overlay service responsibilities.
3. Add minimal architecture decision records for each extraction milestone.
