package com.zeynbakers.order_management_system.product.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "default_price")
    val defaultPrice: BigDecimal,
    
    @ColumnInfo(name = "emoji")
    val emoji: String,
    
    @ColumnInfo(name = "archived", defaultValue = "0")
    val archived: Boolean = false
)
