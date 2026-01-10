class PaymentProcessor(
    private val accountingDao: AccountingDao
) {

    suspend fun recordPayment(
        customerId: Long,
        amount: Double,
        note: String?
    ) {
        accountingDao.insertAccountEntry(
            AccountEntryEntity(
                customerId = customerId,
                amount = amount,
                type = EntryType.CREDIT,
                description = note ?: "Payment"
            )
        )
    }
}
