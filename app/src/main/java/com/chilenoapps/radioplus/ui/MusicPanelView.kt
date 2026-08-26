package com.chilenoapps.radioplus.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.media3.common.Player
import com.chilenoapps.radioplus.R
import com.chilenoapps.radioplus.media.AlbumArtRepository
import com.chilenoapps.radioplus.media.MusicPlaybackController
import com.chilenoapps.radioplus.lyrics.LyricsRepository
import com.chilenoapps.radioplus.model.MusicTrack
import java.util.Locale

class MusicPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val search: EditText
    private val status: TextView
    private val permissionButton: Button
    private val trackList: LinearLayout
    private val albumArtView: ImageView
    private val albumPlaceholder: TextView
    private val nowTitle: TextView
    private val nowArtist: TextView
    private val progress: SeekBar
    private val elapsed: TextView
    private val total: TextView
    private val previous: Button
    private val playPause: Button
    private val next: Button
    private val shuffleButton: Button
    private val repeatButton: Button
    private val lyricsButton: Button
    private var allTracks: List<MusicTrack> = emptyList()
    private var visibleTracks: List<MusicTrack> = emptyList()
    private var playback: MusicPlaybackController? = null
    private var shuffle = false
    private var repeat = false
    private val albumArt = AlbumArtRepository(context)
    private val lyricsRepository = LyricsRepository(context)
    private val lyricsPopup = LyricsPopupController(context)
    private val progressHandler = Handler(Looper.getMainLooper())
    private var artTrackId: Long? = null
    private val progressTicker = object : Runnable {
        override fun run() {
            refreshProgress()
            progressHandler.postDelayed(this, 1_000L)
        }
    }

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_music_panel, this, true)
        search = findViewById(R.id.search)
        status = findViewById(R.id.status)
        permissionButton = findViewById(R.id.permissionButton)
        trackList = findViewById(R.id.trackList)
        albumArtView = findViewById(R.id.albumArt)
        albumPlaceholder = findViewById(R.id.albumPlaceholder)
        nowTitle = findViewById(R.id.nowTitle)
        nowArtist = findViewById(R.id.nowArtist)
        progress = findViewById(R.id.progress)
        elapsed = findViewById(R.id.elapsed)
        total = findViewById(R.id.total)
        previous = findViewById(R.id.previous)
        playPause = findViewById(R.id.playPause)
        next = findViewById(R.id.next)
        shuffleButton = findViewById(R.id.shuffle)
        repeatButton = findViewById(R.id.repeat)
        lyricsButton = findViewById(R.id.lyrics)
        search.doAfterTextChanged { filter(it?.toString().orEmpty()) }
        playPause.setOnClickListener { playback?.togglePlayPause(); refreshPlayer() }
        next.setOnClickListener { playback?.next(); refreshPlayer() }
        previous.setOnClickListener { playback?.previous(); refreshPlayer() }
        shuffleButton.setOnClickListener {
            shuffle = !shuffle
            playback?.setShuffle(shuffle)
            shuffleButton.isSelected = shuffle
            shuffleButton.setBackgroundResource(if (shuffle) R.drawable.bg_button_selected else R.drawable.bg_button)
        }
        repeatButton.setOnClickListener {
            repeat = !repeat
            playback?.setRepeat(repeat)
            repeatButton.setBackgroundResource(if (repeat) R.drawable.bg_button_selected else R.drawable.bg_button)
        }
        lyricsButton.setOnClickListener {
            val track = playback?.currentTrack
            if (track == null) {
                status.text = "Selecione uma música antes de abrir a letra"
            } else {
                status.text = "Buscando letra…"
                lyricsRepository.load(track) { document ->
                    if (document == null) {
                        status.text = "Letra não encontrada"
                    } else {
                        status.text = if (document.synchronized) "Letra sincronizada disponível" else "Letra com rolagem automática"
                        lyricsPopup.show(this, document, { playback?.currentPosition ?: 0L }, { playback?.duration ?: track.durationMs })
                    }
                }
            }
        }
        progress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) elapsed.text = formatTime(progress.toLong())
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                playback?.seekTo(seekBar?.progress?.toLong() ?: 0L)
            }
        })
    }

    fun bind(controller: MusicPlaybackController) {
        playback = controller
        controller.setListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) = refreshPlayer()
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) = refreshPlayer()
        })
        progressHandler.post(progressTicker)
    }

    fun showPermissionRequired(onRequest: () -> Unit) {
        status.text = "Permita o acesso às músicas da central"
        permissionButton.visibility = View.VISIBLE
        permissionButton.setOnClickListener { onRequest() }
    }

    fun submitTracks(tracks: List<MusicTrack>) {
        allTracks = tracks
        permissionButton.visibility = View.GONE
        status.text = if (tracks.isEmpty()) "Nenhuma música encontrada na memória, USB ou cartão SD" else "${tracks.size} músicas encontradas"
        filter(search.text?.toString().orEmpty())
    }

    private fun filter(query: String) {
        visibleTracks = if (query.isBlank()) allTracks else allTracks.filter {
            it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true)
        }
        renderTrackList()
    }

    private fun renderTrackList() {
        trackList.removeAllViews()
        visibleTracks.forEachIndexed { index, track ->
            val row = TextView(context).apply {
                text = "${track.title}\n${track.artist}  •  ${track.sourceLabel}  •  ${formatTime(track.durationMs)}"
                setTextColor(ContextCompat.getColor(context, R.color.rp_text))
                textSize = 15f
                setPadding(18, 14, 18, 14)
                setBackgroundResource(R.drawable.bg_track_row)
                setOnClickListener {
                    playback?.setQueue(visibleTracks, index)
                    refreshPlayer()
                }
            }
            val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 6
            }
            trackList.addView(row, params)
        }
    }

    private fun refreshPlayer() {
        val track = playback?.currentTrack
        nowTitle.text = track?.title ?: "Selecione uma música"
        nowArtist.text = track?.artist ?: "Biblioteca local"
        playPause.text = if (playback?.isPlaying == true) "Ⅱ" else "▶"
        if (track?.id != artTrackId) {
            artTrackId = track?.id
            albumArtView.setImageDrawable(null)
            albumPlaceholder.visibility = View.VISIBLE
            if (track != null) albumArt.load(track) { bitmap ->
                if (artTrackId == track.id && bitmap != null) {
                    albumArtView.setImageBitmap(bitmap)
                    albumPlaceholder.visibility = View.GONE
                }
            }
        }
        refreshProgress()
    }

    private fun refreshProgress() {
        val position = playback?.currentPosition ?: 0L
        val duration = playback?.duration ?: 0L
        progress.max = duration.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        progress.progress = position.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        elapsed.text = formatTime(position)
        total.text = formatTime(duration)
    }

    fun release() {
        progressHandler.removeCallbacks(progressTicker)
        albumArt.close()
        lyricsRepository.close()
        lyricsPopup.dismiss()
    }

    private fun formatTime(milliseconds: Long): String {
        val totalSeconds = milliseconds.coerceAtLeast(0L) / 1000
        return String.format(Locale.getDefault(), "%d:%02d", totalSeconds / 60, totalSeconds % 60)
    }
}
