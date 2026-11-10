package com.example.savingsgoal.ui.settings

import androidx.compose.ui.res.stringResource
import com.example.savingsgoal.R
import android.text.TextUtils
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.example.savingsgoal.data.GitHubReleaseNotes
import com.example.savingsgoal.data.ReleaseNote
import kotlinx.coroutines.CancellationException

@Composable
internal fun ReleaseNotesDialog(onDismiss: () -> Unit) {
    var releases by remember { mutableStateOf<List<ReleaseNote>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }

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
        title = { Text(stringResource(R.string.ui_change_log)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val notes = releases
                when {
                    failed -> {
                        Text(stringResource(R.string.ui_could_not_load_release_notes_from_github))
                        TextButton(onClick = { attempt++ }) { Text(stringResource(R.string.ui_try_again)) }
                    }
                    notes == null -> {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.ui_loading_release_notes_from_github))
                    }
                    notes.isEmpty() -> Text(stringResource(R.string.ui_no_published_releases_yet))
                    else -> ReleaseNotesContent(notes)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_close)) } }
    )
}


@Composable
private fun ReleaseNotesContent(releases: List<ReleaseNote>) {
    val colors = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current
    val emptyNote = stringResource(R.string.ui_no_notes_were_published_for_this_release)
    val viewOnGitHub = stringResource(R.string.ui_view_on_github)
    val document = remember(releases, colors, emptyNote, viewOnGitHub) {
        releaseDocument(releases, colors, emptyNote, viewOnGitHub)
    }
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(420.dp),
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                settings.javaScriptEnabled = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.blockNetworkLoads = true
            }
        },
        onRelease = { it.destroy() },
        update = { webView ->
            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    if (request.isForMainFrame && request.hasGesture() && request.url.scheme == "https") {
                        runCatching { uriHandler.openUri(request.url.toString()) }
                            .onFailure { Log.w("ReleaseNotes", "Could not open release link", it) }
                    }
                    return true
                }
            }
            if (webView.tag != document) {
                webView.tag = document
                webView.loadDataWithBaseURL(null, document, "text/html", "UTF-8", null)
            }
        }
    )
}

private fun releaseDocument(
    releases: List<ReleaseNote>,
    colors: androidx.compose.material3.ColorScheme,
    emptyNote: String,
    viewOnGitHub: String
): String = buildString {
    append("""
        <!doctype html><html><head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'">
        <style>
          body { margin: 0; font: 16px/1.55 system-ui, sans-serif; color: ${colors.onSurface.css()}; overflow-wrap: anywhere; }
          article + article { border-top: 1px solid ${colors.outlineVariant.css()}; margin-top: 24px; padding-top: 16px; }
          h1, h2 { border-bottom: 1px solid ${colors.outlineVariant.css()}; padding-bottom: .3em; }
          h1 { font-size: 1.4em; } h2 { font-size: 1.2em; } h3 { font-size: 1.1em; }
          a { color: ${colors.primary.css()}; }
          pre, :not(pre) > code { background: ${colors.surfaceVariant.css()}; border-radius: 6px; }
          pre { padding: 12px; overflow-x: auto; } :not(pre) > code { padding: .15em .3em; }
          pre, code, tt { font-family: monospace; font-size: .875em; }
          blockquote { margin-left: 0; padding-left: 12px; border-left: 3px solid ${colors.outlineVariant.css()}; }
          table { display: block; overflow-x: auto; border-collapse: collapse; }
          td, th { border: 1px solid ${colors.outlineVariant.css()}; padding: 6px 10px; }
        </style></head><body>
    """.trimIndent())
    releases.forEach { release ->
        append("<article><h1>${TextUtils.htmlEncode(release.title)}</h1>")
        append(release.bodyHtml?.takeIf { it.isNotBlank() } ?: "<p>${TextUtils.htmlEncode(emptyNote)}</p>")
        if (release.isGitHubReleaseLink) {
            append("<p><a href=\"${TextUtils.htmlEncode(release.url)}\">${TextUtils.htmlEncode(viewOnGitHub)}</a></p>")
        }
        append("</article>")
    }
    append("</body></html>")
}

private fun Color.css(): String = "#%06x".format(toArgb() and 0xFFFFFF)
