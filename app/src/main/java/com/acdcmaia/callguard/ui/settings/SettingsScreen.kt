package com.acdcmaia.callguard.ui.settings

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
    var input by remember(windowSeconds) { mutableStateOf(windowSeconds.toString()) }
    val secs = input.toIntOrNull()
    val isValid = secs != null && secs in 1..86400
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
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Janela de tempo (segundos)", style = MaterialTheme.typography.bodyLarge)
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.filter { c -> c.isDigit() } },
                label = { Text("Segundos") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = !isValid && input.isNotEmpty(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { if (isValid) vm.setWindowSeconds(secs!!) },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salvar")
            }
        }
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
