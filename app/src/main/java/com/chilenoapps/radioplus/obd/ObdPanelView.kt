package com.chilenoapps.radioplus.obd

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.chilenoapps.radioplus.databinding.ViewObdPanelBinding

class ObdPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs), Elm327BluetoothClient.Listener {
    private val binding = ViewObdPanelBinding.inflate(LayoutInflater.from(context), this, true)
    private var client: Elm327BluetoothClient? = null
    private var selectedDevice: ObdDevice? = null
    private var adminMode = false

    fun bind(value: Elm327BluetoothClient, isAdmin: Boolean) {
        client = value
        adminMode = isAdmin
        value.setListener(this)
        binding.adminObd.text = if (adminMode) "ADMIN • DIAGNÓSTICO ATIVO" else "DIAGNÓSTICO OBD-II"
        binding.chooseDevice.setOnClickListener { requestPermissionThenChoose() }
        binding.connect.setOnClickListener {
            val device = selectedDevice
            if (device == null) chooseDevice() else client?.connect(device)
        }
        binding.disconnect.setOnClickListener { client?.disconnect() }
        binding.readCodes.setOnClickListener { client?.readTroubleCodes() }
        binding.clearCodes.setOnClickListener { confirmClearCodes() }
    }

    private fun requestPermissionThenChoose() {
        if (Build.VERSION.SDK_INT < 31 || (
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                )) {
            chooseDevice()
            return
        }
        val activity = context as? AppCompatActivity ?: return
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN),
            4201
        )
        toast("Autorize o Bluetooth e toque novamente em SELECIONAR")
    }

    private fun chooseDevice() {
        client?.pairedDevices()?.fold(
            onSuccess = { devices ->
                if (devices.isEmpty()) {
                    toast("Pareie o ELM327 nas configurações Bluetooth primeiro")
                    return@fold
                }
                AlertDialog.Builder(context)
                    .setTitle("Selecionar ELM327 pareado")
                    .setItems(devices.map { "${it.name}\n${it.address}" }.toTypedArray()) { _, index ->
                        selectedDevice = devices[index]
                        binding.device.text = "${devices[index].name} • ${devices[index].address}"
                        binding.connect.isEnabled = true
                    }
                    .setNegativeButton("CANCELAR", null)
                    .show()
            },
            onFailure = { toast(it.message ?: "Não foi possível acessar o Bluetooth") }
        )
    }

    private fun confirmClearCodes() {
        AlertDialog.Builder(context)
            .setTitle("Apagar códigos de falha?")
            .setMessage("Faça isso somente com o veículo parado e a ignição ligada. Os monitores de emissões poderão ser reiniciados.")
            .setNegativeButton("CANCELAR", null)
            .setPositiveButton("APAGAR") { _, _ -> client?.clearTroubleCodes() }
            .show()
    }

    override fun onState(state: ObdConnectionState) {
        when (state) {
            ObdConnectionState.Disconnected -> {
                binding.status.text = "DESCONECTADO"
                binding.status.setTextColor(0xFF9AA6B5.toInt())
                setConnected(false)
            }
            is ObdConnectionState.Connecting -> {
                binding.status.text = "CONECTANDO A ${state.device.name.uppercase()}…"
                binding.status.setTextColor(0xFF5EDBFF.toInt())
                binding.connect.isEnabled = false
            }
            is ObdConnectionState.Connected -> {
                binding.status.text = "CONECTADO • ${state.adapter} • ${state.protocol}"
                binding.status.setTextColor(0xFF57E389.toInt())
                setConnected(true)
            }
            is ObdConnectionState.Error -> {
                binding.status.text = "ERRO • ${state.message}"
                binding.status.setTextColor(0xFFFFB454.toInt())
                setConnected(false)
            }
        }
    }

    override fun onLiveData(data: ObdLiveData) {
        binding.rpm.text = data.rpm?.let { "%04d".format(it) } ?: "—"
        binding.speed.text = data.speedKmh?.toString() ?: "—"
        binding.coolant.text = data.coolantCelsius?.let { "$it °C" } ?: "—"
        binding.intake.text = data.intakeCelsius?.let { "$it °C" } ?: "—"
        binding.load.text = data.engineLoadPercent?.let { "$it%" } ?: "—"
        binding.throttle.text = data.throttlePercent?.let { "$it%" } ?: "—"
        binding.voltage.text = data.voltage?.let { "%.1f V".format(it) } ?: "—"
    }

    override fun onTroubleCodes(codes: List<String>) {
        binding.codes.text = if (codes.isEmpty()) "Nenhum código confirmado" else codes.joinToString("  •  ")
    }

    private fun setConnected(connected: Boolean) {
        binding.connect.isEnabled = !connected && selectedDevice != null
        binding.disconnect.isEnabled = connected
        binding.readCodes.isEnabled = connected
        binding.clearCodes.isEnabled = connected
    }

    private fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_LONG).show()

    fun release() {
        client?.setListener(null)
        client?.release()
        client = null
    }
}
