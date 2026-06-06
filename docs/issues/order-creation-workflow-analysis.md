# Order Creation Workflow Analysis & OrderItems Bug

## Date
June 1, 2026

## Overview
This document provides a comprehensive analysis of the Order Creation workflow from UI editor through database persistence to display screens, and identifies a critical bug in OrderItems handling during order editing.

---

# COMPLETE ORDER CREATION WORKFLOW

## 1. UI Layer - Order Editor Components

### OrderEditorSheet.kt (`order/ui/OrderEditorSheet.kt`)
- **Purpose:** Main composable dialog for order creation and editing
- **Responsibilities:**
  - Manages order editor state (customer, cart, pickup time, total)
  - Integrates customer selection via OrderEditorCustomerSection
  - Displays cart via OrderCartSummary
  - Handles pickup time selection with time picker
  - Validates form before save
  - Calls onSave callback with order data
- **Key Parameters:**
  - `cartItems: List<OrderItemDraft>` - Current cart items
  - `onCartItemsChange: (List<OrderItemDraft>) -> Unit` - Cart update callback
  - `customerName, customerPhone` - Customer information
  - `pickupTimeText` - Pickup time string
  - `canSave: Boolean` - Validation state
  - `onSave: () -> Unit` - Save action callback

### OrderEditorCustomerSection.kt (`order/ui/OrderEditorCustomerSection.kt`)
- **Purpose:** Customer selection and creation UI component
- **Responsibilities:**
  - Displays customer search input
  - Shows customer suggestions dropdown
  - Allows creating new customer from query
  - Displays confirmed customer as InputChip badge
- **Key Features:**
  - Customer suggestion dropdown with top 5 matches
  - "Create customer" option when no exact match found
  - Customer confirmation state management

### OrderCartSummary.kt (`order/ui/OrderCartSummary.kt`)
- **Purpose:** Displays and manages cart items in order editor
- **Responsibilities:**
  - Renders list of OrderItemDraft items
  - Shows emoji, name, unit price, quantity, line total per item
  - Allows item deletion via delete button
  - Triggers AddProductBottomSheet for adding items
- **Display Format:**
  - Item row: `[emoji] name (@ price) x quantity = total`

### AddProductBottomSheet.kt (`order/ui/AddProductBottomSheet.kt`)
- **Purpose:** Bottom sheet for adding products to cart
- **Responsibilities:**
  - Product search and selection from catalog
  - Custom product creation
  - Quantity and unit price input
  - Creates OrderItemDraft objects
  - Adds to cart via onCartItemsChange callback
- **Key Features:**
  - Product suggestion dropdown
  - "Create product" option for custom items
  - Line total calculation display
  - Form dirty state management
  - Keyboard and focus management

### OrderEditorFields.kt (`order/ui/OrderEditorFields.kt`)
- **Purpose:** Shared UI components for order editor
- **Components:**
  - `OrderEditorFieldShape` - RoundedCornerShape(12.dp) for consistent styling
  - `OrderEditorOutlinedField` - Reusable OutlinedTextField with theming

### OrderItemDraft.kt (`order/ui/OrderItemDraft.kt`)
- **Purpose:** Temporary UI state class for order items before saving
- **Properties:**
  - `emoji: String` - Product emoji for display
  - `name: String` - Product/item name
  - `quantity: Int` - Item quantity
  - `unitPrice: BigDecimal` - Unit price (default ZERO)
- **Computed Property:**
  - `lineTotal: BigDecimal` = unitPrice * quantity

---

## 2. UI Layer - Order Screens

### DayDetailScreen.kt (`order/ui/DayDetailScreen.kt`)
- **Purpose:** Main screen for viewing and managing orders for a specific date
- **Responsibilities:**
  - Displays orders for selected date with filtering and search
  - Opens order editor for creating/editing orders
  - Handles order deletion with payment allocation
  - Manages order import/export
  - Bluetooth printing integration
