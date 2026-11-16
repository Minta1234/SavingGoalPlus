package com.minta1234.savingsgoal.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.minta1234.savingsgoal.R
import com.minta1234.savingsgoal.BuildConfig
import com.minta1234.savingsgoal.ui.settings.UpdateCheckDialog

@Composable
fun FunctionMenu(onHome: () -> Unit, onTable: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var showUpdate by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.table_menu))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.table_home)) }, onClick = { expanded = false; onHome() })
            DropdownMenuItem(text = { Text(stringResource(R.string.table_title)) }, onClick = { expanded = false; onTable() })
            DropdownMenuItem(text = { Text(stringResource(R.string.update_title)) }, onClick = { expanded = false; showUpdate = true })
        }
    }
    if (showUpdate) UpdateCheckDialog(installedVersion = BuildConfig.VERSION_NAME, onDismiss = { showUpdate = false })
}
