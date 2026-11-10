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
    @SerialName("body_html") val bodyHtml: String? = null,
    @SerialName("html_url") val url: String
) {
    val title: String get() = name?.takeIf { it.isNotBlank() } ?: tag
    val isGitHubReleaseLink: Boolean
        get() = url.startsWith("https://github.com/Minta1234/SavingGoalPlus/releases/tag/")
}

internal object GitHubReleaseNotes {
    private const val RELEASES_API = "https://api.github.com/repos/Minta1234/SavingGoalPlus/releases"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(): List<ReleaseNote> = json.decodeFromString(request("$RELEASES_API?per_page=10"))

    suspend fun latest(): ReleaseNote = json.decodeFromString(request("$RELEASES_API/latest"))

    private suspend fun request(url: String): String = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/vnd.github.html+json")
            connection.setRequestProperty("User-Agent", "SavingGoalPlus")
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("GitHub returned HTTP ${connection.responseCode}")
            }
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}


internal fun compareReleaseVersions(installed: String, published: String): Int? {
    fun parts(version: String): List<Int>? {
        val components = version.removePrefix("v").split('.')
        if (components.size != 3 || components.any { it.isEmpty() || it.any { char -> char !in '0'..'9' } }) {
            return null
        }
        return components.map { it.toIntOrNull() ?: return null }
    }

    val current = parts(installed) ?: return null
    val latest = parts(published) ?: return null
    for (index in current.indices) {
        val comparison = current[index].compareTo(latest[index])
        if (comparison != 0) return comparison
    }
    return 0
}
