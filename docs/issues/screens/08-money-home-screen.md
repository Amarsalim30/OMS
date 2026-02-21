# Money Home Screen UI/UX Issues

Status: Open

## Scope
- Route: `money`
- Primary file: `app/src/main/java/com/zeynbakers/order_management_system/core/ui/MoneyScreen.kt`

## Owner goals
- Switch between collection tasks quickly.
- Keep context while moving across tabs.
- Minimize accounting navigation overhead.

## Open issues

### P1
- Tab semantics are clear to power users but still require onboarding for new users.
- No consolidated KPI strip (today collected, unapplied, outstanding).
- Cross-tab context transfer can be made more explicit to prevent confusion.

### P2
- Tab-level guidance can be shorter and more action-first.

## Acceptance criteria
- First-time user understands each money tab immediately.
- KPI snapshot appears at entry and updates in near real-time.
- Context handoff between tabs is visible and predictable.
