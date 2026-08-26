package com.chilenoapps.radioplus.recognition

/**
 * Provedores de fingerprint recebem uma amostra curta. Tokens e segredos nunca
 * devem ser incluídos no APK ou no repositório público; a produção usará backend.
 */
interface MusicRecognitionProvider {
    val providerName: String
    suspend fun recognize(sample: AudioSample): RecognitionResult
}

data class AudioSample(
    val bytes: ByteArray,
    val mimeType: String,
    val durationMs: Long,
    val sourceName: String
)

sealed class RecognitionResult {
    data class Match(val info: NowPlayingInfo) : RecognitionResult()
    data object NoMatch : RecognitionResult()
    data class Error(val message: String) : RecognitionResult()
}
