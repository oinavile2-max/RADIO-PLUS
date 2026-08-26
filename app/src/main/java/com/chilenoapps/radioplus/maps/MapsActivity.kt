package com.chilenoapps.radioplus.maps

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.chilenoapps.radioplus.R
import com.chilenoapps.radioplus.settings.AppSettingsStore
import com.chilenoapps.radioplus.ui.AccentStyler

class MapsActivity : AppCompatActivity() {
    private lateinit var destination: EditText
    private lateinit var accessStatus: TextView
    private val settingsStore by lazy { AppSettingsStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(16), dp(20), dp(18)); setBackgroundResource(R.drawable.bg_app) }
        root.addView(TextView(this).apply { text = "‹  MAPAS E NAVEGAÇÃO"; textSize = 24f; setTextColor(getColor(R.color.rp_text)); setTypeface(typeface, 1); setOnClickListener { finish() } })
        val content = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val searchPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16)); setBackgroundResource(R.drawable.bg_panel) }
        searchPanel.addView(TextView(this).apply { text = "DESTINO"; textSize = 17f; setTextColor(getColor(R.color.rp_text)); setTypeface(typeface, 1) })
        destination = EditText(this).apply { hint = "Pesquisar endereço ou destino"; setTextColor(getColor(R.color.rp_text)); setHintTextColor(getColor(R.color.rp_text_muted)); setSingleLine(); setBackgroundResource(R.drawable.bg_button); setPadding(dp(14), 0, dp(14), 0) }
        searchPanel.addView(destination, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)).apply { topMargin = dp(12) })
        searchPanel.addView(Button(this).apply { text = "ABRIR ROTA NO GOOGLE MAPS"; setOnClickListener { openGoogleMaps() } }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)).apply { topMargin = dp(12) })
        searchPanel.addView(TextView(this).apply { text = "O destino será enviado ao aplicativo Google Maps instalado na central."; textSize = 12f; setTextColor(getColor(R.color.rp_text_muted)); setPadding(0, dp(12), 0, 0) })
        content.addView(searchPanel, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply { marginEnd = dp(7) })

        val popupPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16)); setBackgroundResource(R.drawable.bg_panel) }
        popupPanel.addView(TextView(this).apply { text = "POPUPS DE ROTA"; textSize = 17f; setTextColor(getColor(R.color.rp_text)); setTypeface(typeface, 1) })
        accessStatus = TextView(this).apply { textSize = 14f; setPadding(0, dp(16), 0, dp(10)) }
        popupPanel.addView(accessStatus)
        popupPanel.addView(Button(this).apply { text = "AUTORIZAR ACESSO ÀS NOTIFICAÇÕES"; setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) } }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)))
        popupPanel.addView(Button(this).apply {
            text = if (settingsStore.routePopups) "POPUPS ATIVOS" else "POPUPS DESATIVADOS"
            setOnClickListener { settingsStore.routePopups = !settingsStore.routePopups; recreate() }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)).apply { topMargin = dp(10) })
        popupPanel.addView(TextView(this).apply { text = "O RADIO+ lê somente o texto das notificações do Google Maps para mostrar instruções enquanto outra tela do RADIO+ estiver aberta."; textSize = 12f; setTextColor(getColor(R.color.rp_text_muted)); setPadding(0, dp(16), 0, 0) })
        content.addView(popupPanel, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply { marginStart = dp(7) })
        root.addView(content, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(14) })
        setContentView(root)
        root.post { AccentStyler.apply(root) }
    }

    override fun onResume() { super.onResume(); if (::accessStatus.isInitialized) renderAccess() }

    private fun renderAccess() {
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners").orEmpty().contains(packageName)
        accessStatus.text = if (enabled) "✓ ACESSO ÀS NOTIFICAÇÕES AUTORIZADO" else "ACESSO ÀS NOTIFICAÇÕES NÃO AUTORIZADO"
        accessStatus.setTextColor(getColor(if (enabled) R.color.rp_blue else R.color.rp_night_amber))
    }

    private fun openGoogleMaps() {
        val query = destination.text.toString().trim()
        if (query.isBlank()) { destination.error = "Informe um destino"; return }
        val uri = android.net.Uri.parse("google.navigation:q=${android.net.Uri.encode(query)}&mode=d")
        val intent = Intent(Intent.ACTION_VIEW, uri).setPackage(RouteNotificationListenerService.GOOGLE_MAPS_PACKAGE)
        runCatching { startActivity(intent) }.onFailure {
            AlertDialog.Builder(this).setTitle("Google Maps não encontrado").setMessage("Instale ou ative o Google Maps nesta central.").setPositiveButton("FECHAR", null).show()
        }
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
