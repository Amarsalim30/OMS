# Order Form UX Pain Points — Review & Improvement Plan

## Date
2025-01-18

## Scope
`OrderEditorSheet`, `AddProductBottomSheet`, `OrderCartSummary`, `OrderEditorCustomerSection`, and related input composables.

---

## 1. BottomSheet — Android back button closes it and loses input

### Problem
`AddProductBottomSheet` (a `ModalBottomSheet`) dismisses on the system back button. When it dismisses, `resetForm()` clears `productQuery`, `selectedProduct`, `quantity`, and `unitPriceText`. Users lose all in-progress product entry with a single accidental back press.

### Root cause
- `ModalBottomSheet.onDismissRequest` unconditionally calls `resetForm()` + `onDismiss()`.
- There is no "dirty check" or confirmation before discarding partially-filled product data.
- The parent `OrderEditorSheet.BackHandler` includes a `showAddProductSheet` branch, but `ModalBottomSheet` consumes the back event at its own window level first, so the branch is effectively dead code in practice.

### Proposed fix
1. **Add dirty-state guard inside the bottom sheet.** Track whether the user has entered meaningful input (`productQuery.isNotBlank() || selectedProduct != null || unitPriceText.isNotBlank()`).
2. **Wrap a `BackHandler` inside `AddProductBottomSheet`** that:
   - If dirty → show a small confirmation (or at minimum, do NOT call `onDismiss` / consume the event).
   - If clean → allow normal dismiss.
3. **Remove the redundant `showAddProductSheet` branch** from `OrderEditorSheet.handleBackPress` to eliminate confusion.

### Affected files
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/AddProductBottomSheet.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderEditorSheet.kt`

---

## 2. Product section not visible when keyboard is open

### Problem
When the user taps into a text field (e.g. customer name, notes, or total), the keyboard opens and the `imePadding()` pushes the bottom of the form up, but the scrollable `Column` does **not** automatically scroll to keep the focused field or the product/cart area in view. Users cannot see what they are typing or the "Add Product" button while the keyboard is open.

### Root cause
- `OrderEditorSheet` uses `rememberScrollState()` but has no `BringIntoViewRequester` or `onFocusChanged`-driven scroll logic.
- `AddProductBottomSheet` also uses `rememberScrollState()` without bringing the focused search field into view when the keyboard appears.
- The `Dialog(decorFitsSystemWindows = false)` disables automatic platform scroll-to-field behavior.

### Proposed fix
1. **Add `BringIntoViewRequester`** to every focusable field in `OrderEditorSheet` and request `bringIntoView()` on focus gain.
2. **Apply the same pattern** to `AddProductBottomSheet`'s search and unit-price fields.
3. **Consider switching** the main editor from `Dialog` + manual scroll to a `Scaffold`-based full-screen layout where `imePadding` + `bringIntoView` work more naturally with Material3.

### Affected files
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderEditorSheet.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/AddProductBottomSheet.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderEditorFields.kt`

---

## 3. Font readability issues

### Problem
- `labelMedium` (12sp / 16sp lineHeight) is used for section headers and field labels — this is small for users doing rapid data entry in a shop environment.
- `bodySmall` (12sp) is used for error text and helper copy — easy to miss.
- `headlineSmall` (24sp) used for quantity display in `AddProductBottomSheet` feels oversized relative to surrounding 16sp body text.
- The current custom `Typography` is entirely system `FontFamily.SansSerif` with no slightly-wider or more-legible choice for dense business UI.

### Root cause
- `Type.kt` sets `labelMedium` to 12sp, which is the Material3 default floor.
- Error/help text consistently uses `bodySmall` for compactness.

### Proposed fix
1. **Bump field labels** from `labelMedium` to `labelLarge` (13sp → still small; consider `bodySmall` at 14sp for labels).
2. **Bump error text** from `bodySmall` to `bodyMedium` (14sp).
3. **Reduce quantity display** in `AddProductBottomSheet` from `headlineSmall` to `titleLarge` (21sp) for better visual harmony.
4. **Evaluate** a slightly larger `bodyLarge` (17sp) or wider `FontFamily` for shop-floor readability.

### Affected files
- `app/src/main/java/com/zeynbakers/order_management_system/core/ui/theme/Type.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/AddProductBottomSheet.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderEditorCustomerSection.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderCartSummary.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderEditorSheet.kt`

---

## 4. Spacing is too cramped

