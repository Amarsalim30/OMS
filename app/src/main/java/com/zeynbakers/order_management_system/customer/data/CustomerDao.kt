@file:Suppress("unused")

package com.zeynbakers.order_management_system.customer.data


import androidx.room.*

@Dao
interface CustomerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: CustomerEntity): Long

    @Update
    suspend fun update(customer: CustomerEntity)

    @Delete
    suspend fun delete(customer: CustomerEntity)

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getById(id: Long): CustomerEntity?

    @Query("SELECT * FROM customers WHERE phone = :phone LIMIT 1")
    suspend fun getByPhone(phone: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<CustomerEntity>

    @Query(
        """
        SELECT * FROM customers
        WHERE name LIKE :query OR phone LIKE :query
        ORDER BY name
        LIMIT 5
        """
    )
    suspend fun searchCustomers(query: String): List<CustomerEntity>

    @Query("SELECT * FROM customers")
    suspend fun getAllCustomers(): List<CustomerEntity>
}
