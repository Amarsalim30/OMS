# Money/Payment Screen — UX Review & Improvement Plan

## 1. Screen Purpose

The **Money** screen is a two-tab hub for collecting and recording customer payments in the OMS bakery app:
- **Collect (M-Pesa Import)** — paste raw M-Pesa SMS messages, auto-detect transactions, match them to customers/orders, then post them in bulk.
- **Record (Manual)** — search a customer, enter an amount + method, optionally pick an order, and save a single payment.

The screen's purpose is communicated adequately through the tab labels, but the tab names ("Collect" / "Record") are jargon-like. The M-Pesa tab flow is especially opaque on first visit.

---

## 2. UI/UX Score

**6 / 10**

Solid structural bones (Scaffold + LazyColumn, Material 3 components, sticky header), but suffers from density, discoverability, and ergonomics problems.

---

## 3. Critical UX Issues

| # | Issue | File | Severity |
|---|-------|------|----------|
| 1 | **Tab labels are jargon** — "Collect" and "Record" are not plain-language; users must guess which tab does M-Pesa import vs. manual entry | [MoneyScreen.kt](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/core/ui/MoneyScreen.kt) | High |
| 2 | **No empty-state guidance on Collect tab** — when no text is pasted, a bare `Text("No payments detected")` appears; there's no onboarding hint for how to get M-Pesa messages | [PaymentIntakeScreen.kt](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeScreen.kt) | High |
| 3 | **ApplyReadyBar is not visually distinct enough** — uses `tonalElevation = 3.dp` on `Surface` but blends into background; the primary CTA ("Apply X selected") competes with "Apply all ready" on the same surface | [PaymentIntakeSections.kt](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeSections.kt) | High |
| 4 | **Customer suggestions rendered as `TextButton`** — each suggestion is a `TextButton` which doesn't look like a selectable list item; no visual divider/separator, so 8 results stacked up are hard to scan | [ManualPaymentScreen.kt](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/ManualPaymentScreen.kt) | High |
| 5 | **[ApplyReadyBar](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeSections.kt#257-303) uses a `FlowRow`** that wraps the buttons: on narrow screens the "Apply selected" button and "Apply all ready" can end up on different lines making the primary action hard to find | [PaymentIntakeSections.kt](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeSections.kt) | Medium |
| 6 | **Hardcoded status strings in [MpesaTransactionRow](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/MpesaTransactionRow.kt#24-118)** (e.g. `"Already recorded (voided)"`, `"Code $it"`, `"No code"`, `"From $it"`, `"Duplicate"`, `"Needs match"`, `"Ready"`, `"Selected"`) — these bypass the strings resource system and break localization | [MpesaTransactionRow.kt](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/MpesaTransactionRow.kt) | Medium |
| 7 | **Save Payment button is right-aligned only** — bottom bar uses `Arrangement.End` so the button occupies only the right side; easy to miss on small or one-hand-use scenarios | [ManualPaymentScreen.kt](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/ManualPaymentScreen.kt) | Medium |
| 8 | **Filter dropdown for transaction list** — a `TextButton` + `DropdownMenu` pattern for 4 fixed options is heavy; `FilterChip` tabs or a `SegmentedButton` would be more discoverable | [PaymentIntakeScreen.kt](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeScreen.kt) | Medium |

---

## 4. Layout & Visual Hierarchy Problems

- **8dp grid violations**: `contentPadding` uses `15.dp` top (Collect) and `12.dp`/`12.dp` sides (Record). Should be multiples of 4 or 8 dp consistently.
- **`verticalArrangement = Arrangement.spacedBy(4.dp)`** in the `LazyColumn` for transactions is too tight — 8 dp between cards is the minimum for comfortable scanning.
- **[SummaryPill](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeSections.kt#237-256)** text (`"$label $value"`) combines label and value in one string — value jumps around when it changes; use a fixed-width number or structured [Row](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeSections.kt#39-83).
- **[IntakeSummaryRow](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeSections.kt#131-174)** is a horizontally-scrolling row but nothing communicates that it is scrollable (no fade/gradient at edges).
- **Typography hierarchy on [ManualPaymentScreen](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/ManualPaymentScreen.kt#64-475)**: Section labels use `titleSmall` but the field contents use standard field styling — the spacing between section labels and their fields (0 dp implicit) is tighter than the spacing between sections (8 dp), which is inverted.

---

## 5. Accessibility Issues

- **[MpesaTransactionRow](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/MpesaTransactionRow.kt#24-118)** — the whole row is clickable via `.clickable { onOpenDetails() }` but has no `contentDescription`; screen readers cannot describe what tapping Opens.
- **`Checkbox` inside the row** has no explicit `contentDescription` — tick/untick state is not labelled for TalkBack.
- **Status badge in [MpesaTransactionRow](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/MpesaTransactionRow.kt#24-118)** is a `Surface` + `Text` with no semantic role — screen readers skip it or read raw text without context.
- **[ApplyReadyBar](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeSections.kt#257-303) Button** — `contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)` makes it shorter than 48 dp vertically. **This is a touch-target violation.**
- **`FilterChip` in [ManualPaymentScreen](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/ManualPaymentScreen.kt#64-475)** has `modifier = Modifier.sizeIn(minHeight = 48.dp)` ✅, but the `FilterChip` on the allocation row (Pick Order / Oldest Orders) does not, so those chips may fall below 48 dp. Should be consistent.
- **Color-only status differentiation** in [MpesaTransactionRow](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/MpesaTransactionRow.kt#24-118) (statusColors) — duplicates = red, needs-match = tertiary/yellow-ish, ready = green-ish, selected = primary. For colorblind users there is no shape/icon distinction.

---

## 6. Android Platform Consistency

- **Good**: `Scaffold`, `TopAppBar`, `ModalBottomSheet`, `FilterChip`, `OutlinedTextField`, `LazyColumn` with `stickyHeader` — all M3-idiomatic.
- **Needs fix**: The inline `BasicTextField` + `OutlinedTextFieldDefaults.DecorationBox` for the SMS paste area is an `@Deprecated` code path (the file itself is annotated `@file:Suppress("DEPRECATION")`). Prefer `OutlinedTextField` with multi-line support.
- **Needs fix**: Tab host ([MoneyScreen](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/core/ui/MoneyScreen.kt#31-98)) wraps `TabRow` in a `Surface(tonalElevation = 1.dp)` which loses the `TabRow`'s native indicator styling. M3 `TabRow` takes its container color from `MaterialTheme.colorScheme.surfaceContainer` via `PrimaryTabIndicator` — the outer `Surface` fight each other.
- **Good**: Navigation back icon uses `Icons.AutoMirrored.Filled.ArrowBack` correctly.

---

## 7. UX Improvements (Actionable)

1. **Rename tabs** → **"M-Pesa"** and **"Cash / Manual"** (or icon + label — M-Pesa icon + cash icon).
2. **Rich empty state on Collect tab**: When `rawText.isBlank()`, show a centered card with step illustration: "1. Open M-Pesa > Messages  2. Copy the message text  3. Paste here". Include a prominent "Paste" button directly in the empty state.
3. **[ApplyReadyBar](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeSections.kt#257-303) redesign**: Use a `BottomAppBar` styled surface. Show "X ready • KES N" at left; put a single `Button(FilledButton)` "Apply Ready (N)" at right. Remove the "Apply selected" / "Apply all ready" split — instead, tapping the button always applies ready; a long-press or secondary `OutlinedButton` handles "selected only".
4. **Customer suggestion list**: Replace `TextButton` with `ListItem` (M3) — `headlineContent = name + phone`, `supportingContent = balance label`, `leadingContent = CircleAvatar/AccountCircle icon`.
5. **Transaction list filter**: Replace the `TextButton` + `DropdownMenu` with M3 `SingleChoiceSegmentedButtonRow` (or a horizontally scrolling row of `FilterChip`s) so all 4 filter options are visible at a glance.
6. **Status badges with icons**: Add a small leading icon to each status badge (`Check`, `Warning`, `ContentCopy`, `RadioButtonUnchecked`) so colorblind users have a second signal.
7. **Fix touch targets**: Ensure *all* `FilterChip` and `Button` components in the apply bar have `minHeight = 48.dp`.
8. **Move `Save Payment` button full-width**: In [ManualPaymentScreen](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/ManualPaymentScreen.kt#64-475) bottom bar, stretch the `Button` to `fillMaxWidth()` for easier one-hand thumb reach.
9. **Externalize hardcoded strings** in [MpesaTransactionRow](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/MpesaTransactionRow.kt#24-118) to [strings.xml](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/res/values/strings.xml).
10. **Fix 8dp grid**: Change `contentPadding` `15.dp` → `16.dp`, `4.dp` list spacing → `8.dp`.

---

## 8. Improved Layout Structure

### MoneyScreen (Tab Host)
```
Scaffold
  topBar: Surface(tonalElevation = 0)  // let TabRow own its color
    TabRow(selectedTabIndex)
      Tab(icon=MpesaIcon, text="M-Pesa")
      Tab(icon=CashIcon,  text="Manual")
  content: MpesaImportScreen | ManualPaymentScreen
```

### MpesaImportScreen (Collect Tab)
```
Scaffold
  bottomBar: ApplyReadyBar (only when readyCount > 0)
  content: LazyColumn(contentPadding=16dp all sides, verticalSpacing=8dp)
    item: PasteInputCard
      if blank → EmptyStateHint (icon + steps + PasteButton)
      else     → OutlinedTextField (multiline) + action buttons (FlowRow)
    stickyHeader: FilterChipRow (All / Needs / Duplicates / Selected)
    items: MpesaTransactionRow (with accessible contentDescription)
```

### ManualPaymentScreen (Record Tab)
```
Scaffold
  bottomBar: Surface(3dp elevation)
    Button(fillMaxWidth, enabled=canSave) { "Save Payment" }
  content: LazyColumn(contentPadding=16dp, verticalSpacing=12dp)
    item: CustomerSelectionCard
      if no customer → SearchField + ListItem suggestions
      else           → CustomerCard + "Change" button
    item: AmountCard
      SectionLabel("Amount")
      OutlinedTextField(amount)
    item: MethodCard
      SectionLabel("Method")
      SingleChoiceSegmentedButtonRow: Cash | M-Pesa
    item: NoteCard
      OutlinedTextField(note, optional)
    item: AllocationCard (visible when customer selected)
      SectionLabel("Allocation")
      FilterChip: "Oldest orders" | "Pick order"
      selectedOrderLabel (if order picked)
```

---

## 9. Proposed Compose Implementation

### Changes summary by file

| File | Change |
|------|--------|
| [MoneyScreen.kt](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/core/ui/MoneyScreen.kt) | Rename tab labels in [strings.xml](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/res/values/strings.xml); remove extra `Surface` wrapper around `TabRow` |
| [PaymentIntakeScreen.kt](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeScreen.kt) | Fix `contentPadding` (`15dp→16dp`), list spacing (`4dp→8dp`); replace filter dropdown with `FilterChip` row; add empty-state composable |
| [PaymentIntakeSections.kt](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeSections.kt) | Redesign [ApplyReadyBar](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeSections.kt#257-303): full-width layout, single primary `Button`, fix touch targets; add icons to [SummaryPill](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeSections.kt#237-256) |
| [MpesaTransactionRow.kt](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/MpesaTransactionRow.kt) | Add `contentDescription` to row + checkbox; add icon to status badge; move hardcoded strings to [strings.xml](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/res/values/strings.xml) |
| [ManualPaymentScreen.kt](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/ManualPaymentScreen.kt) | `fillMaxWidth()` on Save button; replace `TextButton` suggestions with `ListItem`; fix allocation `FilterChip` touch targets |
| [strings.xml](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/res/values/strings.xml) | Add new string keys for tab labels, status labels, empty state, [MpesaTransactionRow](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/MpesaTransactionRow.kt#24-118) hardcoded strings |

---

## 10. Optional Enhancements

- **Animated counter** in [ApplyReadyBar](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeSections.kt#257-303) using `AnimatedContent` — when count changes, number slides up/down.
- **Progressive disclosure** in the M-Pesa paste field — auto-collapse after paste (already partially done) but add a subtle `AnimatedVisibility` for smoother UX.
- **Shimmer loading state** while `transactions` is computing (currently it just jumps from empty to populated).
- **Swipe-to-select** on [MpesaTransactionRow](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/MpesaTransactionRow.kt#24-118) — swipe right = select, swipe left = deselect, using `SwipeToDismissBox`.

---

## Verification Plan

### Automated Tests

**Existing test to run (no changes needed):**
```
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.zeynbakers.order_management_system.ui.AccessibilitySmokeTest
```
This checks `MpesaAllocationSheet`, `CustomerListScreen`, `CalendarScreen`, and `DayDetailScreen` touch targets and discoverability. After our changes this must still pass.

**New tests to add to [AccessibilitySmokeTest.kt](file:///c:/Users/USER/Documents/CODING/OMS/app/src/androidTest/java/com/zeynbakers/order_management_system/ui/AccessibilitySmokeTest.kt):**

1. `applyReadyBarButtonMeetsTouchTarget` — render [ApplyReadyBar(readyCount=2, ...)](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeSections.kt#257-303) in isolation; assert the `Button` node height ≥ 48 dp.
2. `mpesaTransactionRowHasContentDescription` — render [MpesaTransactionRow(...)](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/MpesaTransactionRow.kt#24-118) in isolation; assert the row node has a non-empty `contentDescription` semantic.
3. `manualPaymentSaveButtonIsFullWidth` — render [ManualPaymentScreen](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/ManualPaymentScreen.kt#64-475) in isolation with a customer set; assert the "Save Payment" button width ≥ `maxWidth × 0.9`.

**Run all instrumentation tests:**
```
./gradlew connectedDebugAndroidTest
```

**Run JVM unit tests:**
```
./gradlew testDebugUnitTest
```

**Run lint:**
```
./gradlew lintDebug
```

### Manual Verification

1. Build debug APK: `./gradlew assembleDebug`, install on device/emulator.
2. Open the **Money** tab from the bottom nav.
3. **Collect tab**:
   - Verify tabs now read "M-Pesa" and "Manual" (or equivalent updated labels).
   - Verify empty state shows an illustration + step-by-step hint.
   - Paste an M-Pesa message. Verify the filter chips (All/Needs/Duplicates/Selected) are visible inline, not hidden in a dropdown.
   - Verify the [ApplyReadyBar](file:///c:/Users/USER/Documents/CODING/OMS/app/src/main/java/com/zeynbakers/order_management_system/accounting/ui/PaymentIntakeSections.kt#257-303) button is at least 48 dp tall and appears full-width or prominently on the bottom bar.
4. **Manual tab**:
   - Run on a phone form-factor; verify the "Save Payment" button spans the full width of the bottom bar.
   - Search for a customer; verify results appear as `ListItem` rows (not unstyled text buttons).
   - Check that all `FilterChip`s (Oldest orders / Pick order) are ≥ 48 dp height.
5. Enable **TalkBack**, navigate the M-Pesa transaction list; verify each row is read with a meaningful description including the amount and sender.
