# Testing Strategy

Date: 2026-03-07

## Current Verification Baseline
```text
adb devices                                                                                                         -> PASS (192.168.0.192:34559 device)
./gradlew :app:compileDebugAndroidTestKotlin --console=plain --no-daemon                                           -> PASS
./gradlew :app:testDebugUnitTest --console=plain --no-daemon                                                       -> PASS
./gradlew :app:lintDebug --console=plain --no-daemon                                                               -> PASS
./gradlew :app:assembleRelease --console=plain --no-daemon                                                         -> PASS
./gradlew "-Pandroid.testInstrumentationRunnerArguments.class=com.zeynbakers.order_management_system.ui.AccessibilitySmokeTest" :app:connectedDebugAndroidTest --console=plain --no-daemon -> FAIL (`No compose hierarchies found in the app`)
```

## Local Connected-Test State
- Early in this pass, targeted connected reruns were useful enough to flush out and fix real issues in accounting fixtures, customer-list assertions, calendar assertions, and accessibility regressions.
- By the end of the session, the local Compose runner on the attached device had degraded and was producing `No compose hierarchies found in the app` even for targeted classes that had previously been green.
- Because of that instability, the local connected lane is not a trustworthy release gate from this machine right now.

## Tests Added Or Strengthened In This Pass
- `app/src/test/java/com/zeynbakers/order_management_system/core/licensing/LicensingRepositoryTest.kt`
  - entitlement missing
  - access denied
  - entitlement expired
  - revoked device
  - existing device success path
  - new device registration path
  - device-limit reached
  - offline grace success path
  - offline grace expired
  - fatal validation failure
- `app/src/test/java/com/zeynbakers/order_management_system/accounting/mpesa/MpesaParserTest.kt`
  - invalid calendar date does not crash parser
  - invalid time does not crash parser
  - malformed segment does not block valid segment parsing
- `app/src/test/java/com/zeynbakers/order_management_system/core/contacts/ContactsLoaderTest.kt`
  - permission-missing handling
  - transient vs permanent failure classification
  - successful contact mapping
- Instrumentation updates:
  - `AccountingLogicTest` fixture integrity fixes
  - `DatabaseMigrationReplayTest` explicit v12 database creation
  - `CustomerListActionsTest`
  - `CalendarMonthScreenTest`
  - `AccessibilitySmokeTest`
- Shared filter affordance:
  - `AppFilterRow` now exposes a stable `app-filter-more` tag in production code
  - the old standalone `AppFilterRowTest` was removed after repeated false negatives from Compose-host instability on this device
  - no replacement overflow-selection test is claimed as verified locally in this pass

## Gaps That Still Matter
| Area | Severity | Gap | Recommendation | Status |
|---|---|---|---|---|
| Licensing/device claims | High | No live, trustworthy multi-device verification against Firebase rules/admin state. | Run the documented licensing matrix on a signed build against real Firebase data. | Open |
| Connected UI/integration | High | Local Compose connected testing is not reproducible on this host/device anymore; late-session reruns fail even for targeted classes with `No compose hierarchies found in the app`. | Treat the local connected lane as blocked, validate the configured CI emulator lane separately, and re-run locally only from a clean Android toolchain/device state before release. | Open |
| Migration replay | Medium-High | The v12 replay path now works in-repo, but broader historical migration confidence is still limited. | Restore/validate the full schema history and keep replay tests mandatory before schema changes. | Open |
| Accounting/order invariants | Medium | Complex receipt reallocation and order reconciliation logic still needs more JVM coverage. | Add focused processor and order-view-model tests before deeper accounting changes. | Open |
| Backup/restore SAF flows | Medium | Worker/SAF behavior still needs device/provider coverage. | Add manual provider matrix and targeted instrumentation later. | Open |

## Strategy Going Forward
1. Keep every correctness/security patch paired with targeted JVM coverage where possible.
2. Validate the configured CI emulator lane separately and do not treat the current local connected runner as a release gate until the Compose-host instability is fixed.
3. Restore broader migration replay confidence before any Room schema change.
4. Expand manual licensing verification with `docs/requirements/licensing-verification-checklist.md` after Firebase/admin setup changes.
5. Track host-side Gradle/UTP failures separately from app regressions so test-harness work does not bleed into licensing-critical changes.
