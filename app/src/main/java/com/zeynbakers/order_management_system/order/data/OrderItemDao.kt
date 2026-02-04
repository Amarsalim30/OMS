@file:Suppress("unused")

package com.zeynbakers.order_management_system.order.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface OrderItemDao {

    @Query("SELECT * FROM order_items")
    suspend fun getAllOrderItems(): List<OrderItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<OrderItemEntity>)
}
