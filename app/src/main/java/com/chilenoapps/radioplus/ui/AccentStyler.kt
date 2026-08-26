package com.chilenoapps.radioplus.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chilenoapps.radioplus.R
import com.chilenoapps.radioplus.settings.AppearanceStore
import java.util.Calendar

object AccentStyler {
    fun apply(root: View) {
        val store = AppearanceStore(root.context)
        val factor = if (store.adaptDayNight && isNight()) 0.68f else 1f
        val accent = withBrightness(store.accentColor, (store.glowIntensity / 100f) * factor)
        styleRecursively(root, store, accent)
    }

    fun styleButton(button: Button, selected: Boolean = button.isSelected) {
        val store = AppearanceStore(button.context)
        val factor = if (store.adaptDayNight && isNight()) 0.68f else 1f
        val accent = withBrightness(store.accentColor, (store.glowIntensity / 100f) * factor)
        button.isSelected = selected
        button.background = buttonDrawable(button.context, accent, store.contourWidth, selected)
    }

    private fun styleRecursively(view: View, store: AppearanceStore, accent: Int) {
        when (view) {
            is SeekBar -> {
                view.progressTintList = ColorStateList.valueOf(accent)
                view.thumbTintList = ColorStateList.valueOf(accent)
            }
            is Switch -> {
                view.thumbTintList = ColorStateList.valueOf(accent)
                view.trackTintList = ColorStateList.valueOf(withAlpha(accent, 110))
            }
            is Button -> {
                if (view.tag == "solid_swatch") return
                val selectedDrawable = ContextCompat.getDrawable(view.context, R.drawable.bg_button_selected)
                val selected = view.isSelected || (view.background?.constantState != null && view.background.constantState == selectedDrawable?.constantState)
                view.background = buttonDrawable(view.context, accent, store.contourWidth, selected)
            }
            is TextView -> {
                val current = view.currentTextColor
                val defaults = intArrayOf(
                    ContextCompat.getColor(view.context, R.color.rp_blue),
                    ContextCompat.getColor(view.context, R.color.rp_glow),
                    ContextCompat.getColor(view.context, R.color.rp_blue_dim)
                )
                if (defaults.contains(current)) view.setTextColor(accent)
            }
        }
        if (view is ViewGroup) for (index in 0 until view.childCount) styleRecursively(view.getChildAt(index), store, accent)
    }

    private fun buttonDrawable(context: Context, accent: Int, width: Int, selected: Boolean): StateListDrawable {
        val density = context.resources.displayMetrics.density
        fun shape(fill: Int, stroke: Int) = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 12f * density
            setColor(fill)
            setStroke((width * density).toInt().coerceAtLeast(1), stroke)
        }
        val normalFill = ContextCompat.getColor(context, R.color.rp_surface_alt)
        val selectedFill = blend(normalFill, accent, 0.16f)
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), shape(blend(normalFill, accent, 0.22f), accent))
            addState(intArrayOf(), shape(if (selected) selectedFill else normalFill, if (selected) accent else withAlpha(accent, 105)))
        }
    }

    private fun isNight(): Boolean = Calendar.getInstance().get(Calendar.HOUR_OF_DAY).let { it < 6 || it >= 18 }
    private fun withAlpha(color: Int, alpha: Int) = Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
    private fun withBrightness(color: Int, factor: Float): Int = Color.rgb(
        (Color.red(color) * factor.coerceIn(0.3f, 1f)).toInt(),
        (Color.green(color) * factor.coerceIn(0.3f, 1f)).toInt(),
        (Color.blue(color) * factor.coerceIn(0.3f, 1f)).toInt()
    )
    private fun blend(base: Int, accent: Int, ratio: Float): Int = Color.rgb(
        (Color.red(base) * (1 - ratio) + Color.red(accent) * ratio).toInt(),
        (Color.green(base) * (1 - ratio) + Color.green(accent) * ratio).toInt(),
        (Color.blue(base) * (1 - ratio) + Color.blue(accent) * ratio).toInt()
    )
}
