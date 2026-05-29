@file:Suppress("unused")

package com.zeynbakers.order_management_system.order.domain

import com.zeynbakers.order_management_system.order.data.OrderDao
import com.zeynbakers.order_management_system.order.data.OrderEntity
import com.zeynbakers.order_management_system.order.data.OrderItemDao
import com.zeynbakers.order_management_system.order.data.OrderItemEntity
import com.zeynbakers.order_management_system.order.data.OrderWithItems
import com.zeynbakers.order_management_system.order.data.OrderItemWithProduct
import com.zeynbakers.order_management_system.order.data.ItemCategory
import com.zeynbakers.order_management_system.order.ui.OrderCartParser
import com.zeynbakers.order_management_system.order.ui.CartItem
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

    suspend fun saveOrderItemsForOrder(orderId: Long, cartItems: List<CartItem>) {
        // Delete existing items for this order
        orderItemDao.deleteOrderItems(orderId)

        // Convert cart items to order items
        val orderItems = cartItems.map { cartItem ->
            val product = productDao.searchActiveProducts(cartItem.name)
                .firstOrNull { it.name.equals(cartItem.name, ignoreCase = true) }

            OrderItemEntity(
                orderId = orderId,
                productId = product?.id,
                productNameSnapshot = cartItem.name,
                unitPriceSnapshot = cartItem.unitPrice,
                categorySnapshot = ItemCategory.OTHER,
                quantity = cartItem.quantity,
                priceOverride = if (cartItem.unitPrice > BigDecimal.ZERO && product != null) {
                    cartItem.unitPrice
                } else {
                    null
                },
                // Legacy fields for backward compatibility
                name = cartItem.name,
                category = ItemCategory.OTHER,
                unitPrice = cartItem.unitPrice
            )
        }

        orderItemDao.insertAll(orderItems)
    }
    
    suspend fun deleteOrderItems(orderId: Long) = 
        orderItemDao.deleteOrderItems(orderId)
    
    suspend fun deleteOrderItem(itemId: Long) = 
        orderItemDao.deleteOrderItem(itemId)
    
    suspend fun updateOrderItemQuantity(itemId: Long, quantity: Int) = 
        orderItemDao.updateOrderItemQuantity(itemId, quantity)
    
    suspend fun updateOrderItemPrice(itemId: Long, priceOverride: BigDecimal?) = 
        orderItemDao.updateOrderItemPrice(itemId, priceOverride)
    
    // Dual-write operations for migration compatibility
    suspend fun saveOrderWithItems(
        order: OrderEntity,
        cartItems: List<CartItem>,
        productMatches: List<ProductEntity> = emptyList()
    ): Long {
        val orderId = orderDao.insert(order)
        
        // Convert cart items to order items with product references
        val orderItems = cartItems.map { cartItem ->
            val product = productMatches.find { it.name.equals(cartItem.name, ignoreCase = true) }
            
            OrderItemEntity(
                orderId = orderId,
                productId = product?.id,
                productNameSnapshot = cartItem.name,
                unitPriceSnapshot = cartItem.unitPrice,
                categorySnapshot = ItemCategory.OTHER, // TODO: Map from product category
                quantity = cartItem.quantity,
                priceOverride = if (cartItem.unitPrice > BigDecimal.ZERO && product != null) {
                    cartItem.unitPrice
                } else {
                    null
                },
                // Legacy fields for backward compatibility
                name = cartItem.name,
                category = ItemCategory.OTHER,
                unitPrice = cartItem.unitPrice
            )
        }
        
        orderItemDao.insertAll(orderItems)
        
        return orderId
    }
    
    suspend fun updateOrderWithItems(
        order: OrderEntity,
        cartItems: List<CartItem>,
        productMatches: List<ProductEntity> = emptyList()
    ) {
        orderDao.update(order)
        
        // Delete existing items and recreate
        orderItemDao.deleteOrderItems(order.id)
        
        // Convert cart items to order items with product references
        val orderItems = cartItems.map { cartItem ->
            val product = productMatches.find { it.name.equals(cartItem.name, ignoreCase = true) }
            
            OrderItemEntity(
                orderId = order.id,
                productId = product?.id,
                productNameSnapshot = cartItem.name,
                unitPriceSnapshot = cartItem.unitPrice,
                categorySnapshot = ItemCategory.OTHER,
                quantity = cartItem.quantity,
                priceOverride = if (cartItem.unitPrice > BigDecimal.ZERO && product != null) {
                    cartItem.unitPrice
                } else {
                    null
                },
                // Legacy fields
                name = cartItem.name,
                category = ItemCategory.OTHER,
                unitPrice = cartItem.unitPrice
            )
        }
        
        orderItemDao.insertAll(orderItems)
    }
    
    // Compatibility layer: parse notes to order items if order items are empty
    suspend fun getOrderItemsWithFallback(orderId: Long, notes: String): List<OrderItemEntity> {
        val items = orderItemDao.getOrderItems(orderId)
        
        return if (items.isEmpty() && notes.isNotBlank()) {
            // Fallback to parsing notes
            val cartItems = OrderCartParser.parseNotesToCart(notes)
            cartItems.map { cartItem ->
                OrderItemEntity(
                    orderId = orderId,
                    productId = null,
                    productNameSnapshot = cartItem.name,
                    unitPriceSnapshot = cartItem.unitPrice,
                    categorySnapshot = ItemCategory.OTHER,
                    quantity = cartItem.quantity,
                    priceOverride = null,
                    // Legacy fields
                    name = cartItem.name,
                    category = ItemCategory.OTHER,
                    unitPrice = cartItem.unitPrice
                )
            }
        } else {
            items
        }
    }
    
    // Validation: check if notes and order items are consistent
    suspend fun validateOrderConsistency(orderId: Long, notes: String): Boolean {
        val items = orderItemDao.getOrderItems(orderId)
        val cartItems = OrderCartParser.parseNotesToCart(notes)
        
        if (items.size != cartItems.size) return false
        
        // Check if items match (simplified check)
        return items.zip(cartItems).all { (orderItem, cartItem) ->
            orderItem.productNameSnapshot.equals(cartItem.name, ignoreCase = true) &&
            orderItem.quantity == cartItem.quantity &&
            orderItem.effectivePrice == cartItem.unitPrice
        }
    }
    
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