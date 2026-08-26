package com.chilenoapps.radioplus.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chilenoapps.radioplus.R
import com.chilenoapps.radioplus.databinding.ViewOnlineRadioPanelBinding
import com.chilenoapps.radioplus.model.OnlineStation
import com.chilenoapps.radioplus.model.StationSearchMode
import com.chilenoapps.radioplus.online.OnlineRadioPlaybackController
import com.chilenoapps.radioplus.online.OnlineRadioStore
import com.chilenoapps.radioplus.online.RemoteImageLoader

class OnlineRadioPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs), OnlineRadioPlaybackController.Listener {

    private val binding = ViewOnlineRadioPanelBinding.inflate(LayoutInflater.from(context), this)
    private val store = OnlineRadioStore(context)
    private val imageLoader = RemoteImageLoader()
    private var playback: OnlineRadioPlaybackController? = null
    private var searchMode = StationSearchMode.NAME
    private var searchListener: ((String, StationSearchMode) -> Unit)? = null
    private var playListener: ((OnlineStation) -> Unit)? = null
    private var logoRequestUuid: String? = null

    init {
        orientation = VERTICAL
        binding.searchButton.setOnClickListener { searchListener?.invoke(binding.search.text.toString(), searchMode) }
        binding.filterName.setOnClickListener { selectMode(StationSearchMode.NAME) }
        binding.filterGenre.setOnClickListener { selectMode(StationSearchMode.GENRE) }
        binding.filterCountry.setOnClickListener { selectMode(StationSearchMode.COUNTRY) }
        binding.filterState.setOnClickListener { selectMode(StationSearchMode.STATE) }
        binding.playPause.setOnClickListener { playback?.togglePlayPause() }
        binding.favorite.setOnClickListener {
            playback?.currentStation?.let { updateFavorite(store.toggleFavorite(it)) }
        }
        binding.showFavorites.setOnClickListener { submitStations(store.favorites(), "FAVORITOS") }
        binding.showHistory.setOnClickListener { submitStations(store.history(), "HISTÓRICO") }
        selectMode(StationSearchMode.NAME)
    }

    fun bind(controller: OnlineRadioPlaybackController, onSearch: (String, StationSearchMode) -> Unit, onPlay: (OnlineStation) -> Unit) {
        playback = controller
        searchListener = onSearch
        playListener = onPlay
        controller.addListener(this)
    }

    fun showLoading() {
        binding.resultStatus.text = "Buscando estações…"
    }

    fun submitStations(stations: List<OnlineStation>, label: String = "ESTAÇÕES") {
        binding.resultStatus.text = "$label  •  ${stations.size} resultados"
        binding.stationList.removeAllViews()
        stations.forEach { station ->
            val row = TextView(context).apply {
                text = buildString {
                    append(station.name)
                    append("\n")
                    append(listOf(station.state, station.country, station.codec, station.bitrateKbps.takeIf { it > 0 }?.let { "$it kbps" }).filterNotNull().filter(String::isNotBlank).joinToString("  •  "))
                }
                setTextColor(ContextCompat.getColor(context, R.color.rp_text))
                textSize = 14f
                setPadding(16, 13, 16, 13)
                setBackgroundResource(R.drawable.bg_track_row)
                setOnClickListener {
                    store.recordHistory(station)
                    playListener?.invoke(station)
                    showStation(station)
                }
            }
            binding.stationList.addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { bottomMargin = 6 })
        }
    }

    fun showError(message: String) {
        binding.resultStatus.text = message
    }

    private fun showStation(station: OnlineStation) {
        binding.stationName.text = station.name
        binding.stationDetails.text = listOf(station.country, station.state, station.codec, station.bitrateKbps.takeIf { it > 0 }?.let { "$it kbps" }).filterNotNull().filter(String::isNotBlank).joinToString("  •  ")
        binding.nowPlaying.text = station.tags.take(3).joinToString("  •  ").ifBlank { "Conectando…" }
        updateFavorite(store.isFavorite(station.uuid))
        logoRequestUuid = station.uuid
        binding.stationLogo.setImageDrawable(null)
        binding.logoPlaceholder.visibility = View.VISIBLE
        imageLoader.load(station.faviconUrl) { bitmap ->
            if (logoRequestUuid == station.uuid && bitmap != null) {
                binding.stationLogo.setImageBitmap(bitmap)
                binding.logoPlaceholder.visibility = View.GONE
            }
        }
    }

    private fun selectMode(mode: StationSearchMode) {
        searchMode = mode
        mapOf(
            binding.filterName to StationSearchMode.NAME,
            binding.filterGenre to StationSearchMode.GENRE,
            binding.filterCountry to StationSearchMode.COUNTRY,
            binding.filterState to StationSearchMode.STATE
        ).forEach { (button, value) -> button.setBackgroundResource(if (value == mode) R.drawable.bg_button_selected else R.drawable.bg_button) }
    }

    private fun updateFavorite(enabled: Boolean) {
        binding.favorite.text = if (enabled) "★ FAVORITA" else "☆ FAVORITAR"
        binding.favorite.setBackgroundResource(if (enabled) R.drawable.bg_button_selected else R.drawable.bg_button)
    }

    override fun onPlaybackChanged(isPlaying: Boolean) {
        binding.playPause.text = if (isPlaying) "Ⅱ" else "▶"
        binding.connectionStatus.text = if (isPlaying) "AO VIVO" else "PAUSADO"
    }

    override fun onMetadataChanged(text: String) {
        if (text.isNotBlank()) binding.nowPlaying.text = text
    }

    override fun onError(message: String) {
        binding.connectionStatus.text = "INDISPONÍVEL"
        showError(message)
    }

    fun release() = imageLoader.close()
}
