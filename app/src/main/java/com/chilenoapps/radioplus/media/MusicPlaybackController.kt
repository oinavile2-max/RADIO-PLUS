package com.chilenoapps.radioplus.media

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.chilenoapps.radioplus.model.MusicTrack

class MusicPlaybackController(context: Context) {
    private val player = ExoPlayer.Builder(context).build()
    private var tracks: List<MusicTrack> = emptyList()

    val currentTrack: MusicTrack?
        get() = tracks.getOrNull(player.currentMediaItemIndex)
    val isPlaying: Boolean get() = player.isPlaying
    val currentPosition: Long get() = player.currentPosition.coerceAtLeast(0L)
    val duration: Long get() = player.duration.coerceAtLeast(0L)

    fun setListener(listener: Player.Listener) = player.addListener(listener)

    fun setQueue(items: List<MusicTrack>, startIndex: Int) {
        tracks = items
        player.setMediaItems(items.map { MediaItem.fromUri(it.contentUri) }, startIndex, 0L)
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun next() = player.seekToNextMediaItem()
    fun previous() = player.seekToPreviousMediaItem()
    fun seekTo(positionMs: Long) = player.seekTo(positionMs)

    fun setShuffle(enabled: Boolean) {
        player.shuffleModeEnabled = enabled
    }

    fun setRepeat(enabled: Boolean) {
        player.repeatMode = if (enabled) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
    }

    fun release() = player.release()
}
