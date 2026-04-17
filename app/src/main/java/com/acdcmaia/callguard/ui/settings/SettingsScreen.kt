package com.acdcmaia.callguard.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SettingsScreen(vm: SettingsViewModel = viewModel()) {
    val windowMinutes by vm.windowMinutes.collectAsState(initial = 5)
    var sliderValue by remember(windowMinutes) { mutableFloatStateOf(windowMinutes.toFloat()) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Configurações") })
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Janela de tempo: ${sliderValue.toInt()} minuto(s)",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                "Uma chamada do mesmo número dentro desse período será encaminhada.",
                style = MaterialTheme.typography.bodySmall
            )
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { vm.setWindowMinutes(sliderValue.toInt()) },
                valueRange = 1f..60f,
                steps = 58
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("1 min", style = MaterialTheme.typography.bodySmall)
                Text("60 min", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