### Problem
- Root padding in `OrderEditorSheet` is `horizontal = 6.dp` — on many devices this makes the form feel edge-to-edge and hard to read.
- Cart item rows have `vertical = 4.dp` padding — text and buttons are nearly touching.
- Customer section uses `vertical = 6.dp` with `spacedBy(6.dp)` — section breathing room is minimal.
- `ValueRow` and `InlineEditorRow` use `vertical = 16.dp` padding but no internal spacing between icon and text, making scanning harder.

### Root cause
Aggressive density-first padding choices that prioritize showing everything on one screen over readability.

### Proposed fix
1. **Increase root horizontal padding** in `OrderEditorSheet` from `6.dp` to `16.dp` (or at minimum `12.dp`).
2. **Increase cart item row vertical padding** from `4.dp` to `12.dp`.
3. **Increase section vertical spacing** in `OrderCartSummary` and `OrderEditorCustomerSection` from `8.dp` to `12.dp`.
4. **Add consistent `16.dp` internal padding** to `ValueRow` / `InlineEditorRow` and increase icon-to-text spacer from `12.dp` to `16.dp`.

### Affected files
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderEditorSheet.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderCartSummary.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderEditorCustomerSection.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderEditorSheet.kt` (ValueRow / InlineEditorRow)

---

## 5. Touch targets below accessibility minimum

### Problem
- `CustomerSuggestionDropdown` suggestion rows use `sizeIn(minHeight = 44.dp)` — below the 48dp accessibility minimum.
- `ProductSuggestionDropdown` items apply `sizeIn(minHeight = 48.dp)` only to the inner `Text`, not the `Surface` or `Row`, so the actual tappable area can shrink below 48dp depending on text length.
- Cart item delete `IconButton` is correctly 48dp, but the surrounding row padding is so tight that adjacent rows feel cramped.
- `ValueRow` and `InlineEditorRow` are at exactly 48dp minimum, which is fine but leaves no buffer for motor-imprecise taps.

### Root cause
Explicit `sizeIn(minHeight = 44.dp)` in dropdown rows; incorrect modifier placement in `ProductSuggestionDropdown`.

### Proposed fix
1. **Change all dropdown rows to `minHeight = 48.dp`** and apply the modifier to the `Surface` or `Row`, not just the `Text`.
2. **Ensure `IconButton`s remain 48dp** (they already are in most places).
3. **Consider increasing `ValueRow` / `InlineEditorRow` minHeight to 56dp** for the primary input rows.

### Affected files
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderEditorCustomerSection.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/AddProductBottomSheet.kt`

---

## 6. OnFocus behavior lacks visual feedback and scroll assistance

### Problem
- When a field gains focus, there is no auto-scroll, so the field may end up behind the keyboard.
- `OrderEditorOutlinedField` uses default `OutlinedTextFieldDefaults.colors()` — focus is visible only via the border color change. No elevation, background tint, or padding animation.
- Focus transitions between fields (e.g. customer name → phone) do not trigger the keyboard action flow (`ImeAction.Next`) consistently.

### Root cause
- Missing `BringIntoViewRequester` integration.
- No custom focus indication beyond Material defaults.
- `OrderEditorOutlinedField` accepts `keyboardOptions` but callers don't always set `ImeAction.Next`.

### Proposed fix
1. **Add `BringIntoViewRequester` to all focusable fields** and request on focus gain.
2. **Add a subtle focus animation** to `OrderEditorOutlinedField` (e.g. background tint in `colorScheme.primaryContainer` at low alpha when focused).
3. **Audit and fix `ImeAction` chains**: customer name → Next, unit price → Done, notes → Done, etc.

### Affected files
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderEditorFields.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderEditorSheet.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderEditorCustomerSection.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/AddProductBottomSheet.kt`

---

## 7. Auto-scroll is missing entirely

### Problem
There is no programmatic scrolling when:
- A field gains focus and the keyboard opens.
- Validation errors appear below fields (errors may render off-screen).
- The product bottom sheet opens and should show the search field.
- Adding a product updates the cart list — the list does not scroll to show the new item.

### Root cause
- `rememberScrollState()` is used but never driven programmatically except for manual user scroll.
- No `LaunchedEffect` observes focus changes, error visibility, or list mutations to call `scrollState.animateScrollTo()`.

### Proposed fix
1. **Focus-driven scroll**: on any focus gain, compute the focused element's position and `animateScrollTo()` it into view.
2. **Error-driven scroll**: when `notesError`, `customerError`, or `totalError` transitions from null to non-null, scroll to the error.
3. **Cart-driven scroll**: after `onCartNotesChange` adds a new item, scroll the cart section to the bottom so the new item is visible.
4. **Bottom-sheet open scroll**: when `AddProductBottomSheet` opens, immediately request focus on the search field and scroll to it (already partially done via `delay(350)` + `requestFocus()`, but scroll is missing).

### Affected files
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderEditorSheet.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderCartSummary.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/AddProductBottomSheet.kt`

