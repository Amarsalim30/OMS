package com.zeynbakers.order_management_system.core.backup

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserManager
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import com.zeynbakers.order_management_system.BuildConfig
import com.zeynbakers.order_management_system.accounting.data.AccountEntryEntity
import com.zeynbakers.order_management_system.accounting.data.EntryType
import com.zeynbakers.order_management_system.accounting.data.PaymentAllocationEntity
import com.zeynbakers.order_management_system.accounting.data.PaymentAllocationStatus
import com.zeynbakers.order_management_system.accounting.data.PaymentAllocationType
import com.zeynbakers.order_management_system.accounting.data.PaymentEntity
import com.zeynbakers.order_management_system.accounting.data.PaymentMethod
import com.zeynbakers.order_management_system.accounting.data.PaymentReceiptEntity
import com.zeynbakers.order_management_system.accounting.data.PaymentReceiptStatus
import com.zeynbakers.order_management_system.core.db.APP_DATABASE_SCHEMA_VERSION
import com.zeynbakers.order_management_system.core.db.AppDatabase
import com.zeynbakers.order_management_system.core.db.DatabaseProvider
import com.zeynbakers.order_management_system.customer.data.CustomerEntity
import com.zeynbakers.order_management_system.order.data.ItemCategory
import com.zeynbakers.order_management_system.order.data.OrderEntity
import com.zeynbakers.order_management_system.order.data.OrderItemEntity
import com.zeynbakers.order_management_system.order.data.OrderStatus
import com.zeynbakers.order_management_system.order.data.OrderStatusOverride
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.math.BigDecimal
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.json.JSONArray
import org.json.JSONObject

typealias ProgressCallback = (Int, String) -> Unit

object BackupManager {
    private const val BACKUP_DIR_NAME = "backups"
    private const val BACKUP_FILE_EXTENSION = ".oms"
    private const val LEGACY_BACKUP_FILE_EXTENSION = ".zip"
    private const val MAX_APP_PRIVATE_BACKUPS = 7
    private const val MAX_SAF_DIRECTORY_BACKUPS = 7
    private const val DEFAULT_STAGE = "Preparing"
    private const val MANIFEST_FORMAT_VERSION = 1
    private const val LATEST_POINTER_NAME = "oms_backup_latest.json"
    private const val MIME_ZIP = "application/zip"
    private const val MIME_JSON = "application/json"
    private const val SAF_READBACK_RETRY_DELAY_MS = 650L
    private const val SAF_READBACK_RETRY_ATTEMPTS = 3
    private const val MAX_ZIP_ENTRY_BYTES = 64L * 1024L * 1024L
    private const val MAX_ZIP_TOTAL_BYTES = 256L * 1024L * 1024L
    private const val MAX_SAFE_ROLLBACK_BYTES = MAX_ZIP_TOTAL_BYTES
    // Legacy-only backup field. Keep for backward-compatible import until schema retirement is complete.
    private const val LEGACY_AMOUNT_PAID_FIELD = "amountPaid"

    private const val ENTRY_METADATA = "metadata.json"
    private const val ENTRY_CUSTOMERS = "customers.json"
    private const val ENTRY_ORDERS = "orders.json"
    private const val ENTRY_ORDER_ITEMS = "order_items.json"
    private const val ENTRY_ACCOUNT_ENTRIES = "account_entries.json"
    private const val ENTRY_PAYMENTS = "payments.json"
    private const val ENTRY_PAYMENT_RECEIPTS = "payment_receipts.json"
    private const val ENTRY_PAYMENT_ALLOCATIONS = "payment_allocations.json"
    private const val ENTRY_LEGACY_MPESA = "mpesa_transactions.json"
    private const val ENTRY_MANIFEST = "manifest.json"
    private const val GOOGLE_DRIVE_PACKAGE = "com.google.android.apps.docs"
    private const val GOOGLE_DRIVE_DOCUMENTS_AUTHORITY_PREFIX = "com.google.android.apps.docs.storage"
    private const val DOCUMENTS_UI_PACKAGE = "com.android.documentsui"
    private const val GOOGLE_DOCUMENTS_UI_PACKAGE = "com.google.android.documentsui"

    private val REQUIRED_ENTRIES =
        setOf(
            ENTRY_METADATA,
            ENTRY_CUSTOMERS,
            ENTRY_ORDERS,
            ENTRY_ORDER_ITEMS,
            ENTRY_ACCOUNT_ENTRIES,
            ENTRY_PAYMENTS
        )

    private val ARRAY_ENTRIES =
        setOf(
            ENTRY_CUSTOMERS,
            ENTRY_ORDERS,
            ENTRY_ORDER_ITEMS,
            ENTRY_ACCOUNT_ENTRIES,
            ENTRY_PAYMENTS,
            ENTRY_PAYMENT_RECEIPTS,
            ENTRY_PAYMENT_ALLOCATIONS,
            ENTRY_LEGACY_MPESA
        )

    suspend fun runBackup(
        context: Context,
        force: Boolean = false,
        progress: ProgressCallback? = null
    ): BackupResult {
        val prefs = BackupPreferences(context)
        val state = prefs.readState()
        if (state.targetType == BackupTargetType.SafDirectory) {
            val now = System.currentTimeMillis()
            val message = "Folder backup is not supported. Choose a backup file."
            prefs.setLastResult(BackupStatus.Failed, message, now)
            return BackupResult(success = false, message = message)
        }
        if (!force && !state.autoEnabled) {
            return BackupResult(success = false, message = "Automatic backup disabled")
        }

        val now = System.currentTimeMillis()
        val fileName = backupFileName(now)

        return try {
            progress?.invoke(5, DEFAULT_STAGE)
            val archive =
                withContext(Dispatchers.IO) {
                    buildBackupArchive(
                        database = DatabaseProvider.getDatabase(context),
                        exportedAt = now,
                        progress = progress
                    )
                }

            val writeResult =
                withContext(Dispatchers.IO) {
                    when (state.targetType) {
                        BackupTargetType.AppPrivate -> {
                            val file = writeAppPrivateBackup(context, fileName, archive.bytes) ?: return@withContext null
                            updateAppPrivateLatestPointer(
                                context = context,
                                pointer = LatestPointer(fileName = file.name, sha256 = archive.sha256, updatedAt = now)
                            )
                            file.name
                        }

                        BackupTargetType.SafDirectory -> {
                            val treeUri = state.targetUri
                            val writtenName = writeSafBackup(context, treeUri, fileName, archive.bytes) ?: return@withContext null
                            updateSafLatestPointer(
                                context = context,
                                treeUriString = treeUri,
                                pointer = LatestPointer(fileName = writtenName, sha256 = archive.sha256, updatedAt = now)
                            )
                            writtenName
                        }

                        BackupTargetType.SafFile -> {
                            val fileUri = state.targetUri
                            val writtenName = writeSafFileBackup(context, fileUri, archive.bytes) ?: return@withContext null
                            writtenName
                        }
                    }
                }

            if (writeResult == null) {
                prefs.setLastResult(
                    status = BackupStatus.Failed,
                    message = "Backup location unavailable",
                    time = now
                )
                return BackupResult(success = false, message = "Backup location unavailable")
            }

            withContext(Dispatchers.IO) {
                when (state.targetType) {
                    BackupTargetType.AppPrivate -> pruneOldAppBackups(context)
                    BackupTargetType.SafDirectory -> pruneOldSafBackups(context, state.targetUri)
                    BackupTargetType.SafFile -> Unit
                }
            }

            progress?.invoke(100, "Backup complete")
            val message =
                if (state.targetType == BackupTargetType.AppPrivate) {
                    "Saved to app storage"
                } else if (state.targetType == BackupTargetType.SafFile) {
                    "Saved to selected file"
                } else {
                    "Saved to selected folder"
                }
            prefs.setLastResult(BackupStatus.Success, message, now)
            BackupResult(success = true, message = message)
        } catch (t: CancellationException) {
            throw t
        } catch (t: Exception) {
            prefs.setLastResult(BackupStatus.Failed, t.message, now)
            BackupResult(
                success = false,
                message = t.message,
                shouldRetry = isRetryableBackupException(t)
            )
        }
    }

