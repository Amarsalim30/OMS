class OrderProcessor(
    private val orderDao: OrderDao,
    private val accountingDao: AccountingDao
) {

    suspend fun completeOrder(orderId: Long) {
        val order = orderDao.getOrderById(orderId)
            ?: throw IllegalStateException("Order not found")

        if (order.status == OrderStatus.COMPLETED) return

        orderDao.markCompleted(orderId)

        accountingDao.insertAccountEntry(
            AccountEntryEntity(
                customerId = order.customerId,
                amount = order.totalAmount,
                type = EntryType.DEBIT,
                referenceId = orderId,
                description = "Order #$orderId"
            )
        )
    }
}
    