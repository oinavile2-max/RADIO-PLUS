package com.chilenoapps.radioplus.obd

data class ObdDevice(val name: String, val address: String)

data class ObdLiveData(
    val rpm: Int? = null,
    val speedKmh: Int? = null,
    val coolantCelsius: Int? = null,
    val intakeCelsius: Int? = null,
    val engineLoadPercent: Int? = null,
    val throttlePercent: Int? = null,
    val voltage: Double? = null
)

sealed class ObdConnectionState {
    object Disconnected : ObdConnectionState()
    data class Connecting(val device: ObdDevice) : ObdConnectionState()
    data class Connected(val device: ObdDevice, val adapter: String, val protocol: String) : ObdConnectionState()
    data class Error(val message: String) : ObdConnectionState()
}
