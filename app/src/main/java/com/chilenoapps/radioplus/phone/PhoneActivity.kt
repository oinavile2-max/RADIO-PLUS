package com.chilenoapps.radioplus.phone

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.chilenoapps.radioplus.R
import com.chilenoapps.radioplus.ui.AccentStyler

class PhoneActivity : AppCompatActivity() {
    private lateinit var number: EditText
    private lateinit var contacts: LinearLayout
    private lateinit var contactStatus: TextView
    private val contactsPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) loadContacts() else contactStatus.text = "Permissão de contatos não concedida" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(14)); setBackgroundResource(R.drawable.bg_app) }
        root.addView(TextView(this).apply { text = "‹  TELEFONE"; textSize = 24f; setTextColor(getColor(R.color.rp_text)); setTypeface(typeface, 1); setOnClickListener { finish() } })
        val body = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

        val bluetoothPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(14), dp(14), dp(14), dp(14)); setBackgroundResource(R.drawable.bg_panel) }
        bluetoothPanel.addView(TextView(this).apply { text = "ᛒ"; textSize = 54f; setTextColor(getColor(R.color.rp_blue)); gravity = Gravity.CENTER })
        val bluetoothOn = runCatching { BluetoothAdapter.getDefaultAdapter()?.isEnabled == true }.getOrDefault(false)
        bluetoothPanel.addView(TextView(this).apply { text = if (bluetoothOn) "BLUETOOTH ATIVO" else "BLUETOOTH DESATIVADO"; textSize = 16f; setTextColor(getColor(R.color.rp_text)); gravity = Gravity.CENTER; setTypeface(typeface, 1) })
        bluetoothPanel.addView(Button(this).apply { text = "CONFIGURAÇÕES BLUETOOTH"; setOnClickListener { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) } }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)).apply { topMargin = dp(18) })
        bluetoothPanel.addView(TextView(this).apply { text = "O áudio e os comandos da chamada dependem do Bluetooth e do hardware da central."; textSize = 11f; gravity = Gravity.CENTER; setTextColor(getColor(R.color.rp_text_muted)); setPadding(0, dp(16), 0, 0) })
        body.addView(bluetoothPanel, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.75f).apply { marginEnd = dp(7) })

        val dialPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(12), dp(12), dp(12), dp(12)); setBackgroundResource(R.drawable.bg_panel) }
        number = EditText(this).apply { inputType = android.text.InputType.TYPE_CLASS_PHONE; gravity = Gravity.CENTER; textSize = 22f; setTextColor(getColor(R.color.rp_text)); setSingleLine(); setBackgroundResource(R.drawable.bg_button) }
        dialPanel.addView(number, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)))
        listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("*","0","#")).forEach { rowValues ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            rowValues.forEach { digit -> row.addView(Button(this).apply { text = digit; textSize = 20f; setOnClickListener { number.append(digit) } }, LinearLayout.LayoutParams(0, dp(58), 1f).apply { setMargins(dp(3), dp(3), dp(3), dp(3)) }) }
            dialPanel.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64)))
        }
        val callActions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        callActions.addView(Button(this).apply { text = "⌫"; setOnClickListener { number.text?.let { if (it.isNotEmpty()) it.delete(it.length - 1, it.length) } } }, LinearLayout.LayoutParams(0, dp(58), 0.35f).apply { marginEnd = dp(4) })
        callActions.addView(Button(this).apply { text = "☎  LIGAR"; setOnClickListener { dialNumber() } }, LinearLayout.LayoutParams(0, dp(58), 0.65f).apply { marginStart = dp(4) })
        dialPanel.addView(callActions)
        body.addView(dialPanel, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.75f).apply { marginStart = dp(7); marginEnd = dp(7) })

        val contactsPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(12), dp(12), dp(12)); setBackgroundResource(R.drawable.bg_panel) }
        contactsPanel.addView(TextView(this).apply { text = "CONTATOS"; textSize = 17f; setTextColor(getColor(R.color.rp_text)); setTypeface(typeface, 1) })
        contactStatus = TextView(this).apply { text = "Carregando contatos…"; textSize = 12f; setTextColor(getColor(R.color.rp_text_muted)); setPadding(0, dp(8), 0, dp(8)) }
        contactsPanel.addView(contactStatus)
        contacts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        contactsPanel.addView(ScrollView(this).apply { addView(contacts) }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        contactsPanel.addView(Button(this).apply { text = "ATUALIZAR CONTATOS"; setOnClickListener { ensureContactsPermission() } }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)))
        body.addView(contactsPanel, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply { marginStart = dp(7) })
        root.addView(body, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(10) })
        setContentView(root)
        root.post { AccentStyler.apply(root) }
        ensureContactsPermission()
    }

    private fun ensureContactsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) loadContacts() else contactsPermission.launch(Manifest.permission.READ_CONTACTS)
    }

    private fun loadContacts() {
        contacts.removeAllViews()
        val seen = hashSetOf<String>()
        contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER), null, null, "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE")?.use { cursor ->
            val nameColumn = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME); val numberColumn = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val phone = cursor.getString(numberColumn).orEmpty(); if (!seen.add(phone)) continue
                val name = cursor.getString(nameColumn).orEmpty().ifBlank { "Contato sem nome" }
                contacts.addView(TextView(this).apply { text = "$name\n$phone"; textSize = 14f; setTextColor(getColor(R.color.rp_text)); setPadding(dp(12), dp(10), dp(12), dp(10)); setBackgroundResource(R.drawable.bg_track_row); setOnClickListener { number.setText(phone) } }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(5) })
            }
        }
        contactStatus.text = if (seen.isEmpty()) "Nenhum contato encontrado" else "${seen.size} contatos"
    }

    private fun dialNumber() {
        val value = number.text.toString().trim()
        if (value.isBlank()) { number.error = "Digite ou selecione um número"; return }
        runCatching { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(value)}"))) }
            .onFailure { contactStatus.text = "Nenhum aplicativo de chamadas compatível foi encontrado" }
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
