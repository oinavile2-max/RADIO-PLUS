package com.chilenoapps.radioplus.settings

import android.app.AlertDialog
import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.chilenoapps.radioplus.BuildConfig
import com.chilenoapps.radioplus.R

class SettingsActivity : AppCompatActivity() {
    private lateinit var store: AppSettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = AppSettingsStore(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(18), dp(22), dp(30))
        }
        content.addView(TextView(this).apply {
            text = "‹  CONFIGURAÇÕES"
            textSize = 25f
            setTextColor(getColor(R.color.rp_text))
            setTypeface(typeface, 1)
            setPadding(0, 0, 0, dp(12))
            setOnClickListener { finish() }
        })
        addSwitch(content, "GERAL E INICIALIZAÇÃO", "Abrir diretamente no rádio físico", store.startOnPhysicalRadio) {
            store.startOnPhysicalRadio = it
        }
        addChoice(content, "LAUNCHER DA MULTIMÍDIA", "Escolher RADIO+ como tela inicial padrão") {
            runCatching { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
                .onFailure {
                    AlertDialog.Builder(this).setTitle("Launcher padrão").setMessage("Pressione o botão Home da central e selecione RADIO+ Launcher. Algumas centrais definem o Launcher apenas no menu original do equipamento.").setPositiveButton("ENTENDI", null).show()
                }
        }
        addChoice(content, "HOME E PAINÉIS LATERAIS", "Auto-recolhimento: ${delayLabel(store.sidePanelDelaySeconds)}") {
            val values = intArrayOf(0, 3, 5, 10, 15)
            AlertDialog.Builder(this).setTitle("Recolher painel após")
                .setSingleChoiceItems(arrayOf("Nunca", "3 segundos", "5 segundos", "10 segundos", "15 segundos"), values.indexOf(store.sidePanelDelaySeconds)) { dialog, index ->
                    store.sidePanelDelaySeconds = values[index]
                    dialog.dismiss()
                    recreate()
                }.show()
        }
        addSwitch(content, "HOME E PAINÉIS LATERAIS", "Manter painel de favoritos fixo", store.sidePanelPinned) {
            store.sidePanelPinned = it
        }
        addSwitch(content, "APARÊNCIA E TEMAS", "Modo noturno de baixa luminosidade", store.nightMode) {
            store.nightMode = it
        }
        addSwitch(content, "VÍDEO E SEGURANÇA", "Bloquear vídeo durante movimento", store.videoMotionLock) {
            store.videoMotionLock = it
        }
        addSwitch(content, "MAPAS E NAVEGAÇÃO", "Exibir instruções de rota sobre outras telas", store.routePopups) {
            store.routePopups = it
        }
        addSwitch(content, "POPUPS E NOTIFICAÇÕES", "Exibir música reconhecida e capa", store.musicPopups) {
            store.musicPopups = it
        }
        addSwitch(content, "BLUETOOTH E CHAMADAS", "Retomar áudio após conexão Bluetooth", store.autoResumeBluetooth) {
            store.autoResumeBluetooth = it
        }
        addSwitch(content, "COMANDOS POR VOZ", "Reconhecimento em português brasileiro", store.voicePortuguese) {
            store.voicePortuguese = it
        }
        if (BuildConfig.ADMIN_MODE) {
            addChoice(content, "RÁDIO FÍSICO E MCU", "Diagnóstico NWD / K2001N") {
                startActivity(Intent(this, NwdRadioDiagnosticActivity::class.java))
            }
        } else {
            addInfo(content, "RÁDIO FÍSICO E MCU", "Compatibilidade e seleção do adaptador original", "O diagnóstico de hardware está disponível somente na build administrativa de homologação.")
        }
        addInfo(content, "MÚSICA E BIBLIOTECA", "Memória interna, USB, cartão SD, capas e letras", "A biblioteca usa o MediaStore do Android. USB e SD aparecem quando o Android os indexa e concede acesso.")
        addInfo(content, "RÁDIO ONLINE", "Busca, favoritos, histórico e metadados", "A reprodução e a busca usam streams reais. A identificação depende dos metadados enviados pela estação.")
        addInfo(content, "EQUALIZADOR E ÁUDIO", "DSP, AutoEQ, resampler e perfis por saída • VIP", if (BuildConfig.ADMIN_MODE) "Recursos VIP liberados nesta build Admin. Os efeitos só serão aplicados quando suportados pela sessão e saída de áudio." else "Disponível no plano VIP após validação da compra.")
        addInfo(content, "CÂMERA DE RÉ", "Detecção de ré e retorno automático", "Requer entrada de câmera e sinal de ré expostos pela central. Nenhuma fonte compatível foi identificada.")
        addInfo(content, "OBD-II", "ELM327 Bluetooth, dados ao vivo e códigos", "O adaptador precisa ser pareado nas configurações Android. O painel OBD conecta por Bluetooth clássico SPP e não simula respostas.")
        addInfo(content, "ARMAZENAMENTO / USB / SD", "Permissões e diagnóstico de mídia", "O RADIO+ solicita somente as permissões de mídia necessárias à versão do Android.")
        addInfo(content, "APLICATIVOS ORIGINAIS DA CENTRAL", "Mapear rádio, Bluetooth e câmera do fabricante", "Os atalhos só serão disponibilizados depois que um pacote instalado for escolhido e validado.")
        addInfo(content, "VIP, COMPRA E RESTAURAÇÃO", if (BuildConfig.ADMIN_MODE) "ADMIN • VIP ATIVO" else "Assinatura não configurada", if (BuildConfig.ADMIN_MODE) "A liberação é exclusiva desta variante de homologação e não representa compra." else "A compra pública permanecerá oculta até o Google Play Billing e a validação de servidor estarem configurados.")
        addInfo(content, "PRIVACIDADE E SEGURANÇA", "Permissões mínimas, ofuscação e integridade", "A versão release usa redução e ofuscação. Segredos e autorizações VIP não devem ser armazenados como valores confiáveis no APK.")
        addInfo(content, "DIAGNÓSTICO E COMPATIBILIDADE", "Hardware pendente: MCU, câmera e telefonia", "Bluetooth OBD, biblioteca local e rádio online podem ser testados. MCU, câmera e chamadas exigem APIs do equipamento.")
        addInfo(content, "SOBRE", "RADIO+ ${BuildConfig.VERSION_NAME}", "Android 7 ou superior • centrais Android e tablets • orientação horizontal.")
        setContentView(ScrollView(this).apply {
            setBackgroundResource(R.drawable.bg_app)
            addView(content)
        })
    }

    private fun addSwitch(parent: LinearLayout, title: String, label: String, checked: Boolean, changed: (Boolean) -> Unit) {
        addHeader(parent, title)
        parent.addView(Switch(this).apply {
            text = label
            textSize = 16f
            setTextColor(getColor(R.color.rp_text))
            isChecked = checked
            setPadding(dp(16), dp(8), dp(16), dp(8))
            setBackgroundResource(R.drawable.bg_button)
            setOnCheckedChangeListener { _, value -> changed(value) }
        }, rowParams())
    }

    private fun addChoice(parent: LinearLayout, title: String, label: String, action: () -> Unit) {
        addHeader(parent, title)
        parent.addView(TextView(this).apply {
            text = "$label   ›"
            textSize = 16f
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(getColor(R.color.rp_text))
            setPadding(dp(16), 0, dp(16), 0)
            setBackgroundResource(R.drawable.bg_button)
            setOnClickListener { action() }
        }, rowParams())
    }

    private fun addInfo(parent: LinearLayout, title: String, label: String, message: String) = addChoice(parent, title, label) {
        AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("FECHAR", null).show()
    }

    private fun addHeader(parent: LinearLayout, value: String) {
        if ((0 until parent.childCount).map { parent.getChildAt(it) }.filterIsInstance<TextView>().any { it.text == value }) return
        parent.addView(TextView(this).apply {
            text = value
            textSize = 12f
            setTextColor(getColor(R.color.rp_blue))
            setTypeface(typeface, 1)
            setPadding(dp(4), dp(15), 0, dp(6))
        })
    }

    private fun rowParams() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)).apply { bottomMargin = dp(5) }
    private fun delayLabel(value: Int) = if (value == 0) "Nunca" else "$value segundos"
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
