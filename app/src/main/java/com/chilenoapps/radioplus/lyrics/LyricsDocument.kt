package com.chilenoapps.radioplus.lyrics

data class LyricsDocument(
    val trackName: String,
    val artistName: String,
    val lines: List<LyricsLine>,
    val synchronized: Boolean,
    val instrumental: Boolean = false,
    val source: String = "LRCLIB"
)

data class LyricsLine(val timestampMs: Long?, val text: String)
