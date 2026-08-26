package com.chilenoapps.radioplus.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.chilenoapps.radioplus.R
import com.chilenoapps.radioplus.hardware.PhysicalRadioController
import com.chilenoapps.radioplus.model.RadioBand
import com.chilenoapps.radioplus.model.RadioState
import java.util.Locale

class RadioPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val bandFm: Button
    private val bandAm: Button
    private val seekDown: Button
    private val seekUp: Button
    private val playPause: Button
    private val frequency: TextView
    private val unit: TextView
    private val stationName: TextView
    private val stereo: TextView
    private val presets: LinearLayout
    private val radioDetails: LinearLayout
    private val hardwareStatus: TextView
    private val presetButtons: List<Button>
    private var controller: PhysicalRadioController? = null

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_radio_panel, this, true)
        bandFm = findViewById(R.id.bandFm)
        bandAm = findViewById(R.id.bandAm)
        seekDown = findViewById(R.id.seekDown)
        seekUp = findViewById(R.id.seekUp)
        playPause = findViewById(R.id.playPause)
        frequency = findViewById(R.id.frequency)
        unit = findViewById(R.id.unit)
        stationName = findViewById(R.id.stationName)
        stereo = findViewById(R.id.stereo)
        presets = findViewById(R.id.presets)
        radioDetails = findViewById(R.id.radioDetails)
        hardwareStatus = findViewById(R.id.hardwareStatus)
        presetButtons = listOf(
            findViewById(R.id.preset1), findViewById(R.id.preset2), findViewById(R.id.preset3),
            findViewById(R.id.preset4), findViewById(R.id.preset5), findViewById(R.id.preset6)
        )
        bandFm.setOnClickListener { render(controller?.setBand(RadioBand.FM)) }
        bandAm.setOnClickListener { render(controller?.setBand(RadioBand.AM)) }
        seekDown.setOnClickListener { render(controller?.seekDown()) }
        seekUp.setOnClickListener { render(controller?.seekUp()) }
        playPause.setOnClickListener { render(controller?.toggleMute()) }
    }

    fun bind(controller: PhysicalRadioController) {
        this.controller = controller
        render(controller.currentState())
        hardwareStatus.text = if (controller.isHardwareAvailable) {
            controller.platformName
        } else {
            context.getString(R.string.hardware_pending)
        }
    }

    fun setEssentialMode(enabled: Boolean) {
        presets.visibility = if (enabled) GONE else VISIBLE
        radioDetails.visibility = if (enabled) GONE else VISIBLE
        frequency.textSize = if (enabled) 82f else 68f
    }

    private fun render(state: RadioState?) {
        state ?: return
        val decimals = if (state.band == RadioBand.FM) 1 else 0
        frequency.text = String.format(Locale.US, "%.${decimals}f", state.frequency)
        unit.text = if (state.band == RadioBand.FM) "MHz" else "kHz"
        stationName.text = state.stationName
        stereo.text = if (state.isStereo) "STEREO" else "MONO"
        playPause.text = if (state.isMuted) "▶" else "Ⅱ"
        bandFm.setBackgroundResource(if (state.band == RadioBand.FM) R.drawable.bg_button_selected else R.drawable.bg_button)
        bandAm.setBackgroundResource(if (state.band == RadioBand.AM) R.drawable.bg_button_selected else R.drawable.bg_button)
        presetButtons.zip(state.presets).forEach { (button, value) ->
            button.text = String.format(Locale.US, "%.1f", value)
            button.setOnClickListener { render(controller?.tune(value)) }
        }
    }
}
