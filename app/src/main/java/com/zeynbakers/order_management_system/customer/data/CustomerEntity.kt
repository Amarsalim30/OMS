@file:Suppress("unused")

package com.zeynbakers.order_management_system.customer.data

import androidx.room.*

@Entity(
    tableName = "customers",
    indices = [Index(value = ["phone"], unique = true)]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,
    val phone: String
)
