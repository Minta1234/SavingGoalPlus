package com.example.savingsgoal.ui.settings

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.example.savingsgoal.data.GitHubReleaseNotes
import com.example.savingsgoal.data.ReleaseNote
import com.example.savingsgoal.data.compareReleaseVersions
import kotlinx.coroutines.CancellationException

@Composable
internal fun UpdateCheckDialog(installedVersion: String, onDismiss: () -> Unit) {
    var latest by remember { mutableStateOf<ReleaseNote?>(null) }
    var failed by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(attempt) {
        failed = false
        try {
            latest = GitHubReleaseNotes.latest()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w("UpdateCheck", "Could not check for updates", error)
            failed = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Check for Updates") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Installed: $installedVersion")
                val release = latest
                when {
                    failed -> {
                        Text("Could not check GitHub for updates.")
                        TextButton(onClick = { attempt++ }) { Text("Try again") }
                    }
                    release == null -> {
                        CircularProgressIndicator()
                        Text("Checking GitHub…")
                    }
                    else -> {
                        Text("Latest release: ${release.tag}")
                        val comparison = compareReleaseVersions(installedVersion, release.tag)
                        Text(
                            when {
                                comparison == null -> "Could not compare the version numbers."
                                comparison < 0 -> "An update is available."
                                comparison == 0 -> "Already up to date."
                                else -> "The installed version is newer than the latest release."
                            }
                        )
                        if (release.isGitHubReleaseLink) {
                            TextButton(onClick = { uriHandler.openUri(release.url) }) { Text("Open GitHub Release") }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
