# Repository Guidelines

## Project Structure & Module Organization
- Main app code lives in `app/src/main/java/com/zeynbakers/order_management_system/`.
- Feature-first packages:
  - `order/` (calendar, day detail, unpaid flows)
  - `customer/` (customer list/detail/statements)
  - `accounting/` (payments, ledger, M-PESA)
  - `core/` (db, navigation, backup, notifications, shared UI)
- Resources are in `app/src/main/res/` (`values/`, `drawable/`, `layout/`, `xml/`).
- Unit tests: `app/src/test/java/...`; instrumentation/UI tests: `app/src/androidTest/java/...`.
- Product and UX documentation lives in `docs/` (requirements, ADRs, issue reviews).

## Build, Test, and Development Commands
- `./gradlew assembleDebug`: build debug APK.
- `./gradlew debug`: convenience alias for `:app:assembleDebug`.
- `./gradlew testDebugUnitTest`: run JVM unit tests.
- `./gradlew connectedDebugAndroidTest`: run instrumentation/Compose UI tests (device/emulator required).
- `./gradlew lintDebug`: run Android lint checks before opening a PR.

## Coding Style & Naming Conventions
- Language: Kotlin (JVM target 11), Jetpack Compose UI.
- Use 4-space indentation and keep functions focused and small.
- Naming:
  - Classes/objects/files: `PascalCase` (`CustomerDetailScreen.kt`)
  - functions/variables: `camelCase`
  - constants: `UPPER_SNAKE_CASE`
- Keep package boundaries aligned to feature folders above.
- Put user-facing text in `app/src/main/res/values/strings.xml` instead of hardcoding in composables.

## Testing Guidelines
- Frameworks: JUnit4 for unit tests, AndroidX test + Compose test APIs for instrumentation tests.
- Test file naming: `FeatureNameTest.kt`; mirror production package path where possible.
- Add/adjust tests for parser logic, payment/accounting logic, and navigation/state behavior when changing those areas.

## Commit & Pull Request Guidelines
- Current history follows prefix style like `Feat: ...`; use `<Type>: <short imperative summary>` (e.g., `Fix: preserve search state on back`).
- Keep commits scoped to one concern; include docs updates when behavior changes (`docs/requirements.md`, `docs/issues/`).
- PRs should include:
  - What changed and why
  - Linked issue/doc reference
  - Test evidence (`testDebugUnitTest`, screenshots/video for UI changes)

## Multi-Agent Scope Boundaries
- UX agent: `docs/issues` only unless explicitly asked to write code.
- UI agent: UI-only changes (Compose screens/components, spacing, typography, accessibility labels).
- Reliability agent: tests and correctness-only (data integrity, lifecycle safety, migration/test coverage).
- Performance agent: performance-only changes; no behavior or product-flow changes.
- Security agent: security audit plus minimal hardening changes.