- **Key State:**
  - `cartItems: List<OrderItemDraft>` - Current cart for order editor
  - `editingOrderId: Long?` - Order being edited (null for new)
  - `isEditorOpen: Boolean` - Editor dialog visibility
  - `draft: OrderDraft?` - Persisted draft state
- **Callbacks:**
  - `onSaveOrder: (List<OrderItemDraft>, BigDecimal, String, String, String?, Long?) -> Unit`
  - `onDeleteOrder: (Long) -> Unit`
  - `onOrderPaymentHistory: (Long) -> Unit`
  - `onReceivePayment: (OrderEntity) -> Unit`

### CalendarScreen.kt (`order/ui/CalendarScreen.kt`)
- **Purpose:** Calendar view for navigating dates and quick order creation
- **Responsibilities:**
  - Displays monthly calendar with order indicators
  - Shows payment status colors per day
  - Quick add order via floating action button
  - Navigates to DayDetailScreen on date tap
- **Key Features:**
  - Month navigation and selection
  - Payment status legend
  - Week start configuration
  - Tutorial integration

### UnpaidOrdersScreen.kt (`order/ui/UnpaidOrdersScreen.kt`)
- **Purpose:** Screen for viewing and managing unpaid/partially paid orders
- **Responsibilities:**
  - Displays all unpaid orders with filtering
  - Grouping by date or flat list
  - Search functionality
  - Swipe-to-delete with confirmation
  - Navigation to day detail or payment receipt
- **Filters:**
  - NEWEST, OLDEST, LARGEST_DUE, OVERDUE
- **Key State:**
  - `orders: List<OrderEntity>` - Unpaid orders
  - `paidAmounts: Map<Long, BigDecimal>` - Payment amounts per order
  - `customerNames: Map<Long, String>` - Customer lookup

### SummaryScreen.kt (`order/ui/SummaryScreen.kt`)
- **Purpose:** Summary screen for chef prep lists and order aggregation
- **Responsibilities:**
  - Aggregates order items by product for chef prep
  - Date range selection (day/week/month)
  - Copy chef list to clipboard
  - Daily breakdown view
- **Key Features:**
  - Mode selection: DAY, WEEK, MONTH
  - Chef message generation
  - Order summary cards
  - Month total display

---

## 3. UI Layer - Models and State

### OrderUiState.kt (`order/ui/OrderUiState.kt`)
- **Purpose:** UI state container for order-related screens
- **Properties:**
  - `date: LocalDate` - Current selected date
  - `orders: List<OrderEntity>` - Orders for the date
  - `editingOrder: OrderEntity?` - Order currently being edited

### DayDetailModels.kt (`order/ui/DayDetailModels.kt`)
- **Purpose:** Models and utilities for day detail screen
- **Key Components:**
  - `PaymentState` enum: UNPAID, PARTIAL, PAID, OVERPAID
  - `DayOrderFilter` enum: All, Due, NoPayment, Partial, Paid, Overpaid
  - `OrderDraft` data class: Persisted order editor state
  - `DaySummaryStats` data class: Statistics for day orders
- **Functions:**
  - `resolvePaymentState(total, paidAmount)` - Determine payment status
  - `computeDayStats(orders, paidAmounts, dayTotal)` - Calculate day statistics
  - `sortOrdersForPlanner(orders)` - Sort by pickup time
  - `dayOrderFilterOptions()` - Generate filter options with counts

### SummarySections.kt (`order/ui/SummarySections.kt`)
- **Purpose:** Composable components for summary screen
- **Components:**
  - `OrderSummaryCard` - Displays order with items and customer
  - `MonthTotalCard` - Shows month total
  - `ChefPrepCard` - Date range selector and chef list
  - `ProductRow` - Single product in chef list
  - `DailySummaryCard` - Orders grouped by date

---

## 4. Data Layer - Database Entities

