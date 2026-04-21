package com.acdcmaia.callguard.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.window.PopupPositionProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acdcmaia.callguard.BuildConfig
import com.acdcmaia.callguard.callGuardApp
import com.acdcmaia.callguard.data.SettingsRepository
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val vm: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(context.callGuardApp)
    )
    val windowSeconds by vm.windowSeconds.collectAsStateWithLifecycle(initialValue = SettingsRepository.DEFAULT_WINDOW_SECONDS)
    val updateStatus by vm.updateStatus.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.checkForUpdates() }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Configurações") })
        ListItem(
            headlineContent = { Text("Janela de tempo") },
            supportingContent = { Text(if (windowSeconds == 1) "1 segundo" else "$windowSeconds segundos") },
            modifier = Modifier.clickable { showDialog = true }
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text("Sobre") },
            leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
            modifier = Modifier.clickable { showAbout = true }
        )
        ListItem(
            headlineContent = {
                val (text, color) = when (updateStatus) {
                    UpdateStatus.CHECKING -> "Verificando atualizações…" to Color.Unspecified
                    UpdateStatus.UP_TO_DATE -> "Sem atualizações a fazer" to Color.Unspecified
                    UpdateStatus.UPDATE_AVAILABLE -> "Atualizações disponíveis!" to Color.Red
                    UpdateStatus.ERROR -> "Não foi possível verificar atualizações" to Color.Unspecified
                }
                Text(text, color = color)
            },
            leadingContent = { Icon(Icons.Default.Refresh, contentDescription = null) },
            modifier = Modifier.clickable {
                uriHandler.openUri("https://github.com/acdcmaia/call-guard/releases")
            }
        )
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
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    val aboveAnchor = remember {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val x = maxOf(0, minOf(anchorBounds.left, windowSize.width - popupContentSize.width))
                val y = maxOf(0, anchorBounds.top - popupContentSize.height)
                return IntOffset(x, y)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Janela de tempo") },
        text = {
            TooltipBox(
                positionProvider = aboveAnchor,
                tooltip = {
                    RichTooltip {
                        Text("Chamadas do mesmo número repetidas dentro deste período serão encaminhadas.")
                    }
                },
                state = tooltipState
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter { c -> c.isDigit() } },
                    label = { Text("Segundos") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = !isValid && input.isNotEmpty(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusable()
                        .onFocusChanged { if (it.isFocused) scope.launch { tooltipState.show() } }
                )
            }
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
