package com.chilenoapps.radioplus.ui

import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chilenoapps.radioplus.R
import com.chilenoapps.radioplus.settings.AppSettingsStore
import com.chilenoapps.radioplus.settings.AppearanceStore

class RoutePopupController(private val activity: Activity) {
    private var popup: PopupWindow? = null

    fun show(anchor: View, title: String, text: String) {
        if (!AppSettingsStore(activity).routePopups) return
        popup?.dismiss()
        val accent = AppearanceStore(activity).accentColor
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(10), dp(10), dp(10))
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(ContextCompat.getColor(activity, R.color.rp_surface_alt))
                setStroke(dp(2), accent)
            }
        }
        content.addView(TextView(activity).apply { this.text = "↱"; textSize = 34f; setTextColor(accent); gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(54), dp(64)))
        content.addView(LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(activity).apply { this.text = title.ifBlank { "INSTRUÇÃO DE ROTA" }; textSize = 16f; setTextColor(ContextCompat.getColor(activity, R.color.rp_text)); setTypeface(typeface, 1) })
            addView(TextView(activity).apply { this.text = text; textSize = 13f; maxLines = 2; setTextColor(ContextCompat.getColor(activity, R.color.rp_text_muted)) })
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        content.addView(Button(activity).apply { this.text = "×"; contentDescription = "Fechar instrução"; setOnClickListener { popup?.dismiss() } }, LinearLayout.LayoutParams(dp(48), dp(48)))
        popup = PopupWindow(content, dp(520), LinearLayout.LayoutParams.WRAP_CONTENT, true).apply {
            isOutsideTouchable = true
            elevation = dp(8).toFloat()
            showAtLocation(anchor, Gravity.TOP or Gravity.END, dp(18), dp(82))
        }
    }

    fun dismiss() { popup?.dismiss(); popup = null }
    private fun dp(value: Int) = (value * activity.resources.displayMetrics.density).toInt()
}
