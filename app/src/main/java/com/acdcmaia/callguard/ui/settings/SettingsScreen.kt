package com.acdcmaia.callguard.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acdcmaia.callguard.BuildConfig
import com.acdcmaia.callguard.callGuardApp
import com.acdcmaia.callguard.data.SettingsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val vm: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(context.callGuardApp)
    )
    val windowSeconds by vm.windowSeconds.collectAsState(initial = SettingsRepository.DEFAULT_WINDOW_SECONDS)
    var showDialog by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Configurações") },
            actions = {
                IconButton(onClick = { showAbout = true }) {
                    Icon(Icons.Default.Info, contentDescription = "Sobre")
                }
            }
        )
        ListItem(
            headlineContent = { Text("Janela de tempo") },
            supportingContent = { Text(if (windowSeconds == 1) "1 segundo" else "$windowSeconds segundos") },
            modifier = Modifier.clickable { showDialog = true }
        )
        HorizontalDivider()
    }

    if (showDialog) {
        WindowSecondsDialog(
            current = windowSeconds,
            onDismiss = { showDialog = false },
            onConfirm = { secs ->
                vm.setWindowSeconds(secs)
                showDialog = false
            }
        )
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("Sobre") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Call Guard")
                    Text("Versão: ${BuildConfig.VERSION_NAME}")
                    Text("Build: ${BuildConfig.BUILD_DATE}")
                    Text("Desenvolvedor: ${BuildConfig.DEVELOPER}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text("Fechar") }
            }
        )
    }
}

@Composable
private fun WindowSecondsDialog(
    current: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var input by remember { mutableStateOf(current.toString()) }
    val secs = input.toIntOrNull()
    val isValid = secs != null && secs in 1..86400

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Janela de tempo") },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.filter { c -> c.isDigit() } },
                label = { Text("Segundos") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = !isValid && input.isNotEmpty(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { if (isValid) onConfirm(secs!!) }, enabled = isValid) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