### OrderEntity.kt (`order/data/OrderEntity.kt`)
- **Purpose:** Database entity for orders table
- **Properties:**
  - `id: Long` - Primary key (auto-generated)
  - `orderDate: LocalDate` - Order date
  - `createdAt: Long` - Creation timestamp
  - `updatedAt: Long` - Last update timestamp
  - `pickupTime: String?` - Pickup time (nullable)
  - `status: OrderStatus` - PENDING, CONFIRMED, COMPLETED, CANCELLED
  - `statusOverride: OrderStatusOverride?` - OPEN, CLOSED (nullable)
  - `totalAmount: BigDecimal` - Order total
  - `customerId: Long?` - Foreign key to customer (SET_NULL on delete)
- **Indexes:**
  - orderDate, customerId
  - Composite: (orderDate, createdAt, id), (customerId, orderDate, createdAt, id)

### OrderItemEntity.kt (`order/data/OrderItemEntity.kt`)
- **Purpose:** Database entity for order_items table
- **Properties:**
  - `id: Long` - Primary key (auto-generated)
  - `orderId: Long` - Foreign key to orders (CASCADE on delete)
  - `productId: Long?` - Foreign key to products (RESTRICT on delete, nullable for custom items)
  - `productNameSnapshot: String` - Immutable product name snapshot
  - `unitPriceSnapshot: BigDecimal` - Immutable unit price snapshot
  - `categorySnapshot: ItemCategory` - BAKED, FRIED, OTHER
  - `quantity: Int` - Item quantity
  - `priceOverride: BigDecimal?` - Optional price override at order time
- **Computed Property:**
  - `effectivePrice: BigDecimal` = priceOverride ?: unitPriceSnapshot
- **Indexes:**
  - orderId, productId

### OrderWithItems.kt (`order/data/OrderWithItems.kt`)
- **Purpose:** Domain model for order with its items (Room relation)
- **Components:**
  - `OrderWithItems` - Order entity with list of OrderItemEntity
  - `OrderItemWithProduct` - Order item joined with product information
- **OrderItemWithProduct Properties:**
  - Item fields from OrderItemEntity
  - `productName: String?` - Current product name from catalog
  - `productEmoji: String?` - Current product emoji from catalog
  - `productDefaultPrice: BigDecimal?` - Current product default price
- **Computed Properties:**
  - `displayName: String` - productName ?: productNameSnapshot
  - `displayEmoji: String?` - productEmoji
  - `effectivePrice: BigDecimal` - priceOverride ?: unitPriceSnapshot
  - `lineTotal: BigDecimal` - effectivePrice * quantity

---

## 5. Data Layer - DAOs

### OrderDao.kt (`order/data/OrderDao.kt`)
- **Purpose:** DAO interface for order database operations
- **Key Methods:**
  - `insert(order): Long` - Insert single order
  - `insertAll(orders)` - Insert multiple orders
  - `update(order)` - Update existing order
  - `delete(order)` - Delete order
  - `getOrdersByDate(date): List<OrderEntity>` - Get orders for specific date
  - `getOrdersByCustomer(customerId): List<OrderEntity>` - Get orders for customer
  - `getOpenOrdersByCustomer(customerId)` - Get non-cancelled, non-closed orders
  - `getOpenOrdersLimited(limit)` - Get open orders with limit
  - `getOrderById(orderId): OrderEntity?` - Get single order by ID
  - `getOrdersByIds(orderIds): List<OrderEntity>` - Get multiple orders by IDs
  - `markCompleted(orderId)` - Set status to COMPLETED
  - `markCancelled(orderId)` - Set status to CANCELLED
  - `updateStatusOverride(orderId, statusOverride, updatedAt)` - Update status override
  - `getTotalBilled(customerId): BigDecimal?` - Sum of order totals for customer
  - `getTotalForDate(date): BigDecimal` - Sum of order totals for date
  - `getTotalBetween(start, end): BigDecimal` - Sum of order totals in date range
  - `getCustomerTotals(): List<CustomerTotal>` - Customer order totals grouped by customer
  - `totalBilled(customerId): BigDecimal` - Total billed for customer (active orders only)
  - `getOrdersBetween(start, end): List<OrderEntity>` - Get orders in date range

