package com.zeynbakers.order_management_system.core.backup

data class BackupState(
    val autoEnabled: Boolean,
    val targetType: BackupTargetType,
    val targetUri: String?,
    val targetAuthority: String?,
    val targetDisplayName: String?,
    val targetHealth: BackupTargetHealth,
    val lastBackupTime: Long?,
    val lastStatus: BackupStatus,
    val lastMessage: String?,
    val consecutiveAutoFailures: Int,
    val manifestPolicy: RestoreManifestPolicy,
    val encryptionEnabled: Boolean,
    val encryptionConfigured: Boolean
)

enum class BackupTargetType {
    AppPrivate,
    SafDirectory,
    SafFile
}

enum class BackupStatus {
    Never,
    Success,
    Failed
}

enum class BackupTargetHealth {
    Healthy,
    NeedsRelink,
    Unavailable
}

enum class RestoreManifestPolicy {
    Strict,
    LegacyCompatible
}

data class BackupResult(
    val success: Boolean,
    val message: String? = null,
    val shouldRetry: Boolean = false
)

data class DriveTroubleshootReport(
    val driveAppInstalled: Boolean,
    val driveAppEnabled: Boolean,
    val documentsUiEnabled: Boolean,
    val selectedAuthority: String?,
    val persistedPermissionValid: Boolean,
    val managedProfile: Boolean,
    val guidance: List<String>
)

data class RestorePreview(
    val sourceName: String,
    val exportedAt: Long?,
    val dbVersion: Int?,
    val appVersionName: String?,
    val customersCount: Int,
    val ordersCount: Int,
    val paymentsCount: Int
)
