package com.chilenoapps.radioplus.recognition

data class NowPlayingInfo(
    val title: String,
    val artist: String = "",
    val album: String = "",
    val artworkUrl: String = "",
    val sourceName: String,
    val sourceType: SourceType,
    val confidence: Float? = null,
    val detectedAt: Long = System.currentTimeMillis()
) {
    val identity: String get() = "${title.trim().lowercase()}|${artist.trim().lowercase()}"
}

enum class SourceType { ONLINE_METADATA, PHYSICAL_RDS, AUDIO_FINGERPRINT }
