package com.chilenoapps.radioplus.video

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.chilenoapps.radioplus.R
import com.chilenoapps.radioplus.ui.AccentStyler
import java.util.Locale

class VideoActivity : AppCompatActivity(), LocationListener {
    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var lockMessage: TextView
    private lateinit var status: TextView
    private lateinit var list: LinearLayout
    private val locationManager by lazy { getSystemService(LOCATION_SERVICE) as LocationManager }
    private var speedKmh: Float? = null
    private var lastSpeedAt = 0L
    private var videos: List<VideoItem> = emptyList()
    private val safetyHandler = Handler(Looper.getMainLooper())
    private val safetyTask = object : Runnable {
        override fun run() {
            if (::player.isInitialized && !isSafelyStopped()) {
                player.pause()
                if (::lockMessage.isInitialized) lockMessage.visibility = View.VISIBLE
            }
            safetyHandler.postDelayed(this, 1_000L)
        }
    }
    private val mediaPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) loadVideos() else status.text = "Permissão de vídeos não concedida" }
    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) startSafetyMonitor() else renderSafety("Localização não autorizada") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(this).build()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(14)); setBackgroundResource(R.drawable.bg_app) }
        root.addView(TextView(this).apply { text = "‹  VÍDEO"; textSize = 24f; setTextColor(getColor(R.color.rp_text)); setTypeface(typeface, 1); setOnClickListener { finish() } })
        val body = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val playerPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), dp(10), dp(10), dp(10)); setBackgroundResource(R.drawable.bg_panel) }
        playerView = PlayerView(this).apply { player = this@VideoActivity.player; useController = true }
        val videoFrame = android.widget.FrameLayout(this).apply {
            addView(playerView, android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            lockMessage = TextView(this@VideoActivity).apply { gravity = Gravity.CENTER; textSize = 18f; setTextColor(getColor(R.color.rp_text)); setBackgroundColor(0xD9080B0F.toInt()); text = "🔒\nVÍDEO BLOQUEADO\nVerifique se o veículo está parado" }
            addView(lockMessage, android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        playerPanel.addView(videoFrame, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        playerPanel.addView(Button(this).apply { text = "VERIFICAR VEÍCULO PARADO"; setOnClickListener { ensureSafetyPermission() } }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)).apply { topMargin = dp(8) })
        body.addView(playerPanel, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.25f).apply { marginEnd = dp(8) })

        val library = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(12), dp(12), dp(12)); setBackgroundResource(R.drawable.bg_panel) }
        library.addView(TextView(this).apply { text = "BIBLIOTECA • MEMÓRIA / USB / SD"; textSize = 17f; setTextColor(getColor(R.color.rp_text)); setTypeface(typeface, 1) })
        status = TextView(this).apply { text = "Carregando vídeos…"; textSize = 12f; setTextColor(getColor(R.color.rp_text_muted)); setPadding(0, dp(8), 0, dp(8)) }
        library.addView(status)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        library.addView(ScrollView(this).apply { addView(list) }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        body.addView(library, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.75f).apply { marginStart = dp(8) })
        root.addView(body, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(10) })
        setContentView(root)
        root.post { AccentStyler.apply(root) }
        ensureMediaPermission()
        ensureSafetyPermission()
        safetyHandler.post(safetyTask)
    }

    private fun ensureMediaPermission() {
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_VIDEO else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) loadVideos() else mediaPermission.launch(permission)
    }

    private fun loadVideos() {
        val collection = if (Build.VERSION.SDK_INT >= 29) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val result = mutableListOf<VideoItem>()
        contentResolver.query(collection, arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.DURATION), null, null, "${MediaStore.Video.Media.DATE_ADDED} DESC")?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID); val name = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME); val duration = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            while (cursor.moveToNext()) result += VideoItem(cursor.getLong(id), cursor.getString(name).orEmpty(), cursor.getLong(duration), ContentUris.withAppendedId(collection, cursor.getLong(id)))
        }
        videos = result
        status.text = if (result.isEmpty()) "Nenhum vídeo encontrado" else "${result.size} vídeos encontrados"
        list.removeAllViews()
        result.forEachIndexed { index, item ->
            list.addView(TextView(this).apply {
                text = "▶  ${item.name}\n     ${formatTime(item.duration)}"
                textSize = 14f; setTextColor(getColor(R.color.rp_text)); setPadding(dp(14), dp(12), dp(14), dp(12)); setBackgroundResource(R.drawable.bg_track_row)
                setOnClickListener { playVideo(index) }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(6) })
        }
    }

    private fun playVideo(index: Int) {
        if (!isSafelyStopped()) { player.pause(); renderSafety("Veículo parado não confirmado"); return }
        player.setMediaItems(videos.map { MediaItem.fromUri(it.uri) }, index, 0L)
        player.prepare(); player.play(); lockMessage.visibility = View.GONE
    }

    private fun ensureSafetyPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) startSafetyMonitor() else locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun startSafetyMonitor() {
        runCatching {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let(::onLocationChanged)
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1_000L, 0f, this)
            renderSafety(if (isSafelyStopped()) "Veículo parado confirmado" else "Aguardando velocidade GPS confiável")
        }.onFailure { renderSafety("GPS indisponível para trava de movimento") }
    }

    override fun onLocationChanged(location: Location) {
        speedKmh = if (location.hasSpeed()) location.speed * 3.6f else null
        lastSpeedAt = location.time
        if (!isSafelyStopped()) { player.pause(); lockMessage.visibility = View.VISIBLE }
        renderSafety(if (isSafelyStopped()) "VEÍCULO PARADO • VÍDEO LIBERADO" else speedKmh?.let { "VÍDEO BLOQUEADO • ${"%.1f".format(Locale.US, it)} km/h" } ?: "Aguardando velocidade GPS")
    }
    private fun isSafelyStopped(): Boolean {
        val obd = getSharedPreferences("obd_live", MODE_PRIVATE)
        val obdAt = obd.getLong("updated_at", 0L)
        val obdSpeed = obd.getInt("speed_kmh", -1)
        if (obdSpeed >= 0 && System.currentTimeMillis() - obdAt < 2_500L) return obdSpeed <= 3
        return speedKmh != null && speedKmh!! <= 3f && System.currentTimeMillis() - lastSpeedAt < 10_000L
    }
    private fun renderSafety(message: String) { if (::lockMessage.isInitialized) { lockMessage.text = "🔒\n$message"; lockMessage.visibility = if (isSafelyStopped()) View.GONE else View.VISIBLE }; if (::status.isInitialized) status.text = message }
    private fun formatTime(ms: Long) = String.format(Locale.getDefault(), "%d:%02d", ms / 60_000, (ms / 1_000) % 60)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    override fun onDestroy() { safetyHandler.removeCallbacksAndMessages(null); runCatching { locationManager.removeUpdates(this) }; player.release(); super.onDestroy() }
    private data class VideoItem(val id: Long, val name: String, val duration: Long, val uri: android.net.Uri)
}
