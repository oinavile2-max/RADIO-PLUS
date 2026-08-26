package com.chilenoapps.radioplus.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.chilenoapps.radioplus.R
import com.chilenoapps.radioplus.databinding.ViewRadioPanelBinding
import com.chilenoapps.radioplus.hardware.PhysicalRadioController
import com.chilenoapps.radioplus.model.RadioBand
import com.chilenoapps.radioplus.model.RadioState
import java.util.Locale

class RadioPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val binding = ViewRadioPanelBinding.inflate(LayoutInflater.from(context), this)
    private var controller: PhysicalRadioController? = null

    init {
        orientation = VERTICAL
        binding.bandFm.setOnClickListener { render(controller?.setBand(RadioBand.FM)) }
        binding.bandAm.setOnClickListener { render(controller?.setBand(RadioBand.AM)) }
        binding.seekDown.setOnClickListener { render(controller?.seekDown()) }
        binding.seekUp.setOnClickListener { render(controller?.seekUp()) }
        binding.playPause.setOnClickListener { render(controller?.toggleMute()) }
    }

    fun bind(controller: PhysicalRadioController) {
        this.controller = controller
        render(controller.currentState())
        binding.hardwareStatus.text = if (controller.isHardwareAvailable) {
            controller.platformName
        } else {
            context.getString(R.string.hardware_pending)
        }
    }

    fun setEssentialMode(enabled: Boolean) {
        binding.presets.visibility = if (enabled) GONE else VISIBLE
        binding.radioDetails.visibility = if (enabled) GONE else VISIBLE
        binding.frequency.textSize = if (enabled) 82f else 68f
    }

    private fun render(state: RadioState?) {
        state ?: return
        val decimals = if (state.band == RadioBand.FM) 1 else 0
        binding.frequency.text = String.format(Locale.US, "%.${decimals}f", state.frequency)
        binding.unit.text = if (state.band == RadioBand.FM) "MHz" else "kHz"
        binding.stationName.text = state.stationName
        binding.stereo.text = if (state.isStereo) "STEREO" else "MONO"
        binding.playPause.text = if (state.isMuted) "▶" else "Ⅱ"
        binding.bandFm.setBackgroundResource(if (state.band == RadioBand.FM) R.drawable.bg_button_selected else R.drawable.bg_button)
        binding.bandAm.setBackgroundResource(if (state.band == RadioBand.AM) R.drawable.bg_button_selected else R.drawable.bg_button)

        val presetButtons: List<android.widget.Button> = listOf(
            binding.preset1, binding.preset2, binding.preset3,
            binding.preset4, binding.preset5, binding.preset6
        )
        presetButtons.zip(state.presets).forEach { (button, value) ->
            button.text = String.format(Locale.US, "%.1f", value)
            button.setOnClickListener { render(controller?.tune(value)) }
        }
    }
}
