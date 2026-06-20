# Fix Payment Reactivity via Unified DAO Join

The goal is to provide a clean, reactive architecture that automatically updates the UI when either an order is modified or a payment is recorded. We achieve this by moving the relational aggregation into the Room DAO layer.

## Proposed Changes

### [Database Layer]

#### [NEW] [OrderWithPaymentSummary.kt](file:///home/amar-salim/StudioProjects/OMS/app/src/main/java/com/zeynbakers/order_management_system/order/data/OrderWithPaymentSummary.kt)

Create a POJO to hold the unified data.

```kotlin
data class OrderWithPaymentSummary(
    @Embedded val order: OrderEntity,
    val paidAmount: BigDecimal
)
```

#### [OrderDao.kt](file:///home/amar-salim/StudioProjects/OMS/app/src/main/java/com/zeynbakers/order_management_system/order/data/OrderDao.kt)

Add an optimized JOIN query. Room will automatically track both `orders` and `account_entries` for invalidation.

```kotlin
@Transaction
@Query("""
    SELECT o.*,
    (
        SELECT IFNULL(SUM(CASE
            WHEN ae.type IN ('CREDIT', 'WRITE_OFF') THEN ae.amount
            WHEN ae.type = 'REVERSAL' THEN -ae.amount
            ELSE 0 END), 0)
        FROM account_entries ae
        WHERE ae.orderId = o.id
    ) as paidAmount
    FROM orders o
    WHERE o.orderDate = :date AND o.status != 'CANCELLED'
    ORDER BY o.createdAt DESC
""")
fun getOrdersWithPaymentsByDate(date: String): Flow<List<OrderWithPaymentSummary>>
```

---

### [Order Feature]

#### [DayDetailViewModel.kt](file:///home/amar-salim/StudioProjects/OMS/app/src/main/java/com/zeynbakers/order_management_system/order/ui/day_detail/DayDetailViewModel.kt)

Simplify the `ViewModel` to a clean observation pipeline.

```kotlin
class DayDetailViewModel(...) : ViewModel() {
    private val _currentDate = MutableStateFlow(initialDate)

    @OptIn(ExperimentalCoroutinesApi::class)
    val orders: StateFlow<List<OrderUiModel>> = _currentDate
        .flatMapLatest { date ->
            orderDao.getOrdersWithPaymentsByDate(date.toString())
        }
        .map { summaries ->
            summaries.map { summary ->
                OrderUiModel(
                    order = summary.order,
                    items = itemDao.getOrderItems(summary.order.id),
                    customer = summary.order.customerId?.let { customerDao.getById(it) },
                    paidAmount = summary.paidAmount
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
```

---

## Verification Plan

### Manual Verification
1.  **Reactive Payment Update**: Open `DayDetailScreen`. Note the "Unpaid" status.
2.  **Receive Payment**: Add a payment for that order.
3.  **Verify**: The status should change to "Paid" or "Partial" **instantly** without any manual reload or navigation.
4.  **Edit Order**: Change the order total. Verify the balance updates immediately.
