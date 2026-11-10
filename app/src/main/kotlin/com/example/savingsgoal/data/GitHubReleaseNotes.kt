package com.example.savingsgoal.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

@Serializable
internal data class ReleaseNote(
    @SerialName("tag_name") val tag: String,
    val name: String? = null,
    val body: String? = null,
    @SerialName("html_url") val url: String
) {
    val title: String get() = name?.takeIf { it.isNotBlank() } ?: tag
}

internal object GitHubReleaseNotes {
    private const val RELEASES_API = "https://api.github.com/repos/Minta1234/SavingGoalPlus/releases?per_page=10"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(): List<ReleaseNote> = withContext(Dispatchers.IO) {
        val connection = URL(RELEASES_API).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "SavingGoalPlus")
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("GitHub returned HTTP ${connection.responseCode}")
            }
            val payload = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            json.decodeFromString<List<ReleaseNote>>(payload)
        } finally {
            connection.disconnect()
        }
    }
}
