package com.canary.data

import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GithubApi(private val token: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://api.github.com"

    fun pushFile(
        owner: String,
        repo: String,
        path: String,
        contentBytes: ByteArray,
        message: String,
    ): Result<Unit> = runCatching {
        val url = "$baseUrl/repos/$owner/$repo/contents/$path"
        val sha = getExistingFileSha(owner, repo, path)

        val body = JSONObject().apply {
            put("message", message)
            put("content", android.util.Base64.encodeToString(
                contentBytes, android.util.Base64.NO_WRAP
            ))
            sha?.let { put("sha", it) }
        }

        val request = Request.Builder()
            .url(url)
            .put(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("GitHub API error ${response.code}: ${response.body?.string()}")
            }
        }
    }

    fun getFileContent(owner: String, repo: String, path: String): String? = runCatching {
        val url = "$baseUrl/repos/$owner/$repo/contents/$path"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github.v3.raw")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) response.body?.string() else null
        }
    }.getOrNull()

    fun fetchPublicFile(owner: String, repo: String, path: String): String? = runCatching {
        val url = "https://raw.githubusercontent.com/$owner/$repo/main/$path"
        val request = Request.Builder()
            .url(url)
            .build()
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) response.body?.string() else null
        }
    }.getOrNull()

    fun listCanaryFiles(owner: String, repo: String, count: Int = 10): List<String> = runCatching {
        val url = "$baseUrl/repos/$owner/$repo/contents/canary"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        val response = client.newCall(request).execute()
        val result = response.use { resp ->
            if (!resp.isSuccessful) {
                android.util.Log.w("GithubApi", "listCanaryFiles HTTP ${resp.code}: ${resp.body?.string()}")
                return@use emptyList()
            }

            val json = JSONArray(resp.body?.string() ?: "[]")
            val files = mutableListOf<String>()
            for (i in 0 until json.length()) {
                val obj = json.getJSONObject(i)
                val name = obj.getString("name")
                if (name.startsWith("canary-") && name.endsWith(".txt")) {
                    files.add(name)
                }
            }
            files.sortedDescending().take(count)
        }
        result
    }.getOrElse { emptyList() }

    private fun getExistingFileSha(owner: String, repo: String, path: String): String? = runCatching {
        val url = "$baseUrl/repos/$owner/$repo/contents/$path"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                JSONObject(response.body?.string() ?: "{}").optString("sha", null)
            } else null
        }
    }.getOrNull()

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
