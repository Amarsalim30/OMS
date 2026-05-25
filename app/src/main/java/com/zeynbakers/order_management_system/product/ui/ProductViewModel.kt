package com.zeynbakers.order_management_system.product.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeynbakers.order_management_system.core.db.AppDatabase
import com.zeynbakers.order_management_system.product.data.ProductEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal

class ProductViewModel(
    private val database: AppDatabase
) : ViewModel() {

    private val dao = database.productDao()

    val products: StateFlow<List<ProductEntity>> = dao.getAllProductsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun addProduct(name: String, defaultPrice: BigDecimal, emoji: String) {
        viewModelScope.launch {
            dao.insertProduct(
                ProductEntity(
                    name = name.trim(),
                    defaultPrice = defaultPrice,
                    emoji = emoji.trim().ifBlank { "📦" }
                )
            )
        }
    }

    fun updateProduct(product: ProductEntity) {
        viewModelScope.launch {
            dao.updateProduct(product)
        }
    }

    fun archiveProduct(productId: Long) {
        viewModelScope.launch {
            dao.archiveProduct(productId)
        }
    }
}
