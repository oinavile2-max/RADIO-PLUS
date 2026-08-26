package com.chilenoapps.radioplus.audio

import android.content.Context

class AudioEngineSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("audio_engine", Context.MODE_PRIVATE)

    fun load(): AudioEngineConfig = AudioEngineConfig(
        processingPrecision = enumValueOrDefault("precision", ProcessingPrecision.FLOAT_64),
        resampler = enumValueOrDefault("resampler", ResamplerMode.AUTO),
        dither = enumValueOrDefault("dither", DitherMode.AUTO),
        volumeSteps = enumValueOrDefault("volume_steps", VolumeSteps.STEPS_50),
        gapless = preferences.getBoolean("gapless", true),
        smoothing = preferences.getBoolean("smoothing", true),
        equalizerEnabled = preferences.getBoolean("equalizer", false),
        stereoExpansion = preferences.getFloat("stereo_expansion", 0f),
        bass = preferences.getFloat("bass", 0f),
        treble = preferences.getFloat("treble", 0f),
        reverb = preferences.getFloat("reverb", 0f),
        tempo = preferences.getFloat("tempo", 1f),
        autoEqPresetId = preferences.getString("autoeq", null)
    )

    fun save(config: AudioEngineConfig) {
        preferences.edit()
            .putString("precision", config.processingPrecision.name)
            .putString("resampler", config.resampler.name)
            .putString("dither", config.dither.name)
            .putString("volume_steps", config.volumeSteps.name)
            .putBoolean("gapless", config.gapless)
            .putBoolean("smoothing", config.smoothing)
            .putBoolean("equalizer", config.equalizerEnabled)
            .putFloat("stereo_expansion", config.stereoExpansion)
            .putFloat("bass", config.bass)
            .putFloat("treble", config.treble)
            .putFloat("reverb", config.reverb)
            .putFloat("tempo", config.tempo)
            .putString("autoeq", config.autoEqPresetId)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(key: String, fallback: T): T =
        runCatching { enumValueOf<T>(preferences.getString(key, fallback.name) ?: fallback.name) }.getOrDefault(fallback)
}
