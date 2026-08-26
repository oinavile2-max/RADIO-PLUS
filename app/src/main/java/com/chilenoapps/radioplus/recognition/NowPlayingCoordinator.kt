package com.chilenoapps.radioplus.recognition

class NowPlayingCoordinator(private val onNewTrack: (NowPlayingInfo) -> Unit) {
    private var lastIdentity: String? = null

    fun fromOnlineMetadata(metadata: String, stationName: String) {
        val clean = metadata.trim()
        if (clean.isBlank() || clean.equals(stationName, ignoreCase = true)) return
        val parts = clean.split(" - ", limit = 2)
        val info = if (parts.size == 2) {
            NowPlayingInfo(title = parts[1].trim(), artist = parts[0].trim(), sourceName = stationName, sourceType = SourceType.ONLINE_METADATA)
        } else {
            NowPlayingInfo(title = clean, sourceName = stationName, sourceType = SourceType.ONLINE_METADATA)
        }
        publish(info)
    }

    fun fromRds(title: String, artist: String, stationName: String) {
        if (title.isBlank()) return
        publish(NowPlayingInfo(title, artist, sourceName = stationName, sourceType = SourceType.PHYSICAL_RDS))
    }

    fun fromFingerprint(info: NowPlayingInfo) = publish(info.copy(sourceType = SourceType.AUDIO_FINGERPRINT))

    private fun publish(info: NowPlayingInfo) {
        if (info.identity == lastIdentity) return
        lastIdentity = info.identity
        onNewTrack(info)
    }
}
