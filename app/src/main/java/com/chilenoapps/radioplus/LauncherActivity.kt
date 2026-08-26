package com.chilenoapps.radioplus

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.chilenoapps.radioplus.databinding.ActivityLauncherBinding
import com.chilenoapps.radioplus.model.AppSection
import com.chilenoapps.radioplus.settings.SettingsActivity
import com.chilenoapps.radioplus.ui.AccentStyler
import com.chilenoapps.radioplus.maps.MapsActivity
import com.chilenoapps.radioplus.video.VideoActivity
import com.chilenoapps.radioplus.phone.PhoneActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LauncherActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLauncherBinding
    private val handler = Handler(Looper.getMainLooper())
    private val clockTask = object : Runnable {
        override fun run() {
            updateLiveState()
            handler.postDelayed(this, 30_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.launcherRadio.setOnClickListener { openSection(AppSection.RADIO) }
        binding.launcherRadioCard.setOnClickListener { openSection(AppSection.RADIO) }
        binding.launcherMusic.setOnClickListener { openSection(AppSection.MUSIC) }
        binding.launcherVideo.setOnClickListener { startActivity(Intent(this, VideoActivity::class.java)) }
        binding.launcherMediaCard.setOnClickListener { openSection(AppSection.MUSIC) }
        binding.launcherOnline.setOnClickListener { openSection(AppSection.ONLINE) }
        binding.launcherMaps.setOnClickListener { startActivity(Intent(this, MapsActivity::class.java)) }
        binding.launcherPhone.setOnClickListener { startActivity(Intent(this, PhoneActivity::class.java)) }
        binding.launcherObd.setOnClickListener { openSection(AppSection.OBD) }
        binding.launcherVehicleCard.setOnClickListener { openSection(AppSection.OBD) }
        binding.launcherSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        binding.launcherApps.setOnClickListener { showInstalledApps() }
        binding.launcherEssential.setOnClickListener {
            val hidden = binding.launcherDashboard.visibility == View.GONE
            binding.launcherDashboard.visibility = if (hidden) View.VISIBLE else View.GONE
            binding.launcherEssential.text = if (hidden) "◐  MODO ESSENCIAL" else "◐  MOSTRAR PAINEL"
        }
        clockTask.run()
    }

    override fun onResume() {
        super.onResume()
        updateLiveState()
        AccentStyler.apply(binding.launcherRoot)
    }

    private fun openSection(section: AppSection) {
        startActivity(Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_SECTION, section.name))
    }

    private fun updateLiveState() {
        val now = Date()
        binding.launcherClock.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        binding.launcherDate.text = SimpleDateFormat("EEEE, dd 'DE' MMMM", Locale("pt", "BR")).format(now).uppercase(Locale("pt", "BR"))
        val bluetooth = runCatching { BluetoothAdapter.getDefaultAdapter()?.isEnabled == true }.getOrDefault(false)
        val gps = runCatching { (getSystemService(Context.LOCATION_SERVICE) as LocationManager).isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)
        val network = runCatching {
            @Suppress("DEPRECATION")
            (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo?.isConnected == true
        }.getOrDefault(false)
        binding.launcherSystemStatus.text = "BT ${if (bluetooth) "ATIVO" else "—"}  •  GPS ${if (gps) "ATIVO" else "—"}  •  REDE ${if (network) "ATIVA" else "—"}"
        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        binding.launcherMediaStatus.text = if (audio.isMusicActive) "ÁUDIO EM REPRODUÇÃO" else "NENHUM ÁUDIO ATIVO"
        binding.launcherObdStatus.text = "TOQUE PARA ABRIR E CONECTAR"
    }

    private fun showInstalledApps() {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val apps = packageManager.queryIntentActivities(launcherIntent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase(Locale.getDefault()) }
        if (apps.isEmpty()) {
            AlertDialog.Builder(this).setMessage("Nenhum aplicativo inicializável encontrado.").setPositiveButton("FECHAR", null).show()
            return
        }
        val labels = apps.map { it.loadLabel(packageManager).toString() }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Aplicativos").setItems(labels) { dialog, index ->
            packageManager.getLaunchIntentForPackage(apps[index].activityInfo.packageName)?.let(::startActivity)
            dialog.dismiss()
        }.setNegativeButton("FECHAR", null).show()
    }

    override fun onBackPressed() {
        // Em modo HOME, Voltar não deve fechar o Launcher e revelar uma tela vazia.
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
