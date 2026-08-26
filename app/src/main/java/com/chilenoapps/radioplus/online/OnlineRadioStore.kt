package com.chilenoapps.radioplus.online

import android.content.Context
import com.chilenoapps.radioplus.model.OnlineStation
import org.json.JSONArray
import org.json.JSONObject

class OnlineRadioStore(context: Context) {
    private val preferences = context.getSharedPreferences("online_radio", Context.MODE_PRIVATE)

    fun favorites(): List<OnlineStation> = read("favorites")
    fun history(): List<OnlineStation> = read("history")
    fun isFavorite(uuid: String): Boolean = favorites().any { it.uuid == uuid }

    fun toggleFavorite(station: OnlineStation): Boolean {
        val items = favorites().toMutableList()
        val removed = items.removeAll { it.uuid == station.uuid }
        if (!removed) items.add(0, station)
        write("favorites", items.take(100))
        return !removed
    }

    fun recordHistory(station: OnlineStation) {
        val items = history().filterNot { it.uuid == station.uuid }.toMutableList()
        items.add(0, station)
        write("history", items.take(30))
    }

    private fun read(key: String): List<OnlineStation> = runCatching {
        val array = JSONArray(preferences.getString(key, "[]"))
        buildList {
            for (index in 0 until array.length()) array.optJSONObject(index)?.let { add(it.toStation()) }
        }
    }.getOrDefault(emptyList())

    private fun write(key: String, stations: List<OnlineStation>) {
        val array = JSONArray()
        stations.forEach { array.put(it.toJson()) }
        preferences.edit().putString(key, array.toString()).apply()
    }

    private fun OnlineStation.toJson() = JSONObject()
        .put("uuid", uuid).put("name", name).put("stream", streamUrl)
        .put("homepage", homepage).put("favicon", faviconUrl).put("tags", JSONArray(tags))
        .put("country", country).put("state", state).put("language", language)
        .put("codec", codec).put("bitrate", bitrateKbps).put("votes", votes).put("hls", isHls)

    private fun JSONObject.toStation() = OnlineStation(
        uuid = optString("uuid"), name = optString("name"), streamUrl = optString("stream"),
        homepage = optString("homepage"), faviconUrl = optString("favicon"),
        tags = optJSONArray("tags")?.let { array -> (0 until array.length()).map { array.optString(it) } }.orEmpty(),
        country = optString("country"), state = optString("state"), language = optString("language"),
        codec = optString("codec"), bitrateKbps = optInt("bitrate"), votes = optInt("votes"), isHls = optBoolean("hls")
    )
}
