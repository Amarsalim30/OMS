package com.zeynbakers.order_management_system.product.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE archived = 0 ORDER BY name ASC")
    fun getAllProductsFlow(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE archived = 0 ORDER BY name ASC")
    suspend fun getActiveProducts(): List<ProductEntity>

    @Query(
        "SELECT * FROM products WHERE archived = 0 AND LOWER(name) LIKE LOWER(:pattern) " +
            "ORDER BY name ASC LIMIT 8"
    )
    suspend fun searchActiveProducts(pattern: String): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("UPDATE products SET archived = 1 WHERE id = :productId")
    suspend fun archiveProduct(productId: Long)
}
