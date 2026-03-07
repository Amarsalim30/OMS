# Architecture Review

Date: 2026-03-07

## Current Shape
- Single-module Android app with feature-first packages for `order`, `customer`, `accounting`, and shared platform concerns under `core`.
- Navigation is already split into graph files, but app-wide state collection and orchestration still concentrate heavily in the root shell.
- Licensing/auth is isolated under `core/licensing`, which made this pass's hardening work contained and reviewable.

## Strengths
- Room schema history and migration artifacts exist.
- Domain language is mostly consistent across orders, customers, receipts, allocations, and ledger concepts.
- The licensing/auth gate already sits at app entry, so policy enforcement can be changed without rewriting the rest of navigation.
- Recent refactors made it feasible to add repository-level tests without cross-cutting changes.

## Findings
| ID | Severity | Affected files | Production impact | Recommendation | Implement now vs later | Change risk | Status |
|---|---|---|---|---|---|---|---|
| AR-1 | High | `MainAppContent.kt` | Large recomposition and orchestration surface makes unrelated changes harder to verify and review. | Split route-scoped state holders and side-effect coordinators without changing navigation contracts. | Later. | Medium-High | Open |
| AR-2 | High | `core/backup/BackupManager.kt` | One file still owns encryption, SAF IO, archive validation, restore orchestration, and logging. | Extract backup archive IO, restore validation, and target-policy collaborators behind stable seams. | Later. | Medium-High | Open |
| AR-3 | Medium | `core/licensing/LicensingRepository.kt`, `core/licensing/LicensingLocalStore.kt` | Prior to this pass, licensing logic was tightly coupled to Firebase and Android storage, limiting unit coverage and safe iteration. | Keep the new remote/cache interfaces and add more test seams around the future trusted claim strategy. | Implemented now, extend later. | Low-Medium | Improved |
| AR-4 | Medium | `app/src/main/java/**` | Single-module growth is starting to blur ownership for future engineers. | Defer modularization, but add stricter package ownership and milestone-based extractions first. | Later. | Medium | Open |

## Implemented In This Pass
- Licensing validation now uses explicit remote/cache contracts instead of direct hard-wiring to Firebase + SharedPreferences in the core decision path.
- The move-target open-order flow now delegates filtering/sorting to Room instead of mixing DAO breadth with view-model post-processing.

## Recommended Next Architecture Milestones
1. Decompose `MainAppContent.kt` into feature state containers without altering screen behavior.
2. Break `BackupManager.kt` into archive builder, target writer, restore validator, and restore executor components.
3. Redesign licensing device claims so the repository can depend on a trusted registration primitive instead of the current client-driven transaction/rules path.
