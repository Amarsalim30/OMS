@file:Suppress("unused")

package com.zeynbakers.order_management_system.customer.data

import androidx.room.*

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["phone"], unique = true),
        Index(value = ["name"]),
        Index(value = ["isArchived"])
    ]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,
    val phone: String,
    val isArchived: Boolean = false
)
