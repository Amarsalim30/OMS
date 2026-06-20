package com.zeynbakers.order_management_system.product.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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

    @Query(
        """
        SELECT p.* FROM products p
        LEFT JOIN order_items oi ON p.id = oi.productId
        WHERE p.archived = 0
        GROUP BY p.id
        ORDER BY COUNT(oi.id) DESC, p.name ASC
        LIMIT 6
    """
    )
    fun getPopularProductsFlow(): Flow<List<ProductEntity>>
}