### OrderItemDao.kt (`order/data/OrderItemDao.kt`)
- **Purpose:** DAO interface for order item database operations
- **Key Methods:**
  - `getAllOrderItems(): List<OrderItemEntity>` - Get all items
  - `getOrderItems(orderId): List<OrderItemEntity>` - Get items for specific order
  - `getOrderItemsFlow(orderId): Flow<List<OrderItemEntity>>` - Reactive items for order
  - `getOrderItemsByProduct(productId): List<OrderItemEntity>` - Get items for product
  - `insertAll(items)` - Insert multiple items
  - `insert(item): Long` - Insert single item
  - `deleteOrderItems(orderId)` - Delete all items for an order
  - `deleteOrderItem(itemId)` - Delete single item by ID
  - `updateOrderItemQuantity(itemId, quantity)` - Update item quantity
  - `updateOrderItemPrice(itemId, priceOverride)` - Update item price override
  - `getOrderWithItems(orderId): OrderWithItems?` - Get order with items (transaction)
  - `getOrderItemsWithProducts(orderId): List<OrderItemWithProduct>` - Get items with product details (SQL join)

---

## 6. Domain Layer

### OrderRepository.kt (`order/domain/OrderRepository.kt`)
- **Purpose:** Repository providing clean abstraction over data layer
- **Order Operations:**
  - `getOrderById(orderId): OrderEntity?`
  - `getOrderWithItems(orderId): OrderWithItems?`
  - `getOrderItemsWithProducts(orderId): List<OrderItemWithProduct>`
  - `getOrdersByDate(date): List<OrderEntity>`
  - `getOrdersByCustomer(customerId): List<OrderEntity>`
  - `saveOrder(order): Long`
  - `updateOrder(order)`
  - `deleteOrder(order)`
- **Order Item Operations:**
  - `getOrderItems(orderId): List<OrderItemEntity>`
  - `getOrderItemsFlow(orderId): Flow<List<OrderItemEntity>>`
  - `saveOrderItems(items): Unit`
  - `deleteOrderItems(orderId): Unit`
  - `deleteOrderItem(itemId): Unit`
  - `updateOrderItemQuantity(itemId, quantity): Unit`
  - `updateOrderItemPrice(itemId, priceOverride): Unit`
- **Product Analytics:**
  - `getProductUsageStats(productId): ProductUsageStats`
    - Returns totalQuantity, totalRevenue, orderCount for a product

---

## 7. ViewModel Layer

### OrderViewModel.kt (`order/ui/OrderViewModel.kt`)
- **Purpose:** Main ViewModel for order operations across screens
- **Dependencies:**
  - AppDatabase (provides OrderDao, OrderItemDao, CustomerDao, ProductDao, AccountingDao, etc.)
  - OrderRepository
  - PaymentReceiptProcessor
- **State Flows:**
  - `calendarDays: List<CalendarDayUi>` - Calendar day data
  - `ordersForDate: List<OrderEntity>` - Orders for selected date
  - `dayTotal: BigDecimal` - Total for selected date
  - `summaryOrders: List<OrderEntity>` - Orders for summary range
  - `summaryTotal: BigDecimal` - Total for summary range
  - `monthTotal: BigDecimal` - Total for current month
  - `monthBadgeCount: Int` - Unpaid count for month
  - `monthSnapshots: Map<MonthKey, MonthSnapshot>` - Cached month data
  - `orderCustomerNames: Map<Long, String>` - Customer name lookup
  - `orderPaidAmounts: Map<Long, BigDecimal>` - Payment amounts per order
  - `unpaidOrders: List<OrderEntity>` - All unpaid orders
  - `creditPrompt: OrderCreditPrompt?` - Credit availability prompt
