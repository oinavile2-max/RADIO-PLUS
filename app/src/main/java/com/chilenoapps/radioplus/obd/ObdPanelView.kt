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
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.chilenoapps.radioplus.databinding.ViewObdPanelBinding
import com.chilenoapps.radioplus.R
import com.chilenoapps.radioplus.settings.AppearanceStore
import com.chilenoapps.radioplus.ui.AccentStyler

class ObdPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs), Elm327BluetoothClient.Listener {
    private val binding = ViewObdPanelBinding.inflate(LayoutInflater.from(context), this, true)
    private var client: Elm327BluetoothClient? = null
    private var selectedDevice: ObdDevice? = null
    private var adminMode = false
    private var latestData = ObdLiveData()
    private val preferences = context.getSharedPreferences("obd_monitor", Context.MODE_PRIVATE)
    private val selectedParameters = preferences.getStringSet("selected", setOf("rpm", "speed", "coolant", "voltage")).orEmpty().toMutableSet()
    private var monitorPopup: PopupWindow? = null
    private var monitorList: LinearLayout? = null

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
        binding.showMonitor.setOnClickListener { toggleMonitorPopup() }
        mapOf(
            binding.rpm to "rpm", binding.speed to "speed", binding.coolant to "coolant",
            binding.intake to "intake", binding.load to "load", binding.throttle to "throttle", binding.voltage to "voltage"
        ).forEach { (valueView, key) ->
            (valueView.parent as? View)?.setOnClickListener { toggleParameter(key) }
            (valueView.parent as? View)?.isClickable = true
        }
        AccentStyler.styleButton(binding.showMonitor)
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
        latestData = data
        context.getSharedPreferences("obd_live", Context.MODE_PRIVATE).edit()
            .putInt("speed_kmh", data.speedKmh ?: -1)
            .putLong("updated_at", System.currentTimeMillis()).apply()
        binding.rpm.text = data.rpm?.let { "%04d".format(it) } ?: "—"
        binding.speed.text = data.speedKmh?.toString() ?: "—"
        binding.coolant.text = data.coolantCelsius?.let { "$it °C" } ?: "—"
        binding.intake.text = data.intakeCelsius?.let { "$it °C" } ?: "—"
        binding.load.text = data.engineLoadPercent?.let { "$it%" } ?: "—"
        binding.throttle.text = data.throttlePercent?.let { "$it%" } ?: "—"
        binding.voltage.text = data.voltage?.let { "%.1f V".format(it) } ?: "—"
        renderMonitorRows()
    }

    override fun onTroubleCodes(codes: List<String>) {
        binding.codes.text = if (codes.isEmpty()) "Nenhum código confirmado" else codes.joinToString("  •  ")
    }

    private fun setConnected(connected: Boolean) {
        binding.connect.alpha = if (connected) 0.55f else 1f
        binding.disconnect.alpha = if (connected) 1f else 0.72f
        binding.readCodes.alpha = if (connected) 1f else 0.72f
        binding.clearCodes.alpha = if (connected) 1f else 0.72f
    }

    private fun toggleParameter(key: String) {
        if (!selectedParameters.add(key)) selectedParameters.remove(key)
        preferences.edit().putStringSet("selected", selectedParameters.toSet()).apply()
        toast(if (selectedParameters.contains(key)) "Parâmetro adicionado ao monitor" else "Parâmetro removido do monitor")
        renderMonitorRows()
    }

    private fun toggleMonitorPopup() {
        if (monitorPopup?.isShowing == true) { monitorPopup?.dismiss(); return }
        val accent = AppearanceStore(context).accentColor
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(12))
            setBackgroundResource(R.drawable.bg_panel)
        }
        val header = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(TextView(context).apply { text = "MONITOR AO VIVO"; textSize = 15f; setTextColor(accent); setTypeface(typeface, 1) }, LinearLayout.LayoutParams(0, dp(46), 1f))
        val collapse = Button(context).apply { text = "⌃"; contentDescription = "Recolher monitor" }
        val close = Button(context).apply { text = "×"; contentDescription = "Fechar monitor"; setOnClickListener { monitorPopup?.dismiss() } }
        header.addView(collapse, LinearLayout.LayoutParams(dp(46), dp(42)))
        header.addView(close, LinearLayout.LayoutParams(dp(46), dp(42)))
        content.addView(header)
        monitorList = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        content.addView(monitorList, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        val pin = Button(context).apply {
            text = if (preferences.getBoolean("pinned", true)) "⌖  POPUP FIXADO" else "⌖  FIXAR POPUP"
            setOnClickListener {
                val value = !preferences.getBoolean("pinned", true)
                preferences.edit().putBoolean("pinned", value).apply()
                text = if (value) "⌖  POPUP FIXADO" else "⌖  FIXAR POPUP"
                monitorPopup?.isOutsideTouchable = !value
            }
        }
        content.addView(pin, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)))
        collapse.setOnClickListener {
            val visible = monitorList?.visibility != View.GONE
            monitorList?.visibility = if (visible) View.GONE else View.VISIBLE
            pin.visibility = if (visible) View.GONE else View.VISIBLE
            collapse.text = if (visible) "⌄" else "⌃"
            monitorPopup?.height = if (visible) dp(74) else dp(460)
            monitorPopup?.update()
        }
        monitorPopup = PopupWindow(content, dp(270), dp(460), false).apply {
            isOutsideTouchable = !preferences.getBoolean("pinned", true)
            elevation = dp(10).toFloat()
            showAtLocation(binding.root, Gravity.END or Gravity.CENTER_VERTICAL, dp(18), 0)
        }
        AccentStyler.apply(content)
        renderMonitorRows()
    }

    private fun renderMonitorRows() {
        val container = monitorList ?: return
        container.removeAllViews()
        val rows = linkedMapOf(
            "rpm" to ("◴  RPM" to latestData.rpm?.let { "$it rpm" }),
            "speed" to ("◉  VELOCIDADE" to latestData.speedKmh?.let { "$it km/h" }),
            "coolant" to ("♨  MOTOR" to latestData.coolantCelsius?.let { "$it °C" }),
            "intake" to ("≋  ADMISSÃO" to latestData.intakeCelsius?.let { "$it °C" }),
            "load" to ("▣  CARGA" to latestData.engineLoadPercent?.let { "$it%" }),
            "throttle" to ("◩  ACELERADOR" to latestData.throttlePercent?.let { "$it%" }),
            "voltage" to ("▤  TENSÃO" to latestData.voltage?.let { "%.1f V".format(it) })
        )
        rows.filterKeys(selectedParameters::contains).forEach { (_, pair) ->
            container.addView(TextView(context).apply {
                text = "${pair.first}\n${pair.second ?: "Não suportado / sem leitura"}"
                textSize = 14f; setTextColor(ContextCompat.getColor(context, R.color.rp_text)); setPadding(dp(12), dp(9), dp(12), dp(9)); setBackgroundResource(R.drawable.bg_track_row)
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(5) })
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_LONG).show()

    fun release() {
        monitorPopup?.dismiss()
        client?.setListener(null)
        client?.release()
        client = null
    }
}
