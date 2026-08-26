package com.chilenoapps.radioplus.model

import android.net.Uri

data class MusicTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: Uri,
    val sourceLabel: String
)