- **Key Methods:**
  - `saveOrder(date, cartItems, totalAmount, customerName, customerPhone, pickupTime, existingOrderId)`
    - Public entry point for saving orders
    - Wraps saveOrderTransactional in database transaction
    - Refreshes calendar data after save
  - `saveOrderTransactional(date, cartItems, totalAmount, customerName, customerPhone, pickupTime, existingOrderId): SaveOrderResult`
    - Core save logic executed in transaction
    - Steps:
      1. Resolve/create customer
      2. Create or update OrderEntity
      3. Delete existing order items if editing (BUG: only when cartItems.isNotEmpty())
      4. Convert OrderItemDraft to OrderItemEntity
      5. Insert new order items if cartItems.isNotEmpty()
      6. Update accounting entries
      7. Reconcile order settlement
      8. Build credit prompt if eligible
  - `importOrders(actions, targetDate)`
    - Import orders with CREATE or MERGE actions
    - Uses mergeOrderItems for merging existing and import items
  - `mergeOrderItems(existingItems, importItems): List<OrderItemDraft>`
    - Merges existing order items with import items by name
    - Sums quantities for matching items
  - `cancelOrder(orderId, date)`
    - Marks order as CANCELLED
    - Cleans up accounting entries
    - Refreshes calendar and unpaid orders
  - `loadOrdersForDate(date)`
    - Loads orders for specific date
    - Computes day total
    - Loads customer names and paid amounts
  - `loadMonth(month, year, forceRefresh)`
    - Loads calendar data for month
    - Caches snapshots for performance
  - `loadUnpaidOrders()`
    - Loads all unpaid orders
    - Loads customer names and paid amounts
  - `ensureProduct(name, defaultPrice, emoji): ProductEntity`
    - Ensures product exists in catalog
    - Creates if not found
  - `searchCustomers(query): List<CustomerEntity>`
  - `searchProducts(query): List<ProductEntity>`

---

## 8. Navigation Layer

### OrdersGraph.kt (`core/navigation/graphs/OrdersGraph.kt`)
- **Purpose:** Navigation graph for orders-related screens
- **Route:** `AppRoutes.Orders`
- **Screen:** `UnpaidOrdersScreen`
- **Navigation Actions:**
  - `onBack` - Pop back stack
  - `onOpenDay(date, orderId)` - Navigate to day detail with optional order focus
  - `onReceivePayment(order)` - Navigate to payment receipt with order context
  - `onDeleteOrder(order)` - Cancel order via ViewModel
- **Integration:**
  - Uses OrderViewModel for data
  - Uses AppOrdersState for shared state
  - Uses AppCalendarCallbacks for date navigation
  - Uses AppFeatureNavigationActions for cross-feature navigation

---

# ORDER CREATION WORKFLOW STEPS

## Step 1: User Opens Order Editor
**Entry Points:**
- DayDetailScreen: FAB button or edit order action
- CalendarScreen: Quick add from calendar day cell

**Component:** `DayDetailDialogs.DayOrderEditorDialog` or `CalendarScreen` quick add sheet

**Actions:**
1. Initialize empty cartItems list
2. Set editingOrderId to null (new order) or existing order ID (edit)
3. Open OrderEditorSheet dialog
4. If editing, load existing order data (customer, pickup time)

## Step 2: Add Items to Cart
**Component:** `AddProductBottomSheet`

**Actions:**
1. User taps "Add Product" in OrderCartSummary
2. AddProductBottomSheet opens
3. User searches/selects product or creates custom
4. User enters quantity and unit price
5. Line total calculated and displayed
6. User taps "Add Item to Order"
7. OrderItemDraft created with emoji, name, quantity, unitPrice
8. OrderItemDraft added to cartItems list via onCartItemsChange
9. Bottom sheet closes

## Step 3: Display Cart
**Component:** `OrderCartSummary`

