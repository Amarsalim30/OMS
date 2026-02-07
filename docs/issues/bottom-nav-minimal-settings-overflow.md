# Title: Bottom navigation and overflow menu - minimal Google-level UX

## Context
The bottom navigation should feel clean, modern, and consistent with Google-level UI/UX. The current look (e.g., in calendarscreen.jfif) shows extra visual weight, uneven padding, and a grey bar treatment that feels out of place against the rest of the UI.

Related files:
- app/src/main/java/com/zeynbakers/order_management_system/core/ui/AppScaffold.kt
- app/src/main/java/com/zeynbakers/order_management_system/MainActivity.kt

## Current behavior
- Bottom nav uses a heavy grey background that visually competes with the calendar grid.
- Vertical spacing feels inconsistent: icons and labels sit slightly high, while the bar itself feels thick.
- The bottom bar appears as a separate slab rather than a subtle, integrated system element.
- Overflow is now a Settings icon (implemented), but the bar styling still feels heavy.

## UX issues
- Visual weight: the bottom bar draws too much attention away from the calendar grid.
- Padding/vertical alignment: icon/label spacing feels off and wastes vertical space.
- Color hierarchy: grey bar color clashes with the dark surface and reduces contrast clarity.

## Requested refinements
1) Make the bottom bar more minimal:
   - Use the app surface color (or surfaceVariant with lower tonal elevation) instead of a heavy grey slab.
   - Reduce tonal elevation and remove extra shadowing.
2) Tighten padding and vertical alignment:
   - Reduce top/bottom padding inside the bar.
   - Ensure icons and labels are vertically centered and consistent across tabs.
3) Keep overflow minimal:
   - Settings icon stays in the bottom bar (already implemented).
   - Settings actions remain limited to Backup & restore, Notifications, Import contacts.

## Expected behavior
- Bottom nav feels lightweight and integrated with the screen.
- Icons + labels align cleanly and don’t waste vertical space.
- The calendar grid remains the dominant visual surface.

## Acceptance criteria
- Bottom bar no longer looks like a thick grey slab.
- Icon/label alignment feels centered and balanced.
- Visual weight shifts back to the calendar grid.
- Settings icon remains in the bar; settings actions remain minimal.
