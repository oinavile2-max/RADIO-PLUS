package com.chilenoapps.radioplus.settings

import android.content.Context
import android.graphics.Color

class AppearanceStore(context: Context) {
    private val preferences = context.getSharedPreferences("radio_plus_appearance", Context.MODE_PRIVATE)

    var accentColor: Int
        get() = preferences.getInt("accent_color", DEFAULT_ACCENT)
        set(value) = preferences.edit().putInt("accent_color", value).apply()

    var glowIntensity: Int
        get() = preferences.getInt("glow_intensity", 65)
        set(value) = preferences.edit().putInt("glow_intensity", value.coerceIn(0, 100)).apply()

    var contourWidth: Int
        get() = preferences.getInt("contour_width", 1)
        set(value) = preferences.edit().putInt("contour_width", value.coerceIn(1, 3)).apply()

    var adaptDayNight: Boolean
        get() = preferences.getBoolean("adapt_day_night", true)
        set(value) = preferences.edit().putBoolean("adapt_day_night", value).apply()

    fun reset() {
        accentColor = DEFAULT_ACCENT
        glowIntensity = 65
        contourWidth = 1
        adaptDayNight = true
    }

    companion object {
        val DEFAULT_ACCENT: Int = Color.parseColor("#4EDCFF")
        val PALETTE = linkedMapOf(
            "Ciano" to "#4EDCFF", "Azul" to "#1976FF", "Roxo" to "#8B3DFF",
            "Magenta" to "#FF20B7", "Vermelho" to "#FF3535", "Laranja" to "#FF7300",
            "Âmbar" to "#FFB000", "Amarelo" to "#FFD400", "Verde" to "#10CE38",
            "Turquesa" to "#00D4B4", "Branco" to "#FFFFFF", "Gelo" to "#BDE3FF"
        ).mapValues { Color.parseColor(it.value) }
    }
}
