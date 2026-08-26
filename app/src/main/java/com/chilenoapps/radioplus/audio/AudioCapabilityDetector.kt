package com.chilenoapps.radioplus.audio

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

class AudioCapabilityDetector(private val context: Context) {
    fun detect(): AudioCapabilities {
        val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val packageManager = context.packageManager
        val outputs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).map { it.toCapability() }
        } else emptyList()

        return AudioCapabilities(
            nativeSampleRate = manager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull() ?: 48_000,
            framesPerBuffer = manager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)?.toIntOrNull() ?: 256,
            lowLatency = packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_LOW_LATENCY),
            professionalAudio = packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_PRO),
            aAudioAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O,
            outputs = outputs,
            baselineFormats = setOf("MP3", "AAC", "M4A", "FLAC", "WAV", "OGG", "OPUS", "MKA"),
            nativeCodecFormats = setOf("TAK", "DSD")
        )
    }

    private fun AudioDeviceInfo.toCapability() = AudioOutputCapability(
        id = id,
        name = productName?.toString().orEmpty().ifBlank { "Saída $id" },
        type = type,
        sampleRates = sampleRates.toList(),
        channelCounts = channelCounts.toList()
    )
}
