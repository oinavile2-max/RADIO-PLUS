package com.chilenoapps.radioplus.online

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class RemoteImageLoader {
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    fun load(url: String, callback: (Bitmap?) -> Unit) {
        if (url.isBlank()) return callback(null)
        executor.execute {
            val bitmap = runCatching {
                (URL(url).openConnection() as HttpURLConnection).run {
                    connectTimeout = 6_000
                    readTimeout = 9_000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "RADIOPlus/0.1")
                    try {
                        if (responseCode !in 200..399) null else inputStream.use(BitmapFactory::decodeStream)
                    } finally {
                        disconnect()
                    }
                }
            }.getOrNull()
            main.post { callback(bitmap) }
        }
    }

    fun close() = executor.shutdownNow()
}
