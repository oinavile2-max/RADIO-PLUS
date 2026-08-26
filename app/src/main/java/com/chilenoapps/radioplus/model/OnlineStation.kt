package com.chilenoapps.radioplus.model

data class OnlineStation(
    val uuid: String,
    val name: String,
    val streamUrl: String,
    val homepage: String,
    val faviconUrl: String,
    val tags: List<String>,
    val country: String,
    val state: String,
    val language: String,
    val codec: String,
    val bitrateKbps: Int,
    val votes: Int,
    val isHls: Boolean
)

enum class StationSearchMode(val apiParameter: String) {
    NAME("name"), GENRE("tag"), COUNTRY("country"), STATE("state")
}
