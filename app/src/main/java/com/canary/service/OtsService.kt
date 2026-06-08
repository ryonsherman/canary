package com.canary.service

import java.net.HttpURLConnection
import java.net.URL

class OtsService {

    suspend fun verifyOtsProof(fileHash: ByteArray, otsProof: ByteArray?): OtsResult {
        if (otsProof == null) return OtsResult.NotFound

        return try {
            val response = submitToOtsCalendar(fileHash)
            parseOtsResponse(response)
        } catch (_: Exception) {
            OtsResult.Pending
        }
    }

    private fun submitToOtsCalendar(fileHash: ByteArray): String {
        val url = URL("https://a.pool.opentimestamps.org/")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/octet-stream")
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000

        connection.outputStream.use { it.write(fileHash) }
        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            val errorBody = try { connection.errorStream?.readBytes()?.toString(Charsets.UTF_8) } catch (_: Exception) { null }
            throw RuntimeException("OTS HTTP $responseCode: ${errorBody ?: "no error body"}")
        }
        return connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
    }

    private fun parseOtsResponse(response: String): OtsResult {
        return when {
            response.contains("Block") || response.contains("bitcoin") -> OtsResult.Confirmed
            response.contains("pending") -> OtsResult.Pending
            else -> OtsResult.Pending
        }
    }

    enum class OtsResult {
        Confirmed,
        Pending,
        NotFound,
    }
}
