package com.deepkosh.goldpulse.data

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/** Minimal blocking HTTPS GET. Call from a background dispatcher. */
class HttpFetcher {

    fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.useCaches = false
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", USER_AGENT)

            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP $code from ${connection.url.host}")
            }
            return connection.inputStream.use(::readLimited)
        } finally {
            connection.disconnect()
        }
    }

    private fun readLimited(input: InputStream): String {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            out.write(buffer, 0, read)
            if (out.size() > MAX_RESPONSE_BYTES) throw IOException("Response too large")
        }
        return out.toString(Charsets.UTF_8.name())
    }

    private companion object {
        const val TIMEOUT_MS = 10_000
        const val MAX_RESPONSE_BYTES = 512 * 1024
        const val USER_AGENT = "GoldPulse-Android"
    }
}
