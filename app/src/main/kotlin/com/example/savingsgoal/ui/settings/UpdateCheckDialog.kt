package com.example.savingsgoal.ui.settings

import androidx.compose.ui.res.stringResource
import com.example.savingsgoal.R
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
        title = { Text(stringResource(R.string.ui_check_for_updates)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.installed_version_value, installedVersion))
                val release = latest
                when {
                    failed -> {
                        Text(stringResource(R.string.ui_could_not_check_github_for_updates))
                        TextButton(onClick = { attempt++ }) { Text(stringResource(R.string.ui_try_again)) }
                    }
                    release == null -> {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.ui_checking_github))
                    }
                    else -> {
                        Text(stringResource(R.string.latest_release_value, release.tag))
                        val comparison = compareReleaseVersions(installedVersion, release.tag)
                        Text(
                            when {
                                comparison == null -> stringResource(R.string.ui_could_not_compare_the_version_numbers)
                                comparison < 0 -> stringResource(R.string.ui_an_update_is_available)
                                comparison == 0 -> stringResource(R.string.ui_already_up_to_date)
                                else -> stringResource(R.string.ui_the_installed_version_is_newer_than_the_latest_release)
                            }
                        )
                        if (release.isGitHubReleaseLink) {
                            TextButton(onClick = { uriHandler.openUri(release.url) }) { Text(stringResource(R.string.ui_open_github_release)) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_close)) } }
    )
}
