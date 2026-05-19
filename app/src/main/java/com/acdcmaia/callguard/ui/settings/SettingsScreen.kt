package com.acdcmaia.callguard.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.ui.Alignment
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
import androidx.compose.material3.LocalContentColor
import android.widget.Toast
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.window.PopupPositionProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acdcmaia.callguard.AutoStartHelper
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
    val downloadProgress by vm.downloadProgress.collectAsStateWithLifecycle()
    val apkUri by vm.apkUri.collectAsStateWithLifecycle()
    val autostartConfigured by vm.autostartConfigured.collectAsStateWithLifecycle(initialValue = true)
    val serviceEnabled by vm.serviceEnabled.collectAsStateWithLifecycle(initialValue = true)
    val autostartSupported = remember { AutoStartHelper.canOpen(context) }
    var showDialog by remember { mutableStateOf(false) }
    val windowHelpTooltipState = rememberTooltipState(isPersistent = true)
    val windowHelpScope = rememberCoroutineScope()
    var showAbout by remember { mutableStateOf(false) }
    var showPixInfo by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        vm.checkForUpdates()
        showPixInfo = false
    }

    LaunchedEffect(apkUri) {
        apkUri?.let { uri ->
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            vm.onInstallTriggered()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Configurações") })
        ListItem(
            headlineContent = { Text("Habilitar Call Guard") },
            trailingContent = {
                Switch(
                    checked = serviceEnabled,
                    onCheckedChange = { vm.setServiceEnabled(it) }
                )
            },
            modifier = Modifier.clickable { vm.setServiceEnabled(!serviceEnabled) }
        )
        HorizontalDivider()
        ListItem(
            headlineContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Janela de tempo: ${if (windowSeconds == 1) "1 segundo" else "$windowSeconds segundos"}")
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                        tooltip = {
                            RichTooltip {
                                Text("Chamadas do mesmo número repetidas dentro deste período serão encaminhadas.")
                            }
                        },
                        state = windowHelpTooltipState
                    ) {
                        IconButton(onClick = { windowHelpScope.launch { windowHelpTooltipState.show() } }) {
                            Icon(
                                Icons.AutoMirrored.Outlined.HelpOutline,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            },
            modifier = Modifier.clickable { showDialog = true }
        )
        HorizontalDivider()
        if (autostartSupported) {
            val autostartColor = if (autostartConfigured) Color.Unspecified else Color.Red
            ListItem(
                headlineContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Início automático do aplicativo", color = autostartColor)
                        IconButton(onClick = {
                            AutoStartHelper.open(context)
                            vm.markAutostartConfigured()
                        }) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                tint = if (autostartConfigured) LocalContentColor.current else Color.Red,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                modifier = Modifier.clickable {
                    AutoStartHelper.open(context)
                    vm.markAutostartConfigured()
                }
            )
            HorizontalDivider()
        }
        val isDownloading = updateStatus == UpdateStatus.DOWNLOADING || updateStatus == UpdateStatus.READY_TO_INSTALL
        ListItem(
            headlineContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f, fill = false)) {
                        when (updateStatus) {
                            UpdateStatus.CHECKING -> Text("Verificando atualizações…")
                            UpdateStatus.UP_TO_DATE -> Text("Sem atualizações a fazer")
                            UpdateStatus.UPDATE_AVAILABLE -> Text("Atualização disponível!", color = Color.Red)
                            UpdateStatus.ERROR -> Text("Não foi possível verificar atualizações")
                            UpdateStatus.DOWNLOADING -> Column {
                                Text("Baixando atualização… $downloadProgress%")
                                LinearProgressIndicator(
                                    progress = { downloadProgress / 100f },
                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                                )
                            }
                            UpdateStatus.READY_TO_INSTALL -> Text("Abrindo instalador…")
                            UpdateStatus.DOWNLOAD_ERROR -> Text("Erro ao baixar atualização", color = Color.Red)
                        }
                    }
                    IconButton(
                        onClick = {
                            when (updateStatus) {
                                UpdateStatus.UPDATE_AVAILABLE, UpdateStatus.DOWNLOAD_ERROR -> {
                                    if (!context.packageManager.canRequestPackageInstalls()) {
                                        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    } else {
                                        vm.downloadUpdate(context)
                                    }
                                }
                                UpdateStatus.UP_TO_DATE -> vm.checkForUpdates()
                                else -> uriHandler.openUri("https://github.com/acdcmaia/call-guard/releases")
                            }
                        },
                        enabled = !isDownloading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(24.dp))
                    }
                }
            },
            modifier = Modifier.clickable(enabled = !isDownloading) {
                when (updateStatus) {
                    UpdateStatus.UPDATE_AVAILABLE, UpdateStatus.DOWNLOAD_ERROR -> {
                        if (!context.packageManager.canRequestPackageInstalls()) {
                            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        } else {
                            vm.downloadUpdate(context)
                        }
                    }
                    else -> uriHandler.openUri("https://github.com/acdcmaia/call-guard/releases")
                }
            }
        )
        HorizontalDivider()
        ListItem(
            headlineContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sobre")
                    IconButton(onClick = { showAbout = true }) {
                        Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(24.dp))
                    }
                }
            },
            modifier = Modifier.clickable { showAbout = true }
        )
        ListItem(
            headlineContent = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Gostou")
                        IconButton(onClick = { showPixInfo = !showPixInfo }) {
                            Icon(
                                Icons.AutoMirrored.Outlined.HelpOutline,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    if (showPixInfo) {
                        Column(modifier = Modifier.padding(start = 16.dp)) {
                            Text("Pague-me um café... 😊")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Pix: pix2maia@gmail.com",
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                IconButton(onClick = {
                                    clipboardManager.setText(AnnotatedString("pix2maia@gmail.com"))
                                    Toast.makeText(context, "Copiado!", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            modifier = Modifier.clickable { showPixInfo = !showPixInfo }
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
            TextButton(onClick = { onConfirm(secs ?: return@TextButton) }, enabled = isValid) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
