package com.zeynbakers.order_management_system.core.helper.data

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class HelperNoteWithCustomer(
    @Embedded
    val note: HelperNoteEntity,
    @ColumnInfo(name = "customerName")
    val customerName: String?
)
