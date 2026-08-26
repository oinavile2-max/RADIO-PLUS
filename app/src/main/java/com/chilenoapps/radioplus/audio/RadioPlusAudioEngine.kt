package com.chilenoapps.radioplus.audio

/**
 * Fronteira do novo motor de áudio. A implementação Media3 é a rota compatível;
 * a implementação NDK/Oboe será responsável por DSP 64-bit e codecs avançados.
 */
interface RadioPlusAudioEngine {
    val engineName: String
    val supportsNativeHiRes: Boolean
    val supportedFormats: Set<String>

    fun configure(config: AudioEngineConfig)
    fun selectOutput(outputId: String)
    fun applyAutoEq(presetId: String?)
}
