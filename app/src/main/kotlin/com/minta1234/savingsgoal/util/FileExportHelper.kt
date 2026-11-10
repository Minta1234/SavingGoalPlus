package com.minta1234.savingsgoal.util

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import com.minta1234.savingsgoal.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

@Composable
fun rememberFileExport(
    mimeType: String,
    fileName: String,
    content: suspend () -> String,
    onResult: suspend (String) -> Unit
): () -> Unit {
    val resolver = LocalContext.current.contentResolver
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(mimeType)) { uri ->
        if (uri != null) {
            scope.launch {
                val message = try {
                    withContext(Dispatchers.IO) {
                        val bytes = content().toByteArray(Charsets.UTF_8)
                        val stream = resolver.openOutputStream(uri, "wt")
                            ?: throw IOException("Cannot open the selected file")
                        stream.use { it.write(bytes) }
                    }
                    resources.getString(R.string.file_saved)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    android.util.Log.e("FileExport", "Could not save export", e)
                    resources.getString(R.string.file_save_failed)
                }
                onResult(message)
            }
        }
    }
    return {
        try {
            launcher.launch(fileName)
        } catch (e: android.content.ActivityNotFoundException) {
            scope.launch { onResult(resources.getString(R.string.file_picker_unavailable)) }
        }
    }
}
