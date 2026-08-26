package com.chilenoapps.radioplus.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chilenoapps.radioplus.R
import com.chilenoapps.radioplus.model.OnlineStation
import com.chilenoapps.radioplus.model.StationSearchMode
import com.chilenoapps.radioplus.online.OnlineRadioPlaybackController
import com.chilenoapps.radioplus.online.OnlineRadioStore
import com.chilenoapps.radioplus.online.RemoteImageLoader

class OnlineRadioPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs), OnlineRadioPlaybackController.Listener {

    private val search: EditText
    private val searchButton: Button
    private val filterName: Button
    private val filterGenre: Button
    private val filterCountry: Button
    private val filterState: Button
    private val showFavorites: Button
    private val showHistory: Button
    private val resultStatus: TextView
    private val stationList: LinearLayout
    private val stationLogo: ImageView
    private val stationBackground: ImageView
    private val backgroundScrim: View
    private val logoContainer: FrameLayout
    private val coverModeIndicator: TextView
    private val logoPlaceholder: TextView
    private val stationName: TextView
    private val stationDetails: TextView
    private val nowPlaying: TextView
    private val connectionStatus: TextView
    private val playPause: Button
    private val favorite: Button
    private val store = OnlineRadioStore(context)
    private val imageLoader = RemoteImageLoader()
    private var playback: OnlineRadioPlaybackController? = null
    private var searchMode = StationSearchMode.NAME
    private var searchListener: ((String, StationSearchMode) -> Unit)? = null
    private var playListener: ((OnlineStation) -> Unit)? = null
    private var metadataListener: ((String, String) -> Unit)? = null
    private var logoRequestUuid: String? = null
    private var currentLogo: Bitmap? = null
    private var coverFillEnabled = context.getSharedPreferences("online_radio_ui", Context.MODE_PRIVATE)
        .getBoolean("cover_fill_enabled", false)

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_online_radio_panel, this, true)
        search = findViewById(R.id.search)
        searchButton = findViewById(R.id.searchButton)
        filterName = findViewById(R.id.filterName)
        filterGenre = findViewById(R.id.filterGenre)
        filterCountry = findViewById(R.id.filterCountry)
        filterState = findViewById(R.id.filterState)
        showFavorites = findViewById(R.id.showFavorites)
        showHistory = findViewById(R.id.showHistory)
        resultStatus = findViewById(R.id.resultStatus)
        stationList = findViewById(R.id.stationList)
        stationLogo = findViewById(R.id.stationLogo)
        stationBackground = findViewById(R.id.stationBackground)
        backgroundScrim = findViewById(R.id.backgroundScrim)
        logoContainer = findViewById(R.id.logoContainer)
        coverModeIndicator = findViewById(R.id.coverModeIndicator)
        logoPlaceholder = findViewById(R.id.logoPlaceholder)
        stationName = findViewById(R.id.stationName)
        stationDetails = findViewById(R.id.stationDetails)
        nowPlaying = findViewById(R.id.nowPlaying)
        connectionStatus = findViewById(R.id.connectionStatus)
        playPause = findViewById(R.id.playPause)
        favorite = findViewById(R.id.favorite)
        searchButton.setOnClickListener { searchListener?.invoke(search.text.toString(), searchMode) }
        filterName.setOnClickListener { selectMode(StationSearchMode.NAME) }
        filterGenre.setOnClickListener { selectMode(StationSearchMode.GENRE) }
        filterCountry.setOnClickListener { selectMode(StationSearchMode.COUNTRY) }
        filterState.setOnClickListener { selectMode(StationSearchMode.STATE) }
        playPause.setOnClickListener { playback?.togglePlayPause() }
        favorite.setOnClickListener {
            playback?.currentStation?.let { updateFavorite(store.toggleFavorite(it)) }
        }
        showFavorites.setOnClickListener { submitStations(store.favorites(), "FAVORITOS") }
        showHistory.setOnClickListener { submitStations(store.history(), "HISTÓRICO") }
        logoContainer.setOnClickListener {
            if (currentLogo != null) {
                coverFillEnabled = !coverFillEnabled
                context.getSharedPreferences("online_radio_ui", Context.MODE_PRIVATE)
                    .edit().putBoolean("cover_fill_enabled", coverFillEnabled).apply()
                renderCoverMode()
            }
        }
        selectMode(StationSearchMode.NAME)
        renderCoverMode()
    }

    fun bind(
        controller: OnlineRadioPlaybackController,
        onSearch: (String, StationSearchMode) -> Unit,
        onPlay: (OnlineStation) -> Unit,
        onMetadata: (String, String) -> Unit
    ) {
        playback = controller
        searchListener = onSearch
        playListener = onPlay
        metadataListener = onMetadata
        controller.addListener(this)
    }

    fun showLoading() {
        resultStatus.text = "Buscando estações…"
    }

    fun submitStations(stations: List<OnlineStation>, label: String = "ESTAÇÕES") {
        resultStatus.text = "$label  •  ${stations.size} resultados"
        stationList.removeAllViews()
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
            stationList.addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { bottomMargin = 6 })
        }
    }

    fun showError(message: String) {
        resultStatus.text = message
    }

    private fun showStation(station: OnlineStation) {
        stationName.text = station.name
        stationDetails.text = listOf(station.country, station.state, station.codec, station.bitrateKbps.takeIf { it > 0 }?.let { "$it kbps" }).filterNotNull().filter(String::isNotBlank).joinToString("  •  ")
        nowPlaying.text = station.tags.take(3).joinToString("  •  ").ifBlank { "Conectando…" }
        updateFavorite(store.isFavorite(station.uuid))
        logoRequestUuid = station.uuid
        currentLogo = null
        stationLogo.setImageDrawable(null)
        stationBackground.setImageDrawable(null)
        logoPlaceholder.visibility = View.VISIBLE
        imageLoader.load(station.faviconUrl) { bitmap ->
            if (logoRequestUuid == station.uuid && bitmap != null) {
                currentLogo = bitmap
                stationLogo.setImageBitmap(bitmap)
                logoPlaceholder.visibility = View.GONE
                renderCoverMode()
            }
        }
    }

    private fun renderCoverMode() {
        val logo = currentLogo
        val showFill = coverFillEnabled && logo != null
        stationBackground.visibility = if (showFill) View.VISIBLE else View.GONE
        backgroundScrim.visibility = if (showFill) View.VISIBLE else View.GONE
        coverModeIndicator.text = if (showFill) "↙" else "↗"
        if (showFill && logo != null) {
            stationBackground.setImageBitmap(createSoftBackground(logo))
            logoContainer.layoutParams = logoContainer.layoutParams.apply {
                width = dp(72)
                height = dp(72)
            }
        } else {
            logoContainer.layoutParams = logoContainer.layoutParams.apply {
                width = dp(132)
                height = dp(132)
            }
        }
        logoContainer.requestLayout()
    }

    /** Desfoque visual compatível com Android 7, sem RenderEffect (API 31). */
    private fun createSoftBackground(source: Bitmap): Bitmap {
        val smallWidth = (source.width / 14).coerceAtLeast(12)
        val smallHeight = (source.height / 14).coerceAtLeast(12)
        val reduced = Bitmap.createScaledBitmap(source, smallWidth, smallHeight, true)
        return Bitmap.createScaledBitmap(reduced, dp(310), dp(420), true)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun selectMode(mode: StationSearchMode) {
        searchMode = mode
        mapOf(
            filterName to StationSearchMode.NAME,
            filterGenre to StationSearchMode.GENRE,
            filterCountry to StationSearchMode.COUNTRY,
            filterState to StationSearchMode.STATE
        ).forEach { (button, value) -> button.setBackgroundResource(if (value == mode) R.drawable.bg_button_selected else R.drawable.bg_button) }
    }

    private fun updateFavorite(enabled: Boolean) {
        favorite.text = if (enabled) "★ FAVORITA" else "☆ FAVORITAR"
        favorite.setBackgroundResource(if (enabled) R.drawable.bg_button_selected else R.drawable.bg_button)
    }

    override fun onPlaybackChanged(isPlaying: Boolean) {
        playPause.text = if (isPlaying) "Ⅱ" else "▶"
        connectionStatus.text = if (isPlaying) "AO VIVO" else "PAUSADO"
    }

    override fun onMetadataChanged(text: String) {
        if (text.isNotBlank()) {
            nowPlaying.text = text
            metadataListener?.invoke(text, playback?.currentStation?.name.orEmpty())
        }
    }

    override fun onError(message: String) {
        connectionStatus.text = "INDISPONÍVEL"
        showError(message)
    }

    fun release() = imageLoader.close()
}