    suspend fun runRestore(
        context: Context,
        uriString: String?,
        progress: ProgressCallback? = null
    ): BackupResult {
        progress?.invoke(5, "Opening backup")
        val state = BackupPreferences(context).readState()
        val input =
            if (uriString.isNullOrBlank()) {
                openLatestInput(context, state)
            } else {
                openSafInput(context, uriString)
            }
        if (input == null) {
            return BackupResult(success = false, message = "Backup file unavailable")
        }

        return try {
            withContext(Dispatchers.IO) {
                input.use { stream ->
                    importDatabase(
                        database = DatabaseProvider.getDatabase(context),
                        inputStream = stream,
                        progress = progress,
                        manifestPolicy = state.manifestPolicy
                    )
                }
            }
            progress?.invoke(100, "Restore complete")
            BackupResult(success = true, message = "Restore complete")
        } catch (t: CancellationException) {
            throw t
        } catch (t: Exception) {
            BackupResult(
                success = false,
                message = t.message,
                shouldRetry = isRetryableBackupException(t)
            )
        }
    }

    suspend fun runStorageProbe(context: Context): BackupResult {
        val state = BackupPreferences(context).readState()
        return withContext(Dispatchers.IO) {
            try {
                when (state.targetType) {
                    BackupTargetType.AppPrivate -> {
                        val dir = ensureAppPrivateDir(context) ?: return@withContext BackupResult(false, "Backup location unavailable")
                        val probe = File(dir, "probe-${System.currentTimeMillis()}.tmp")
                        probe.writeText("probe", Charsets.UTF_8)
                        val echo = probe.readText(Charsets.UTF_8)
                        probe.delete()
                        if (echo == "probe") BackupResult(true, "Write test passed")
                        else BackupResult(false, "Write verification failed")
                    }

                    BackupTargetType.SafDirectory -> probeSafDirectoryInternal(context, state.targetUri)

                    BackupTargetType.SafFile -> {
                        val fileUri = state.targetUri ?: return@withContext BackupResult(false, "Backup file unavailable")
                        val canWrite = isSafFileWritable(context, fileUri)
                        if (canWrite) BackupResult(true, "Write test passed")
                        else BackupResult(false, "Backup file unavailable")
                    }
                }
            } catch (t: Exception) {
                BackupResult(
                    success = false,
                    message = t.message,
                    shouldRetry = isRetryableBackupException(t)
                )
            }
        }
    }

    suspend fun findLatestBackupName(
        context: Context,
        targetType: BackupTargetType,
        targetUri: String?
    ): String? {
        return withContext(Dispatchers.IO) {
            when (targetType) {
                BackupTargetType.AppPrivate -> {
                    val pointer = readAppPrivateLatestPointer(context)
                    if (pointer != null) {
                        val file = File(File(context.filesDir, BACKUP_DIR_NAME), pointer.fileName)
                        if (file.exists()) {
                            return@withContext file.name
                        }
                    }
                    listAppPrivateBackups(context).firstOrNull()?.name
                }

                BackupTargetType.SafDirectory -> {
                    if (targetUri.isNullOrBlank()) {
                        return@withContext null
                    }
                    val tree = DocumentFile.fromTreeUri(context, targetUri.toUri()) ?: return@withContext null
                    val pointer = readSafLatestPointer(context, targetUri)
                    if (pointer != null) {
                        val pointed = tree.findFile(pointer.fileName)
                        if (pointed != null && pointed.isFile) {
                            return@withContext pointed.name ?: pointer.fileName
                        }
                    }
                    listSafBackups(tree).firstOrNull()?.name
                }

                BackupTargetType.SafFile -> {
                    if (targetUri.isNullOrBlank()) {
                        return@withContext null
                    }
                    resolveDisplayNameForUri(context, targetUri) ?: "selected_backup.oms"
                }
            }
        }
    }

    suspend fun runSafDirectoryProbe(context: Context, treeUriString: String?): BackupResult {
        return withContext(Dispatchers.IO) {
            try {
                probeSafDirectoryInternal(context, treeUriString)
            } catch (t: Exception) {
                BackupResult(
                    success = false,
                    message = t.message,
                    shouldRetry = isRetryableBackupException(t)
                )
            }
        }
    }

    fun evaluateTargetHealth(context: Context, state: BackupState): BackupTargetHealth {
        return when (state.targetType) {
            BackupTargetType.AppPrivate -> {
                if (ensureAppPrivateDir(context) != null) BackupTargetHealth.Healthy
                else BackupTargetHealth.Unavailable
            }

            BackupTargetType.SafDirectory -> {
                if (state.targetUri.isNullOrBlank()) {
                    BackupTargetHealth.NeedsRelink
                } else if (!hasPersistedReadWritePermission(context, state.targetUri)) {
                    BackupTargetHealth.NeedsRelink
                } else if (!isSafTargetAccessible(context, state.targetUri)) {
                    BackupTargetHealth.Unavailable
                } else {
                    BackupTargetHealth.Healthy
                }
            }

            BackupTargetType.SafFile -> {
                if (state.targetUri.isNullOrBlank()) {
                    BackupTargetHealth.NeedsRelink
                } else if (!hasPersistedReadWritePermission(context, state.targetUri)) {
                    BackupTargetHealth.NeedsRelink
                } else if (!isSafFileWritable(context, state.targetUri)) {
                    BackupTargetHealth.Unavailable
                } else {
                    BackupTargetHealth.Healthy
                }
            }
        }
    }

    fun isSafTargetAccessible(context: Context, uriString: String?): Boolean {
        val uri = uriString?.toUri() ?: return false
        val tree = DocumentFile.fromTreeUri(context, uri) ?: return false
        return tree.exists() && tree.isDirectory
    }

