package com.zeynbakers.order_management_system.core.navigation

import kotlinx.datetime.LocalDate

object AppRoutes {
    const val Splash = "splash"
    const val Intro = "intro"
    const val SetupChecklist = "setup_checklist"
    const val BusinessProfile = "business_profile"
    const val ContactsPermissionPrimer = "contacts_permission_primer"
    const val NotificationsPermissionPrimer = "notifications_permission_primer"
    const val MicrophonePermissionPrimer = "microphone_permission_primer"
    const val OverlayPermissionPrimer = "overlay_permission_primer"
    const val Calendar = "calendar"
    const val CalendarTutorial = "calendar_tutorial"
    const val Orders = "orders"
    const val Customers = "customers"
    const val Money = "money"
    const val Summary = "summary"
    const val Backup = "backup"
    const val Notifications = "notifications"
    const val NotesHistory = "notes_history"
    const val HelperSettings = "helper_settings"
    const val Tutorial = "tutorial"
    const val FirstRunTutorial = "first_run_tutorial"
    const val ImportContacts = "import_contacts"

    const val Day = "day/{date}"
    const val CustomerDetail = "customer/{customerId}"
    const val CustomerStatement = "customer/{customerId}/statement"

    const val PaymentHistoryAll = "payment_history/all"
    const val PaymentHistoryCustomer = "payment_history/customer/{customerId}"
    const val PaymentHistoryOrder = "payment_history/order/{orderId}"

    const val ARG_DATE = "date"
    const val ARG_CUSTOMER_ID = "customerId"
    const val ARG_ORDER_ID = "orderId"
    const val ARG_FOCUS_RECEIPT_ID = "focusReceiptId"

    fun day(date: LocalDate): String = "day/${date}"
    fun customerDetail(customerId: Long): String = "customer/$customerId"
    fun customerStatement(customerId: Long): String = "customer/$customerId/statement"
    fun paymentHistoryCustomer(customerId: Long): String = "payment_history/customer/$customerId"
    fun paymentHistoryOrder(orderId: Long): String = "payment_history/order/$orderId"
    fun paymentHistoryAll(focusReceiptId: Long? = null): String {
        return if (focusReceiptId == null) {
            PaymentHistoryAll
        } else {
            "${PaymentHistoryAll}?${ARG_FOCUS_RECEIPT_ID}=$focusReceiptId"
        }
    }
}
