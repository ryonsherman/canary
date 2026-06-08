package com.canary.service

import com.canary.data.GithubApi
import com.canary.model.Canary
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class GithubService(
    private val token: String,
    private val owner: String,
    private val repo: String,
) {
    private val api = GithubApi(token)

    fun pushCanary(
        content: String,
        date: String,
        signature: ByteArray,
        publicKeyFingerprint: String,
    ): Result<Unit> {
        val sigHex = signature.joinToString("") { "%02x".format(it) }
        val signedContent = buildString {
            appendLine("--- BEGIN CANARY SIGNATURE ---")
            appendLine("Public Key Fingerprint: $publicKeyFingerprint")
            appendLine("Signature (hex): $sigHex")
            appendLine("--- END CANARY SIGNATURE ---")
            appendLine()
            append(content)
        }

        val path = "canary/canary-$date.txt"
        val message = "canary: $date"
        return api.pushFile(owner, repo, path, signedContent, message)
    }

    fun fetchRecentCanaries(count: Int = 10): List<Canary> {
        val fileNames = api.listCanaryFiles(owner, repo, count)
        return fileNames.mapNotNull { name ->
            val date = name.removePrefix("canary-").removeSuffix(".txt")
            val content = api.getFileContent(owner, repo, "canary/$name") ?: return@mapNotNull null
            parseCanaryContent(content, date)
        }
    }

    fun getLatestCounter(): Int {
        val canaries = fetchRecentCanaries(1)
        return canaries.firstOrNull()?.counter ?: 0
    }

    fun getLatestCanaryContent(): String? {
        val files = api.listCanaryFiles(owner, repo, 1)
        if (files.isEmpty()) return null
        return api.getFileContent(owner, repo, "canary/${files.first()}")
    }

    fun getPreviousHash(): String {
        val canaries = fetchRecentCanaries(1)
        return canaries.firstOrNull()?.fileHash ?: "GENESIS_NO_PREVIOUS"
    }

    private fun parseCanaryContent(content: String, date: String): Canary? {
        val lines = content.lines()
        var counter = 0
        var previousHash = ""
        var fileHash = ""

        for (line in lines) {
            when {
                line.startsWith("Counter:") -> {
                    counter = line.substringAfter("Counter:").trim().toIntOrNull() ?: 0
                }
                line.startsWith("Previous Hash") -> {
                    previousHash = line.substringAfter("SHA256):").trim()
                }
            }
        }

        fileHash = MessageDigest.getInstance("SHA-256")
            .digest(content.toByteArray())
            .joinToString("") { "%02x".format(it) }

        return Canary(
            date = date,
            counter = counter,
            previousHash = previousHash,
            fileHash = fileHash,
        )
    }

    companion object {
        fun todayDate(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            return sdf.format(Date())
        }

        fun nowTimestamp(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            return sdf.format(Date())
        }
    }
}
