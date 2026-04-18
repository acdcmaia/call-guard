package com.acdcmaia.callguard.ui.calls

import android.Manifest
import android.content.pm.PackageManager
import android.provider.CallLog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acdcmaia.callguard.callGuardApp
import com.acdcmaia.callguard.data.CallHistoryItem
import com.acdcmaia.callguard.data.db.BlockReason
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentCallsScreen() {
    val context = LocalContext.current
    val vm: RecentCallsViewModel = viewModel(
        factory = RecentCallsViewModel.factory(context.callGuardApp)
    )
    val calls by vm.calls.collectAsState()
    var permissionsDenied by remember { mutableStateOf(false) }

    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            permissionsDenied = false
            vm.loadHistory()
        } else {
            permissionsDenied = true
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val callLogGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_CALL_LOG
            ) == PackageManager.PERMISSION_GRANTED
            val contactsGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED

            if (callLogGranted && contactsGranted) {
                permissionsDenied = false
                vm.loadHistory()
            } else if (!permissionsDenied) {
                val toRequest = buildList {
                    if (!callLogGranted) add(Manifest.permission.READ_CALL_LOG)
                    if (!contactsGranted) add(Manifest.permission.READ_CONTACTS)
                }.toTypedArray()
                multiplePermissionsLauncher.launch(toRequest)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Chamadas") })
        if (permissionsDenied) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Permissões necessárias não foram concedidas. Acesse Configurações do dispositivo → Aplicativos → Call Guard → Permissões para habilitar o acesso ao histórico de chamadas e contatos.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else if (calls.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhuma chamada encontrada")
            }
        } else {
            LazyColumn {
                items(calls) { call ->
                    CallHistoryRow(call)
                    HorizontalDivider()
                }
            }
        }
    }
}

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm")

@Composable
private fun CallHistoryRow(item: CallHistoryItem) {
    val formattedDate = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(item.timestamp), ZoneId.systemDefault()
    ).format(dateFormatter)

    val typeLabel = when (item.callType) {
        CallLog.Calls.INCOMING_TYPE -> "Recebida"
        CallLog.Calls.OUTGOING_TYPE -> "Efetuada"
        CallLog.Calls.MISSED_TYPE -> "Perdida"
        else -> "Recebida"
    }

    val statusLabel = when (item.blockReason) {
        BlockReason.BLACKLIST -> "Bloqueada (lista negra): ${item.matchedPatternLabel}"
        BlockReason.FIRST_CALL -> "Bloqueada (1ª chamada)"
        null -> typeLabel
    }

    val isBlocked = item.blockReason != null
    val textColor = if (isBlocked) Color.Red else Color.Unspecified

    ListItem(
        headlineContent = {
            if (item.contactName != null) {
                Column {
                    Text(item.contactName, color = textColor)
                    Text(
                        item.number,
                        color = textColor,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                Text(item.number, color = textColor)
            }
        },
        supportingContent = {
            Text(
                "$statusLabel · $formattedDate",
                color = textColor,
                style = MaterialTheme.typography.bodySmall
            )
        },
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
