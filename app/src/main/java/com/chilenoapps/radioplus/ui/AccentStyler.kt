package com.chilenoapps.radioplus.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
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
        button.background = if (button.tag == "round_control") knobDrawable(button.context, accent, selected) else buttonDrawable(button.context, accent, store.contourWidth, selected)
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
                view.background = if (view.tag == "round_control") knobDrawable(view.context, accent, selected) else buttonDrawable(view.context, accent, store.contourWidth, selected)
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

    private fun knobDrawable(context: Context, accent: Int, selected: Boolean): StateListDrawable {
        val density = context.resources.displayMetrics.density
        fun oval(colors: IntArray, stroke: Int, strokeWidth: Int) = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            colors
        ).apply {
            shape = GradientDrawable.OVAL
            setStroke((strokeWidth * density).toInt().coerceAtLeast(1), stroke)
        }
        fun relief(pressed: Boolean): LayerDrawable {
            val shadow = oval(intArrayOf(Color.rgb(2, 3, 5), Color.BLACK), Color.BLACK, 3)
            val ring = oval(
                intArrayOf(
                    if (pressed) blend(Color.rgb(34, 45, 56), accent, 0.18f) else Color.rgb(72, 84, 96),
                    Color.rgb(12, 18, 25),
                    Color.rgb(2, 4, 7)
                ),
                if (selected || pressed) accent else withAlpha(accent, 210),
                if (selected || pressed) 4 else 3
            )
            return LayerDrawable(arrayOf(shadow, ring)).apply {
                setLayerInset(0, 0, (4 * density).toInt(), 0, 0)
                val offset = if (pressed) (3 * density).toInt() else 0
                setLayerInset(1, (4 * density).toInt(), offset, (4 * density).toInt(), (7 * density).toInt() - offset)
            }
        }
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), relief(true))
            addState(intArrayOf(), relief(false))
        }
    }

    private fun buttonDrawable(context: Context, accent: Int, width: Int, selected: Boolean): StateListDrawable {
        val density = context.resources.displayMetrics.density
        fun gradient(top: Int, middle: Int, bottom: Int, stroke: Int, radius: Float = 12f) = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(top, middle, bottom)
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius * density
            setStroke((width * density).toInt().coerceAtLeast(1), stroke)
        }
        fun relief(pressed: Boolean, active: Boolean): LayerDrawable {
            val edge = if (active || pressed) accent else withAlpha(accent, 95)
            val shadow = gradient(Color.rgb(2, 4, 6), Color.rgb(1, 2, 3), Color.BLACK, withAlpha(edge, 45), 13f)
            val top = if (pressed) blend(Color.rgb(18, 25, 33), accent, 0.18f) else if (active) blend(Color.rgb(35, 46, 58), accent, 0.14f) else Color.rgb(34, 43, 54)
            val middle = if (pressed) Color.rgb(8, 12, 17) else if (active) blend(Color.rgb(12, 18, 25), accent, 0.10f) else Color.rgb(13, 19, 26)
            val bottom = if (pressed) Color.rgb(3, 5, 7) else Color.rgb(5, 8, 12)
            val face = gradient(top, middle, bottom, edge)
            val shine = gradient(withAlpha(Color.WHITE, if (active) 82 else 48), withAlpha(Color.WHITE, 14), Color.TRANSPARENT, Color.TRANSPARENT, 10f)
            return LayerDrawable(arrayOf(shadow, face, shine)).apply {
                val pressOffset = if (pressed) 3 else 0
                setLayerInset(0, 0, (4 + pressOffset) * density.toInt(), 0, 0)
                setLayerInset(1, 1 * density.toInt(), pressOffset * density.toInt(), 1 * density.toInt(), (4 - pressOffset).coerceAtLeast(1) * density.toInt())
                setLayerInset(2, 5 * density.toInt(), (2 + pressOffset) * density.toInt(), 5 * density.toInt(), 0)
            }
        }
        return StateListDrawable().apply {
            addState(intArrayOf(-android.R.attr.state_enabled), relief(false, false).also { it.alpha = 105 })
            addState(intArrayOf(android.R.attr.state_pressed), relief(true, selected))
            addState(intArrayOf(android.R.attr.state_selected), relief(false, true))
            addState(intArrayOf(), relief(false, selected))
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