**Actions:**
1. Iterates through cartItems list
2. For each item, displays:
   - Emoji + name
   - Unit price (if > 0)
   - Quantity
   - Line total
   - Delete button
3. User can delete items, which removes from cartItems
4. Cart total calculated from sum of lineTotals

## Step 4: Select Customer
**Component:** `OrderEditorCustomerSection`

**Actions:**
1. User types customer name
2. Customer suggestions dropdown appears (top 5 matches)
3. User can:
   - Select existing customer → fills name and phone
   - Create new customer → confirms customer
4. Customer displayed as InputChip badge when confirmed
5. Customer can be cleared by tapping X on badge

## Step 5: Set Pickup Time
**Component:** `OrderEditorSheet` (ValueRow + TimePicker)

**Actions:**
1. User taps pickup time row
2. TimePicker dialog opens
3. User selects hour and minute
4. Time formatted as HH:MM and saved
5. Quick pickup time chips available (09:00, 12:00, 15:00, 18:00)

## Step 6: Validate and Save
**Component:** `DayDetailDialogs.submitOrder()` or `CalendarScreen.submitOrder()`

**Validation Checks:**
1. cartItems must not be empty
2. Total must be valid and > 0
3. Pickup time must be valid (if provided)

**If Valid:**
1. Call OrderViewModel.saveOrder() with:
   - date
   - cartItems (List<OrderItemDraft>)
   - totalAmount (BigDecimal)
   - customerName
   - customerPhone
   - pickupTime
   - existingOrderId (null for new, ID for edit)
2. Clear form state
3. Close editor dialog

## Step 7: Database Transaction
**Component:** `OrderViewModel.saveOrderTransactional()`

**Steps:**
1. **Resolve Customer:**
   - Normalize phone number
   - Search for existing customer by phone
   - Create new customer if not found
   - Return customerId or null

2. **Create/Update Order:**
   - If existingOrderId provided: fetch existing OrderEntity
   - Create or update OrderEntity with:
     - orderDate
     - totalAmount
     - customerId
     - pickupTime
     - updatedAt (current timestamp)
   - Insert or update in database
   - Get resulting orderId

3. **Save Order Items (BUG LOCATION):**
   - **BUG:** Delete existing items only if cartItems.isNotEmpty()
   - Convert each OrderItemDraft to OrderItemEntity:
     - orderId
     - productId = null (not resolved from catalog)
     - productNameSnapshot = draft.name
     - unitPriceSnapshot = draft.unitPrice
     - categorySnapshot = OTHER
     - quantity = draft.quantity
   - Insert all OrderItemEntity if cartItems.isNotEmpty()

4. **Update Accounting:**
   - Update customerId for order entries if changed
   - Upsert debit entry for order
   - Reconcile order settlement to total

5. **Build Credit Prompt:**
   - Check if customer has available credit
   - Build OrderCreditPrompt if eligible

6. **Return Result:**
   - date
   - affectedMonths (for calendar refresh)
   - creditPrompt

## Step 8: Refresh UI
**Component:** OrderViewModel after save

**Actions:**
1. loadOrdersForDate(result.date)
2. refreshMonthSnapshots(result.affectedMonths)
3. Show credit prompt if applicable

## Step 9: Display in Screens
**Components:** DayDetailScreen, CalendarScreen, UnpaidOrdersScreen, SummaryScreen

**DayDetailScreen:**
1. Receives updated orders list via StateFlow
2. Displays orders with payment status
3. Shows order count and day total
4. Orders can be edited or deleted

**CalendarScreen:**
1. Receives updated calendarDays via StateFlow
2. Updates day cells with payment status colors
3. Updates month total and badge count

**UnpaidOrdersScreen:**
1. Receives updated unpaidOrders via StateFlow
2. Displays unpaid orders with filters
3. Shows total outstanding amount

**SummaryScreen:**
1. Receives orders for selected range
2. Aggregates order items for chef list
3. Displays daily breakdown

---

