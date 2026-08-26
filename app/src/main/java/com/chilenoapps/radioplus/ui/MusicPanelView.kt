package com.chilenoapps.radioplus.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.graphics.Bitmap
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
    private val albumBackground: ImageView
    private val albumBackgroundScrim: View
    private val albumContainer: View
    private val albumModeIndicator: TextView
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
    private val queueButton: Button
    private val equalizerButton: Button
    private val favoriteButton: Button
    private val musicVolume: SeekBar
    private val dayNightStatus: TextView
    private val sourceMemory: Button
    private val sourceUsb: Button
    private val sourceSd: Button
    private var allTracks: List<MusicTrack> = emptyList()
    private var visibleTracks: List<MusicTrack> = emptyList()
    private var playback: MusicPlaybackController? = null
    private var shuffle = false
    private var repeat = false
    private var selectedSource: String? = null
    private var currentArt: Bitmap? = null
    private val preferences = context.getSharedPreferences("music_player", Context.MODE_PRIVATE)
    private var expandedAlbumArt = preferences.getBoolean("expanded_album_art", false)
    private val favoriteIds = preferences.getStringSet("favorite_track_ids", emptySet()).orEmpty().toMutableSet()
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
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
        albumBackground = findViewById(R.id.albumBackground)
        albumBackgroundScrim = findViewById(R.id.albumBackgroundScrim)
        albumContainer = findViewById(R.id.albumContainer)
        albumModeIndicator = findViewById(R.id.albumModeIndicator)
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
        queueButton = findViewById(R.id.queue)
        equalizerButton = findViewById(R.id.equalizer)
        favoriteButton = findViewById(R.id.favoriteTrack)
        musicVolume = findViewById(R.id.musicVolume)
        dayNightStatus = findViewById(R.id.dayNightStatus)
        sourceMemory = findViewById(R.id.sourceMemory)
        sourceUsb = findViewById(R.id.sourceUsb)
        sourceSd = findViewById(R.id.sourceSd)
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
        albumContainer.setOnClickListener {
            expandedAlbumArt = !expandedAlbumArt
            preferences.edit().putBoolean("expanded_album_art", expandedAlbumArt).apply()
            applyAlbumMode()
        }
        favoriteButton.setOnClickListener { toggleFavorite() }
        queueButton.setOnClickListener { showQueue() }
        equalizerButton.setOnClickListener { openSystemEqualizer() }
        sourceMemory.setOnClickListener { selectSource("MEMÓRIA") }
        sourceUsb.setOnClickListener { selectSource("USB") }
        sourceSd.setOnClickListener { selectSource("CARTÃO SD") }
        musicVolume.max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        musicVolume.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        musicVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                if (fromUser) audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
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
        visibleTracks = allTracks.filter {
            (selectedSource == null || it.sourceLabel == selectedSource) &&
                (query.isBlank() || it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true))
        }
        status.text = if (visibleTracks.isEmpty()) "Nenhuma música encontrada neste filtro" else "${visibleTracks.size} músicas exibidas"
        renderTrackList()
    }

    private fun selectSource(source: String) {
        selectedSource = if (selectedSource == source) null else source
        listOf(sourceMemory to "MEMÓRIA", sourceUsb to "USB", sourceSd to "CARTÃO SD").forEach { (button, value) ->
            button.setBackgroundResource(if (selectedSource == value) R.drawable.bg_button_selected else R.drawable.bg_button)
        }
        filter(search.text?.toString().orEmpty())
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
        favoriteButton.text = if (track != null && favoriteIds.contains(track.id.toString())) "★" else "☆"
        favoriteButton.isEnabled = track != null
        playPause.text = if (playback?.isPlaying == true) "Ⅱ" else "▶"
        if (track?.id != artTrackId) {
            artTrackId = track?.id
            currentArt = null
            albumArtView.setImageDrawable(null)
            albumBackground.setImageDrawable(null)
            albumPlaceholder.visibility = View.VISIBLE
            applyAlbumMode()
            if (track != null) albumArt.load(track) { bitmap ->
                if (artTrackId == track.id && bitmap != null) {
                    currentArt = bitmap
                    albumArtView.setImageBitmap(bitmap)
                    albumPlaceholder.visibility = View.GONE
                    applyAlbumMode()
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
        musicVolume.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        updateDayNightAppearance()
    }

    private fun toggleFavorite() {
        val track = playback?.currentTrack ?: return
        val key = track.id.toString()
        if (!favoriteIds.add(key)) favoriteIds.remove(key)
        preferences.edit().putStringSet("favorite_track_ids", favoriteIds.toSet()).apply()
        favoriteButton.text = if (favoriteIds.contains(key)) "★" else "☆"
        status.text = if (favoriteIds.contains(key)) "Adicionada aos favoritos" else "Removida dos favoritos"
    }

    private fun showQueue() {
        val controller = playback ?: return
        if (controller.queue.isEmpty()) {
            status.text = "A fila está vazia"
            return
        }
        val labels = controller.queue.mapIndexed { index, track ->
            "${if (index == controller.currentIndex) "▶ " else ""}${track.title} — ${track.artist}"
        }.toTypedArray()
        AlertDialog.Builder(context)
            .setTitle("Fila de reprodução")
            .setItems(labels) { dialog, index ->
                controller.playQueueIndex(index)
                dialog.dismiss()
                refreshPlayer()
            }
            .setNegativeButton("FECHAR", null)
            .show()
    }

    private fun openSystemEqualizer() {
        val session = playback?.audioSessionId ?: 0
        if (session <= 0) {
            status.text = "Inicie uma música para abrir o equalizador"
            return
        }
        val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, session)
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { status.text = "Esta central não fornece um painel de equalizador compatível" }
    }

    private fun applyAlbumMode() {
        val hasArt = currentArt != null
        albumModeIndicator.text = if (expandedAlbumArt) "↙" else "↗"
        albumBackground.visibility = if (expandedAlbumArt && hasArt) View.VISIBLE else View.GONE
        albumBackgroundScrim.visibility = if (expandedAlbumArt && hasArt) View.VISIBLE else View.GONE
        if (expandedAlbumArt && hasArt) {
            val art = currentArt ?: return
            val small = Bitmap.createScaledBitmap(art, 72, 72, true)
            albumBackground.setImageBitmap(small)
        } else {
            albumBackground.setImageDrawable(null)
        }
    }

    private fun updateDayNightAppearance() {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val isNight = hour < 6 || hour >= 18
        dayNightStatus.text = if (isNight) "AUTO • NOITE" else "AUTO • DIA"
        albumBackground.alpha = if (isNight) 0.24f else 0.42f
        albumBackgroundScrim.alpha = if (isNight) 0.90f else 0.72f
        dayNightStatus.alpha = if (isNight) 0.72f else 1f
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
