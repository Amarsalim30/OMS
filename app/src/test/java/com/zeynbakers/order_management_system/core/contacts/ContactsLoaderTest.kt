package com.zeynbakers.order_management_system.core.contacts

import android.os.OperationCanceledException
import com.zeynbakers.order_management_system.ContactsLoadFailureKind
import com.zeynbakers.order_management_system.classifyContactsLoadFailure
import org.junit.Assert.assertEquals
import org.junit.Test

class ContactsLoaderTest {

    @Test
    fun `classifies cancelled provider query as transient`() {
        val result = classifyContactsLoadFailure(OperationCanceledException("cancelled"))

        assertEquals(ContactsLoadFailureKind.Transient, result)
    }

    @Test
    fun `classifies projection mismatch as permanent`() {
        val result = classifyContactsLoadFailure(IllegalArgumentException("bad projection"))

        assertEquals(ContactsLoadFailureKind.Permanent, result)
    }

    @Test
    fun `classifies runtime provider failure as transient`() {
        val result = classifyContactsLoadFailure(RuntimeException("provider died"))

        assertEquals(ContactsLoadFailureKind.Transient, result)
    }
}
