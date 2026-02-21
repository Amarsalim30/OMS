package com.zeynbakers.order_management_system.core.helper.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "helper_notes",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["pinned", "createdAt"]),
        Index(value = ["detectedPhoneDigits"]),
        Index(value = ["detectedAmountNormalized"]),
        Index(value = ["type"])
    ]
)
data class HelperNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val type: HelperNoteType,
    val rawTranscript: String,
    val displayText: String,
    val calculatorExpression: String? = null,
    val calculatorResult: String? = null,
    val detectedPhone: String? = null,
    val detectedPhoneDigits: String? = null,
    val detectedAmountRaw: String? = null,
    val detectedAmountNormalized: String? = null,
    val linkedCustomerId: Long? = null,
    val sourceApp: String? = null,
    val pinned: Boolean = false,
    val deleted: Boolean = false
)
