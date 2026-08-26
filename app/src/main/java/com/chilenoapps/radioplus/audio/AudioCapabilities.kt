package com.chilenoapps.radioplus.audio

data class AudioCapabilities(
    val nativeSampleRate: Int,
    val framesPerBuffer: Int,
    val lowLatency: Boolean,
    val professionalAudio: Boolean,
    val aAudioAvailable: Boolean,
    val outputs: List<AudioOutputCapability>,
    val baselineFormats: Set<String>,
    val nativeCodecFormats: Set<String>
)

data class AudioOutputCapability(
    val id: Int,
    val name: String,
    val type: Int,
    val sampleRates: List<Int>,
    val channelCounts: List<Int>
)
