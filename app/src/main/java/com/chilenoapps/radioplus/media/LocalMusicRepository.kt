package com.chilenoapps.radioplus.media

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.chilenoapps.radioplus.model.MusicTrack

class LocalMusicRepository(private val context: Context) {

    fun loadTracks(): List<MusicTrack> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(MediaStore.Audio.Media.RELATIVE_PATH)
            else add(MediaStore.Audio.Media.DATA)
        }.toTypedArray()

        val result = mutableListOf<MusicTrack>()
        context.contentResolver.query(
            collection,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val pathColumn = cursor.getColumnIndex(projection.last())

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val path = if (pathColumn >= 0) cursor.getString(pathColumn).orEmpty() else ""
                result += MusicTrack(
                    id = id,
                    title = cursor.getString(titleColumn).orEmpty().ifBlank { "Faixa sem título" },
                    artist = cursor.getString(artistColumn).orEmpty().takeUnless { it == "<unknown>" }.orEmpty().ifBlank { "Artista desconhecido" },
                    album = cursor.getString(albumColumn).orEmpty(),
                    durationMs = cursor.getLong(durationColumn),
                    contentUri = ContentUris.withAppendedId(collection, id),
                    sourceLabel = sourceFromPath(path)
                )
            }
        }
        return result
    }

    private fun sourceFromPath(path: String): String {
        val normalized = path.lowercase()
        return when {
            "usb" in normalized -> "USB"
            "sdcard1" in normalized || "external_sd" in normalized -> "CARTÃO SD"
            else -> "MEMÓRIA"
        }
    }
}