# IDENTIFIED ISSUES

## Issue 1: ProductId Not Preserved in Order Items (FIXED)

**Status:** ✅ FIXED - June 1, 2026

**Problem:** `OrderItemDraft` did not contain `productId`, so all items were saved with `productId = null` in `OrderItemEntity`, even when selected from the catalog. This broke:
- `getProductUsageStats(productId)` - could not query by productId
- Product analytics - no reliable product sales tracking
- Name-based product matching was fragile (duplicate names)
- Foreign key relationship to products was unused

**Fix Applied:**
1. Added `productId: Long?` and `categorySnapshot: ItemCategory?` fields to `OrderItemDraft`
2. Updated `AddProductBottomSheet` to pass `productId` when creating `OrderItemDraft`
3. Updated `OrderViewModel.saveOrderTransactional` to use `draft.productId` and `draft.categorySnapshot`
4. Updated `OrderViewModel.mergeOrderItems` to preserve `productId` and `category` during merge
5. Updated `OrderViewModel.importOrders` to preserve `productId` in import logic

**Files Modified:**
- `OrderItemDraft.kt` - Added productId and categorySnapshot fields
- `AddProductBottomSheet.kt` - Pass productId and category when creating OrderItemDraft
- `OrderViewModel.kt` - Use draft.productId in saveOrderTransactional, update mergeOrderItems and importOrders

---

## Issue 2: Total Amount Not Derived from Cart Items (FIXED)

**Status:** ✅ FIXED - June 1, 2026

**Problem:** `totalAmount` was passed separately from `cartItems`, creating two sources of truth that could be inconsistent. The UI calculated total from cartItems, but users could manually edit the total, and both values were passed to saveOrder without validation.

**Fix Applied:**
1. Changed `saveOrderTransactional` to derive total from `cartItems.sumOf { it.lineTotal }`
2. Removed `totalAmount` parameter from `saveOrder` and `saveOrderTransactional` signatures
3. Updated all call sites to remove `totalAmount` parameter:
   - `CalendarScreen.kt`
   - `DayDetailDialogs.kt`
   - `DayDetailScreen.kt`
   - `CalendarGraph.kt`
   - `AccessibilitySmokeTest.kt`

**Files Modified:**
- `OrderViewModel.kt` - Derive total from cartItems in saveOrderTransactional
- `CalendarScreen.kt` - Remove totalAmount from onSaveOrder callback
- `DayDetailDialogs.kt` - Remove totalAmount from onSaveOrder callback
- `DayDetailScreen.kt` - Remove totalAmount from onSaveOrder callback
- `CalendarGraph.kt` - Remove totalAmount from all onSaveOrder call sites
- `AccessibilitySmokeTest.kt` - Update test mocks

---

## Issue 3: Merge Order Items by Name Only (FIXED)

**Status:** ✅ FIXED - June 1, 2026

**Problem:** `mergeOrderItems()` merged by name only, which was fragile:
- Products with same name but different IDs would merge incorrectly
- No productId preservation during merge
- Category information lost during merge

**Fix Applied:**
Updated `mergeOrderItems` to:
1. Match by productId first, then by name as fallback
2. Preserve productId from existing item if import has none
3. Preserve categorySnapshot during merge

**Files Modified:**
- `OrderViewModel.kt` - Updated mergeOrderItems function

---

## Issue 4: Empty-Cart Deletion Bug (FALSE POSITIVE)

**Status:** ❌ FALSE POSITIVE - Not reachable in production

**Finding:** The validation logic in both `DayDetailDialogs.submitOrder()` (lines 138-142) and `CalendarScreen.submitOrder()` (lines 564-568) explicitly checks `cartItems.isEmpty()` and sets an error state, preventing `onSaveOrder()` from being called when the cart is empty.

**Conclusion:** The empty-cart deletion bug is **not reachable** in production because validation prevents empty carts from reaching `saveOrderTransactional()`. No fix required.
