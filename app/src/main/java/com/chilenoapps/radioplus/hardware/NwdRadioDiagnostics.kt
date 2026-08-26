package com.chilenoapps.radioplus.hardware

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat

class NwdRadioDiagnostics(private val context: Context) {
    companion object {
        const val RADIO_PACKAGE = "com.nwd.radio"
        const val RADIO_SERVICE_ACTION = "com.nwd.radio.service.ACTION_RADIO_SERVICE"
        const val REQUEST_FREQUENCY = "com.nwd.action.radiowidget_request_frequence"
        const val FREQUENCY_FROM_MCU = "com.nwd.ACTION_RADIO_FREQUENCY_FROM_MCU"
        const val RADIO_STATE = "com.nwd.action.ACTION_RADIO_STATE"
        const val SET_FREQUENCY = "com.nwd.action.ACTION_SET_RADIO_FREQUENCE"
        const val EXTRA_FREQUENCY = "extra_radio_frequence"
        const val EXTRA_BAND = "extra.radio.band_type"
    }

    data class Identification(
        val model: String,
        val hardware: String,
        val android: String,
        val originalPackageInstalled: Boolean,
        val servicePackage: String?
    )

    fun identify(): Identification {
        val installed = runCatching {
            context.packageManager.getApplicationInfo(RADIO_PACKAGE, 0)
        }.isSuccess
        val services = context.packageManager.queryIntentServices(Intent(RADIO_SERVICE_ACTION), PackageManager.MATCH_ALL)
        return Identification(
            model = Build.MODEL.orEmpty(),
            hardware = Build.HARDWARE.orEmpty(),
            android = Build.VERSION.RELEASE.orEmpty(),
            originalPackageInstalled = installed,
            servicePackage = services.firstOrNull()?.serviceInfo?.packageName
        )
    }

    fun listen(listener: (String, Map<String, String>) -> Unit): BroadcastReceiver {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent ?: return
                listener(intent.action.orEmpty(), extrasToMap(intent.extras))
            }
        }
        val filter = IntentFilter().apply {
            addAction(FREQUENCY_FROM_MCU)
            addAction(RADIO_STATE)
            addAction("com.nwd.ACTION_RADIO_FREQUENCY_FROM_APP")
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        return receiver
    }

    fun requestCurrentState() {
        context.sendBroadcast(Intent(REQUEST_FREQUENCY).setPackage(RADIO_PACKAGE))
    }

    fun tuneFm(frequencyMhz: Double) {
        val encodedFrequency = (frequencyMhz * 100).toInt()
        val command = Intent(SET_FREQUENCY)
            .putExtra(EXTRA_FREQUENCY, encodedFrequency)
            .putExtra(EXTRA_BAND, 0)
        context.sendBroadcast(command.setPackage(RADIO_PACKAGE))
    }

    private fun extrasToMap(extras: Bundle?): Map<String, String> {
        if (extras == null) return emptyMap()
        return extras.keySet().associateWith { key -> extras.get(key)?.toString().orEmpty() }
    }
}
