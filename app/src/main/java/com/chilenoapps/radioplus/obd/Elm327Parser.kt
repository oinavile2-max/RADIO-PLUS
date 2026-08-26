package com.chilenoapps.radioplus.obd

object Elm327Parser {
    fun hexBytes(raw: String, expectedModeAndPid: String): List<Int>? {
        val clean = raw.uppercase()
            .replace("SEARCHING...", "")
            .replace("BUS INIT: OK", "")
            .replace("\r", " ").replace("\n", " ").replace(">", " ")
        if (listOf("NO DATA", "UNABLE TO CONNECT", "STOPPED", "ERROR").any(clean::contains)) return null
        val tokens = Regex("[0-9A-F]{2}").findAll(clean).map { it.value }.toList()
        val marker = expectedModeAndPid.chunked(2)
        val start = tokens.windowed(marker.size).indexOfFirst { it == marker }
        if (start < 0) return null
        return tokens.drop(start + marker.size).map { it.toInt(16) }
    }

    fun decode(pid: String, raw: String): Number? {
        val b = hexBytes(raw, "41$pid") ?: return null
        return when (pid) {
            "0C" -> if (b.size >= 2) ((b[0] * 256 + b[1]) / 4) else null
            "0D" -> b.firstOrNull()
            "05", "0F" -> b.firstOrNull()?.minus(40)
            "04", "11" -> b.firstOrNull()?.times(100)?.div(255)
            else -> null
        }
    }

    fun voltage(raw: String): Double? = Regex("(\\d{1,2}\\.\\d{1,2})V", RegexOption.IGNORE_CASE)
        .find(raw)?.groupValues?.getOrNull(1)?.toDoubleOrNull()

    fun troubleCodes(raw: String): List<String> {
        val bytes = hexBytes(raw, "43") ?: return emptyList()
        return bytes.chunked(2).mapNotNull { pair ->
            if (pair.size < 2 || (pair[0] == 0 && pair[1] == 0)) return@mapNotNull null
            val family = "PCBU"[(pair[0] shr 6) and 0x03]
            val digit = (pair[0] shr 4) and 0x03
            val tail = "%X%02X".format(pair[0] and 0x0F, pair[1])
            "$family$digit$tail"
        }.distinct()
    }
}
