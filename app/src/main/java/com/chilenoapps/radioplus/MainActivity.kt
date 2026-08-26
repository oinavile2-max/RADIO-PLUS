package com.chilenoapps.radioplus

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.chilenoapps.radioplus.databinding.ActivityMainBinding
import com.chilenoapps.radioplus.hardware.PreviewRadioController
import com.chilenoapps.radioplus.media.LocalMusicRepository
import com.chilenoapps.radioplus.media.MusicPlaybackController
import com.chilenoapps.radioplus.model.AppSection
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
    private val musicRepository by lazy { LocalMusicRepository(this) }
    private val requestMusicPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) loadMusicLibrary()
        else binding.musicPanel.showPermissionRequired { askForMusicPermission() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.radioPanel.bind(PreviewRadioController())
        musicPlayback = MusicPlaybackController(this)
        binding.musicPanel.bind(musicPlayback)
        binding.clock.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        binding.adminBadge.visibility = if (BuildConfig.ADMIN_MODE) View.VISIBLE else View.GONE

        binding.essentialToggle.setOnClickListener {
            essentialMode = !essentialMode
            binding.radioPanel.setEssentialMode(essentialMode)
            binding.sidePanel.visibility = if (essentialMode) View.GONE else View.VISIBLE
            binding.essentialToggle.text = if (essentialMode) "MOSTRAR TUDO" else "MODO ESSENCIAL"
        }

        binding.nightToggle.setOnClickListener {
            nightMode = !nightMode
            binding.root.alpha = if (nightMode) 0.72f else 1f
            binding.nightToggle.text = if (nightMode) "NOTURNO ATIVO" else "MODO NOTURNO"
        }

        configureNavigation()
        selectSection(AppSection.RADIO)
    }

    private fun configureNavigation() {
        mapOf(
            binding.navRadio to AppSection.RADIO,
            binding.navMusic to AppSection.MUSIC,
            binding.navVideo to AppSection.VIDEO,
            binding.navOnline to AppSection.ONLINE,
            binding.navMaps to AppSection.MAPS,
            binding.navPhone to AppSection.PHONE
        ).forEach { (view, section) ->
            view.text = "${section.symbol}  ${section.title}"
            view.setOnClickListener { selectSection(section) }
        }
    }

    private fun selectSection(section: AppSection) {
        binding.sectionTitle.text = section.title
        val onRadio = section == AppSection.RADIO
        val onMusic = section == AppSection.MUSIC
        binding.radioPanel.visibility = if (onRadio) View.VISIBLE else View.GONE
        binding.musicPanel.visibility = if (onMusic) View.VISIBLE else View.GONE
        binding.modulePlaceholder.visibility = if (onRadio || onMusic) View.GONE else View.VISIBLE
        binding.sidePanel.visibility = if (onRadio && !essentialMode) View.VISIBLE else View.GONE
        binding.essentialToggle.visibility = if (onRadio) View.VISIBLE else View.GONE
        binding.modulePlaceholder.text = "${section.symbol}\n${section.title}\nMódulo interno em desenvolvimento"
        if (onMusic) ensureMusicLibraryPermission()

        mapOf(
            binding.navRadio to AppSection.RADIO,
            binding.navMusic to AppSection.MUSIC,
            binding.navVideo to AppSection.VIDEO,
            binding.navOnline to AppSection.ONLINE,
            binding.navMaps to AppSection.MAPS,
            binding.navPhone to AppSection.PHONE
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

    private fun askForMusicPermission() = requestMusicPermission.launch(musicPermission())

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

    override fun onDestroy() {
        binding.musicPanel.release()
        musicPlayback.release()
        super.onDestroy()
    }
}
