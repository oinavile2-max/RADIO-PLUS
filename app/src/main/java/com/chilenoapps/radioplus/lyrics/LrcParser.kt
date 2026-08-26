package com.chilenoapps.radioplus.lyrics

class LrcParser {
    private val timestamp = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]")

    fun parse(text: String): List<LyricsLine> = text.lineSequence().mapNotNull { raw ->
        val match = timestamp.find(raw) ?: return@mapNotNull null
        val minutes = match.groupValues[1].toLongOrNull() ?: 0L
        val seconds = match.groupValues[2].toLongOrNull() ?: 0L
        val fractionRaw = match.groupValues[3]
        val fractionMs = when (fractionRaw.length) {
            1 -> fractionRaw.toLongOrNull()?.times(100) ?: 0L
            2 -> fractionRaw.toLongOrNull()?.times(10) ?: 0L
            else -> fractionRaw.take(3).padEnd(3, '0').toLongOrNull() ?: 0L
        }
        LyricsLine((minutes * 60 + seconds) * 1_000 + fractionMs, raw.substring(match.range.last + 1).trim())
    }.filter { it.text.isNotBlank() }.sortedBy { it.timestampMs }.toList()
}
