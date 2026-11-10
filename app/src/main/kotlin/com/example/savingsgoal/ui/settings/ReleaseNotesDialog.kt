package com.example.savingsgoal.ui.settings

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.example.savingsgoal.data.GitHubReleaseNotes
import com.example.savingsgoal.data.ReleaseNote
import kotlinx.coroutines.CancellationException

@Composable
internal fun ReleaseNotesDialog(onDismiss: () -> Unit) {
    var releases by remember { mutableStateOf<List<ReleaseNote>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(attempt) {
        failed = false
        try {
            releases = GitHubReleaseNotes.load()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w("ReleaseNotes", "Could not load release notes", error)
            failed = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Log") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val notes = releases
                when {
                    failed -> {
                        Text("Could not load release notes from GitHub.")
                        TextButton(onClick = { attempt++ }) { Text("Try again") }
                    }
                    notes == null -> {
                        CircularProgressIndicator()
                        Text("Loading release notes from GitHub…")
                    }
                    notes.isEmpty() -> Text("No published releases yet.")
                    else -> notes.forEachIndexed { index, release ->
                        if (index > 0) HorizontalDivider()
                        Text(release.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            release.body?.takeIf { it.isNotBlank() } ?: "No notes were published for this release.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (release.url.startsWith("https://github.com/Minta1234/SavingGoalPlus/releases/tag/")) {
                            TextButton(onClick = { uriHandler.openUri(release.url) }) {
                                Text("View on GitHub")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
