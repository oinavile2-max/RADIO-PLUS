package com.chilenoapps.radioplus.online

import com.chilenoapps.radioplus.model.OnlineStation
import com.chilenoapps.radioplus.model.StationSearchMode
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class RadioBrowserClient {
    private val mirrors = listOf(
        "https://de1.api.radio-browser.info",
        "https://nl1.api.radio-browser.info",
        "https://at1.api.radio-browser.info"
    )

    fun search(query: String, mode: StationSearchMode, limit: Int = 80): List<OnlineStation> {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val path = "/json/stations/search?${mode.apiParameter}=$encoded&hidebroken=true&order=votes&reverse=true&limit=$limit"
        return requestJson(path)?.let(::parseStations).orEmpty()
    }

    fun popularBrazil(limit: Int = 80): List<OnlineStation> {
        val path = "/json/stations/search?countrycode=BR&hidebroken=true&order=clickcount&reverse=true&limit=$limit"
        return requestJson(path)?.let(::parseStations).orEmpty()
    }

    fun registerClick(stationUuid: String) {
        if (stationUuid.isBlank()) return
        requestJson("/json/url/${URLEncoder.encode(stationUuid, "UTF-8")}")
    }

    private fun requestJson(path: String): String? {
        mirrors.forEach { mirror ->
            val result = runCatching {
                (URL(mirror + path).openConnection() as HttpURLConnection).run {
                    connectTimeout = 7_000
                    readTimeout = 12_000
                    setRequestProperty("User-Agent", "RADIOPlus/0.1 (oinavile2-max/RADIO-PLUS)")
                    setRequestProperty("Accept", "application/json")
                    try {
                        if (responseCode !in 200..299) null else inputStream.bufferedReader().use { it.readText() }
                    } finally {
                        disconnect()
                    }
                }
            }.getOrNull()
            if (!result.isNullOrBlank()) return result
        }
        return null
    }

    private fun parseStations(json: String): List<OnlineStation> {
        val array = JSONArray(json)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val stream = item.optString("url_resolved").ifBlank { item.optString("url") }
                if (stream.isBlank()) continue
                add(
                    OnlineStation(
                        uuid = item.optString("stationuuid"),
                        name = item.optString("name").trim().ifBlank { "Rádio sem nome" },
                        streamUrl = stream,
                        homepage = item.optString("homepage"),
                        faviconUrl = item.optString("favicon"),
                        tags = item.optString("tags").split(',').map(String::trim).filter(String::isNotBlank).distinct().take(5),
                        country = item.optString("country"),
                        state = item.optString("state"),
                        language = item.optString("language"),
                        codec = item.optString("codec"),
                        bitrateKbps = item.optInt("bitrate"),
                        votes = item.optInt("votes"),
                        isHls = item.optInt("hls") == 1
                    )
                )
            }
        }.distinctBy { station -> station.uuid.ifBlank { station.streamUrl } }
    }
}
