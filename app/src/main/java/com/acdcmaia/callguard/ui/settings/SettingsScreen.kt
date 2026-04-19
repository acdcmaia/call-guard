package com.acdcmaia.callguard.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acdcmaia.callguard.BuildConfig
import com.acdcmaia.callguard.callGuardApp
import com.acdcmaia.callguard.data.SettingsRepository

private val STEPS = listOf(1, 5, 10, 30, 60, 120, 180, 300, 600, 1800, 3600)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val vm: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(context.callGuardApp)
    )
    val windowSeconds by vm.windowSeconds.collectAsState(initial = SettingsRepository.DEFAULT_WINDOW_SECONDS)
    var showAbout by remember { mutableStateOf(false) }

    fun decrease() {
        val next = STEPS.lastOrNull { it < windowSeconds } ?: STEPS.first()
        vm.setWindowSeconds(next)
    }

    fun increase() {
        val next = STEPS.firstOrNull { it > windowSeconds } ?: STEPS.last()
        vm.setWindowSeconds(next)
    }

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
            Text("Janela de tempo", style = MaterialTheme.typography.bodyLarge)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FilledTonalIconButton(
                    onClick = ::decrease,
                    enabled = windowSeconds > STEPS.first()
                ) {
                    Text("−", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = formatSeconds(windowSeconds),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.widthIn(min = 80.dp),
                )
                FilledTonalIconButton(
                    onClick = ::increase,
                    enabled = windowSeconds < STEPS.last()
                ) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
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

private fun formatSeconds(secs: Int): String = when {
    secs < 60  -> "$secs s"
    secs < 3600 -> "${secs / 60} min"
    else        -> "${secs / 3600} h"
}
