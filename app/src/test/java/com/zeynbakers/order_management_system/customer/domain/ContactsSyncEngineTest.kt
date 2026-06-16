package com.zeynbakers.order_management_system.customer.domain

import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import com.zeynbakers.order_management_system.customer.ui.ImportContact
import org.junit.Assert.assertEquals
import org.junit.Test

class ContactsSyncEngineTest {

    @Test
    fun `marks unseen contact as new`() {
        val preview =
            previewContactImportStatuses(
                existingCustomers = emptyList(),
                contacts = listOf(ImportContact(name = "Asha", phone = "+254712345678"))
            )

        assertEquals(ContactImportPreviewStatus.New, preview["+254712345678"])
    }

    @Test
    fun `marks archived matching contact as update`() {
        val preview =
            previewContactImportStatuses(
                existingCustomers =
                    listOf(
                        CustomerEntity(
                            id = 1L,
                            name = "Asha Bakers",
                            phone = "+254712345678",
                            isArchived = true
                        )
                    ),
                contacts = listOf(ImportContact(name = "Asha Bakers", phone = "+254712345678"))
            )

        assertEquals(ContactImportPreviewStatus.Update, preview["+254712345678"])
    }

    @Test
    fun `marks matching active contact as existing`() {
        val preview =
            previewContactImportStatuses(
                existingCustomers =
                    listOf(
                        CustomerEntity(
                            id = 1L,
                            name = "Asha Bakers",
                            phone = "+254712345678",
                            isArchived = false
                        )
                    ),
                contacts = listOf(ImportContact(name = "Asha Bakers", phone = "+254712345678"))
            )

        assertEquals(ContactImportPreviewStatus.Existing, preview["+254712345678"])
    }
}
