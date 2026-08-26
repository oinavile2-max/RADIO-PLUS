package com.chilenoapps.radioplus.online

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.chilenoapps.radioplus.model.OnlineStation

class OnlineRadioPlaybackController(context: Context) {
    private val player = ExoPlayer.Builder(context).build()
    var currentStation: OnlineStation? = null
        private set
    val isPlaying: Boolean get() = player.isPlaying

    fun addListener(listener: Listener) {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) = listener.onPlaybackChanged(isPlaying)
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                listener.onMetadataChanged(mediaMetadata.title?.toString().orEmpty())
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                listener.onError("Não foi possível reproduzir esta estação")
            }
        })
    }

    fun play(station: OnlineStation) {
        currentStation = station
        player.setMediaItem(MediaItem.fromUri(station.streamUrl))
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun stop() = player.stop()
    fun release() = player.release()

    interface Listener {
        fun onPlaybackChanged(isPlaying: Boolean)
        fun onMetadataChanged(text: String)
        fun onError(message: String)
    }
}
