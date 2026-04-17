package com.acdcmaia.callguard.ui.calls

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acdcmaia.callguard.data.db.BlockReason
import com.acdcmaia.callguard.data.db.RecentCall
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecentCallsScreen(vm: RecentCallsViewModel = viewModel()) {
    val calls by vm.calls.collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Chamadas Recentes") })
        if (calls.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Nenhuma chamada registrada")
            }
        } else {
            LazyColumn {
                items(calls) { call ->
                    CallItem(call)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun CallItem(call: RecentCall) {
    val fmt = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
    val status = when {
        call.allowed -> "Permitida"
        call.blockReason == BlockReason.BLACKLIST -> "Bloqueada (lista negra)"
        call.blockReason == BlockReason.FIRST_CALL -> "Bloqueada (primeira chamada)"
        else -> "Bloqueada"
    }
    ListItem(
        headlineContent = { Text(call.number) },
        supportingContent = { Text("$status · ${fmt.format(Date(call.timestamp))}") }
    )
}
