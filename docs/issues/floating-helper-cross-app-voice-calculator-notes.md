# Title: Floating helper icon v2 - cross-app two-function capture + Notes history in More

## Scope
This issue covers one focused feature set:
1. Floating helper captures voice and acts in two modes only:
   - `Voice Calculator`
   - `Voice Note`
2. Captured entries are stored and managed in a dedicated `Notes history` screen.
3. `Notes history` is reachable from the `More` bottom sheet.

## Codebase research summary (current state)
### Voice capture and overlay
- Current helper UI is Compose-only and app-bound:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/ui/VoiceCalculatorOverlay.kt`
- It is mounted globally in app shell:
  - `app/src/main/java/com/zeynbakers/order_management_system/MainAppHostScaffold.kt`
- A second overlay instance is also used in order editor:
  - `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderEditorSheet.kt`
- Field targeting is tied to in-app forms via:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/ui/VoiceInputRouter.kt`
  - `app/src/main/java/com/zeynbakers/order_management_system/core/ui/AmountFieldRegistry.kt`

### Navigation and More menu
- `More` actions are built in:
  - `app/src/main/java/com/zeynbakers/order_management_system/MainAppHostScaffold.kt`
- Settings graph currently contains backup/notifications/tutorial only:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/graphs/SettingsGraph.kt`
- Routes live in:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/AppRoutes.kt`
- More sheet UI shell:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/ui/AppScaffold.kt`

### Persistence and migration
- DB schema version is `10`:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/db/AppDatabase.kt`
- Migrations are manual SQL blocks:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/db/DatabaseProvider.kt`
- Backup/restore serializes table payloads explicitly, so new tables must be added there:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt`

### Parser and tests
- Voice math parser exists but has narrow test corpus:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/util/VoiceMathParser.kt`
  - `app/src/test/java/com/zeynbakers/order_management_system/core/util/VoiceMathParserTest.kt`
- Voice notes parser is currently tuned for order-item structuring:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/util/VoiceNotesParser.kt`
  - `app/src/test/java/com/zeynbakers/order_management_system/core/util/VoiceNotesParserTest.kt`

## Product requirements (final for this issue)
### Helper behavior
- Floating helper exposes exactly two actions:
  - `Voice Calculator`
  - `Voice Note`
- User can capture while outside OMS.
- If overlay path is blocked on device/OEM, fallback entry points must still allow capture.

### Notes history behavior
- `Notes history` is an inbox, not chip-driven category navigation.
- Primary find path: search and one filter sheet.
- Row actions live in overflow menu to avoid accidental taps.
- Calculator captures are visible in Notes history as first-class entries.

## UX spec
### Floating helper panel
- Collapsed draggable bubble.
- Tap opens compact panel with 2 actions only.
- No extra quick actions in MVP.

### UX behavior standards for non-technical users (research-informed)
- Keep one primary action visible at a time:
  - helper panel only shows `Voice Calculator` and `Voice Note`.
- Ask permissions in context (when user taps helper setup or tries first capture), not at app launch.
- Never block the whole app when permission is denied:
  - show clear feature-level fallback state and let user continue normal OMS workflows.
- Use specific denial messaging:
  - clearly state which helper function is unavailable (`overlay`, `mic`, or both).
- Keep actions large and forgiving:
  - interactive targets at least 48dp.
- Default to recognition over classification:
  - Notes history opens in recency inbox with search first.
- Keep filtering secondary:
  - one modal filter sheet instead of persistent chip rows.
- Use overflow actions for destructive/secondary actions:
  - row-level menu triggered from a single trailing icon.
- Preserve trust with explicit states:
  - visible listening state, processing state, saved state, and undo where possible.

### Notes history screen
- Top app bar:
  - title: `Notes`
  - actions: Search, Filter, Overflow
- List:
  - default sort: newest first
  - optional day grouping: Today / Yesterday / Earlier
- Filter bottom sheet:
  - Type (multi-select): All, Money, Phone, Plain text, Calculator, Voice
  - Time: Today, 7 days, 30 days, Custom
  - toggles: Pinned first, Has phone, Has amount
  - Clear filters action
- Row design:
  - leading semantic icon
  - title (1-2 lines)
  - metadata (amount/phone + timestamp)
  - trailing overflow menu
- Overflow actions:
  - Copy
  - Share
  - Pin/Unpin
  - Delete
  - Call (only when phone exists)
  - Create/Attach customer (deferred hook)

