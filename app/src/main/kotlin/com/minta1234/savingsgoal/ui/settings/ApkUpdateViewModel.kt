package com.minta1234.savingsgoal.ui.settings

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.minta1234.savingsgoal.BuildConfig
import com.minta1234.savingsgoal.data.GitHubReleaseNotes
import com.minta1234.savingsgoal.data.ReleaseAsset
import com.minta1234.savingsgoal.data.ReleaseNote
import com.minta1234.savingsgoal.data.compareReleaseVersions
import com.minta1234.savingsgoal.data.isUpdateDownloadUrl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

internal data class ApkUpdateState(
    val release: ReleaseNote? = null,
    val checking: Boolean = true,
    val checkFailed: Boolean = false,
    val downloading: Boolean = false,
    val downloaded: Long = 0,
    val total: Long = 0,
    val apk: File? = null,
    val downloadFailed: Boolean = false
)

/** Navigation-scoped so rotation or temporarily opening Android settings keeps the APK. */
internal class ApkUpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val mutableState = MutableStateFlow(ApkUpdateState())
    val state = mutableState.asStateFlow()
    private var downloadJob: Job? = null
    private var checkJob: Job? = null

    init { check() }

    fun check() {
        if (checkJob?.isActive == true || mutableState.value.downloading) return
        checkJob = viewModelScope.launch {
            mutableState.update { it.copy(checking = true, checkFailed = false) }
            try {
                val release = GitHubReleaseNotes.latest()
                mutableState.update { it.copy(release = release, checking = false) }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                android.util.Log.w("ApkUpdate", "Update check failed", error)
                mutableState.update { it.copy(checking = false, checkFailed = true) }
            }
        }
    }

    fun download() {
        if (downloadJob?.isActive == true) return
        val release = mutableState.value.release ?: return
        val asset = release.androidApk ?: return
        if ((compareReleaseVersions(BuildConfig.VERSION_NAME, release.tag) ?: 0) >= 0) return
        mutableState.update { it.copy(downloading = true, downloaded = 0, total = asset.size, downloadFailed = false, apk = null) }
        downloadJob = viewModelScope.launch {
            try {
                val file = downloadApk(asset, release.tag)
                mutableState.update { it.copy(downloading = false, apk = file) }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                android.util.Log.w("ApkUpdate", "APK download or validation failed", error)
                mutableState.update { it.copy(downloading = false, downloadFailed = true) }
            }
        }
    }

    fun clearDownload() {
        mutableState.update { it.copy(apk = null) }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        mutableState.update { it.copy(downloading = false, downloaded = 0, downloadFailed = false) }
    }

    private suspend fun downloadApk(asset: ReleaseAsset, tag: String): File = withContext(Dispatchers.IO) {
        val directory = File(getApplication<Application>().cacheDir, "updates").apply { mkdirs() }
        // Remove expired update downloads only; savings data lives elsewhere.
        directory.listFiles()?.filter { it.lastModified() < System.currentTimeMillis() - 24 * 60 * 60 * 1000L }
            ?.forEach { it.delete() }
        val file = File.createTempFile("update-", ".apk", directory)
        var success = false
        try {
            val connection = openDownload(asset.downloadUrl)
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                connection.inputStream.use { input ->
                    file.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var received = 0L
                        var lastProgress = -1L
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(buffer)
                            currentCoroutineContext().ensureActive()
                            if (count < 0) break
                            received += count
                            if (received > asset.size) throw IOException("APK exceeds expected size")
                            output.write(buffer, 0, count)
                            digest.update(buffer, 0, count)
                            val percent = received * 100 / asset.size
                            if (percent != lastProgress) {
                                lastProgress = percent
                                mutableState.update { it.copy(downloaded = received) }
                            }
                        }
                        if (received != asset.size) throw IOException("Incomplete APK")
                    }
                }
                asset.digest?.let { expected ->
                    val actual = digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
                    if (!expected.equals("sha256:$actual", ignoreCase = true)) throw IOException("APK checksum mismatch")
                }
            } finally { connection.disconnect() }
            currentCoroutineContext().ensureActive()
            validateApk(file, tag)
            success = true
            file
        } finally { if (!success) file.delete() }
    }

    @Suppress("DEPRECATION")
    private fun validateApk(file: File, tag: String) {
        val app = getApplication<Application>()
        val manager = app.packageManager
        val archive = manager.getPackageArchiveInfo(file.absolutePath, 0) ?: throw IOException("Invalid APK")
        val installed = manager.getPackageInfo(app.packageName, 0)
        val newCode = if (Build.VERSION.SDK_INT >= 28) archive.longVersionCode else archive.versionCode.toLong()
        val oldCode = if (Build.VERSION.SDK_INT >= 28) installed.longVersionCode else installed.versionCode.toLong()
        if (archive.packageName != app.packageName || newCode <= oldCode || compareReleaseVersions(archive.versionName.orEmpty(), tag) != 0) {
            throw IOException("APK does not match this app or release")
        }
        // Android's installer verifies the signing certificate and enforces update compatibility.
    }

    private fun openDownload(initialUrl: String): HttpURLConnection {
        var next = initialUrl
        repeat(6) {
            if (!isUpdateDownloadUrl(next)) throw IOException("Untrusted APK download URL")
            val connection = URL(next).openConnection() as HttpURLConnection
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                connection.setRequestProperty("User-Agent", "SavingGoalPlus")
                when (connection.responseCode) {
                    HttpURLConnection.HTTP_OK -> return connection
                    301, 302, 303, 307, 308 -> {
                        val location = connection.getHeaderField("Location") ?: throw IOException("Missing download redirect")
                        next = URL(URL(next), location).toString()
                    }
                    else -> throw IOException("APK download HTTP ${connection.responseCode}")
                }
            } catch (error: Exception) {
                connection.disconnect()
                throw error
            }
            connection.disconnect()
        }
        throw IOException("Too many download redirects")
    }
}
