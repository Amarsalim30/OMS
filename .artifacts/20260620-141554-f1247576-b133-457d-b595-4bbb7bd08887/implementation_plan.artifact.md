# Implement Quick Fill Product Selection

The goal is to enhance the "Add Product" flow with a "Quick Fill" feature, allowing users to select common products with a single tap instead of searching.

## Proposed Changes

### [Database Layer]

#### [ProductDao.kt](file:///home/amar-salim/StudioProjects/OMS/app/src/main/java/com/zeynbakers/order_management_system/product/data/ProductDao.kt)

- **Query for popular products**: Add a function to fetch the top 6 most frequently ordered products.

```kotlin
@Query("""
    SELECT p.* FROM products p
    JOIN order_items oi ON p.id = oi.productId
    WHERE p.archived = 0
    GROUP BY p.id
    ORDER BY COUNT(oi.id) DESC
    LIMIT 6
""")
suspend fun getPopularProducts(): List<ProductEntity>
```

---

### [Order Feature]

#### [DayDetailViewModel.kt](file:///home/amar-salim/StudioProjects/OMS/app/src/main/java/com/zeynbakers/order_management_system/order/ui/day_detail/DayDetailViewModel.kt)

- **Expose suggestions**: Add a `StateFlow<List<ProductEntity>>` for quick fill suggestions.
- **Fetch on initialization**: Load the popular products when the ViewModel starts.

#### [DayDetailScreen.kt](file:///home/amar-salim/StudioProjects/OMS/app/src/main/java/com/zeynbakers/order_management_system/order/ui/day_detail/DayDetailScreen.kt)

- **Collect suggestions**: Collect the `quickFillProducts` from the ViewModel.
- **Pass to Dialog**: Pass the list to `DayOrderEditorDialog`.

#### [DayDetailDialogs.kt](file:///home/amar-salim/StudioProjects/OMS/app/src/main/java/com/zeynbakers/order_management_system/order/ui/day_detail/components/DayDetailDialogs.kt)

- **Pass to BottomSheet**: Update `DayOrderEditorDialog` to accept and pass `quickFillProducts` to `OrderEditorSheet`.

#### [OrderEditorSheet.kt](file:///home/amar-salim/StudioProjects/OMS/app/src/main/java/com/zeynbakers/order_management_system/order/ui/order_editor/OrderEditorSheet.kt)

- **Pass to BottomSheet**: Pass `quickFillProducts` to `AddProductBottomSheet`.

#### [AddProductBottomSheet.kt](file:///home/amar-salim/StudioProjects/OMS/app/src/main/java/com/zeynbakers/order_management_system/order/ui/order_editor/components/AddProductBottomSheet.kt)

- **UI Layout**: Implement a `LazyRow` with `SuggestionChip` below the search bar.
- **Auto-Focus**: Use a `FocusRequester` to programmatically focus the quantity field when a chip is tapped.

```kotlin
// In AddProductBottomSheet
LazyRow(
    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(horizontal = 16.dp)
) {
    items(quickFillProducts) { product ->
        SuggestionChip(
            onClick = {
                // 1. Select product
                // 2. Request focus on quantity field
            },
            label = { Text("${product.emoji} ${product.name}") }
        )
    }
}
```

---

## Verification Plan

### Manual Verification
1.  **Open Add Product**: Click "Add Item" in the order editor.
2.  **Verify Quick Fill**: Observe a row of 6 (or fewer) product chips below the search bar.
3.  **Tap a Chip**: Tap a product chip.
4.  **Verify Result**: The product details should populate, the "Quantity" field should be focused, and the numeric keyboard should open automatically.
