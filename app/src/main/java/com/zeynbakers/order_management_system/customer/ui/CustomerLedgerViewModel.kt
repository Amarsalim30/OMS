class CustomerLedgerViewModel(
    private val accountingDao: AccountingDao
) : ViewModel() {

    private val _ledger = MutableStateFlow<LedgerSummary?>(null)
    val ledger = _ledger.asStateFlow()

    fun loadLedger(customerId: Long) {
        viewModelScope.launch {
            val totals = accountingDao.getLedgerTotals(customerId)
            val billed = totals.billed ?: 0.0
            val paid = totals.paid ?: 0.0

            _ledger.value = LedgerSummary(
                customerId,
                billed,
                paid,
                billed - paid
            )
        }
    }
}
