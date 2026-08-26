package com.chilenoapps.radioplus.lyrics

import android.content.Context
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

class LyricsRepository(context: Context) {
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val cache = File(context.cacheDir, "lyrics").apply { mkdirs() }
    private val parser = LrcParser()

    fun load(track: MusicTrack, callback: (LyricsDocument?) -> Unit) {
        executor.execute {
            val document = readCache(track) ?: download(track)?.also { writeCache(track, it) }
            main.post { callback(document) }
        }
    }

    fun close() = executor.shutdownNow()

    private fun download(track: MusicTrack): LyricsDocument? {
        val params = linkedMapOf(
            "track_name" to track.title,
            "artist_name" to track.artist,
            "album_name" to track.album,
            "duration" to (track.durationMs / 1_000).toString()
        ).filterValues(String::isNotBlank).map { (key, value) -> "$key=${URLEncoder.encode(value, "UTF-8")}" }.joinToString("&")
        val json = request("https://lrclib.net/api/get?$params") ?: return null
        return parseResponse(json)
    }

    private fun parseResponse(json: String): LyricsDocument? = runCatching {
        val item = JSONObject(json)
        val syncedText = item.optString("syncedLyrics")
        val plainText = item.optString("plainLyrics")
        val syncedLines = parser.parse(syncedText)
        val lines = if (syncedLines.isNotEmpty()) syncedLines else plainText.lineSequence()
            .map(String::trim).filter(String::isNotBlank).map { LyricsLine(null, it) }.toList()
        LyricsDocument(
            trackName = item.optString("trackName"),
            artistName = item.optString("artistName"),
            lines = lines,
            synchronized = syncedLines.isNotEmpty(),
            instrumental = item.optBoolean("instrumental")
        ).takeIf { it.lines.isNotEmpty() || it.instrumental }
    }.getOrNull()

    private fun request(url: String): String? = runCatching {
        (URL(url).openConnection() as HttpURLConnection).run {
            connectTimeout = 8_000
            readTimeout = 12_000
            setRequestProperty("User-Agent", "RADIOPlus/0.1 (https://github.com/oinavile2-max/RADIO-PLUS)")
            setRequestProperty("Accept", "application/json")
            try {
                if (responseCode !in 200..299) null else inputStream.bufferedReader().use { it.readText() }
            } finally { disconnect() }
        }
    }.getOrNull()

    private fun readCache(track: MusicTrack): LyricsDocument? = cacheFile(track).takeIf(File::exists)?.readText()?.let(::parseResponse)
    private fun writeCache(track: MusicTrack, document: LyricsDocument) {
        val synced = if (document.synchronized) document.lines.joinToString("\n") { line ->
            val ms = line.timestampMs ?: 0L
            "[%02d:%02d.%02d] %s".format(ms / 60_000, (ms / 1_000) % 60, (ms % 1_000) / 10, line.text)
        } else ""
        val json = JSONObject().put("trackName", document.trackName).put("artistName", document.artistName)
            .put("syncedLyrics", synced).put("plainLyrics", document.lines.joinToString("\n") { it.text })
            .put("instrumental", document.instrumental)
        runCatching { cacheFile(track).writeText(json.toString()) }
    }

    private fun cacheFile(track: MusicTrack): File {
        val key = "${track.artist}|${track.title}|${track.album}".lowercase()
        val hash = MessageDigest.getInstance("SHA-256").digest(key.toByteArray()).joinToString("") { "%02x".format(it) }
        return File(cache, "$hash.json")
    }
}