### Search behavior
- Search must match:
  - text
  - partial phone digits
  - amount expressions (`1500`, `1,500`, `1.5k`)
  - linked customer name (when available)
  - source app (when available)

### Capture flow behavior (simple and efficient)
- `Voice Note` happy path:
  1. Tap helper -> `Voice Note`
  2. Speak
  3. Preview text + detected phone/amount
  4. `Save` (default) or `Try again`
- `Voice Calculator` happy path:
  1. Tap helper -> `Voice Calculator`
  2. Speak expression
  3. Show expression + result
  4. Primary CTA: `Copy result`; secondary CTA: `Save to Notes`
- Error behavior:
  - show short plain-language error and one retry action.
  - never strand user in modal loops.

## Information architecture and navigation changes
### Required route additions
- Add `AppRoutes.NotesHistory = "notes_history"` in:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/AppRoutes.kt`
- Add permission primer routes for helper setup:
  - `AppRoutes.MicrophonePermissionPrimer = "microphone_permission_primer"`
  - `AppRoutes.OverlayPermissionPrimer = "overlay_permission_primer"`

### Required graph additions
- Register Notes history composable in settings graph:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/graphs/SettingsGraph.kt`

### Required More menu additions
- Add a new More action button for Notes history in:
  - `app/src/main/java/com/zeynbakers/order_management_system/MainAppHostScaffold.kt`
- Add new label string:
  - `more_notes_history` in `app/src/main/res/values/strings.xml`

### Notes history location decision
- Notes history lives under `More` first (not a top-level tab).
- Rationale: utility workflow, not primary daily navigation root.

### Onboarding location decision
- Helper onboarding is optional and non-blocking.
- Add helper setup card to existing setup checklist flow, after notifications.
- Keep `Start using app` available without helper setup completion.

## Technical architecture plan
### Track A - Cross-app capture shell
### A1. Permission and capability layer
- Add helper capability manager:
  - checks draw-over-apps permission
  - checks record audio permission
  - exposes fallback availability
- Use app-level setting toggle for enabling helper.
- Add runtime capability states:
  - `Ready`
  - `NeedsMicPermission`
  - `NeedsOverlayPermission`
  - `OverlayUnsupported` (for devices where overlay cannot be granted)
  - `FallbackOnly` (tile/notification path)

### A2. Overlay runtime
- Add `HelperOverlayService` using `WindowManager` and `TYPE_APPLICATION_OVERLAY`.
- Service owns bubble view lifecycle and launches voice sessions only by explicit user action.
- Add foreground notification while helper service is active.

### A3. Fallback runtime
- If overlay unavailable:
  - launch compact capture activity from notification action
  - optional quick settings tile path (MVP fallback if effort remains low)

### Track B - Voice session and parser hardening
### B1. Session controller extraction
- Extract recognizer lifecycle logic from `VoiceCalculatorOverlay` into reusable controller.
- Reuse same engine from in-app overlay and cross-app helper.
- Ensure controller supports explicit session states for UI:
  - idle, listening, processing, result, error.

### B2. Math parser upgrades
- Extend `VoiceMathParser` coverage for:
  - currency words/noise
  - chained operations
  - percentage phrasing
  - decimal and shorthand variants
- Keep parser deterministic and preview-first.

### B3. Notes parsing split
- Keep current order-entry parser behavior intact for order workflows.
- Add helper-specific note normalization/classification path for Notes history entries.

### Track C - Notes storage and query model
### C1. Entity + DAO
- Add `helper_notes` table with indexed fields for fast recency and filtering.

Proposed columns:
- `id` (PK)
- `createdAt`
- `updatedAt`
- `type` (`VOICE`, `TEXT`, `MONEY`, `PHONE`, `CALCULATOR`)
- `rawTranscript`
- `displayText`
- `calculatorExpression`
- `calculatorResult`
- `detectedPhone`
- `detectedPhoneDigits`
- `detectedAmountRaw`
- `detectedAmountNormalized`
- `linkedCustomerId`
- `sourceApp`
- `pinned`
- `deleted` (soft delete optional; hard delete allowed for MVP)

Recommended indexes:
- `(createdAt DESC)`
- `(pinned, createdAt DESC)`
- `detectedPhoneDigits`
- `detectedAmountNormalized`
- `type`

### C2. Search strategy
- MVP: SQL `LIKE` with normalized companion fields.
- Defer FTS unless list size or performance proves insufficient.