    fun isSafFileAccessible(context: Context, uriString: String?): Boolean {
        val uri = uriString?.toUri() ?: return false
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        }.getOrDefault(false)
    }

    private fun isSafFileWritable(context: Context, uriString: String?): Boolean {
        val uri = uriString?.toUri() ?: return false
        val viaDescriptor =
            runCatching {
                context.contentResolver.openFileDescriptor(uri, "rw")?.use { true } ?: false
            }.getOrDefault(false)
        if (viaDescriptor) return true
        return hasPersistedReadWritePermission(context, uriString)
    }

    fun buildDriveTroubleshootReport(context: Context, state: BackupState): DriveTroubleshootReport {
        val packageManager = context.packageManager
        val driveProviderAuthorities = queryDriveDocumentsAuthorities(packageManager)
        val driveProviderVisible = driveProviderAuthorities.isNotEmpty()
        val driveInfo = packageManager.packageInfoOrNull(GOOGLE_DRIVE_PACKAGE)
        val driveInstalled = driveInfo != null || driveProviderVisible
        val driveEnabled =
            when {
                driveInfo != null -> packageManager.applicationEnabled(GOOGLE_DRIVE_PACKAGE)
                else -> driveProviderVisible
            }

        val documentsUiEnabled =
            packageManager.applicationEnabled(DOCUMENTS_UI_PACKAGE) ||
                packageManager.applicationEnabled(GOOGLE_DOCUMENTS_UI_PACKAGE)

        val selectedAuthority = state.targetAuthority ?: state.targetUri?.let { runCatching { it.toUri().authority }.getOrNull() }
        val persistedPermissionValid = hasPersistedReadWritePermission(context, state.targetUri)
        val managedProfile =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.getSystemService(UserManager::class.java)?.isManagedProfile == true
            } else {
                false
            }

        val guidance = buildList {
            if (!driveInstalled) {
                add("Install Google Drive app from Play Store.")
            } else if (!driveEnabled) {
                add("Enable Google Drive app in system app settings.")
            } else {
                add("Open Google Drive once and confirm you are signed in.")
            }
            if (!driveProviderVisible) {
                add("Drive provider is not visible to Android file picker. Reboot device and re-open Drive app.")
            }
            if (!documentsUiEnabled) {
                add("Enable DocumentsUI/Files system app to expose cloud providers.")
            }
            if (!persistedPermissionValid) {
                add("Re-select backup folder and grant persistent read/write access.")
            }
            add("In picker, use Browse and choose Drive under storage providers.")
            add("Some OEM picker UIs may hide providers; if Drive is missing, open system Files/Browse or test on a Pixel/Samsung device.")
            if (managedProfile) {
                add("This device has a managed/work profile. Admin policy may hide Drive provider.")
            }
        }

        return DriveTroubleshootReport(
            driveAppInstalled = driveInstalled,
            driveAppEnabled = driveEnabled,
            documentsUiEnabled = documentsUiEnabled,
            selectedAuthority = selectedAuthority,
            persistedPermissionValid = persistedPermissionValid,
            managedProfile = managedProfile,
            guidance = guidance
        )
    }

    fun listAppPrivateBackups(context: Context): List<File> {
        val dir = File(context.filesDir, BACKUP_DIR_NAME)
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        return dir.listFiles { file -> isBackupArchiveName(file.name) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    internal fun readBackupPayloadsForTest(
        inputStream: InputStream,
        maxEntryBytes: Long = MAX_ZIP_ENTRY_BYTES,
        maxTotalBytes: Long = MAX_ZIP_TOTAL_BYTES
    ): Map<String, String> {
        return readZipEntries(
            inputStream = inputStream,
            maxEntryBytes = maxEntryBytes,
            maxTotalBytes = maxTotalBytes
        )
            .mapValues { (_, payload) -> payload.toString(Charsets.UTF_8) }
    }

    internal fun validateRestorePayloadsForTest(payloads: Map<String, String>, currentDbVersion: Int) {
        val rawEntries = payloads.mapValues { (_, payload) -> payload.toByteArray(Charsets.UTF_8) }
        validateRestoreEntries(
            rawEntries = rawEntries,
            currentDbVersion = currentDbVersion,
            manifestPolicy = RestoreManifestPolicy.LegacyCompatible
        )
    }

    internal fun sha256HexForTest(raw: String): String = sha256Hex(raw.toByteArray(Charsets.UTF_8))

    private fun backupFileName(timestamp: Long): String {
        val formatter = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
        return "oms-backup-${formatter.format(Date(timestamp))}$BACKUP_FILE_EXTENSION"
    }

    private fun ensureAppPrivateDir(context: Context): File? {
        val dir = File(context.filesDir, BACKUP_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return if (dir.exists() && dir.isDirectory) dir else null
    }

    private fun openLatestInput(context: Context, state: BackupState): InputStream? {
        return when (state.targetType) {
            BackupTargetType.AppPrivate -> openLatestAppPrivateInput(context)
            BackupTargetType.SafDirectory -> openLatestSafInput(context, state.targetUri)
            BackupTargetType.SafFile -> state.targetUri?.let { openSafInput(context, it) }
        }
    }

    private fun openLatestAppPrivateInput(context: Context): InputStream? {
        val pointer = readAppPrivateLatestPointer(context)
        if (pointer != null) {
            val file = File(File(context.filesDir, BACKUP_DIR_NAME), pointer.fileName)
            if (file.exists()) {
                return file.inputStream()
            }
        }
        val latest = listAppPrivateBackups(context).firstOrNull() ?: return null
        return latest.inputStream()
    }

    private fun openLatestSafInput(context: Context, treeUriString: String?): InputStream? {
        val treeUri = treeUriString?.toUri() ?: return null
        val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return null

        val pointer = readSafLatestPointer(context, treeUriString)
        if (pointer != null) {
            val pointed = tree.findFile(pointer.fileName)
            if (pointed != null && pointed.isFile) {
                return context.contentResolver.openInputStream(pointed.uri)
            }
        }

        val latest = listSafBackups(tree).firstOrNull() ?: return null
        return context.contentResolver.openInputStream(latest.uri)
    }

    private fun openSafInput(context: Context, uriString: String): InputStream? {
        val uri = uriString.toUri()
        return context.contentResolver.openInputStream(uri)
    }

    private fun listSafBackups(tree: DocumentFile): List<DocumentFile> {
        return tree.listFiles()
            .filter { file ->
                file.isFile && isBackupArchiveName(file.name)
            }
            .sortedWith(
                compareByDescending<DocumentFile> { it.lastModified() }
                    .thenByDescending { it.name ?: "" }
            )
    }

    private fun probeSafDirectoryInternal(context: Context, treeUriString: String?): BackupResult {
        val treeUri = treeUriString?.toUri() ?: return BackupResult(false, "Backup location unavailable")
        val tree = DocumentFile.fromTreeUri(context, treeUri)
            ?: return BackupResult(false, "Backup location unavailable")
        val probeName = "oms_probe_${System.currentTimeMillis()}.txt"
        tree.findFile(probeName)?.delete()
        val probeDoc =
            tree.createFile("text/plain", probeName)
                ?: return BackupResult(false, "Backup location unavailable")

        return try {
            context.contentResolver.openOutputStream(probeDoc.uri, "w")?.use { out ->
                out.write("probe".toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: return BackupResult(false, "Backup location unavailable")

            val echoed =
                context.contentResolver.openInputStream(probeDoc.uri)?.use { input ->
                    input.readBytes().toString(Charsets.UTF_8)
                } ?: ""

            if (echoed == "probe") {
                BackupResult(true, "Write test passed")
            } else {
                BackupResult(false, "Write verification failed")
            }
        } finally {
            probeDoc.delete()
        }
    }

    private fun pruneOldAppBackups(context: Context) {
        val dir = File(context.filesDir, BACKUP_DIR_NAME)
        if (!dir.exists() || !dir.isDirectory) return
        val backups =
            dir.listFiles { file -> isBackupArchiveName(file.name) }
                ?.sortedByDescending { it.lastModified() }
                ?: return
        backups.drop(MAX_APP_PRIVATE_BACKUPS).forEach { it.delete() }
    }

    private fun pruneOldSafBackups(context: Context, treeUriString: String?) {
        val treeUri = treeUriString?.toUri() ?: return
        val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return
        val backups = listSafBackups(tree)
        backups.drop(MAX_SAF_DIRECTORY_BACKUPS).forEach { it.delete() }
    }

    suspend fun buildRestorePreview(context: Context, uriString: String?): RestorePreview {
        val state = BackupPreferences(context).readState()
        val sourceName =
            if (uriString.isNullOrBlank()) {
                findLatestBackupName(context, state.targetType, state.targetUri) ?: "Latest backup"
            } else {
                resolveDisplayNameForUri(context, uriString) ?: "Selected backup"
            }
        val input =
            if (uriString.isNullOrBlank()) {
                openLatestInput(context, state)
            } else {
                openSafInput(context, uriString)
            } ?: throw IllegalArgumentException("Backup file unavailable")

        return withContext(Dispatchers.IO) {
            input.use { stream ->
                val rawPayloads = readZipEntries(stream)
                val payloads =
                    validateRestoreEntries(
                        rawEntries = rawPayloads,
                        currentDbVersion = currentDatabaseVersion(context = context),
                        manifestPolicy = state.manifestPolicy
                    )
                val metadata =
                    runCatching { JSONObject(payloads.getValue(ENTRY_METADATA)) }.getOrNull()
                RestorePreview(
                    sourceName = sourceName,
                    exportedAt = metadata?.optLong("exportedAt")?.takeIf { it > 0L },
                    dbVersion = metadata?.optInt("dbVersion")?.takeIf { it > 0 },
                    appVersionName =
                        metadata?.optString("appVersionName", "")?.trim()?.takeIf { it.isNotEmpty() },
                    customersCount = parseArray(payloads[ENTRY_CUSTOMERS])?.length() ?: 0,
                    ordersCount = parseArray(payloads[ENTRY_ORDERS])?.length() ?: 0,
                    paymentsCount = parseArray(payloads[ENTRY_PAYMENTS])?.length() ?: 0
                )
            }
        }
    }

    private suspend fun buildBackupArchive(
        database: AppDatabase,
        exportedAt: Long,
        progress: ProgressCallback?
    ): BackupArchive {
        progress?.invoke(10, "Reading data")
        val currentDbVersion = currentDatabaseVersion(database = database)
        if (currentDbVersion <= 0) {
            throw IllegalStateException("Unable to resolve app database version")
        }
        val snapshot =
            database.withTransaction {
                BackupSnapshot(
                    customers = database.customerDao().getAllCustomers(),
                    orders = database.orderDao().getAllOrders(),
                    orderItems = database.orderItemDao().getAllOrderItems(),
                    accountEntries = database.accountingDao().getAllAccountEntries(),
                    payments = database.accountingDao().getAllPayments(),
                    paymentReceipts = database.paymentReceiptDao().getAll(),
                    paymentAllocations = database.paymentAllocationDao().getAll()
                )
            }

        val payloadEntries = linkedMapOf<String, ByteArray>()
        payloadEntries[ENTRY_METADATA] = buildMetadata(exportedAt, currentDbVersion).toString().toByteArray(Charsets.UTF_8)
        payloadEntries[ENTRY_CUSTOMERS] = customersToJson(snapshot.customers).toString().toByteArray(Charsets.UTF_8)
        payloadEntries[ENTRY_ORDERS] = ordersToJson(snapshot.orders).toString().toByteArray(Charsets.UTF_8)
        payloadEntries[ENTRY_ORDER_ITEMS] = orderItemsToJson(snapshot.orderItems).toString().toByteArray(Charsets.UTF_8)
        payloadEntries[ENTRY_ACCOUNT_ENTRIES] = accountEntriesToJson(snapshot.accountEntries).toString().toByteArray(Charsets.UTF_8)
        payloadEntries[ENTRY_PAYMENTS] = paymentsToJson(snapshot.payments).toString().toByteArray(Charsets.UTF_8)
        payloadEntries[ENTRY_PAYMENT_RECEIPTS] = paymentReceiptsToJson(snapshot.paymentReceipts).toString().toByteArray(Charsets.UTF_8)
        payloadEntries[ENTRY_PAYMENT_ALLOCATIONS] = paymentAllocationsToJson(snapshot.paymentAllocations).toString().toByteArray(Charsets.UTF_8)

        progress?.invoke(65, "Writing manifest")
        payloadEntries[ENTRY_MANIFEST] = buildManifest(exportedAt, payloadEntries, currentDbVersion).toByteArray(Charsets.UTF_8)

        progress?.invoke(80, "Finalizing archive")
        val zippedBytes = zipEntries(payloadEntries)

        // Ensure every produced archive passes strict restore preflight before writing out.
        validateRestoreEntries(
            rawEntries = readZipEntries(ByteArrayInputStream(zippedBytes)),
            currentDbVersion = currentDbVersion,
            manifestPolicy = RestoreManifestPolicy.Strict
        )
        return BackupArchive(bytes = zippedBytes, sha256 = sha256Hex(zippedBytes))
    }

    private fun buildMetadata(exportedAt: Long, dbVersion: Int): JSONObject {
        return JSONObject()
            .put("exportedAt", exportedAt)
            .put("appVersionName", BuildConfig.VERSION_NAME)
            .put("appVersionCode", BuildConfig.VERSION_CODE)
            .put("dbVersion", dbVersion)
    }

    private fun buildManifest(
        exportedAt: Long,
        payloadEntries: Map<String, ByteArray>,
        dbVersion: Int
    ): String {
        val checksums = JSONArray()
        payloadEntries
            .filterKeys { it != ENTRY_MANIFEST }
            .forEach { (name, payload) ->
                checksums.put(
                    JSONObject()
                        .put("name", name)
                        .put("sha256", sha256Hex(payload))
                        .put("size", payload.size)
                )
            }

        return JSONObject()
            .put("formatVersion", MANIFEST_FORMAT_VERSION)
            .put("createdAt", exportedAt)
            .put("dbVersion", dbVersion)
            .put("entries", checksums)
            .toString()
    }

    private fun zipEntries(entries: Map<String, ByteArray>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(BufferedOutputStream(output)).use { zip ->
            entries.forEach { (name, payload) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(payload)
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    private fun writeAppPrivateBackup(context: Context, fileName: String, archiveBytes: ByteArray): File? {
        val dir = ensureAppPrivateDir(context) ?: return null
        val finalFile = File(dir, fileName)
        val partialFile = File(dir, "$fileName.partial")

        partialFile.outputStream().use { out ->
            out.write(archiveBytes)
            out.flush()
        }

        partialFile.inputStream().use { verifyArchiveStream(it, currentDatabaseVersion(context = context)) }

        if (finalFile.exists() && !finalFile.delete()) {
            throw IOException("Unable to replace existing backup")
        }

        if (!partialFile.renameTo(finalFile)) {
            partialFile.inputStream().use { source ->
                finalFile.outputStream().use { target -> source.copyTo(target) }
            }
            partialFile.delete()
        }

        if (!finalFile.exists()) {
            return null
        }
        return finalFile
    }

    private fun writeSafBackup(
        context: Context,
        treeUriString: String?,
        fileName: String,
        archiveBytes: ByteArray
    ): String? {
        val treeUri = treeUriString?.toUri() ?: return null
        val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        val partialName = "$fileName.partial"

        tree.findFile(partialName)?.delete()
        val partial = tree.createFile(MIME_ZIP, partialName) ?: return null

        context.contentResolver.openOutputStream(partial.uri, "w")?.use { out ->
            out.write(archiveBytes)
            out.flush()
        } ?: return null

        context.contentResolver.openInputStream(partial.uri)?.use { verifyArchiveStream(it, currentDatabaseVersion(context = context)) }
            ?: return null

        val renamed = partial.renameTo(fileName)
        if (renamed) {
            val finalized = tree.findFile(fileName) ?: return fileName
            context.contentResolver.openInputStream(finalized.uri)?.use { verifyArchiveStream(it, currentDatabaseVersion(context = context)) }
                ?: return null
            return finalized.name ?: fileName
        }

        tree.findFile(fileName)?.delete()
        val finalFile = tree.createFile(MIME_ZIP, fileName) ?: return null
        context.contentResolver.openInputStream(partial.uri)?.use { source ->
            context.contentResolver.openOutputStream(finalFile.uri, "w")?.use { target ->
                source.copyTo(target)
                target.flush()
            }
        } ?: return null
        partial.delete()
        context.contentResolver.openInputStream(finalFile.uri)?.use { verifyArchiveStream(it, currentDatabaseVersion(context = context)) }
            ?: return null
        return finalFile.name ?: fileName
    }

    private fun writeSafFileBackup(
        context: Context,
        fileUriString: String?,
        archiveBytes: ByteArray
    ): String? {
        val fileUri = fileUriString?.toUri() ?: return null
        val expectedHash = sha256Hex(archiveBytes)
        val rollbackBytes =
            runCatching {
                readUriBytesLimited(
                    context = context,
                    uri = fileUri,
                    maxBytes = MAX_SAFE_ROLLBACK_BYTES
                )
            }.getOrNull()

        var writeCompleted = false
        return try {
            val wrote = writeSafFileBytes(context, fileUri, archiveBytes)
            if (!wrote) {
                throw IOException("Backup file unavailable")
            }
            writeCompleted = true
            verifySafFileWrite(
                context = context,
                uri = fileUri,
                expectedBytes = archiveBytes.size.toLong(),
                expectedHash = expectedHash
            )

            resolveDisplayNameForUri(context, fileUriString) ?: "selected_backup.oms"
        } catch (error: Exception) {
            // Roll back only if we could not complete the write. If write finished but
            // verification is inconclusive on cloud providers, avoid restoring stale bytes.
            if (!writeCompleted && rollbackBytes != null) {
                runCatching {
                    writeSafFileBytes(context, fileUri, rollbackBytes)
                }
            }
            throw error
        }
    }

    private fun writeSafFileBytes(
        context: Context,
        uri: android.net.Uri,
        bytes: ByteArray
    ): Boolean {
        val wroteViaDescriptor =
            runCatching {
                context.contentResolver.openFileDescriptor(uri, "rwt")?.use { pfd ->
                    FileOutputStream(pfd.fileDescriptor).use { out ->
                        out.channel.truncate(0L)
                        out.write(bytes)
                        out.flush()
                        runCatching { out.fd.sync() }
                    }
                    true
                } ?: false
            }.getOrDefault(false)
        if (wroteViaDescriptor) return true

        val wroteViaReadWrite =
            runCatching {
                context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                    FileOutputStream(pfd.fileDescriptor).use { out ->
                        out.channel.truncate(0L)
                        out.write(bytes)
                        out.flush()
                        runCatching { out.fd.sync() }
                    }
                    true
                } ?: false
            }.getOrDefault(false)
        if (wroteViaReadWrite) return true

        return runCatching {
            openSafOutputStreamForWrite(context, uri)?.use { out ->
                out.write(bytes)
                out.flush()
            } != null
        }.getOrDefault(false)
    }

    private fun verifySafFileWrite(
        context: Context,
        uri: android.net.Uri,
        expectedBytes: Long,
        expectedHash: String
    ) {
        var lastError: Throwable? = null
        var sawReadableFingerprint = false
        repeat(SAF_READBACK_RETRY_ATTEMPTS) { attempt ->
            val fingerprint =
                runCatching { readStreamFingerprint(context, uri) }
                    .onFailure { lastError = it }
                    .getOrNull()
            if (fingerprint != null) {
                sawReadableFingerprint = true
                if (fingerprint.length == expectedBytes && fingerprint.sha256 == expectedHash) {
                    return
                }
                lastError =
                    IOException(
                        "Backup file verification failed (length=${fingerprint.length}, expected=$expectedBytes)"
                    )
            }
            if (attempt < SAF_READBACK_RETRY_ATTEMPTS - 1) {
                runCatching { Thread.sleep(SAF_READBACK_RETRY_DELAY_MS * (attempt + 1L)) }
            }
        }

        // Last resort for providers that fail stream re-open entirely: size must exactly match.
        if (!sawReadableFingerprint && isSafFileSizePlausible(context, uri, expectedBytes)) return

        when (lastError) {
            is Exception -> throw lastError as Exception
            else -> throw IOException("Backup file verification failed")
        }
    }

    private fun readStreamFingerprint(
        context: Context,
        uri: android.net.Uri
    ): StreamFingerprint {
        val digest = MessageDigest.getInstance("SHA-256")
        val length =
            context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(8_192)
                var total = 0L
                var read = input.read(buffer)
                while (read >= 0) {
                    if (read > 0) {
                        digest.update(buffer, 0, read)
                        total += read.toLong()
                    }
                    read = input.read(buffer)
                }
                total
            } ?: throw IOException("Backup file unavailable")
        val hash = digest.digest().joinToString(separator = "") { "%02x".format(it) }
        return StreamFingerprint(length = length, sha256 = hash)
    }

    private fun openSafOutputStreamForWrite(
        context: Context,
        uri: android.net.Uri
    ): OutputStream? {
        return context.contentResolver.openOutputStream(uri, "wt")
            ?: context.contentResolver.openOutputStream(uri, "w")
            ?: context.contentResolver.openOutputStream(uri)
    }

    private fun isSafFileSizePlausible(
        context: Context,
        uri: android.net.Uri,
        expectedBytes: Long
    ): Boolean {
        return runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
                val statSize = fd.statSize
                statSize == expectedBytes
            } ?: false
        }.getOrDefault(false)
    }

    private data class StreamFingerprint(
        val length: Long,
        val sha256: String
    )

    private fun updateAppPrivateLatestPointer(context: Context, pointer: LatestPointer) {
        val dir = ensureAppPrivateDir(context) ?: return
        val pointerFile = File(dir, LATEST_POINTER_NAME)
        pointerFile.writeText(pointer.toJson().toString(), Charsets.UTF_8)
    }

    private fun updateSafLatestPointer(context: Context, treeUriString: String?, pointer: LatestPointer) {
        val treeUri = treeUriString?.toUri() ?: return
        val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return
        val file = tree.findFile(LATEST_POINTER_NAME) ?: tree.createFile(MIME_JSON, LATEST_POINTER_NAME) ?: return
        context.contentResolver.openOutputStream(file.uri, "w")?.use { out ->
            out.write(pointer.toJson().toString().toByteArray(Charsets.UTF_8))
            out.flush()
        }
    }

    private fun readAppPrivateLatestPointer(context: Context): LatestPointer? {
        val dir = File(context.filesDir, BACKUP_DIR_NAME)
        val pointerFile = File(dir, LATEST_POINTER_NAME)
        if (!pointerFile.exists()) return null
        val payload = runCatching { pointerFile.readText(Charsets.UTF_8) }.getOrNull() ?: return null
        return parseLatestPointer(payload)
    }

    private fun readSafLatestPointer(context: Context, treeUriString: String?): LatestPointer? {
        val treeUri = treeUriString?.toUri() ?: return null
        val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        val pointerFile = tree.findFile(LATEST_POINTER_NAME) ?: return null
        val payload =
            runCatching {
                context.contentResolver.openInputStream(pointerFile.uri)?.use { input ->
                    input.readBytes().toString(Charsets.UTF_8)
                }
            }.getOrNull() ?: return null
        return parseLatestPointer(payload)
    }

    private fun resolveDisplayNameForUri(context: Context, uriString: String?): String? {
        val uri = uriString?.toUri() ?: return null
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val idx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx)?.trim().takeIf { !it.isNullOrBlank() } else null
            }
        }.getOrNull()
    }

    private fun parseLatestPointer(payload: String): LatestPointer? {
        return runCatching {
            val json = JSONObject(payload)
            val fileName = json.optString("fileName", "").trim()
            if (fileName.isEmpty()) {
                null
            } else {
                LatestPointer(
                    fileName = fileName,
                    sha256 = json.optString("sha256", "").trim(),
                    updatedAt = json.optLong("updatedAt", 0L)
                )
            }
        }.getOrNull()
    }

    private fun isBackupArchiveName(name: String?): Boolean {
        val safeName = name?.lowercase(Locale.US) ?: return false
        return safeName.endsWith(BACKUP_FILE_EXTENSION) || safeName.endsWith(LEGACY_BACKUP_FILE_EXTENSION)
    }

    private fun readUriBytesLimited(context: Context, uri: android.net.Uri, maxBytes: Long): ByteArray? {
        if (maxBytes <= 0L) return null
        val input = context.contentResolver.openInputStream(uri) ?: return null
        input.use { stream ->
            val buffer = ByteArray(8_192)
            val output = ByteArrayOutputStream()
            var total = 0L
            var read = stream.read(buffer)
            while (read >= 0) {
                if (read > 0) {
                    total += read.toLong()
                    if (total > maxBytes) return null
                    output.write(buffer, 0, read)
                }
                read = stream.read(buffer)
            }
            return output.toByteArray()
        }
    }

    private fun verifyArchiveStream(inputStream: InputStream, currentDbVersion: Int) {
        val entries = readZipEntries(inputStream)
        validateRestoreEntries(
            rawEntries = entries,
            currentDbVersion = currentDbVersion,
            manifestPolicy = RestoreManifestPolicy.Strict
        )
    }

    private suspend fun importDatabase(
        database: AppDatabase,
        inputStream: InputStream,
        progress: ProgressCallback?,
        manifestPolicy: RestoreManifestPolicy
    ) {
        progress?.invoke(15, "Reading backup")
        val currentDbVersion = currentDatabaseVersion(database = database)
        val rawPayloads = readZipEntries(inputStream)
        val payloads =
            validateRestoreEntries(
                rawEntries = rawPayloads,
                currentDbVersion = currentDbVersion,
                manifestPolicy = manifestPolicy
            )

        val customers = parseCustomers(payloads[ENTRY_CUSTOMERS])
        val orders = parseOrders(payloads[ENTRY_ORDERS])
        val orderItems = parseOrderItems(payloads[ENTRY_ORDER_ITEMS])
        val accountEntries = parseAccountEntries(payloads[ENTRY_ACCOUNT_ENTRIES])
        val payments = parsePayments(payloads[ENTRY_PAYMENTS])
        val receipts = parsePaymentReceipts(payloads[ENTRY_PAYMENT_RECEIPTS])
        val allocations = parsePaymentAllocations(payloads[ENTRY_PAYMENT_ALLOCATIONS])
        val legacyMpesa = parseLegacyMpesaTransactions(payloads[ENTRY_LEGACY_MPESA])
        val resolvedReceipts =
            if (receipts.isNotEmpty()) receipts else legacyMpesaToReceipts(legacyMpesa)
        val resolvedAllocations =
            if (allocations.isNotEmpty()) allocations else legacyMpesaToAllocations(legacyMpesa)

        progress?.invoke(70, "Restoring data")
        database.withTransaction {
            database.clearAllTables()
            database.customerDao().insertAll(customers)
            database.orderDao().insertAll(orders)
            database.orderItemDao().insertAll(orderItems)
            database.accountingDao().insertAccountEntries(accountEntries)
            database.accountingDao().insertPayments(payments)
            database.paymentReceiptDao().insertAll(resolvedReceipts)
            database.paymentAllocationDao().insertAll(resolvedAllocations)
        }
        progress?.invoke(95, "Finalizing")
    }

    private fun validateRestoreEntries(
        rawEntries: Map<String, ByteArray>,
        currentDbVersion: Int,
        manifestPolicy: RestoreManifestPolicy
    ): Map<String, String> {
        if (rawEntries.isEmpty()) {
            throw IllegalArgumentException("Backup archive is empty")
        }

        val missingRequired = REQUIRED_ENTRIES.filterNot { rawEntries.containsKey(it) }
        if (missingRequired.isNotEmpty()) {
            throw IllegalArgumentException("Backup is missing required files: ${missingRequired.joinToString()}")
        }

        val textEntries = rawEntries.mapValues { (_, payload) -> payload.toString(Charsets.UTF_8) }

        val metadataPayload = textEntries[ENTRY_METADATA]
            ?: throw IllegalArgumentException("Backup is missing metadata")
        val metadata = runCatching { JSONObject(metadataPayload) }
            .getOrElse { throw IllegalArgumentException("Backup metadata is invalid") }

        val backupDbVersion = metadata.optInt("dbVersion", -1)
        if (backupDbVersion <= 0) {
            throw IllegalArgumentException("Backup metadata is missing dbVersion")
        }
        if (backupDbVersion > currentDbVersion) {
            throw IllegalStateException("Backup requires newer app database version")
        }

        REQUIRED_ENTRIES
            .filter { it != ENTRY_METADATA }
            .forEach { entryName ->
                val payload = textEntries[entryName]
                    ?: throw IllegalArgumentException("Backup is missing required files: $entryName")
                parseArrayStrict(payload, entryName)
            }

        ARRAY_ENTRIES
            .filter { rawEntries.containsKey(it) }
            .forEach { entryName ->
                parseArrayStrict(textEntries.getValue(entryName), entryName)
            }

        if (rawEntries.containsKey(ENTRY_MANIFEST)) {
            verifyManifest(rawEntries, textEntries.getValue(ENTRY_MANIFEST))
        } else if (manifestPolicy == RestoreManifestPolicy.Strict) {
            throw IllegalArgumentException("Backup manifest is required in strict mode")
        }

        return textEntries
    }

    private fun verifyManifest(rawEntries: Map<String, ByteArray>, manifestPayload: String) {
        val manifest =
            runCatching { JSONObject(manifestPayload) }
                .getOrElse { throw IllegalArgumentException("Backup manifest is invalid") }

        val formatVersion = manifest.optInt("formatVersion", -1)
        if (formatVersion != MANIFEST_FORMAT_VERSION) {
            throw IllegalArgumentException("Unsupported backup manifest version")
        }

        val manifestEntries =
            manifest.optJSONArray("entries")
                ?: throw IllegalArgumentException("Backup manifest is missing entries")

        val declaredNames = mutableSetOf<String>()
        for (index in 0 until manifestEntries.length()) {
            val obj = manifestEntries.optJSONObject(index)
                ?: throw IllegalArgumentException("Backup manifest entry is invalid")
            val name = obj.optString("name", "").trim()
            val expectedHash = obj.optString("sha256", "").trim().lowercase(Locale.US)
            val expectedSize = obj.optLong("size", -1)

            if (name.isEmpty() || expectedHash.isEmpty()) {
                throw IllegalArgumentException("Backup manifest entry is missing required fields")
            }
            if (!declaredNames.add(name)) {
                throw IllegalArgumentException("Backup manifest contains duplicate entries")
            }

            val payload = rawEntries[name]
                ?: throw IllegalArgumentException("Backup is missing file referenced by manifest: $name")

            if (expectedSize >= 0 && expectedSize != payload.size.toLong()) {
                throw IllegalArgumentException("Backup file size mismatch for $name")
            }

            val actualHash = sha256Hex(payload).lowercase(Locale.US)
            if (actualHash != expectedHash) {
                throw IllegalArgumentException("Backup file checksum mismatch for $name")
            }
        }

        val missing = REQUIRED_ENTRIES.filterNot { declaredNames.contains(it) }
        if (missing.isNotEmpty()) {
            throw IllegalArgumentException("Backup manifest is missing required checksum entries")
        }
    }

    private fun parseArrayStrict(payload: String, entryName: String) {
        runCatching { JSONArray(payload) }
            .getOrElse { throw IllegalArgumentException("Backup payload is invalid for $entryName") }
    }

    private fun readZipEntries(
        inputStream: InputStream,
        maxEntryBytes: Long = MAX_ZIP_ENTRY_BYTES,
        maxTotalBytes: Long = MAX_ZIP_TOTAL_BYTES
    ): Map<String, ByteArray> {
        val result = linkedMapOf<String, ByteArray>()
        var totalBytes = 0L
        ZipInputStream(BufferedInputStream(inputStream)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    if (result.containsKey(entry.name)) {
                        throw IllegalArgumentException("Backup archive contains duplicate entry: ${entry.name}")
                    }
                    val payload =
                        readEntryBytes(
                            zip = zip,
                            entryName = entry.name,
                            maxEntryBytes = maxEntryBytes,
                            maxRemainingBytes = maxTotalBytes - totalBytes
                        )
                    totalBytes += payload.size.toLong()
                    result[entry.name] = payload
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return result
    }

    private fun readEntryBytes(
        zip: ZipInputStream,
        entryName: String,
        maxEntryBytes: Long,
        maxRemainingBytes: Long
    ): ByteArray {
        if (maxRemainingBytes <= 0L) {
            throw IllegalArgumentException("Backup archive exceeds supported size")
        }
        val buffer = ByteArray(4_096)
        val output = ByteArrayOutputStream()
        var written = 0L
        var read = zip.read(buffer)
        while (read >= 0) {
            if (read > 0) {
                written += read.toLong()
                if (written > maxEntryBytes) {
                    throw IllegalArgumentException("Backup entry is too large: $entryName")
                }
                if (written > maxRemainingBytes) {
                    throw IllegalArgumentException("Backup archive exceeds supported size")
                }
                output.write(buffer, 0, read)
            }
            read = zip.read(buffer)
        }
        return output.toByteArray()
    }

    private fun customersToJson(customers: List<CustomerEntity>): JSONArray {
        val array = JSONArray()
        customers.forEach { customer ->
            array.put(
                JSONObject()
                    .put("id", customer.id)
                    .put("name", customer.name)
                    .put("phone", customer.phone)
                    .put("isArchived", customer.isArchived)
            )
        }
        return array
    }

    private fun ordersToJson(orders: List<OrderEntity>): JSONArray {
        val array = JSONArray()
        orders.forEach { order ->
            array.put(
                JSONObject()
                    .put("id", order.id)
                    .put("orderDate", order.orderDate.toString())
                    .put("createdAt", order.createdAt)
                    .put("updatedAt", order.updatedAt)
                    .put("notes", order.notes)
                    .put("pickupTime", order.pickupTime ?: JSONObject.NULL)
                    .put("status", order.status.name)
                    .put("statusOverride", order.statusOverride?.name ?: JSONObject.NULL)
                    .put("totalAmount", order.totalAmount.toPlainString())
                    .put("customerId", order.customerId ?: JSONObject.NULL)
            )
        }
        return array
    }

    private fun orderItemsToJson(items: List<OrderItemEntity>): JSONArray {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("orderId", item.orderId)
                    .put("name", item.name)
                    .put("category", item.category.name)
                    .put("quantity", item.quantity)
                    .put("unitPrice", item.unitPrice.toPlainString())
            )
        }
        return array
    }

    private fun accountEntriesToJson(entries: List<AccountEntryEntity>): JSONArray {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("id", entry.id)
                    .put("orderId", entry.orderId ?: JSONObject.NULL)
                    .put("customerId", entry.customerId ?: JSONObject.NULL)
                    .put("type", entry.type.name)
                    .put("amount", entry.amount.toPlainString())
                    .put("date", entry.date)
                    .put("description", entry.description)
            )
        }
        return array
    }

    private fun paymentsToJson(payments: List<PaymentEntity>): JSONArray {
        val array = JSONArray()
        payments.forEach { payment ->
            array.put(
                JSONObject()
                    .put("id", payment.id)
                    .put("orderId", payment.orderId)
                    .put("amount", payment.amount.toPlainString())
                    .put("method", payment.method.name)
                    .put("paidAt", payment.paidAt)
            )
        }
        return array
    }

    private fun paymentReceiptsToJson(receipts: List<PaymentReceiptEntity>): JSONArray {
        val array = JSONArray()
        receipts.forEach { receipt ->
            array.put(
                JSONObject()
                    .put("id", receipt.id)
                    .put("amount", receipt.amount.toPlainString())
                    .put("receivedAt", receipt.receivedAt)
                    .put("method", receipt.method.name)
                    .put("transactionCode", receipt.transactionCode ?: JSONObject.NULL)
                    .put("hash", receipt.hash ?: JSONObject.NULL)
                    .put("senderName", receipt.senderName ?: JSONObject.NULL)
                    .put("senderPhone", receipt.senderPhone ?: JSONObject.NULL)
                    .put("rawText", receipt.rawText ?: JSONObject.NULL)
                    .put("customerId", receipt.customerId ?: JSONObject.NULL)
                    .put("note", receipt.note ?: JSONObject.NULL)
                    .put("status", receipt.status.name)
                    .put("createdAt", receipt.createdAt)
                    .put("voidedAt", receipt.voidedAt ?: JSONObject.NULL)
                    .put("voidReason", receipt.voidReason ?: JSONObject.NULL)
            )
        }
        return array
    }

    private fun paymentAllocationsToJson(allocations: List<PaymentAllocationEntity>): JSONArray {
        val array = JSONArray()
        allocations.forEach { allocation ->
            array.put(
                JSONObject()
                    .put("id", allocation.id)
                    .put("receiptId", allocation.receiptId)
                    .put("orderId", allocation.orderId ?: JSONObject.NULL)
                    .put("customerId", allocation.customerId ?: JSONObject.NULL)
                    .put("amount", allocation.amount.toPlainString())
                    .put("type", allocation.type.name)
                    .put("status", allocation.status.name)
                    .put("accountEntryId", allocation.accountEntryId ?: JSONObject.NULL)
                    .put("reversalEntryId", allocation.reversalEntryId ?: JSONObject.NULL)
                    .put("createdAt", allocation.createdAt)
                    .put("voidedAt", allocation.voidedAt ?: JSONObject.NULL)
                    .put("voidReason", allocation.voidReason ?: JSONObject.NULL)
            )
        }
        return array
    }

    private fun parseCustomers(payload: String?): List<CustomerEntity> {
        val array = parseArray(payload) ?: return emptyList()
        return (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            CustomerEntity(
                id = obj.getLong("id"),
                name = obj.getString("name"),
                phone = obj.getString("phone"),
                isArchived = obj.optBoolean("isArchived", false)
            )
        }
    }

    private fun parseOrders(payload: String?): List<OrderEntity> {
        val array = parseArray(payload) ?: return emptyList()
        return (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            val status = OrderStatus.valueOf(obj.getString("status"))
            val statusOverride =
                if (!obj.has("statusOverride") || obj.isNull("statusOverride")) {
                    null
                } else {
                    OrderStatusOverride.valueOf(obj.getString("statusOverride"))
                }
            val pickupTime =
                if (obj.has("pickupTime") && !obj.isNull("pickupTime")) {
                    obj.getString("pickupTime")
                } else {
                    null
                }
            val customerId =
                if (!obj.has("customerId") || obj.isNull("customerId")) null else obj.getLong("customerId")
            val totalAmount = parseRequiredDecimal(obj.optString("totalAmount", ""), "orders.totalAmount", index)
            // Legacy field in older backups; intentionally ignored now that orders.amountPaid is retired.
            obj.optString(LEGACY_AMOUNT_PAID_FIELD, "")
            OrderEntity(
                id = obj.getLong("id"),
                orderDate = LocalDate.parse(obj.getString("orderDate")),
                createdAt = obj.getLong("createdAt"),
                updatedAt = obj.getLong("updatedAt"),
                notes = obj.getString("notes"),
                pickupTime = pickupTime,
                status = status,
                statusOverride = statusOverride,
                totalAmount = totalAmount,
                customerId = customerId
            )
        }
    }

    private fun parseOrderItems(payload: String?): List<OrderItemEntity> {
        val array = parseArray(payload) ?: return emptyList()
        return (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            val unitPrice = parseRequiredDecimal(obj.optString("unitPrice", ""), "order_items.unitPrice", index)
            OrderItemEntity(
                id = obj.getLong("id"),
                orderId = obj.getLong("orderId"),
                name = obj.getString("name"),
                category = ItemCategory.valueOf(obj.getString("category")),
                quantity = obj.getInt("quantity"),
                unitPrice = unitPrice
            )
        }
    }

    private fun parseAccountEntries(payload: String?): List<AccountEntryEntity> {
        val array = parseArray(payload) ?: return emptyList()
        return (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            val orderId = if (obj.isNull("orderId")) null else obj.getLong("orderId")
            val customerId = if (obj.isNull("customerId")) null else obj.getLong("customerId")
            val amount = parseRequiredDecimal(obj.optString("amount", ""), "account_entries.amount", index)
            AccountEntryEntity(
                id = obj.getLong("id"),
                orderId = orderId,
                customerId = customerId,
                type = EntryType.valueOf(obj.getString("type")),
                amount = amount,
                date = obj.getLong("date"),
                description = obj.getString("description")
            )
        }
    }

    private fun parsePayments(payload: String?): List<PaymentEntity> {
        val array = parseArray(payload) ?: return emptyList()
        return (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            val amount = parseRequiredDecimal(obj.optString("amount", ""), "payments.amount", index)
            PaymentEntity(
                id = obj.getLong("id"),
                orderId = obj.getLong("orderId"),
                amount = amount,
                method = PaymentMethod.valueOf(obj.getString("method")),
                paidAt = obj.getLong("paidAt")
            )
        }
    }

    private fun parsePaymentReceipts(payload: String?): List<PaymentReceiptEntity> {
        val array = parseArray(payload) ?: return emptyList()
        return (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            val transactionCode =
                if (obj.isNull("transactionCode")) null else obj.getString("transactionCode")
            val hash = if (obj.isNull("hash")) null else obj.getString("hash")
            val senderName = if (obj.isNull("senderName")) null else obj.getString("senderName")
            val senderPhone = if (obj.isNull("senderPhone")) null else obj.getString("senderPhone")
            val rawText = if (obj.isNull("rawText")) null else obj.getString("rawText")
            val customerId = if (obj.isNull("customerId")) null else obj.getLong("customerId")
            val note = if (obj.isNull("note")) null else obj.getString("note")
            val voidedAt = if (obj.isNull("voidedAt")) null else obj.getLong("voidedAt")
            val voidReason = if (obj.isNull("voidReason")) null else obj.getString("voidReason")
            val amount = parseRequiredDecimal(obj.optString("amount", ""), "payment_receipts.amount", index)
            PaymentReceiptEntity(
                id = obj.getLong("id"),
                amount = amount,
                receivedAt = obj.getLong("receivedAt"),
                method = PaymentMethod.valueOf(obj.getString("method")),
                transactionCode = transactionCode,
                hash = hash,
                senderName = senderName,
                senderPhone = senderPhone,
                rawText = rawText,
                customerId = customerId,
                note = note,
                status = PaymentReceiptStatus.valueOf(obj.getString("status")),
                createdAt = obj.getLong("createdAt"),
                voidedAt = voidedAt,
                voidReason = voidReason
            )
        }
    }

    private fun parsePaymentAllocations(payload: String?): List<PaymentAllocationEntity> {
        val array = parseArray(payload) ?: return emptyList()
        return (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            val orderId = if (obj.isNull("orderId")) null else obj.getLong("orderId")
            val customerId = if (obj.isNull("customerId")) null else obj.getLong("customerId")
            val accountEntryId = if (obj.isNull("accountEntryId")) null else obj.getLong("accountEntryId")
            val reversalEntryId = if (obj.isNull("reversalEntryId")) null else obj.getLong("reversalEntryId")
            val voidedAt = if (obj.isNull("voidedAt")) null else obj.getLong("voidedAt")
            val voidReason = if (obj.isNull("voidReason")) null else obj.getString("voidReason")
            val amount = parseRequiredDecimal(obj.optString("amount", ""), "payment_allocations.amount", index)
            PaymentAllocationEntity(
                id = obj.getLong("id"),
                receiptId = obj.getLong("receiptId"),
                orderId = orderId,
                customerId = customerId,
                amount = amount,
                type = PaymentAllocationType.valueOf(obj.getString("type")),
                status = PaymentAllocationStatus.valueOf(obj.getString("status")),
                accountEntryId = accountEntryId,
                reversalEntryId = reversalEntryId,
                createdAt = obj.getLong("createdAt"),
                voidedAt = voidedAt,
                voidReason = voidReason
            )
        }
    }

    private fun parseLegacyMpesaTransactions(payload: String?): List<LegacyMpesaTransaction> {
        val array = parseArray(payload) ?: return emptyList()
        return (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            val transactionCode =
                if (obj.isNull("transactionCode")) null else obj.getString("transactionCode")
            val senderName = if (obj.isNull("senderName")) null else obj.getString("senderName")
            val senderPhone = if (obj.isNull("senderPhone")) null else obj.getString("senderPhone")
            val rawText = if (obj.isNull("rawText")) null else obj.getString("rawText")
            val customerId = if (obj.isNull("customerId")) null else obj.getLong("customerId")
            val orderId = if (obj.isNull("orderId")) null else obj.getLong("orderId")
            val accountEntryId = if (obj.isNull("accountEntryId")) null else obj.getLong("accountEntryId")
            val amount = parseRequiredDecimal(obj.optString("amount", ""), "mpesa_transactions.amount", index)
            LegacyMpesaTransaction(
                id = obj.getLong("id"),
                transactionCode = transactionCode,
                hash = obj.getString("hash"),
                amount = amount,
                senderName = senderName,
                senderPhone = senderPhone,
                receivedAt = obj.getLong("receivedAt"),
                rawText = rawText,
                customerId = customerId,
                orderId = orderId,
                accountEntryId = accountEntryId
            )
        }
    }

    private fun legacyMpesaToReceipts(
        transactions: List<LegacyMpesaTransaction>
    ): List<PaymentReceiptEntity> {
        if (transactions.isEmpty()) return emptyList()
        return transactions.map { tx ->
            PaymentReceiptEntity(
                id = tx.id,
                amount = tx.amount,
                receivedAt = tx.receivedAt,
                method = PaymentMethod.MPESA,
                transactionCode = tx.transactionCode,
                hash = tx.hash,
                senderName = tx.senderName,
                senderPhone = tx.senderPhone,
                rawText = tx.rawText,
                customerId = tx.customerId,
                note = null,
                status = PaymentReceiptStatus.APPLIED,
                createdAt = tx.receivedAt,
                voidedAt = null,
                voidReason = null
            )
        }
    }

    private fun legacyMpesaToAllocations(
        transactions: List<LegacyMpesaTransaction>
    ): List<PaymentAllocationEntity> {
        if (transactions.isEmpty()) return emptyList()
        return transactions.map { tx ->
            PaymentAllocationEntity(
                receiptId = tx.id,
                orderId = tx.orderId,
                customerId = tx.customerId,
                amount = tx.amount,
                type = if (tx.orderId == null) PaymentAllocationType.CUSTOMER_CREDIT else PaymentAllocationType.ORDER,
                status = PaymentAllocationStatus.APPLIED,
                accountEntryId = tx.accountEntryId,
                reversalEntryId = null,
                createdAt = tx.receivedAt,
                voidedAt = null,
                voidReason = null
            )
        }
    }

    private fun parseArray(payload: String?): JSONArray? {
        if (payload.isNullOrBlank()) return null
        return runCatching { JSONArray(payload) }.getOrNull()
    }

    private fun parseRequiredDecimal(raw: String?, field: String, index: Int): BigDecimal {
        return parseBackupDecimal(raw)
            ?: throw IllegalArgumentException("Invalid decimal for $field at index $index: '$raw'")
    }

    internal fun parseBackupDecimal(raw: String?): BigDecimal? {
        if (raw == null) return null
        val compact = raw.trim().replace(" ", "").replace("\u00A0", "")
        if (compact.isEmpty()) return null
        val normalized =
            when {
                compact.contains(',') && compact.contains('.') -> {
                    if (compact.lastIndexOf(',') > compact.lastIndexOf('.')) {
                        compact.replace(".", "").replace(',', '.')
                    } else {
                        compact.replace(",", "")
                    }
                }

                compact.contains(',') -> {
                    if (Regex("^-?\\d{1,3}(,\\d{3})+$").matches(compact)) {
                        compact.replace(",", "")
                    } else {
                        compact.replace(',', '.')
                    }
                }

                else -> compact
            }
        return normalized.toBigDecimalOrNull()
    }

    internal fun isRetryableBackupException(error: Exception): Boolean {
        return when (error) {
            is IOException -> true
            is SecurityException -> false
            is IllegalArgumentException -> false
            is IllegalStateException -> false
            else -> false
        }
    }

    private fun hasPersistedReadWritePermission(context: Context, uriString: String?): Boolean {
        val uri = uriString?.toUri() ?: return false
        return context.contentResolver.persistedUriPermissions.any { permission ->
            permission.uri == uri && permission.isReadPermission && permission.isWritePermission
        }
    }

    private fun queryDriveDocumentsAuthorities(packageManager: PackageManager): List<String> {
        val providers = queryDocumentsProviders(packageManager)
        return providers.filter { authority ->
            authority.contains(GOOGLE_DRIVE_DOCUMENTS_AUTHORITY_PREFIX, ignoreCase = true)
        }
    }

    private fun queryDocumentsProviders(packageManager: PackageManager): List<String> {
        val queryIntent = Intent(DocumentsContract.PROVIDER_INTERFACE)
        val resolveInfos =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentContentProviders(
                    queryIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DISABLED_COMPONENTS.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentContentProviders(queryIntent, PackageManager.MATCH_DISABLED_COMPONENTS)
            }

        return resolveInfos
            .asSequence()
            .mapNotNull { it.providerInfo?.authority }
            .flatMap { authority -> authority.split(';').asSequence().map { it.trim() } }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
    }

    private fun PackageManager.packageInfoOrNull(packageName: String): android.content.pm.PackageInfo? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                getPackageInfo(packageName, 0)
            }
        }.getOrNull()
    }

    private fun PackageManager.applicationEnabled(packageName: String): Boolean {
        return runCatching {
            getApplicationInfo(packageName, 0).enabled
        }.getOrDefault(false)
    }

    private fun currentDatabaseVersion(
        context: Context? = null,
        database: AppDatabase? = null
    ): Int {
        val dbVersionFromOpenHelper =
            runCatching {
                when {
                    database != null -> database.openHelper.readableDatabase.version
                    context != null -> DatabaseProvider.getDatabase(context).openHelper.readableDatabase.version
                    else -> 0
                }
            }.getOrDefault(0)
        if (dbVersionFromOpenHelper > 0) {
            return dbVersionFromOpenHelper
        }
        return APP_DATABASE_SCHEMA_VERSION
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }

    private data class BackupSnapshot(
        val customers: List<CustomerEntity>,
        val orders: List<OrderEntity>,
        val orderItems: List<OrderItemEntity>,
        val accountEntries: List<AccountEntryEntity>,
        val payments: List<PaymentEntity>,
        val paymentReceipts: List<PaymentReceiptEntity>,
        val paymentAllocations: List<PaymentAllocationEntity>
    )

    private data class BackupArchive(
        val bytes: ByteArray,
        val sha256: String
    )

    private data class LatestPointer(
        val fileName: String,
        val sha256: String,
        val updatedAt: Long
    ) {
        fun toJson(): JSONObject {
            return JSONObject()
                .put("fileName", fileName)
                .put("sha256", sha256)
                .put("updatedAt", updatedAt)
        }
    }

    private data class LegacyMpesaTransaction(
        val id: Long,
        val transactionCode: String?,
        val hash: String,
        val amount: BigDecimal,
        val senderName: String?,
        val senderPhone: String?,
        val receivedAt: Long,
        val rawText: String?,
        val customerId: Long?,
        val orderId: Long?,
        val accountEntryId: Long?
    )
}
