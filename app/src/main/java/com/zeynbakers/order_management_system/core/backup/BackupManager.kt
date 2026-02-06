package com.zeynbakers.order_management_system.core.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.room.Database
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.math.BigDecimal
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
    private const val MAX_APP_PRIVATE_BACKUPS = 7
    private const val DEFAULT_STAGE = "Preparing"
    // Legacy-only backup field. Keep for backward-compatible import until schema retirement is complete.
    private const val LEGACY_AMOUNT_PAID_FIELD = "amountPaid"

    suspend fun runBackup(
        context: Context,
        force: Boolean = false,
        progress: ProgressCallback? = null
    ): BackupResult {
        val prefs = BackupPreferences(context)
        val state = prefs.readState()
        if (!force && !state.autoEnabled) {
            return BackupResult(success = false, message = "Automatic backup disabled")
        }

        val now = System.currentTimeMillis()
        val fileName = backupFileName(now)
        val target = state.targetType
        val output =
            when (target) {
                BackupTargetType.AppPrivate ->
                    openAppPrivateOutput(context, fileName)
                BackupTargetType.SafDirectory ->
                    openSafOutput(context, state.targetUri, fileName)
            }

        if (output == null) {
            prefs.setLastResult(
                status = BackupStatus.Failed,
                message = "Backup location unavailable",
                time = now
            )
            return BackupResult(success = false, message = "Backup location unavailable")
        }

        return try {
            progress?.invoke(5, DEFAULT_STAGE)
            withContext(Dispatchers.IO) {
                output.use { stream ->
                    exportDatabase(
                        database = DatabaseProvider.getDatabase(context),
                        outputStream = stream,
                        exportedAt = now,
                        progress = progress
                    )
                }
            }
            if (target == BackupTargetType.AppPrivate) {
                pruneOldAppBackups(context)
            }
            progress?.invoke(100, "Backup complete")
            val message =
                if (target == BackupTargetType.AppPrivate) {
                    "Saved to app storage"
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
        val input =
            if (uriString.isNullOrBlank()) {
                openLatestAppPrivateInput(context)
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
                        progress = progress
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

    private fun backupFileName(timestamp: Long): String {
        val formatter = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
        return "oms-backup-${formatter.format(Date(timestamp))}.zip"
    }

    fun listAppPrivateBackups(context: Context): List<File> {
        val dir = File(context.filesDir, BACKUP_DIR_NAME)
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        return dir.listFiles { file -> file.extension.equals("zip", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    private fun openAppPrivateOutput(context: Context, fileName: String): OutputStream? {
        val dir = File(context.filesDir, BACKUP_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        if (!dir.exists()) {
            return null
        }
        val file = File(dir, fileName)
        return file.outputStream()
    }

    private fun openLatestAppPrivateInput(context: Context): java.io.InputStream? {
        val latest = listAppPrivateBackups(context).firstOrNull() ?: return null
        return latest.inputStream()
    }

    private fun openSafOutput(context: Context, uriString: String?, fileName: String): OutputStream? {
        val uri = uriString?.let { runCatching { Uri.parse(it) }.getOrNull() } ?: return null
        val tree = DocumentFile.fromTreeUri(context, uri) ?: return null
        val target = tree.createFile("application/zip", fileName) ?: return null
        return context.contentResolver.openOutputStream(target.uri)
    }

    private fun openSafInput(context: Context, uriString: String): java.io.InputStream? {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return null
        return context.contentResolver.openInputStream(uri)
    }

    private fun pruneOldAppBackups(context: Context) {
        val dir = File(context.filesDir, BACKUP_DIR_NAME)
        if (!dir.exists() || !dir.isDirectory) return
        val backups =
            dir.listFiles { file -> file.extension.equals("zip", ignoreCase = true) }
                ?.sortedByDescending { it.lastModified() }
                ?: return
        backups.drop(MAX_APP_PRIVATE_BACKUPS).forEach { it.delete() }
    }

    private suspend fun exportDatabase(
        database: AppDatabase,
        outputStream: OutputStream,
        exportedAt: Long,
        progress: ProgressCallback?
    ) {
        progress?.invoke(10, "Reading data")
        val metadata = buildMetadata(exportedAt)
        val customers = database.customerDao().getAllCustomers()
        val orders = database.orderDao().getAllOrders()
        val orderItems = database.orderItemDao().getAllOrderItems()
        val entries = database.accountingDao().getAllAccountEntries()
        val payments = database.accountingDao().getAllPayments()
        val receipts = database.paymentReceiptDao().getAll()
        val allocations = database.paymentAllocationDao().getAll()

        ZipOutputStream(BufferedOutputStream(outputStream)).use { zip ->
            progress?.invoke(20, "Writing metadata")
            writeJsonEntry(zip, "metadata.json", metadata)
            progress?.invoke(35, "Writing customers")
            writeJsonEntry(zip, "customers.json", customersToJson(customers))
            progress?.invoke(55, "Writing orders")
            writeJsonEntry(zip, "orders.json", ordersToJson(orders))
            progress?.invoke(70, "Writing order items")
            writeJsonEntry(zip, "order_items.json", orderItemsToJson(orderItems))
            progress?.invoke(85, "Writing account entries")
            writeJsonEntry(zip, "account_entries.json", accountEntriesToJson(entries))
            progress?.invoke(95, "Writing payments")
            writeJsonEntry(zip, "payments.json", paymentsToJson(payments))
            progress?.invoke(96, "Writing payment receipts")
            writeJsonEntry(zip, "payment_receipts.json", paymentReceiptsToJson(receipts))
            progress?.invoke(97, "Writing payment allocations")
            writeJsonEntry(zip, "payment_allocations.json", paymentAllocationsToJson(allocations))
        }
    }

    private fun buildMetadata(exportedAt: Long): JSONObject {
        val dbVersion =
            AppDatabase::class.java.getAnnotation(Database::class.java)?.version ?: 0
        return JSONObject()
            .put("exportedAt", exportedAt)
            .put("appVersionName", BuildConfig.VERSION_NAME)
            .put("appVersionCode", BuildConfig.VERSION_CODE)
            .put("dbVersion", dbVersion)
    }

    private fun writeJsonEntry(zip: ZipOutputStream, name: String, json: Any) {
        val payload = when (json) {
            is JSONArray -> json.toString()
            is JSONObject -> json.toString()
            else -> json.toString()
        }
        zip.putNextEntry(ZipEntry(name))
        zip.write(payload.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
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

    private suspend fun importDatabase(
        database: AppDatabase,
        inputStream: InputStream,
        progress: ProgressCallback?
    ) {
        progress?.invoke(15, "Reading backup")
        val payloads = readZipEntries(inputStream)
        val customers = parseCustomers(payloads["customers.json"])
        val orders = parseOrders(payloads["orders.json"])
        val orderItems = parseOrderItems(payloads["order_items.json"])
        val accountEntries = parseAccountEntries(payloads["account_entries.json"])
        val payments = parsePayments(payloads["payments.json"])
        val receipts = parsePaymentReceipts(payloads["payment_receipts.json"])
        val allocations = parsePaymentAllocations(payloads["payment_allocations.json"])
        val legacyMpesa = parseLegacyMpesaTransactions(payloads["mpesa_transactions.json"])
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

    private fun readZipEntries(inputStream: InputStream): Map<String, String> {
        val result = mutableMapOf<String, String>()
        ZipInputStream(BufferedInputStream(inputStream)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    result[entry.name] = readEntryText(zip)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return result
    }

    internal fun readBackupPayloadsForTest(inputStream: InputStream): Map<String, String> =
        readZipEntries(inputStream)

    private fun readEntryText(zip: ZipInputStream): String {
        val buffer = ByteArray(4_096)
        val output = ByteArrayOutputStream()
        var read = zip.read(buffer)
        while (read >= 0) {
            if (read > 0) {
                output.write(buffer, 0, read)
            }
            read = zip.read(buffer)
        }
        return output.toString(Charsets.UTF_8.name())
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
            is java.io.IOException -> true
            is SecurityException -> false
            is IllegalArgumentException -> false
            is IllegalStateException -> false
            else -> false
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
