package com.chilenoapps.radioplus

import android.Manifest
import android.content.pm.PackageManager
import android.content.Context
import android.content.Intent
import android.bluetooth.BluetoothAdapter
import android.location.LocationManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.chilenoapps.radioplus.databinding.ActivityMainBinding
import com.chilenoapps.radioplus.hardware.PreviewRadioController
import com.chilenoapps.radioplus.media.LocalMusicRepository
import com.chilenoapps.radioplus.media.MusicPlaybackController
import com.chilenoapps.radioplus.model.AppSection
import com.chilenoapps.radioplus.online.OnlineRadioPlaybackController
import com.chilenoapps.radioplus.online.RadioBrowserClient
import com.chilenoapps.radioplus.obd.Elm327BluetoothClient
import com.chilenoapps.radioplus.recognition.NowPlayingCoordinator
import com.chilenoapps.radioplus.ui.NowPlayingPopupController
import com.chilenoapps.radioplus.vip.VipAccess
import com.chilenoapps.radioplus.vip.VipAccessManager
import com.chilenoapps.radioplus.settings.AppSettingsStore
import com.chilenoapps.radioplus.settings.SettingsActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var essentialMode = false
    private var nightMode = false
    private lateinit var musicPlayback: MusicPlaybackController
    private lateinit var onlinePlayback: OnlineRadioPlaybackController
    private val radioBrowserClient = RadioBrowserClient()
    private lateinit var nowPlayingPopup: NowPlayingPopupController
    private lateinit var nowPlayingCoordinator: NowPlayingCoordinator
    private val obdClient by lazy { Elm327BluetoothClient(this) }
    private val musicRepository by lazy { LocalMusicRepository(this) }
    private val vipAccessManager = VipAccessManager()
    private lateinit var settings: AppSettingsStore
    private lateinit var audioManager: AudioManager
    private val uiHandler = Handler(Looper.getMainLooper())
    private var currentSection = AppSection.RADIO
    private val clockTask = object : Runnable {
        override fun run() {
            if (::binding.isInitialized) {
                binding.clock.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                updateSystemStatus()
                uiHandler.postDelayed(this, 30_000)
            }
        }
    }
    private val collapseTask = Runnable { collapseSidePanel() }
    private val requestMusicPermission: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) loadMusicLibrary()
        else binding.musicPanel.showPermissionRequired { askForMusicPermission() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settings = AppSettingsStore(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        applySystemBarInsets()
        nowPlayingPopup = NowPlayingPopupController(this)
        nowPlayingCoordinator = NowPlayingCoordinator { info -> nowPlayingPopup.show(binding.root, info) }

        binding.radioPanel.bind(PreviewRadioController())
        musicPlayback = MusicPlaybackController(this)
        binding.musicPanel.bind(musicPlayback)
        onlinePlayback = OnlineRadioPlaybackController(this)
        binding.onlineRadioPanel.bind(
            controller = onlinePlayback,
            onSearch = { query, mode -> searchOnlineStations(query, mode) },
            onPlay = { station ->
                musicPlayback.pause()
                onlinePlayback.play(station)
                lifecycleScope.launch(Dispatchers.IO) { radioBrowserClient.registerClick(station.uuid) }
            },
            onMetadata = { metadata, station -> nowPlayingCoordinator.fromOnlineMetadata(metadata, station) }
        )
        binding.obdPanel.bind(obdClient, BuildConfig.ADMIN_MODE)
        clockTask.run()
        configureAdminVip()
        configureHomeControls()

        binding.essentialToggle.setOnClickListener {
            essentialMode = !essentialMode
            binding.radioPanel.setEssentialMode(essentialMode)
            binding.sidePanel.visibility = if (essentialMode) View.GONE else View.VISIBLE
            binding.essentialToggle.text = if (essentialMode) "MOSTRAR TUDO" else "MODO ESSENCIAL"
        }

        binding.nightToggle.setOnClickListener {
            nightMode = !nightMode
            settings.nightMode = nightMode
            binding.root.alpha = if (nightMode) 0.72f else 1f
            binding.nightToggle.text = if (nightMode) "NOTURNO ATIVO" else "MODO NOTURNO"
        }

        configureNavigation()
        val requestedSection = intent.getStringExtra(EXTRA_SECTION)
            ?.let { runCatching { AppSection.valueOf(it) }.getOrNull() }
            ?.takeIf { it == AppSection.RADIO || it == AppSection.MUSIC || it == AppSection.ONLINE || it == AppSection.OBD }
            ?: AppSection.RADIO
        selectSection(requestedSection)
    }

    override fun onResume() {
        super.onResume()
        nightMode = settings.nightMode
        binding.root.alpha = if (nightMode) 0.72f else 1f
        binding.nightToggle.text = if (nightMode) "NOTURNO ATIVO" else "☾  NOTURNO"
        renderPin()
        scheduleSidePanelCollapse()
    }

    private fun configureHomeControls() {
        binding.settingsButton.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        binding.equalizerButton.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        val frequencies = listOf(89.1, 92.7, 98.5, 101.3, 104.7, 107.9)
        val names = listOf("NOVA FM", "CLÁSSICOS", "RÁDIO FM", "MIX FM", "TRANSMÉRICA", "JB FM")
        val buttons = listOf(binding.favorite1, binding.favorite2, binding.favorite3, binding.favorite4, binding.favorite5, binding.favorite6)
        buttons.forEachIndexed { index, button ->
            button.text = "${frequencies[index]}    ${names[index]}"
            button.setOnClickListener {
                binding.radioPanel.tune(frequencies[index])
                scheduleSidePanelCollapse()
            }
        }
        binding.pinSidePanel.setOnClickListener {
            settings.sidePanelPinned = !settings.sidePanelPinned
            renderPin()
            scheduleSidePanelCollapse()
        }
        binding.showSidePanel.setOnClickListener {
            binding.sidePanel.visibility = View.VISIBLE
            binding.showSidePanel.visibility = View.GONE
            scheduleSidePanelCollapse()
        }
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        binding.volumeControl.max = max
        binding.volumeControl.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        binding.volumeValue.text = binding.volumeControl.progress.toString()
        binding.volumeControl.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.volumeValue.text = progress.toString()
                if (fromUser) audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
    }

    private fun renderPin() {
        binding.pinSidePanel.text = if (settings.sidePanelPinned) "FIXO" else "PIN"
        binding.pinSidePanel.setTextColor(ContextCompat.getColor(this, if (settings.sidePanelPinned) R.color.rp_blue else R.color.rp_text_muted))
    }

    private fun scheduleSidePanelCollapse() {
        uiHandler.removeCallbacks(collapseTask)
        val delay = settings.sidePanelDelaySeconds
        if (!settings.sidePanelPinned && delay > 0 && currentSection == AppSection.RADIO && !essentialMode) {
            uiHandler.postDelayed(collapseTask, delay * 1000L)
        }
    }

    private fun collapseSidePanel() {
        if (currentSection == AppSection.RADIO && !settings.sidePanelPinned) {
            binding.sidePanel.visibility = View.GONE
            binding.showSidePanel.visibility = View.VISIBLE
        }
    }

    private fun updateSystemStatus() {
        val bluetoothOn = runCatching { BluetoothAdapter.getDefaultAdapter()?.isEnabled == true }.getOrDefault(false)
        val gpsOn = runCatching {
            (getSystemService(Context.LOCATION_SERVICE) as LocationManager).isProviderEnabled(LocationManager.GPS_PROVIDER)
        }.getOrDefault(false)
        binding.systemStatus.text = "BT ${if (bluetoothOn) "ATIVO" else "—"}  •  GPS ${if (gpsOn) "ATIVO" else "—"}"
    }

    private fun applySystemBarInsets() {
        val initialLeft = binding.root.paddingLeft
        val initialTop = binding.root.paddingTop
        val initialRight = binding.root.paddingRight
        val initialBottom = binding.root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                initialLeft + bars.left,
                initialTop + bars.top,
                initialRight + bars.right,
                initialBottom + bars.bottom
            )
            insets
        }
    }

    private fun configureAdminVip() {
        val access = vipAccessManager.access
        binding.adminBadge.visibility = if (access is VipAccess.AdminTest) View.VISIBLE else View.GONE
        binding.adminBadge.text = "ADMIN • VIP ATIVO"
        binding.adminBadge.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Modo administrativo")
                .setMessage("VIP liberado somente para testes nesta instalação Admin. Esta autorização não representa uma compra e não existe na versão pública.")
                .setPositiveButton("ENTENDI", null)
                .show()
        }
    }

    private fun configureNavigation() {
        mapOf(
            binding.navRadio to AppSection.RADIO,
            binding.navMusic to AppSection.MUSIC,
            binding.navOnline to AppSection.ONLINE,
            binding.navObd to AppSection.OBD
        ).forEach { (view, section) ->
            view.text = "${section.symbol}  ${section.title}"
            view.setOnClickListener { selectSection(section) }
        }
    }

    private fun selectSection(section: AppSection) {
        currentSection = section
        val onRadio = section == AppSection.RADIO
        val onMusic = section == AppSection.MUSIC
        val onOnline = section == AppSection.ONLINE
        val onObd = section == AppSection.OBD
        binding.radioPanel.visibility = if (onRadio) View.VISIBLE else View.GONE
        binding.musicPanel.visibility = if (onMusic) View.VISIBLE else View.GONE
        binding.onlineRadioPanel.visibility = if (onOnline) View.VISIBLE else View.GONE
        binding.obdPanel.visibility = if (onObd) View.VISIBLE else View.GONE
        binding.sidePanel.visibility = if (onRadio && !essentialMode) View.VISIBLE else View.GONE
        binding.showSidePanel.visibility = View.GONE
        binding.audioRail.visibility = if (onRadio) View.VISIBLE else View.GONE
        binding.essentialToggle.visibility = if (onRadio) View.VISIBLE else View.GONE
        when (section) {
            AppSection.RADIO -> { musicPlayback.pause(); onlinePlayback.stop() }
            AppSection.MUSIC -> onlinePlayback.stop()
            AppSection.ONLINE -> musicPlayback.pause()
            AppSection.OBD -> { musicPlayback.pause(); onlinePlayback.stop() }
            else -> Unit
        }
        if (onMusic) ensureMusicLibraryPermission()
        if (onOnline && onlinePlayback.currentStation == null) loadPopularOnlineStations()
        scheduleSidePanelCollapse()

        mapOf(
            binding.navRadio to AppSection.RADIO,
            binding.navMusic to AppSection.MUSIC,
            binding.navOnline to AppSection.ONLINE,
            binding.navObd to AppSection.OBD
        ).forEach { (view, value) ->
            view.setBackgroundResource(if (value == section) R.drawable.bg_button_selected else R.drawable.bg_button)
        }
    }

    private fun ensureMusicLibraryPermission() {
        val permission = musicPermission()
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadMusicLibrary()
        } else {
            binding.musicPanel.showPermissionRequired { askForMusicPermission() }
        }
    }

    private fun askForMusicPermission() {
        requestMusicPermission.launch(musicPermission())
    }

    private fun musicPermission(): String = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    private fun loadMusicLibrary() {
        lifecycleScope.launch {
            val tracks = withContext(Dispatchers.IO) { musicRepository.loadTracks() }
            binding.musicPanel.submitTracks(tracks)
        }
    }

    private fun loadPopularOnlineStations() {
        binding.onlineRadioPanel.showLoading()
        lifecycleScope.launch {
            val stations = withContext(Dispatchers.IO) { radioBrowserClient.popularBrazil() }
            if (stations.isEmpty()) binding.onlineRadioPanel.showError("Não foi possível carregar as estações")
            else binding.onlineRadioPanel.submitStations(stations, "POPULARES NO BRASIL")
        }
    }

    private fun searchOnlineStations(query: String, mode: com.chilenoapps.radioplus.model.StationSearchMode) {
        if (query.isBlank()) return loadPopularOnlineStations()
        binding.onlineRadioPanel.showLoading()
        lifecycleScope.launch {
            val stations = withContext(Dispatchers.IO) { radioBrowserClient.search(query, mode) }
            if (stations.isEmpty()) binding.onlineRadioPanel.showError("Nenhuma estação encontrada")
            else binding.onlineRadioPanel.submitStations(stations)
        }
    }

    override fun onDestroy() {
        uiHandler.removeCallbacksAndMessages(null)
        nowPlayingPopup.dismiss()
        binding.musicPanel.release()
        musicPlayback.release()
        binding.onlineRadioPanel.release()
        onlinePlayback.release()
        binding.obdPanel.release()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_SECTION = "radio_plus_section"
    }
}
