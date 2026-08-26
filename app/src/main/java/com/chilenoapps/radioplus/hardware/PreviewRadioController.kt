package com.chilenoapps.radioplus.hardware

import com.chilenoapps.radioplus.model.RadioBand
import com.chilenoapps.radioplus.model.RadioState

/**
 * Controlador seguro para desenvolvimento da interface.
 * Não declara compatibilidade com uma MCU e não transmite comandos reais.
 */
class PreviewRadioController : PhysicalRadioController {
    override val platformName = "Hardware não identificado"
    override val isHardwareAvailable = false

    private var state = RadioState()

    override fun currentState() = state

    override fun setBand(band: RadioBand): RadioState {
        state = if (band == RadioBand.FM) {
            state.copy(band = band, frequency = 98.5, stationName = "RÁDIO FM")
        } else {
            state.copy(band = band, frequency = 810.0, stationName = "RÁDIO AM")
        }
        return state
    }

    override fun tune(frequency: Double): RadioState {
        state = state.copy(frequency = frequency, stationName = "ESTAÇÃO")
        return state
    }

    override fun seekUp() = step(if (state.band == RadioBand.FM) 0.2 else 10.0)
    override fun seekDown() = step(if (state.band == RadioBand.FM) -0.2 else -10.0)

    override fun toggleMute(): RadioState {
        state = state.copy(isMuted = !state.isMuted)
        return state
    }

    private fun step(delta: Double): RadioState {
        val range = if (state.band == RadioBand.FM) 87.5..108.0 else 520.0..1710.0
        val next = (state.frequency + delta).coerceIn(range)
        state = state.copy(frequency = next, stationName = "BUSCANDO…")
        return state
    }
}
