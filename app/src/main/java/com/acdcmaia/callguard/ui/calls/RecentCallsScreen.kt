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
import com.acdcmaia.callguard.CallGuardApp
import com.acdcmaia.callguard.data.CallHistoryItem
import com.acdcmaia.callguard.data.db.BlockReason
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentCallsScreen() {
    val context = LocalContext.current
    val vm: RecentCallsViewModel = viewModel(
        factory = RecentCallsViewModel.factory(context.applicationContext as CallGuardApp)
    )
    val calls by vm.calls.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) vm.loadHistory() }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG)
                == PackageManager.PERMISSION_GRANTED
            ) {
                vm.loadHistory()
            } else {
                permissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Chamadas") })
        if (calls.isEmpty()) {
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

@Composable
private fun CallHistoryRow(item: CallHistoryItem) {
    val fmt = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())

    val typeLabel = when (item.callType) {
        CallLog.Calls.INCOMING_TYPE -> "Recebida"
        CallLog.Calls.OUTGOING_TYPE -> "Efetuada"
        CallLog.Calls.MISSED_TYPE -> "Perdida"
        else -> "Chamada"
    }

    val statusLabel = when {
        item.blockReason == BlockReason.BLACKLIST -> "Bloqueada (lista negra): ${item.matchedPatternLabel}"
        item.blockReason == BlockReason.FIRST_CALL -> "Bloqueada (1ª chamada)"
        item.isBlacklisted -> "Lista negra: ${item.matchedPatternLabel}"
        else -> typeLabel
    }

    val isBlocked = item.blockReason != null || item.isBlacklisted
    val textColor = if (isBlocked) Color.Red else Color.Unspecified

    ListItem(
        headlineContent = {
            Text(item.number, color = textColor)
        },
        supportingContent = {
            Text(
                "$statusLabel · ${fmt.format(Date(item.timestamp))}",
                color = textColor,
                style = MaterialTheme.typography.bodySmall
            )
        },
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
