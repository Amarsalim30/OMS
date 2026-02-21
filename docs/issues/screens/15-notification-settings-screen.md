# Notification Settings Screen UI/UX Issues

Status: Open

## Scope
- Route: `notifications`
- Primary file: `app/src/main/java/com/zeynbakers/order_management_system/core/notifications/NotificationSettingsScreen.kt`

## Owner goals
- Keep reminders useful and trusted.
- Tune lead time once and move on.
- Understand permission state quickly.

## Open issues

### P1
- Lead-time options are many; recommended default is not strongly indicated.
- Permission-required state is shown but setup guidance can be clearer.
- Daily summary timing is fixed and not visible as a controllable setting.

### P2
- No preview examples of reminder timing impact.
- No per-channel testing action for confidence.

## Acceptance criteria
- Recommended reminder setup is obvious.
- Permission and enabled state are never ambiguous.
- User can verify notification setup without waiting for a real event.
