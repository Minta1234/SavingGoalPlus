package com.example.savingsgoal.ui.components

import androidx.compose.ui.res.stringResource
import com.example.savingsgoal.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun ColorChoiceDialog(
    title: String,
    initialColor: Int,
    onDismiss: () -> Unit,
    onSave: (Int?) -> Unit
) {
    fun formatHex(rgb: Int): String = String.format(Locale.ROOT, "%06X", rgb and 0xFFFFFF)
    var hex by rememberSaveable { mutableStateOf(formatHex(initialColor)) }
    val parsed = hex.removePrefix("#").takeIf { it.length == 6 }?.toIntOrNull(16)
    val preview = parsed?.let { Color(it or 0xFF000000.toInt()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(8.dp))
                        .background(preview ?: Color(initialColor))
                )
                OutlinedTextField(
                    value = hex,
                    onValueChange = { input ->
                        if (input.length <= 7 && input.removePrefix("#").all { it in "0123456789abcdefABCDEF" }) hex = input
                    },
                    prefix = { if (!hex.startsWith("#")) Text("#") },
                    label = { Text(stringResource(R.string.ui_hex_color)) },
                    singleLine = true,
                    isError = parsed == null,
                    modifier = Modifier.fillMaxWidth()
                )
                val rgb = parsed ?: (initialColor and 0xFFFFFF)
                listOf(stringResource(R.string.ui_red), stringResource(R.string.ui_green), stringResource(R.string.ui_blue)).forEachIndexed { index, label ->
                    val shift = 16 - index * 8
                    Text(label)
                    Slider(
                        value = ((rgb shr shift) and 255).toFloat(),
                        onValueChange = { channel ->
                            hex = formatHex((rgb and (255 shl shift).inv()) or (channel.roundToInt() shl shift))
                        },
                        valueRange = 0f..255f,
                        modifier = Modifier.semantics { contentDescription = label }
                    )
                }
                TextButton(onClick = { onSave(null) }) { Text(stringResource(R.string.ui_use_default)) }
            }
        },
        confirmButton = {
            Button(enabled = parsed != null, onClick = { parsed?.let { onSave(it or 0xFF000000.toInt()) } }) {
                Text(stringResource(R.string.ui_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel)) } }
    )
}
