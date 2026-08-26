package com.chilenoapps.radioplus.model

data class RadioState(
    val band: RadioBand = RadioBand.FM,
    val frequency: Double = 98.5,
    val stationName: String = "RÁDIO FM",
    val isMuted: Boolean = false,
    val isStereo: Boolean = true,
    val signalLevel: Int = 4,
    val presets: List<Double> = listOf(89.1, 92.7, 98.5, 101.3, 104.7, 107.9)
)

enum class RadioBand { FM, AM }
