package com.zeynbakers.order_management_system.core.db

import androidx.room.*
import kotlinx.datetime.LocalDate
import java.math.BigDecimal

class Converters {

    // LocalDate <-> String
    @TypeConverter
    fun fromLocalDate(date: LocalDate): String = date.toString()

    @TypeConverter
    fun toLocalDate(value: String): LocalDate = LocalDate.parse(value)

    // BigDecimal <-> String
    @TypeConverter
    fun fromBigDecimal(decimal: BigDecimal): String = decimal.toPlainString()

    @TypeConverter
    fun toBigDecimal(value: String): BigDecimal = BigDecimal(value)

    // Enums
    @TypeConverter
    fun fromOrderStatus(status: com.zeynbakers.order_management_system.order.data.OrderStatus): String =
        status.name

    @TypeConverter
    fun toOrderStatus(value: String): com.zeynbakers.order_management_system.order.data.OrderStatus =
        com.zeynbakers.order_management_system.order.data.OrderStatus.valueOf(value)

    @TypeConverter
    fun fromItemCategory(category: com.zeynbakers.order_management_system.order.data.ItemCategory): String =
        category.name

    @TypeConverter
    fun toItemCategory(value: String): com.zeynbakers.order_management_system.order.data.ItemCategory =
        com.zeynbakers.order_management_system.order.data.ItemCategory.valueOf(value)

    @TypeConverter
    fun fromPaymentMethod(method: com.zeynbakers.order_management_system.accounting.data.PaymentMethod): String =
        method.name

    @TypeConverter
    fun toPaymentMethod(value: String): com.zeynbakers.order_management_system.accounting.data.PaymentMethod =
        com.zeynbakers.order_management_system.accounting.data.PaymentMethod.valueOf(value)

    @TypeConverter
    fun fromEntryType(type: com.zeynbakers.order_management_system.accounting.data.EntryType): String =
        type.name

    @TypeConverter
    fun toEntryType(value: String): com.zeynbakers.order_management_system.accounting.data.EntryType =
        com.zeynbakers.order_management_system.accounting.data.EntryType.valueOf(value)
}
