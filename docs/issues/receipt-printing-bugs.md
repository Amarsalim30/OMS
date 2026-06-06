# Receipt Printing Bugs

## Bug #1: Receipt Shows No Products (Empty Order Items)

**Location**: `app/src/main/java/com/zeynbakers/order_management_system/order/ui/DayDetailScreen.kt:186`

**Description**: The `printOrder` function passes an empty list for `orderItems` to the receipt formatter, causing receipts to print without any product lines.

```kotlin
suspend fun printOrder(order: OrderEntity, macAddress: String, printerName: String) {
    val customerLabel = order.customerId?.let { customerNames[it] }
    val customerPhone = order.customerId?.let { customerPhones[it] }
    val orderItems = emptyList<OrderItemEntity>()  // BUG: Empty list
    val receiptText = ReceiptFormatter.formatOrder(storeName, order, orderItems, customerLabel, customerPhone)
    ...
}
```

**Impact**: Receipts only show order header, total, and thank you message - no product details.

**Root Cause**: The `DayDetailScreen` doesn't have a callback parameter to load order items from the database. The screen receives `orders: List<OrderEntity>` but not the associated items.

**Fix Required**:
1. Add a callback parameter to `DayDetailScreen`: `loadOrderItems: suspend (Long) -> List<OrderItemEntity>`
2. Pass this callback from `CalendarGraph.kt` using `orderViewModel.orderRepository.getOrderItems(orderId)`
3. In `printOrder`, load actual items: `val orderItems = loadOrderItems(order.id)`

---

## Bug #2: Duplicate Products Can Be Added to Cart

**Location**: `app/src/main/java/com/zeynbakers/order_management_system/order/ui/AddProductBottomSheet.kt:426`

**Description**: When adding a product to the cart, the code simply appends without checking if an item with the same name already exists.

```kotlin
onCartItemsChange(cartItems + newItem)  // No deduplication check
```

**Impact**: Users can add the same product multiple times, creating duplicate entries in the cart that get saved to the database.

**Root Cause**: No deduplication logic in the cart. The `OrderCartSummary` component doesn't merge items with the same name.

**Fix Required**:
- In `AddProductBottomSheet`, check if an item with the same name already exists
- If it exists, update the quantity instead of adding a new entry
- Alternatively, add deduplication in `OrderCartSummary` or before saving to database

---

## Bug #3: Database Lacks Unique Constraint for Order Items

**Location**: `app/src/main/java/com/zeynbakers/order_management_system/order/data/OrderItemEntity.kt:25`

**Description**: The `order_items` table has no UNIQUE constraint on `(orderId, productNameSnapshot)` to prevent duplicate items for the same order.

```kotlin
@Entity(
    tableName = "order_items",
    indices = [Index("orderId"), Index("productId")]  // No composite unique index
)
```

**Impact**: 
- Even if cart deduplication is added, database inserts with `OnConflictStrategy.REPLACE` won't prevent duplicates
- The `saveOrderTransactional` function deletes existing items before inserting, but this relies on application logic rather than database constraints

**Root Cause**: Missing database constraint to enforce uniqueness at the database level.

**Fix Required**:
- Add a composite unique index: `Index(value = ["orderId", "productNameSnapshot"], unique = true)`
- This would require a database migration

---

## Bug #4: Import Merge Logic May Create Duplicates

**Location**: `app/src/main/java/com/zeynbakers/order_management_system/order/ui/OrderViewModel.kt:210-233`

**Description**: The `mergeOrderItems` function merges existing and import items by name, but if the import has duplicate items with the same name, they will be added separately.

```kotlin
private fun mergeOrderItems(
    existingItems: List<OrderItemEntity>,
    importItems: List<OrderItemDraft>
): List<OrderItemDraft> {
    val mergedItems = existingItems.map { ... }.toMutableList()
    
    importItems.forEach { importItem ->
        val existing = mergedItems.find { it.name == importItem.name }
        if (existing != null) {
            mergedItems[mergedItems.indexOf(existing)] = existing.copy(quantity = existing.quantity + importItem.quantity)
        } else {
            mergedItems.add(importItem)  // No check for duplicates within importItems
        }
    }
    
    return mergedItems
}
```

**Impact**: If the imported JSON has duplicate entries for the same product, they will appear as separate items after merge.

**Root Cause**: The import logic doesn't deduplicate `importItems` before merging.

**Fix Required**:
- Deduplicate `importItems` by name before the merge loop
- Use `importItems.groupBy { it.name }.map { (name, items) -> items.reduce { acc, item -> acc.copy(quantity = acc.quantity + item.quantity) } }`

---

## Summary

The receipt printing has multiple interconnected issues:

1. **Immediate bug**: Receipts print with no products because `printOrder` uses an empty list
2. **Data integrity bugs**: Cart and database allow duplicate items, which would cause duplicate product lines on receipts once bug #1 is fixed
3. **Import bug**: Merged orders can have duplicate items from the import source

**Recommended Fix Order**:
1. Fix bug #1 first (add order items loading callback) - this is the immediate user-facing issue
2. Fix bug #2 (cart deduplication) - prevents user error
3. Fix bug #4 (import deduplication) - prevents data corruption from imports
4. Fix bug #3 (database constraint) - adds long-term data integrity protection (requires migration)
