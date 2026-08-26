package com.chilenoapps.radioplus.media

import java.net.HttpURLConnection
import java.net.URL

data class StreamEntry(val title: String, val url: String)

class StreamPlaylistParser {
    fun load(url: String): List<StreamEntry> {
        val text = download(url) ?: return emptyList()
        return when {
            url.substringBefore('?').endsWith(".pls", ignoreCase = true) || text.contains("[playlist]", true) -> parsePls(text)
            else -> parseM3u(text)
        }
    }

    private fun parseM3u(text: String): List<StreamEntry> {
        val result = mutableListOf<StreamEntry>()
        var pendingTitle = "Fluxo online"
        text.lineSequence().map(String::trim).filter(String::isNotBlank).forEach { line ->
            when {
                line.startsWith("#EXTINF", true) -> pendingTitle = line.substringAfter(',', "Fluxo online").trim()
                !line.startsWith("#") -> {
                    result += StreamEntry(pendingTitle, line)
                    pendingTitle = "Fluxo online"
                }
            }
        }
        return result
    }

    private fun parsePls(text: String): List<StreamEntry> {
        val values = text.lineSequence().map(String::trim).filter { '=' in it }
            .associate { it.substringBefore('=').lowercase() to it.substringAfter('=') }
        return values.filterKeys { it.startsWith("file") }.mapNotNull { (key, streamUrl) ->
            val index = key.removePrefix("file")
            streamUrl.takeIf(String::isNotBlank)?.let {
                StreamEntry(values["title$index"].orEmpty().ifBlank { "Fluxo online" }, it)
            }
        }
    }

    private fun download(url: String): String? = runCatching {
        (URL(url).openConnection() as HttpURLConnection).run {
            connectTimeout = 8_000
            readTimeout = 12_000
            setRequestProperty("User-Agent", "RADIOPlus/0.1")
            try {
                if (responseCode !in 200..299) return@run null
                inputStream.bufferedReader().use { it.readText() }
            } finally {
                disconnect()
            }
        }
    }.getOrNull()
}
