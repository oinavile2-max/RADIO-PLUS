package com.chilenoapps.radioplus.hardware

import com.chilenoapps.radioplus.model.RadioBand
import com.chilenoapps.radioplus.model.RadioState

/**
 * Contrato entre a interface do RADIO+ e o rádio físico da central.
 * Implementações reais serão selecionadas por plataforma/MCU.
 */
interface PhysicalRadioController {
    val platformName: String
    val isHardwareAvailable: Boolean

    fun currentState(): RadioState
    fun setBand(band: RadioBand): RadioState
    fun tune(frequency: Double): RadioState
    fun seekUp(): RadioState
    fun seekDown(): RadioState
    fun toggleMute(): RadioState
}