### Track D - Notes history UI
### D1. Screen and ViewModel
- Add:
  - `NotesHistoryViewModel`
  - `NotesHistoryScreen`
  - `NotesFilterSheet`
  - row component with overflow actions

### D2. Integration into app factory
- Register new ViewModel in:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/ui/AppViewModelFactory.kt`

### D3. More menu entry wiring
- Add Notes history action in More list and navigate to `AppRoutes.NotesHistory`.

### Track E - Existing in-app overlay alignment
- Keep current in-app overlay functional during rollout.
- Remove duplicated behavior between global shell overlay and order editor overlay by moving both to shared voice session controller.
- Preserve existing `VoiceInputRouter` behavior for form field targeting to avoid regressions.

## Database and backup impact (mandatory)
### DB migration
- Bump DB version `10 -> 11`:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/db/AppDatabase.kt`
- Add migration `10 -> 11`:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/db/DatabaseProvider.kt`
- Add migration SQL constants and migration unit tests:
  - `app/src/test/java/com/zeynbakers/order_management_system/core/db/DatabaseProviderMigrationTest.kt`

### Backup/restore
- Add helper notes payload entry to archive export/import flow:
  - snapshot build
  - JSON serialization
  - restore parse + insert
  - manifest entry checksum
- File:
  - `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt`

## Manifest and platform additions
- Manifest updates in `app/src/main/AndroidManifest.xml`:
  - draw-over-apps permission entry
  - foreground service declaration for helper
  - service registration for `HelperOverlayService`
- Keep `RECORD_AUDIO` flow and add overlay settings intent flow.

Required declarations for targetSdk 36:
- `android.permission.SYSTEM_ALERT_WINDOW` (special app access flow).
- `android.permission.FOREGROUND_SERVICE`.
- `android.permission.FOREGROUND_SERVICE_MICROPHONE` (Android 14+ foreground service type permission).
- Service declaration includes `android:foregroundServiceType="microphone"` for helper capture service.

Platform constraints to reflect in UX:
- Special permission grant is done in Settings (not normal runtime dialog).
- On Android 14+ foreground service types are mandatory and enforced.
- Microphone foreground service is tied to while-in-use microphone constraints; do not auto-start from background without user action.
- On Android 13+, `POST_NOTIFICATIONS` is not required to start FGS, but helper should still guide users if they want persistent notification visibility.

## Permission and onboarding integration plan
### Existing onboarding pattern reuse
- Reuse current primer and setup infrastructure:
  - `PermissionPrimerScreen` in onboarding package
  - `SetupChecklistScreen` step-based UI
  - `OnboardingPreferences` for completion flags

### New onboarding state fields
Add to `OnboardingState`:
- `helperSetupDone: Boolean`
- `helperMicGranted: Boolean` (derived can also be acceptable)
- `helperOverlayGranted: Boolean` (derived can also be acceptable)

Add persisted keys in `OnboardingPreferences` for helper setup completion.

### Setup checklist extension
- Add optional setup step:
  - title: `Floating helper`
  - body: `Capture quick notes and calculations over other apps.`
- Step actions:
  - `Enable helper`
  - `Not now`
- Completion rule:
  - mark done when user explicitly enables helper and finishes permission flow, or user selects a fallback-only mode.

### Primer flow sequence
1. `MicrophonePermissionPrimer`
   - Why: capture your speech for calculator/notes.
   - Privacy: no auto background recording.
2. `OverlayPermissionPrimer`
   - Why: show helper above other apps for quick capture.
   - If unsupported/denied: offer fallback mode.

### Permission request timing policy
- Do not prompt for mic/overlay at startup splash/intro.
- Trigger each request from explicit helper setup action.
- If denied:
  - show inline state in helper settings card and allow retry.
  - keep app fully usable.

### Settings follow-up after onboarding
- Add helper settings section under `More` path with:
  - enable/disable helper
  - permission status chips (mic, overlay)
  - retry/open settings actions
  - fallback mode toggles

## UX copy style guardrails
- Use plain, action-first wording.
- Avoid technical terms in user copy (`SYSTEM_ALERT_WINDOW`, `FGS`); keep these internal to code/docs.
- Use short result toasts/snackbars:
  - `Saved to Notes`
  - `Copied result`
  - `Microphone permission needed for voice capture`
  - `Overlay access needed for floating helper`

## File-level implementation checklist
### New files (expected)
- `app/src/main/java/com/zeynbakers/order_management_system/core/helper/HelperOverlayService.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/helper/HelperOverlayController.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/helper/HelperPreferences.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/helper/CaptureSessionController.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/helper/data/HelperNoteEntity.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/helper/data/HelperNoteDao.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/helper/ui/NotesHistoryViewModel.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/helper/ui/NotesHistoryScreen.kt`

### Existing files to modify (required)
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/AppRoutes.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/graphs/SettingsGraph.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/navigation/graphs/OnboardingGraph.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/MainAppHostScaffold.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/ui/AppViewModelFactory.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/db/AppDatabase.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/db/DatabaseProvider.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/backup/BackupManager.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/util/VoiceMathParser.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/util/VoiceNotesParser.kt` (or helper-specific parser split)
- `app/src/main/java/com/zeynbakers/order_management_system/core/onboarding/OnboardingPreferences.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/onboarding/OnboardingScreens.kt`
- `app/src/main/res/values/strings.xml`

