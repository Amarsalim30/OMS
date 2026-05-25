package com.zeynbakers.order_management_system.order.printing

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PairedBluetoothPrinter(
    val macAddress: String,
    val name: String
)

class BluetoothPrinterManager(private val context: Context) {
    private val printerDpi = 203
    private val printerWidthMm = 48f
    private val printerCharsPerLine = 32

    suspend fun getPairedPrinters(): List<PairedBluetoothPrinter> =
        withContext(Dispatchers.IO) {
            runCatching {
                BluetoothPrintersConnections()
                    .list
                    ?.mapNotNull { connection ->
                        val device = connection.device ?: return@mapNotNull null
                        PairedBluetoothPrinter(
                            macAddress = device.address,
                            name = device.name?.takeIf { it.isNotBlank() } ?: device.address
                        )
                    }
                    .orEmpty()
            }.getOrDefault(emptyList())
        }

    suspend fun printReceipt(macAddress: String, text: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            var connection: BluetoothConnection? = null
            try {
                connection = connect(macAddress)
                    ?: return@withContext Result.failure(
                        IllegalStateException("Could not connect to printer")
                    )
                print(connection, text)
                Result.success(Unit)
            } catch (t: Throwable) {
                Result.failure(t)
            } finally {
                disconnect(connection)
            }
        }

    private fun bluetoothDeviceForMac(macAddress: String) =
        BluetoothPrintersConnections()
            .list
            ?.firstOrNull { it.device?.address == macAddress }
            ?.device
            ?: run {
                val adapter =
                    context.getSystemService(BluetoothManager::class.java)?.adapter
                        ?: BluetoothAdapter.getDefaultAdapter()
                adapter?.getRemoteDevice(macAddress)
            }

    private suspend fun connect(macAddress: String): BluetoothConnection? {
        val device = bluetoothDeviceForMac(macAddress) ?: return null
        var lastError: Throwable? = null
        repeat(2) { attempt ->
            runCatching {
                val connection = BluetoothConnection(device)
                connection.connect()
                return connection
            }.onFailure { lastError = it }
            if (attempt == 0) {
                runCatching { Thread.sleep(400) }
            }
        }
        lastError?.let { throw it }
        return null
    }

    private fun print(connection: BluetoothConnection, text: String) {
        val printer = EscPosPrinter(
            connection,
            printerDpi,
            printerWidthMm,
            printerCharsPerLine
        )
        val formatted =
            text.lines().joinToString(separator = "") { line ->
                val safeLine = line.replace("[", "(").replace("]", ")")
                "[L]$safeLine\n"
            }
        printer.printFormattedText(formatted)
        printer.disconnectPrinter()
    }

    private fun disconnect(connection: BluetoothConnection?) {
        runCatching { connection?.disconnect() }
    }
}
