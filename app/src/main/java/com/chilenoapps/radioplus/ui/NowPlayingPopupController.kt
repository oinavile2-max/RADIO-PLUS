package com.chilenoapps.radioplus.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import com.chilenoapps.radioplus.databinding.PopupNowPlayingBinding
import com.chilenoapps.radioplus.recognition.NowPlayingInfo

class NowPlayingPopupController(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private var popup: PopupWindow? = null

    fun show(anchor: View, info: NowPlayingInfo, displayMs: Long = 8_000L) {
        popup?.dismiss()
        handler.removeCallbacksAndMessages(null)
        val binding = PopupNowPlayingBinding.inflate(LayoutInflater.from(context))
        binding.trackTitle.text = info.title
        binding.trackArtist.text = info.artist.ifBlank { "Música identificada" }
        binding.source.text = "${info.sourceName}  •  ${sourceLabel(info)}"

        popup = PopupWindow(
            binding.root,
            dp(560),
            dp(92),
            false
        ).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = false
            elevation = dp(12).toFloat()
            showAtLocation(anchor, Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, dp(58))
        }
        handler.postDelayed({ popup?.dismiss() }, displayMs)
    }

    fun dismiss() {
        handler.removeCallbacksAndMessages(null)
        popup?.dismiss()
        popup = null
    }

    private fun sourceLabel(info: NowPlayingInfo): String = when (info.sourceType) {
        com.chilenoapps.radioplus.recognition.SourceType.ONLINE_METADATA -> "DADOS DA ESTAÇÃO"
        com.chilenoapps.radioplus.recognition.SourceType.PHYSICAL_RDS -> "RDS"
        com.chilenoapps.radioplus.recognition.SourceType.AUDIO_FINGERPRINT -> "RECONHECIMENTO"
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
}
