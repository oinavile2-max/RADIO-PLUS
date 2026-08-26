package com.chilenoapps.radioplus.settings

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.chilenoapps.radioplus.BuildConfig
import com.chilenoapps.radioplus.R
import com.chilenoapps.radioplus.hardware.NwdRadioDiagnostics
import com.chilenoapps.radioplus.ui.AccentStyler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NwdRadioDiagnosticActivity : AppCompatActivity() {
    private val diagnostics by lazy { NwdRadioDiagnostics(this) }
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var report: TextView
    private lateinit var save: Button
    private lateinit var share: Button
    private var receiver: BroadcastReceiver? = null
    private var stopListeningTask: Runnable? = null
    private val lines = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.ADMIN_MODE) {
            finish()
            return
        }
        setContentView(buildScreen())
        findViewById<ViewGroup>(android.R.id.content).post {
            AccentStyler.apply(findViewById<ViewGroup>(android.R.id.content))
        }
        renderIdentification()
    }

    private fun buildScreen(): View {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(14))
            setBackgroundResource(R.drawable.bg_app)
        }
        page.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(title("RADIO+", 27f, R.color.rp_text), LinearLayout.LayoutParams(0, dp(52), 1f))
            addView(title("ADMIN • DIAGNÓSTICO", 14f, R.color.rp_night_amber), LinearLayout.LayoutParams(dp(190), dp(44)))
            addView(title("RÁDIO NWD / K2001N", 23f, R.color.rp_text).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(0, dp(52), 2f))
            addView(title(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()), 24f, R.color.rp_text).apply { gravity = Gravity.END or Gravity.CENTER_VERTICAL }, LinearLayout.LayoutParams(dp(100), dp(52)))
        })
        page.addView(Button(this).apply {
            text = "‹  VOLTAR"
            setBackgroundResource(R.drawable.bg_button)
            setTextColor(color(R.color.rp_text))
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(dp(130), dp(44)).apply { bottomMargin = dp(8) })

        val columns = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val identification = panel("IDENTIFICAÇÃO DA CENTRAL")
        identification.tag = "identification"
        val tests = panel("TESTES SEGUROS")
        tests.addView(testRow("1", "LOCALIZAR SERVIÇO NWD", "Somente leitura") { testService() })
        tests.addView(testRow("2", "CONSULTAR ESTADO DO RÁDIO", "Não altera frequência") { testCurrentState() })
        tests.addView(testRow("3", "TESTAR RETORNO DA MCU", "Escuta eventos por 10 segundos") { testMcuEvents() })
        tests.addView(testRow("4", "TESTAR SINTONIA", "Requer confirmação do usuário", true) { confirmTune() })
        columns.addView(identification, LinearLayout.LayoutParams(0, 0, 1f).apply { height = ViewGroup.LayoutParams.MATCH_PARENT; marginEnd = dp(7) })
        columns.addView(tests, LinearLayout.LayoutParams(0, 0, 1f).apply { height = ViewGroup.LayoutParams.MATCH_PARENT; marginStart = dp(7) })
        page.addView(columns, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val reportPanel = panel("RELATÓRIO")
        report = TextView(this).apply {
            text = "Nenhum teste executado. O diagnóstico não altera firmware, root ou MCU."
            textSize = 13f
            setTextColor(color(R.color.rp_text_muted))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        reportPanel.addView(report, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        val actions = LinearLayout(this).apply { gravity = Gravity.END }
        save = actionButton("SALVAR RELATÓRIO") { saveReport() }.apply { isEnabled = false }
        share = actionButton("COMPARTILHAR") { shareReport() }.apply { isEnabled = false }
        actions.addView(save, LinearLayout.LayoutParams(dp(190), dp(44)).apply { marginEnd = dp(8) })
        actions.addView(share, LinearLayout.LayoutParams(dp(170), dp(44)))
        reportPanel.addView(actions)
        page.addView(reportPanel, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(145)).apply { topMargin = dp(10) })
        return page
    }

    private fun renderIdentification() {
        val data = diagnostics.identify()
        val panel = findViewById<ViewGroup>(android.R.id.content)
            .findViewWithTag<LinearLayout>("identification")
        panel.addView(statusRow("Plataforma", data.model.ifBlank { "Não informada" }, data.model.contains("K2001", true)))
        panel.addView(statusRow("Processador", data.hardware.ifBlank { "Não informado" }, data.hardware.isNotBlank()))
        panel.addView(statusRow("Android", data.android, true))
        panel.addView(statusRow("Pacote original", NwdRadioDiagnostics.RADIO_PACKAGE, data.originalPackageInstalled))
        panel.addView(statusRow("Serviço", data.servicePackage ?: "Aguardando teste", data.servicePackage != null))
        panel.addView(statusRow("Permissão", "ACCESS_FM_RADIO • privilegiada", false))
        panel.addView(actionButton("VERIFICAR COMPATIBILIDADE") { testService() }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply { topMargin = dp(8) })
    }

    private fun testService() {
        val data = diagnostics.identify()
        append("Pacote NWD: ${if (data.originalPackageInstalled) "instalado" else "não encontrado"}.")
        append("Serviço por intent: ${data.servicePackage ?: "não exportado ou protegido"}.")
    }

    private fun testCurrentState() {
        startListening(4_000, "Consulta do estado")
        diagnostics.requestCurrentState()
        append("Solicitação de estado enviada; aguardando retorno por 4 segundos.")
    }

    private fun testMcuEvents() {
        startListening(10_000, "Escuta da MCU")
        append("Escutando eventos NWD/MCU por 10 segundos; use os controles físicos do rádio.")
    }

    private fun startListening(duration: Long, label: String) {
        stopListening()
        receiver = diagnostics.listen { action, extras ->
            append("$label: $action ${extras.entries.joinToString { "${it.key}=${it.value}" }}")
        }
        stopListeningTask = Runnable {
            stopListening()
            append("$label concluída.")
        }.also { handler.postDelayed(it, duration) }
    }

    private fun confirmTune() {
        val input = EditText(this).apply {
            hint = "Ex.: 98.5"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        AlertDialog.Builder(this)
            .setTitle("Testar sintonia FM")
            .setMessage("Este teste enviará uma frequência real ao rádio físico. Informe uma frequência entre 87.5 e 108.0 MHz.")
            .setView(input)
            .setNegativeButton("CANCELAR", null)
            .setPositiveButton("CONTINUAR") { _, _ ->
                val frequency = input.text.toString().replace(',', '.').toDoubleOrNull()
                if (frequency == null || frequency !in 87.5..108.0) {
                    append("Teste cancelado: frequência inválida.")
                } else confirmTuneAgain(frequency)
            }.show()
    }

    private fun confirmTuneAgain(frequency: Double) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar comando à central")
            .setMessage("Sintonizar $frequency MHz agora? O RADIO+ aguardará o retorno da MCU para confirmar.")
            .setNegativeButton("NÃO", null)
            .setPositiveButton("SIM, TESTAR") { _, _ ->
                startListening(5_000, "Retorno da sintonia")
                diagnostics.tuneFm(frequency)
                append("Comando $frequency MHz enviado; isso ainda não confirma que o firmware aceitou.")
            }.show()
    }

    private fun stopListening() {
        stopListeningTask?.let(handler::removeCallbacks)
        stopListeningTask = null
        receiver?.let { runCatching { unregisterReceiver(it) } }
        receiver = null
    }

    private fun append(value: String) {
        lines += "${SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())}  $value"
        report.text = lines.takeLast(8).joinToString("\n")
        save.isEnabled = true
        share.isEnabled = true
    }

    private fun saveReport() {
        getSharedPreferences("nwd_diagnostics", MODE_PRIVATE).edit()
            .putString("last_report", lines.joinToString("\n")).apply()
        append("Relatório salvo no armazenamento privado do RADIO+.")
    }

    private fun shareReport() {
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Diagnóstico RADIO+ NWD")
            putExtra(Intent.EXTRA_TEXT, lines.joinToString("\n"))
        }, "Compartilhar diagnóstico"))
    }

    private fun panel(header: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12), dp(10), dp(12), dp(10))
        setBackgroundResource(R.drawable.bg_panel)
        addView(title(header, 15f, R.color.rp_blue), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(34)))
    }

    private fun statusRow(label: String, value: String, success: Boolean) = TextView(this).apply {
        text = "$label     $value     ${if (success) "✓" else "!"}"
        textSize = 13f
        gravity = Gravity.CENTER_VERTICAL
        setTextColor(color(if (success) R.color.rp_text else R.color.rp_night_amber))
        setBackgroundResource(R.drawable.bg_track_row)
        setPadding(dp(12), 0, dp(12), 0)
    }.also { it.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)).apply { bottomMargin = dp(4) } }

    private fun testRow(number: String, name: String, detail: String, warning: Boolean = false, action: () -> Unit): View {
        return LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(4), dp(8), dp(4))
            setBackgroundResource(R.drawable.bg_track_row)
            addView(title(number, 28f, R.color.rp_blue).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(48), dp(54)))
            addView(LinearLayout(this@NwdRadioDiagnosticActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                addView(title(name, 14f, R.color.rp_text))
                addView(title(detail, 11f, R.color.rp_text_muted))
            }, LinearLayout.LayoutParams(0, dp(58), 1f))
            addView(actionButton(if (warning) "CONFIRMAR E TESTAR" else "EXECUTAR", warning, action), LinearLayout.LayoutParams(if (warning) dp(180) else dp(120), dp(44)))
        }.also { it.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply { bottomMargin = dp(5) } }
    }

    private fun actionButton(text: String, warning: Boolean = false, action: () -> Unit) = Button(this).apply {
        this.text = text
        textSize = 11f
        setTextColor(color(if (warning) R.color.rp_night_amber else R.color.rp_blue))
        setBackgroundResource(R.drawable.bg_button)
        setOnClickListener { action() }
    }

    private fun title(text: String, size: Float, colorId: Int) = TextView(this).apply {
        this.text = text
        textSize = size
        gravity = Gravity.CENTER_VERTICAL
        setTextColor(color(colorId))
    }

    private fun color(id: Int) = ContextCompat.getColor(this, id)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        stopListening()
        super.onDestroy()
    }
}
