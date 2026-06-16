@file:Suppress("unused")

package com.zeynbakers.order_management_system.order.domain

import com.zeynbakers.order_management_system.order.data.OrderDao
import com.zeynbakers.order_management_system.order.data.OrderEntity
import com.zeynbakers.order_management_system.order.data.OrderItemDao
import com.zeynbakers.order_management_system.order.data.OrderItemEntity
import com.zeynbakers.order_management_system.order.data.OrderWithItems
import com.zeynbakers.order_management_system.order.data.OrderItemWithProduct
import com.zeynbakers.order_management_system.order.data.ItemCategory
import com.zeynbakers.order_management_system.product.data.ProductEntity
import com.zeynbakers.order_management_system.product.data.ProductDao
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

/**
 * Repository for order operations.
 * Provides a clean abstraction over the data layer and handles business logic.
 */
class OrderRepository(
    private val orderDao: OrderDao,
    private val orderItemDao: OrderItemDao,
    private val productDao: ProductDao
) {

    // Order operations
    suspend fun getOrderById(orderId: Long): OrderEntity? = orderDao.getOrderById(orderId)
    
    suspend fun getOrderWithItems(orderId: Long): OrderWithItems? = 
        orderItemDao.getOrderWithItems(orderId)
    
    suspend fun getOrderItemsWithProducts(orderId: Long): List<OrderItemWithProduct> = 
        orderItemDao.getOrderItemsWithProducts(orderId)
    
    suspend fun getOrdersByDate(date: String): List<OrderEntity> = 
        orderDao.getOrdersByDate(date)
    
    suspend fun getOrdersByCustomer(customerId: Long): List<OrderEntity> = 
        orderDao.getOrdersByCustomer(customerId)
    
    suspend fun saveOrder(order: OrderEntity): Long = orderDao.insert(order)
    
    suspend fun updateOrder(order: OrderEntity) = orderDao.update(order)
    
    suspend fun deleteOrder(order: OrderEntity) = orderDao.delete(order)
    
    // Order item operations
    suspend fun getOrderItems(orderId: Long): List<OrderItemEntity> =
        orderItemDao.getOrderItems(orderId)

    suspend fun getOrderItemsFlow(orderId: Long): Flow<List<OrderItemEntity>> =
        orderItemDao.getOrderItemsFlow(orderId)

    suspend fun saveOrderItems(items: List<OrderItemEntity>) =
        orderItemDao.insertAll(items)

    suspend fun deleteOrderItems(orderId: Long) =
        orderItemDao.deleteOrderItems(orderId)

    suspend fun deleteOrderItem(itemId: Long) =
        orderItemDao.deleteOrderItem(itemId)

    suspend fun updateOrderItemQuantity(itemId: Long, quantity: Int) =
        orderItemDao.updateOrderItemQuantity(itemId, quantity)

    suspend fun updateOrderItemPrice(itemId: Long, priceOverride: BigDecimal?) =
        orderItemDao.updateOrderItemPrice(itemId, priceOverride)

    // Product analytics
    suspend fun getProductUsageStats(productId: Long): ProductUsageStats {
        val orderItems = orderItemDao.getOrderItemsByProduct(productId)
        val totalQuantity = orderItems.sumOf { it.quantity }
        val totalRevenue = orderItems.sumOf { 
            it.effectivePrice.multiply(BigDecimal.valueOf(it.quantity.toLong()))
        }
        val orderCount = orderItems.map { it.orderId }.distinct().size
        
        return ProductUsageStats(
            productId = productId,
            totalQuantity = totalQuantity,
            totalRevenue = totalRevenue,
            orderCount = orderCount
        )
    }
}

data class ProductUsageStats(
    val productId: Long,
    val totalQuantity: Int,
    val totalRevenue: BigDecimal,
    val orderCount: Int
)