---

## Priority ranking

| Rank | Pain point | Impact | Effort | Files |
|------|------------|--------|--------|-------|
| P0 | BottomSheet back button loses input | Data loss on accidental back | Small | `AddProductBottomSheet`, `OrderEditorSheet` |
| P0 | Product section hidden behind keyboard | Cannot see what you are ordering | Medium | `OrderEditorSheet`, `AddProductBottomSheet`, `OrderEditorFields` |
| P1 | Touch targets too small | Mis-taps, accessibility fail | Small | `OrderEditorCustomerSection`, `AddProductBottomSheet` |
| P1 | Spacing too cramped | Hard to scan, fatigue | Small | `OrderEditorSheet`, `OrderCartSummary`, `OrderEditorCustomerSection` |
| P1 | Auto-scroll missing | Errors / new items off-screen | Medium | `OrderEditorSheet`, `OrderCartSummary`, `AddProductBottomSheet` |
| P2 | Font readability | Slower reading, eye strain | Small | `Type`, `AddProductBottomSheet`, `OrderEditorSheet` |
| P2 | OnFocus feedback weak | Users unsure which field is active | Small | `OrderEditorFields`, `OrderEditorSheet` |

---

## Implementation status

| Pain point | Status | Commit details |
|------------|--------|----------------|
| BottomSheet back button loses input | **Done** | `isDirty` state + `confirmValueChange` on `ModalBottomSheet`, `BackHandler` guard, confirmation `AlertDialog`, animated dismiss via `sheetState.hide()`. Removed redundant `showAddProductSheet` branch from `OrderEditorSheet.BackHandler`. |
| Product section hidden behind keyboard | **Done** | `BringIntoViewRequester` added to customer name field, product search field, and unit-price field. Auto-scrolls on focus gain. |
| Touch targets too small | **Done** | Customer suggestion rows bumped from `44.dp` to `48.dp`. Product suggestion `Surface`s now carry `sizeIn(minHeight = 48.dp)` instead of inner `Text`. |
| Auto-scroll missing | **Partial** | Focus-driven scroll implemented. Cart-driven scroll, error-driven scroll, and main editor auto-scroll still pending. |
| Spacing too cramped | **Partial** | Cart item row vertical padding bumped from `4.dp` to `12.dp`. Root horizontal padding and section spacing still pending. |
| Font readability | **Partial** | Quantity display reduced from `headlineSmall` (24sp) to `titleLarge` (21sp). Label/error text bumps still pending. |
| OnFocus feedback weak | **Pending** | No custom focus background tint added yet. |
| Bottom sheet height/keyboard snap | **Done** | Adopted `skipPartiallyExpanded = true`, `windowInsets = WindowInsets.ime.union(WindowInsets.systemBars)`, and dynamic `fillMaxHeight`/`fillMaxSize` content sizing. |

---

## Acceptance criteria for the fix batch
1. Pressing the Android back button while `AddProductBottomSheet` has input no longer discards the input.
2. Tapping any text field in the order editor automatically scrolls the field above the keyboard.
3. All tappable rows in customer/product suggestion dropdowns are at least `48.dp` tall.
4. Adding a product causes the cart list to scroll so the new item is visible.
5. Error text appears at `14sp` minimum; field labels appear at `13sp` minimum.
6. Horizontal root padding in the order editor is at least `12.dp`.
7. No regressions in save/cancel/validation behavior.

## Verification checklist
1. Open Add Product sheet, type a product name, press Android back — confirm a guard or confirmation appears instead of instant dismiss.
2. Tap the customer name field with a long form, confirm the field scrolls into view above the keyboard.
3. Tap the unit-price field in Add Product sheet, confirm it stays visible when keyboard opens.
4. Tap suggestion dropdown items and measure/confirm they are at least 48dp tall.
5. Add multiple products and confirm each new row auto-scrolls into view.
6. Trigger a validation error (e.g. blank total), confirm the error text is readable and the form scrolls to it.
7. Run `./gradlew lintDebug` and confirm no new accessibility warnings.
