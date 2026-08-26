package com.chilenoapps.radioplus.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Handler
import android.os.Looper
import com.chilenoapps.radioplus.model.MusicTrack
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.Executors

class AlbumArtRepository(private val context: Context) {
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val cacheDirectory = File(context.cacheDir, "album_art").apply { mkdirs() }
    private var lastMusicBrainzRequestAt = 0L

    fun load(track: MusicTrack, callback: (Bitmap?) -> Unit) {
        executor.execute {
            val bitmap = readEmbedded(track)
                ?: readCached(track)
                ?: downloadCover(track)?.also { saveCached(track, it) }
            mainHandler.post { callback(bitmap) }
        }
    }

    fun close() = executor.shutdownNow()

    private fun readEmbedded(track: MusicTrack): Bitmap? = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, track.contentUri)
            retriever.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        } finally {
            retriever.release()
        }
    }.getOrNull()

    private fun readCached(track: MusicTrack): Bitmap? {
        val file = cacheFile(track)
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }

    private fun saveCached(track: MusicTrack, bitmap: Bitmap) {
        runCatching {
            cacheFile(track).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        }
    }

    private fun downloadCover(track: MusicTrack): Bitmap? {
        if (track.album.isBlank() || track.artist == "Artista desconhecido") return null
        val releaseId = findReleaseId(track) ?: return null
        return requestBytes("https://coverartarchive.org/release/$releaseId/front-1200")
            ?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    }

    private fun findReleaseId(track: MusicTrack): String? {
        val waitMs = 1_050L - (System.currentTimeMillis() - lastMusicBrainzRequestAt)
        if (waitMs > 0) Thread.sleep(waitMs)
        lastMusicBrainzRequestAt = System.currentTimeMillis()

        val query = "release:\"${track.album}\" AND artist:\"${track.artist}\""
        val encoded = URLEncoder.encode(query, "UTF-8")
        val bytes = requestBytes("https://musicbrainz.org/ws/2/release/?query=$encoded&fmt=json&limit=1") ?: return null
        return runCatching {
            JSONObject(String(bytes, Charsets.UTF_8)).getJSONArray("releases").optJSONObject(0)?.optString("id")
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun requestBytes(url: String): ByteArray? = runCatching {
        (URL(url).openConnection() as HttpURLConnection).run {
            connectTimeout = 8_000
            readTimeout = 12_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "RADIOPlus/0.1 (https://github.com/oinavile2-max/RADIO-PLUS)")
            setRequestProperty("Accept", "application/json, image/*")
            try {
                if (responseCode !in 200..399) return@run null
                inputStream.use { it.readBytes() }
            } finally {
                disconnect()
            }
        }
    }.getOrNull()

    private fun cacheFile(track: MusicTrack): File {
        val key = "${track.artist.lowercase()}|${track.album.lowercase()}"
        val hash = MessageDigest.getInstance("SHA-256").digest(key.toByteArray()).joinToString("") { "%02x".format(it) }
        return File(cacheDirectory, "$hash.jpg")
    }
}
