package com.acdcmaia.callguard.ui.settings

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acdcmaia.callguard.BuildConfig
import com.acdcmaia.callguard.callGuardApp
import com.acdcmaia.callguard.data.SettingsRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val vm: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(context.callGuardApp)
    )
    val windowSeconds by vm.windowSeconds.collectAsState(initial = SettingsRepository.DEFAULT_WINDOW_SECONDS)
    var secondsInput by remember { mutableStateOf(windowSeconds.toString()) }
    var fieldFocused by remember { mutableStateOf(false) }
    LaunchedEffect(windowSeconds) {
        if (!fieldFocused) secondsInput = windowSeconds.toString()
    }
    var showAbout by remember { mutableStateOf(false) }
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    val belowAnchor = remember {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val x = maxOf(0, minOf(anchorBounds.left, windowSize.width - popupContentSize.width))
                val y = anchorBounds.bottom
                return IntOffset(x, y)
            }
        }
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
            Text("Janela de tempo (segundos)", style = MaterialTheme.typography.bodyLarge)
            TooltipBox(
                positionProvider = belowAnchor,
                tooltip = {
                    RichTooltip {
                        Text("Chamadas do mesmo número repetidas dentro desse período serão encaminhadas.")
                    }
                },
                state = tooltipState
            ) {
                OutlinedTextField(
                    value = secondsInput,
                    onValueChange = { v ->
                        secondsInput = v.filter { it.isDigit() }
                    },
                    label = { Text("Segundos") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusable()
                        .onFocusChanged { focusState ->
                            fieldFocused = focusState.isFocused
                            if (focusState.isFocused) {
                                scope.launch { tooltipState.show() }
                            } else {
                                val secs = (secondsInput.toIntOrNull() ?: 1).coerceIn(1, 86400)
                                secondsInput = secs.toString()
                                vm.setWindowSeconds(secs)
                            }
                        }
                )
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
