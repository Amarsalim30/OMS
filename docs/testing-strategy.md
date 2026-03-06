# Testing Strategy

Date: 2026-03-06 (refresh)

## Testing tiers

### Tier 1 (required on every PR)
- `./gradlew :app:testDebugUnitTest --console=plain`
- `./gradlew :app:lintDebug --console=plain`
- `./gradlew :app:assembleDebug --console=plain`

### Tier 2 (release candidate)
- `./gradlew :app:lintRelease --console=plain`
- `./gradlew :app:assembleRelease --console=plain`
- `./gradlew :app:connectedDebugAndroidTest` (requires emulator/device)

### Tier 3 (manual acceptance)
- Execute `docs/requirements/licensing-verification-checklist.md` on signed builds.
- Run backup/restore round-trip with realistic dataset.
- Validate accessibility basics (TalkBack, touch targets, focus order) in high-frequency flows.

## Coverage priorities
1. Licensing/auth gate correctness and offline grace behavior.
2. Backup/restore integrity.
3. Payment/accounting data integrity flows.
4. Order-entry parsing and customer lookup correctness.

## Gaps
- Device-backed automation remains the largest confidence gap in this environment.
