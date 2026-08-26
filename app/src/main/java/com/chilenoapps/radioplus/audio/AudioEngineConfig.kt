package com.chilenoapps.radioplus.audio

data class AudioEngineConfig(
    val processingPrecision: ProcessingPrecision = ProcessingPrecision.FLOAT_64,
    val resampler: ResamplerMode = ResamplerMode.AUTO,
    val dither: DitherMode = DitherMode.AUTO,
    val volumeSteps: VolumeSteps = VolumeSteps.STEPS_50,
    val gapless: Boolean = true,
    val smoothing: Boolean = true,
    val equalizerEnabled: Boolean = false,
    val stereoExpansion: Float = 0f,
    val bass: Float = 0f,
    val treble: Float = 0f,
    val reverb: Float = 0f,
    val tempo: Float = 1f,
    val autoEqPresetId: String? = null,
    val outputProfiles: Map<String, OutputProfile> = emptyMap()
)

enum class ProcessingPrecision { FLOAT_32, FLOAT_64 }
enum class ResamplerMode { AUTO, SYSTEM, SINC_FAST, SINC_BEST }
enum class DitherMode { OFF, AUTO, TRIANGULAR, NOISE_SHAPED }
enum class VolumeSteps(val count: Int) { STEPS_30(30), STEPS_50(50), STEPS_100(100) }

data class OutputProfile(
    val outputId: String,
    val displayName: String,
    val preferredSampleRate: Int? = null,
    val preferredBitDepth: Int? = null,
    val dspPresetId: String? = null,
    val exclusiveMode: Boolean = false
)
