@file:Suppress("unused")

package com.zeynbakers.order_management_system.core.db

import androidx.room.*
import com.zeynbakers.order_management_system.core.helper.data.HelperNoteType
import kotlinx.datetime.LocalDate
import java.math.BigDecimal
import java.math.RoundingMode

class Converters {
    companion object {
        private const val MONEY_SCALE = 2
        private val HUNDRED = BigDecimal("100")
    }

    // LocalDate <-> String
    @TypeConverter
    fun fromLocalDate(date: LocalDate): String = date.toString()

    @TypeConverter
    fun toLocalDate(value: String): LocalDate = LocalDate.parse(value)

    // BigDecimal <-> String
    @TypeConverter
    fun fromBigDecimal(decimal: BigDecimal): String {
        return decimal
            .setScale(MONEY_SCALE, RoundingMode.HALF_UP)
            .multiply(HUNDRED)
            .toBigIntegerExact()
            .toString()
    }

    @TypeConverter
    fun toBigDecimal(value: String): BigDecimal {
        return BigDecimal(value).divide(HUNDRED).setScale(MONEY_SCALE, RoundingMode.HALF_UP)
    }

    // Enums
    @TypeConverter
    fun fromOrderStatus(status: com.zeynbakers.order_management_system.order.data.OrderStatus): String =
        status.name

    @TypeConverter
    fun toOrderStatus(value: String): com.zeynbakers.order_management_system.order.data.OrderStatus =
        com.zeynbakers.order_management_system.order.data.OrderStatus.valueOf(value)

    @TypeConverter
    fun fromOrderStatusOverride(
        status: com.zeynbakers.order_management_system.order.data.OrderStatusOverride?
    ): String? = status?.name

    @TypeConverter
    fun toOrderStatusOverride(
        value: String?
    ): com.zeynbakers.order_management_system.order.data.OrderStatusOverride? =
        value?.let { com.zeynbakers.order_management_system.order.data.OrderStatusOverride.valueOf(it) }

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

    @TypeConverter
    fun fromReceiptStatus(
        status: com.zeynbakers.order_management_system.accounting.data.PaymentReceiptStatus
    ): String = status.name

    @TypeConverter
    fun toReceiptStatus(
        value: String
    ): com.zeynbakers.order_management_system.accounting.data.PaymentReceiptStatus =
        com.zeynbakers.order_management_system.accounting.data.PaymentReceiptStatus.valueOf(value)

    @TypeConverter
    fun fromAllocationType(
        type: com.zeynbakers.order_management_system.accounting.data.PaymentAllocationType
    ): String = type.name

    @TypeConverter
    fun toAllocationType(
        value: String
    ): com.zeynbakers.order_management_system.accounting.data.PaymentAllocationType =
        com.zeynbakers.order_management_system.accounting.data.PaymentAllocationType.valueOf(value)

    @TypeConverter
    fun fromAllocationStatus(
        status: com.zeynbakers.order_management_system.accounting.data.PaymentAllocationStatus
    ): String = status.name

    @TypeConverter
    fun toAllocationStatus(
        value: String
    ): com.zeynbakers.order_management_system.accounting.data.PaymentAllocationStatus =
        com.zeynbakers.order_management_system.accounting.data.PaymentAllocationStatus.valueOf(value)

    @TypeConverter
    fun fromHelperNoteType(type: HelperNoteType): String = type.name

    @TypeConverter
    fun toHelperNoteType(value: String): HelperNoteType = HelperNoteType.valueOf(value)
}
