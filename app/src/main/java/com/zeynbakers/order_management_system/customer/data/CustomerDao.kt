@file:Suppress("unused")

package com.zeynbakers.order_management_system.customer.data


import androidx.room.*

@Dao
interface CustomerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: CustomerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<CustomerEntity>)

    @Update
    suspend fun update(customer: CustomerEntity)

    @Delete
    suspend fun delete(customer: CustomerEntity)

    @Query("UPDATE customers SET isArchived = 1 WHERE id = :id")
    suspend fun archiveById(id: Long)

    @Query("UPDATE customers SET isArchived = 0 WHERE id = :id")
    suspend fun unarchiveById(id: Long)

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getById(id: Long): CustomerEntity?

    @Query("SELECT * FROM customers WHERE phone = :phone LIMIT 1")
    suspend fun getByPhone(phone: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE phone IN (:phones) LIMIT 1")
    suspend fun getByPhones(phones: List<String>): CustomerEntity?

    @Query("SELECT * FROM customers WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<CustomerEntity>

    @Query(
        """
        SELECT * FROM customers
        WHERE isArchived = 0 AND (name LIKE :query OR phone LIKE :query)
        ORDER BY name
        LIMIT 5
        """
    )
    suspend fun searchCustomers(query: String): List<CustomerEntity>

    @Query("SELECT * FROM customers")
    suspend fun getAllCustomers(): List<CustomerEntity>
}
