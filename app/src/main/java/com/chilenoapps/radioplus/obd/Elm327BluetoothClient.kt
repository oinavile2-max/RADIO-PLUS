package com.chilenoapps.radioplus.obd

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class Elm327BluetoothClient(private val context: Context) {
    interface Listener {
        fun onState(state: ObdConnectionState)
        fun onLiveData(data: ObdLiveData)
        fun onTroubleCodes(codes: List<String>)
    }

    private val main = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val running = AtomicBoolean(false)
    private val adapter: BluetoothAdapter? by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }
    @Volatile private var socket: BluetoothSocket? = null
    @Volatile private var listener: Listener? = null
    @Volatile private var lastSpeedKmh: Int? = null

    fun setListener(value: Listener?) { listener = value }

    @SuppressLint("MissingPermission")
    fun pairedDevices(): Result<List<ObdDevice>> = runCatching {
        requireBluetoothPermission()
        val bluetooth = adapter ?: error("Bluetooth não disponível nesta central")
        if (!bluetooth.isEnabled) error("Ative o Bluetooth da central")
        bluetooth.bondedDevices.orEmpty()
            .map { ObdDevice(it.name ?: "Dispositivo Bluetooth", it.address) }
            .sortedBy { if (it.name.contains("OBD", true) || it.name.contains("ELM", true)) 0 else 1 }
    }

    @SuppressLint("MissingPermission")
    fun connect(device: ObdDevice) {
        disconnect()
        emit(ObdConnectionState.Connecting(device))
        executor.execute {
            try {
                requireBluetoothPermission()
                val remote = adapter?.getRemoteDevice(device.address) ?: error("Bluetooth indisponível")
                adapter?.cancelDiscovery()
                val newSocket = remote.createRfcommSocketToServiceRecord(SPP_UUID)
                newSocket.connect()
                socket = newSocket
                running.set(true)
                initialize(device, newSocket)
                schedulePoll(newSocket)
            } catch (error: Exception) {
                closeSocket()
                emit(ObdConnectionState.Error(error.message ?: "Falha ao conectar ao ELM327"))
            }
        }
    }

    fun disconnect() {
        running.set(false)
        closeSocket()
        emit(ObdConnectionState.Disconnected)
    }

    fun readTroubleCodes() = executor.execute {
        try {
            val connected = socket ?: error("Conecte o ELM327 primeiro")
            val codes = Elm327Parser.troubleCodes(command(connected, "03", 4500))
            main.post { listener?.onTroubleCodes(codes) }
        } catch (error: Exception) {
            emit(ObdConnectionState.Error(error.message ?: "Falha ao ler códigos"))
        }
    }

    fun clearTroubleCodes() = executor.execute {
        try {
            if ((lastSpeedKmh ?: 0) > 0) error("Pare o veículo antes de apagar falhas")
            val connected = socket ?: error("Conecte o ELM327 primeiro")
            val response = command(connected, "04", 4500).uppercase()
            if (!response.contains("44") && !response.contains("OK")) error("A ECU não confirmou a limpeza")
            main.post { listener?.onTroubleCodes(emptyList()) }
        } catch (error: Exception) {
            emit(ObdConnectionState.Error(error.message ?: "Falha ao apagar códigos"))
        }
    }

    private fun initialize(device: ObdDevice, connected: BluetoothSocket) {
        command(connected, "ATZ", 3500)
        command(connected, "ATE0")
        command(connected, "ATL0")
        command(connected, "ATS0")
        command(connected, "ATH0")
        command(connected, "ATSP0", 2500)
        val identity = command(connected, "ATI").lineSequence().firstOrNull { it.isNotBlank() } ?: "ELM327"
        command(connected, "0100", 6000)
        val protocol = command(connected, "ATDP").replace(">", "").trim()
        emit(ObdConnectionState.Connected(device, identity, protocol))

    }

    private fun schedulePoll(connected: BluetoothSocket) {
        executor.schedule({
            if (!running.get() || socket !== connected || !connected.isConnected) return@schedule
            try {
            val data = ObdLiveData(
                rpm = readNumber(connected, "0C")?.toInt(),
                speedKmh = readNumber(connected, "0D")?.toInt(),
                coolantCelsius = readNumber(connected, "05")?.toInt(),
                intakeCelsius = readNumber(connected, "0F")?.toInt(),
                engineLoadPercent = readNumber(connected, "04")?.toInt(),
                throttlePercent = readNumber(connected, "11")?.toInt(),
                voltage = runCatching { Elm327Parser.voltage(command(connected, "ATRV")) }.getOrNull()
            )
            lastSpeedKmh = data.speedKmh
            main.post { listener?.onLiveData(data) }
                schedulePoll(connected)
            } catch (error: Exception) {
                closeSocket()
                running.set(false)
                emit(ObdConnectionState.Error(error.message ?: "Conexão OBD interrompida"))
            }
        }, 350, TimeUnit.MILLISECONDS)
    }

    private fun readNumber(connected: BluetoothSocket, pid: String): Number? =
        runCatching { Elm327Parser.decode(pid, command(connected, "01$pid")) }.getOrNull()

    private fun command(connected: BluetoothSocket, value: String, timeoutMs: Long = 2200): String {
        val output = BufferedOutputStream(connected.outputStream)
        val input = BufferedInputStream(connected.inputStream)
        while (input.available() > 0) input.read()
        output.write("$value\r".toByteArray(Charsets.US_ASCII))
        output.flush()
        val result = StringBuilder()
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (input.available() > 0) {
                val next = input.read()
                if (next >= 0) {
                    val char = next.toChar()
                    result.append(char)
                    if (char == '>') return result.toString()
                }
            } else Thread.sleep(20)
        }
        error("ELM327 não respondeu ao comando $value")
    }

    private fun requireBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= 31 && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            error("Permissão Bluetooth não concedida")
        }
        if (Build.VERSION.SDK_INT >= 31 && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            error("Permissão de busca Bluetooth não concedida")
        }
    }

    private fun emit(state: ObdConnectionState) = main.post { listener?.onState(state) }

    private fun closeSocket() {
        runCatching { socket?.close() }
        socket = null
    }

    fun release() {
        listener = null
        disconnect()
        executor.shutdownNow()
    }

    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
