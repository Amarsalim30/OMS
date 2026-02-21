package com.zeynbakers.order_management_system.core.helper.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HelperNoteDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(note: HelperNoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<HelperNoteEntity>)

    @Update
    suspend fun update(note: HelperNoteEntity)

    @Query("SELECT * FROM helper_notes ORDER BY createdAt DESC, id DESC")
    suspend fun getAll(): List<HelperNoteEntity>

    @Query(
        """
        SELECT n.*, c.name AS customerName
        FROM helper_notes n
        LEFT JOIN customers c ON c.id = n.linkedCustomerId
        WHERE n.deleted = 0
        ORDER BY n.createdAt DESC, n.id DESC
        """
    )
    fun observeActive(): Flow<List<HelperNoteWithCustomer>>

    @Query("SELECT * FROM helper_notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): HelperNoteEntity?

    @Query("UPDATE helper_notes SET pinned = :pinned, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean, updatedAt: Long)

    @Query("UPDATE helper_notes SET deleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: Long, updatedAt: Long)
}
