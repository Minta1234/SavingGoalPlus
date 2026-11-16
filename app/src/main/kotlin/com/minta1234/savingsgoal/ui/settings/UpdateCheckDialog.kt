package com.minta1234.savingsgoal.ui.settings

import android.app.Application
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minta1234.savingsgoal.R
import com.minta1234.savingsgoal.data.compareReleaseVersions

@Composable
internal fun UpdateCheckDialog(installedVersion: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val updater: ApkUpdateViewModel = viewModel(factory = AndroidViewModelFactory.getInstance(application))
    val state by updater.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    var installFailed by remember { mutableStateOf(false) }
    var permissionNeeded by remember { mutableStateOf(false) }
    var installerOpened by remember { mutableStateOf(false) }
    var installAfterDownload by rememberSaveable { mutableStateOf(false) }
    val installer = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { /* The system installer reports its own result. */ }

    fun install() {
        val file = state.apk ?: return
        installFailed = false
        permissionNeeded = false
        try {
            check(file.isFile)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newRawUri("App update", uri)
            }
            installer.launch(intent)
            installerOpened = true
        } catch (error: Exception) {
            android.util.Log.w("ApkUpdate", "Could not open installer", error)
            installFailed = true
            if (!file.isFile) updater.clearDownload()
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (context.packageManager.canRequestPackageInstalls()) install()
        else permissionNeeded = true
    }
    fun requestInstall() {
        if (context.packageManager.canRequestPackageInstalls()) install()
        else {
            try {
                permission.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
            } catch (_: Exception) { permissionNeeded = true }
        }
    }
    LaunchedEffect(state.apk, state.downloadFailed) {
        if (state.downloadFailed) installAfterDownload = false
        else if (state.apk != null && installAfterDownload) {
            installAfterDownload = false
            requestInstall()
        }
    }
    val dismiss = {
        installAfterDownload = false
        if (state.downloading) updater.cancelDownload()
        onDismiss()
    }
    val release = state.release
    val available = release != null && (compareReleaseVersions(installedVersion, release.tag) ?: 0) < 0

    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(stringResource(R.string.update_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.installed_version_value, installedVersion))
                when {
                    state.checking -> {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.ui_checking_github))
                    }
                    state.checkFailed -> {
                        Text(stringResource(R.string.ui_could_not_check_github_for_updates))
                        TextButton(onClick = { updater.check() }) { Text(stringResource(R.string.ui_try_again)) }
                    }
                    release != null -> {
                        Text(stringResource(R.string.latest_release_value, release.tag))
                        val comparison = compareReleaseVersions(installedVersion, release.tag)
                        Text(when {
                            comparison == null -> stringResource(R.string.ui_could_not_compare_the_version_numbers)
                            comparison < 0 -> stringResource(R.string.ui_an_update_is_available)
                            comparison == 0 -> stringResource(R.string.ui_already_up_to_date)
                            else -> stringResource(R.string.ui_the_installed_version_is_newer_than_the_latest_release)
                        })
                        if (available) {
                            Text(stringResource(R.string.update_explanation))
                            if (release.androidApk == null) Text(stringResource(R.string.update_no_apk))
                        }
                        if (state.downloading) {
                            LinearProgressIndicator(progress = { (state.downloaded.toFloat() / state.total.coerceAtLeast(1)).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                            Text(stringResource(R.string.update_downloading, (state.downloaded * 100 / state.total.coerceAtLeast(1)).toInt()))
                        }
                        if (state.downloadFailed) Text(stringResource(R.string.update_download_failed), color = MaterialTheme.colorScheme.error)
                        if (state.apk != null) Text(stringResource(R.string.update_ready))
                        if (permissionNeeded) Text(stringResource(R.string.update_permission), color = MaterialTheme.colorScheme.error)
                        if (installFailed) Text(stringResource(R.string.update_install_failed), color = MaterialTheme.colorScheme.error)
                        if (installerOpened) Text(stringResource(R.string.update_installer_opened))
                        if (release.isGitHubReleaseLink) {
                            TextButton(onClick = { runCatching { uriHandler.openUri(release.url) } }) { Text(stringResource(R.string.ui_open_github_release)) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!state.checking && !state.checkFailed && available && release?.androidApk != null && !state.downloading) {
                Button(onClick = {
                    installFailed = false
                    if (state.apk != null) requestInstall()
                    else { installAfterDownload = true; updater.download() }
                }) {
                    Text(stringResource(if (state.apk != null) R.string.update_install else R.string.update_download_install))
                }
            }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text(stringResource(if (state.downloading) R.string.ui_cancel else R.string.ui_close)) } }
    )
}
