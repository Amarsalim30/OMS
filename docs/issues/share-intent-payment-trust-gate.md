# Title: External share-intent payment text requires explicit trust gate

Status: Fixed (2026-02-26)

## Context
Screen: Global app shell -> Money Collect handoff
Files:
- `app/src/main/java/com/zeynbakers/order_management_system/MainAppContent.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/MainDialogs.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/AppNavigationHelpers.kt`
- `app/src/main/res/values/strings.xml`

## Previous behavior (risk)
- Shared text from external apps was ingested immediately.
- If user was already on `Money > Collect`, text was appended without confirmation.
- If user was elsewhere, app switched to Money and prefilled directly.

## UX rule
- All externally shared payment text must pass an explicit trust gate first.
- User sees a preview and chooses:
  - `Review in Collect` to proceed
  - `Ignore` to discard

## Acceptance criteria
- No external shared payment text is ingested without explicit user confirmation.
- Confirm path:
  - Append in place only when already in `Money > Collect`.
  - Otherwise switch to `Money` root, set `Collect`, and prefill there.
- Ignore path performs no ingestion or navigation.

## Implementation notes (2026-02-26)
- Added shared-text trust dialog and preview copy.
- Added helper logic for:
  - append-vs-route decision
  - safe preview formatting
- Added unit tests for the helper behavior.
