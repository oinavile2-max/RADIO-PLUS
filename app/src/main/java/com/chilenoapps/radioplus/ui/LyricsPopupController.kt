package com.chilenoapps.radioplus.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chilenoapps.radioplus.R
import com.chilenoapps.radioplus.databinding.PopupLyricsBinding
import com.chilenoapps.radioplus.lyrics.LyricsDocument

class LyricsPopupController(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private var popup: PopupWindow? = null
    private var currentIndex = -1
    private var autoScroll = true
    private var lineViews: List<TextView> = emptyList()

    fun show(anchor: View, document: LyricsDocument, position: () -> Long, duration: () -> Long) {
        dismiss()
        val binding = PopupLyricsBinding.inflate(LayoutInflater.from(context))
        binding.title.text = document.trackName
        binding.artist.text = document.artistName
        binding.syncMode.text = if (document.synchronized) "SINCRONIZADA" else "ROLAGEM AUTOMÁTICA"
        binding.autoScroll.setOnClickListener {
            autoScroll = !autoScroll
            binding.autoScroll.text = if (autoScroll) "ROLAGEM ATIVA" else "ROLAGEM PAUSADA"
        }
        binding.close.setOnClickListener { dismiss() }

        lineViews = document.lines.map { line ->
            TextView(context).apply {
                text = line.text
                setTextColor(ContextCompat.getColor(context, R.color.rp_text_muted))
                textSize = 19f
                gravity = Gravity.CENTER
                setPadding(dp(16), dp(12), dp(16), dp(12))
                binding.lines.addView(this, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            }
        }

        popup = PopupWindow(binding.root, dp(720), dp(480), true).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = dp(14).toFloat()
            showAtLocation(anchor, Gravity.CENTER, 0, 0)
        }

        val ticker = object : Runnable {
            override fun run() {
                if (popup?.isShowing != true) return
                if (autoScroll && lineViews.isNotEmpty()) {
                    val index = if (document.synchronized) {
                        document.lines.indexOfLast { (it.timestampMs ?: Long.MAX_VALUE) <= position() }.coerceAtLeast(0)
                    } else {
                        val total = duration().coerceAtLeast(1L)
                        ((position().toDouble() / total) * document.lines.size).toInt().coerceIn(0, document.lines.lastIndex)
                    }
                    highlight(index, binding)
                }
                handler.postDelayed(this, 250L)
            }
        }
        handler.post(ticker)
    }

    fun dismiss() {
        handler.removeCallbacksAndMessages(null)
        popup?.dismiss()
        popup = null
        currentIndex = -1
    }

    private fun highlight(index: Int, binding: PopupLyricsBinding) {
        if (index == currentIndex) return
        lineViews.getOrNull(currentIndex)?.apply {
            setTextColor(ContextCompat.getColor(context, R.color.rp_text_muted)); setTypeface(typeface, Typeface.NORMAL)
        }
        lineViews.getOrNull(index)?.apply {
            setTextColor(ContextCompat.getColor(context, R.color.rp_glow)); setTypeface(typeface, Typeface.BOLD)
            post { binding.scroll.smoothScrollTo(0, (top - binding.scroll.height / 2 + height / 2).coerceAtLeast(0)) }
        }
        currentIndex = index
    }

    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()
}
