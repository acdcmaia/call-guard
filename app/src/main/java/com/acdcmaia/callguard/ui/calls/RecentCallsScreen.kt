package com.acdcmaia.callguard.ui.calls

import android.provider.CallLog
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acdcmaia.callguard.callGuardApp
import com.acdcmaia.callguard.data.CallHistoryItem
import com.acdcmaia.callguard.data.db.BlockReason
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RecentCallsScreen() {
    val context = LocalContext.current
    val vm: RecentCallsViewModel = viewModel(
        factory = RecentCallsViewModel.factory(context.callGuardApp)
    )
    val calls by vm.calls.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            vm.loadHistory()
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

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm")

@Composable
private fun CallHistoryRow(item: CallHistoryItem) {
    val formattedDate = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(item.timestamp), ZoneId.systemDefault()
    ).format(dateFormatter)

    val typeLabel = when (item.callType) {
        CallLog.Calls.INCOMING_TYPE -> "Liberada"
        CallLog.Calls.OUTGOING_TYPE -> "Efetuada"
        CallLog.Calls.MISSED_TYPE -> "Perdida"
        else -> "Liberada"
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
