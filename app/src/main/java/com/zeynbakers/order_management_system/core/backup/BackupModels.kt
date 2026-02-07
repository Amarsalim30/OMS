package com.zeynbakers.order_management_system.core.backup

data class BackupState(
    val autoEnabled: Boolean,
    val targetType: BackupTargetType,
    val targetUri: String?,
    val lastBackupTime: Long?,
    val lastStatus: BackupStatus,
    val lastMessage: String?
)

enum class BackupTargetType {
    AppPrivate,
    SafDirectory
}

enum class BackupStatus {
    Never,
    Success,
    Failed
}

data class BackupResult(
    val success: Boolean,
    val message: String? = null,
    val shouldRetry: Boolean = false
)
