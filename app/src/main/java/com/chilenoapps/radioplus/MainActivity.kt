package com.chilenoapps.radioplus

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.chilenoapps.radioplus.databinding.ActivityMainBinding
import com.chilenoapps.radioplus.hardware.PreviewRadioController
import com.chilenoapps.radioplus.model.AppSection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var essentialMode = false
    private var nightMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.radioPanel.bind(PreviewRadioController())
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
        binding.radioPanel.visibility = if (onRadio) View.VISIBLE else View.GONE
        binding.modulePlaceholder.visibility = if (onRadio) View.GONE else View.VISIBLE
        binding.modulePlaceholder.text = "${section.symbol}\n${section.title}\nMódulo interno em desenvolvimento"

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
}
