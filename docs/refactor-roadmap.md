# Refactor Roadmap

Date: 2026-03-06 (refresh)

## Guiding principles
- Incremental, test-backed refactors only.
- Reduce risk in highest-blast-radius code first.
- Preserve behavior unless a defect/security issue is confirmed.

## Milestones

### R1. Security/repo hygiene (completed)
- Remove accidental personal artifacts and document controls.
- Status: completed in this pass.

### R2. Backup subsystem decomposition (planned)
- Extract archive I/O, crypto, restore mapping, and orchestration layers.
- Add unit tests around each extracted boundary.

### R3. Helper overlay service decomposition (planned)
- Separate lifecycle, permission gating, command parsing, and UI coordination.
- Add targeted tests for parser/permission logic.

### R4. App-shell simplification (planned)
- Continue reducing side-effect density in `MainAppContent.kt`.
- Keep navigation behavior stable.

### R5. UX/state consistency (planned)
- Standardize loading/empty/error states and retry affordances across major flows.

### R6. Release confidence hardening (planned)
- Expand device-backed and signed-build validation coverage.
