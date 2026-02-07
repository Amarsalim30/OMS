# Title: Orders (Unpaid) screen redesign to "Toasted Utility" system

## Context
Project: OrderBook (Android/Compose)
Goal: Redesign the Orders (Unpaid) screen to match the "Toasted Utility" design system, focusing on a **professional, sophisticated, and minimalist** aesthetic.

Screen:
- Orders (Unpaid list)

Files:
- `app/src/main/java/com/zeynbakers/order_management_system/order/ui/UnpaidOrdersScreen.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/ui/AppScaffold.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/ui/theme/Color.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/ui/theme/Theme.kt`
- `app/src/main/java/com/zeynbakers/order_management_system/core/ui/theme/Type.kt`

## Current behavior (facts)
- The Orders screen is visually dark and monochromatic with weak hierarchy.
- Summary card uses a muted surface container and does not emphasize the balance.
- Order cards are note-first and text-heavy; key financial information is not scannable.
- "Record payment" is a text button with low visual prominence.
- Dates are ISO formatted (YYYY-MM-DD) and scroll with the content (not sticky).
- Bottom navigation includes a Settings gear alongside the 4 primary tabs.

## Expected behavior (requirements)

### 1) Global Theme & Aesthetics (Sophisticated Minimalism)
- **Palette**: Use "Toasted Utility" colors but with sophisticated application:
  - Primary: `Color(0xFFC06014)` (Toasted Copper) - use sparsely for high-priority actions/data.
  - Surface (Dark): `Color(0xFF121212)` - Deep elegant black/gray.
  - SurfaceVariant (Card BG): `Color(0xFF1E1E1E)` - Subtle contrast against background.
  - OnSurface (Text): `Color(0xFFE0E0E0)` - High readability.
  - OnSurfaceVariant (Subtext): `Color(0xFFA0A0A0)` - Distinct secondary information.
  - Status/Error: `Color(0xFFEF5350)` (Soft Red) for "Due" amounts.
- **Typography**:
  - **Monospace** (e.g., JetBrains Mono/Roboto Mono) for ALL currency and numerical data to ensure perfect alignment and "utility" feel.
  - **Sans-serif** (e.g., Roboto/Inter) for names and notes.

### 2) Summary Dashboard (Top of screen)
- **Design**: A "Statement Card" that floats at the top.
- **Visuals**: Dark Card with a subtle left-edge accent strip (Toasted Copper).
- **Content**:
  - **Label**: "TOTAL OUTSTANDING" (Overline/SmallCaps, tracking spaced, opacity 0.7).
  - **Value**: "KES 1,900" (Display Large, Monospace, Bold).
  - **Context**: "2 orders pending" (separated by a subtle dot divider).
- **Layout**: Clean, spacious, vertically centered.

### 3) Order List (Smart Scanning & Hierarchy)
- **List Structure**:
  - Use **Sticky Headers** for dates. As the user scrolls, the date stays pinned until the section passes.
- **Date Formatting**:
  - Use **Relative Dates** for recent items: "Today", "Yesterday".
  - Use readable formats for older dates: "Mon, 24 Oct".
  - Avoid raw `2023-10-24`.
- **Card Design**:
  - **Minimalist Container**: `Surface` with low tonal elevation (1.dp) and rounded corners (12.dp).
  - **Header Row**:
    - **Left**: Customer Name (Title Medium, Bold, Color: Primary/White).
      - *Logic*: If no customer, show "Walk-in" with a small `Person` or `Storefront` icon in muted color.
    - **Right**: Time/Date (if needed here, otherwise represented by sticky header).
  - **Content Body**:
    - Product/Note (Body Medium, Color: OnSurfaceVariant, maxLines=2).
  - **Financial Data (The "Utility" Part)**:
    - **Visual Bar**: A thin, elegant `LinearProgressIndicator` (height 4.dp, rounded caps) showing % paid.
      - Track color: Very dark grey. Progress color: Primary (if partial) or Green (if fully paid - though this screen is for unpaid).
    - **Data Row** (Below bar):
      - Left: "Paid: 100" (Small, Monospace).
      - Right: "Due: KES 900" (Bold, Red/Primary, Monospace).
  - **Actions**:
    - "Record Payment": **OutlinedButton** (White border, low opacity) -> Becomes solid on hover/press.
    - **Call Action**: If phone number exists, show a subtle Circular `IconButton` (Phone icon).

### 4) Navigation & Layout
- **TopAppBar**: Clean, center-aligned title "Unpaid Orders".
  - Move "Settings" icon here (top right).
- **Bottom Navigation**:
  - 4 items evenly spaced.
  - Remove "Settings" from bottom tab.

### 5) Polish & Micro-interactions
- **Touch Targets**: Ensure all buttons satisfy 48dp min touch target.
- **Empty State**:
  - If no orders: Show a minimalist icon (e.g., `CheckCircle` or `Inbox`) with text "All caught up! No unpaid orders."
  - Center aligned, vertically spaced.

## Acceptance Criteria (DoD)
1. **Sticky Date Headers** implemented with relative formatting ("Today", "Yesterday").
2. **Hierarchy Fix**: Customer Name > Note.
3. **Financial Clarity**: Monospace fonts for numbers, Progress bar for payment status.
4. **Summary Card**: High-impact "Statement" design.
5. **Settings Moved**: From Bottom Nav to TopAppBar.
6. **Walk-in Handling**: Explicit label/icon for null customers.
7. **The "Look"**: Dark mode, high contrast for text, subtle backgrounds for cards.

## Implementation Plan (Order of Operations)
1. **Theme**: Verify Colors and Typography in `Theme.kt`.
2. **Scaffold**: specific updates to `UnpaidOrdersScreen` top bar actions.
3. **Components**:
   - Create `StickyDateHeader` composable.
   - Refactor `UnpaidOrderRow` to new "Card" spec.
   - Redesign `SummaryCard`.
4. **Integration**: specific logic for date formatting and grouping.