### Documentation to update after implementation
- `README.md`
- `docs/requirements.md`
- `docs/current-app-state.md`
- `docs/issues/screens/16-app-navigation-shell.md`

## Test plan
### Unit tests
- Parser:
  - expanded `VoiceMathParserTest` for phrase/noise variants and invalid cases
- Notes parsing/classification:
  - new tests for type detection and normalization
- DAO:
  - query tests for search, filters, pinned-first, time range
- Migration:
  - validate `10 -> 11` SQL and indexes

### Instrumentation/UI tests
- More sheet includes `Notes history` and opens correct route.
- Notes history:
  - search/filter sheet behavior
  - overflow actions visibility by row type
- Onboarding helper step:
  - non-blocking skip path works
  - mic/overlay primer navigation and return behavior
- Regression smoke:
  - existing voice entry in Calendar/Day detail/Manual payment still works.

### Manual QA matrix
- API levels: 24, 29, 33, 34+
- OEM sanity: Samsung + one strict OEM profile
- Cases:
  - overlay granted/revoked mid-session
  - mic permission denied then granted
  - helper onboarding skipped, then enabled later from More
  - overlay permission screen unavailable/unsupported path
  - helper service restart after process death
  - capture in non-OMS app then verify entry in Notes history

## Delivery phases
1. Navigation + Notes history shell in More (no overlay yet).
2. Data model + migration + backup wiring.
3. Voice session extraction + parser upgrades.
4. Cross-app overlay service and fallback entry points.
5. QA hardening and documentation sync.

## Risks and mitigations
- Overlay restrictions vary by OEM:
  - Mitigation: fallback entry points and clear capability messaging.
- Regressions in current in-app voice targeting:
  - Mitigation: preserve `VoiceInputRouter` contract; add regression tests.
- Backup incompatibility from new table:
  - Mitigation: include helper notes in backup manifest and restore path from first release.

## Acceptance criteria (DoD)
1. `More` includes a new `Notes history` action and opens a dedicated Notes history screen.
2. Floating helper offers exactly two actions: `Voice Calculator` and `Voice Note`.
3. User can capture outside OMS via overlay path or supported fallback path.
4. Notes history uses search + filter sheet model (no permanent chips row).
5. Row actions are overflow-based and include conditional `Call`.
6. Calculator captures appear in Notes history with expression and result copy actions.
7. DB migration to v11 succeeds; backup/restore includes helper notes.
8. Helper setup is present in onboarding as optional and non-blocking, with mic/overlay permission education and retry paths.
9. Unit + instrumentation tests for new flows pass.

## UX and platform references used for this plan
- Android runtime permission guidance:
  - https://developer.android.com/training/permissions/requesting
- Android special permissions guidance:
  - https://developer.android.com/training/permissions/requesting-special
- Notification permission behavior:
  - https://developer.android.com/develop/ui/views/notifications/notification-permission
- Android 14 foreground service type requirements:
  - https://developer.android.com/about/versions/14/changes/fgs-types-required
- Compose SearchBar guidance:
  - https://developer.android.com/develop/ui/compose/components/search-bar
- Compose ModalBottomSheet guidance:
  - https://developer.android.com/develop/ui/compose/components/bottom-sheets
- Compose menu pattern guidance:
  - https://developer.android.com/develop/ui/compose/components/menu
- Compose accessibility touch target guidance:
  - https://developer.android.com/develop/ui/compose/accessibility/api-defaults

## Out of scope
- Cloud sync for helper notes.
- OCR/screen text extraction.
- Conversational assistant behavior.
