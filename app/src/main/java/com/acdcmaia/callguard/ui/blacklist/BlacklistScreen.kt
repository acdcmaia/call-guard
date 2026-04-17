package com.acdcmaia.callguard.ui.blacklist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acdcmaia.callguard.data.db.BlacklistPattern

@Composable
fun BlacklistScreen(vm: BlacklistViewModel = viewModel()) {
    val patterns by vm.patterns.collectAsState(initial = emptyList())
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Lista Negra") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar")
            }
        }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            items(patterns) { p ->
                PatternItem(p, onDelete = { vm.deletePattern(p) })
                HorizontalDivider()
            }
        }
    }

    if (showDialog) {
        AddPatternDialog(
            onDismiss = { showDialog = false },
            onConfirm = { pattern, label ->
                vm.addPattern(pattern, label)
                showDialog = false
            },
            isValid = vm::isValidRegex
        )
    }
}

@Composable
private fun PatternItem(pattern: BlacklistPattern, onDelete: () -> Unit) {
    ListItem(
        headlineContent = { Text(pattern.pattern) },
        supportingContent = { if (pattern.label.isNotBlank()) Text(pattern.label) },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Remover")
            }
        }
    )
}

@Composable
private fun AddPatternDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    isValid: (String) -> Boolean
) {
    var pattern by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    val valid = pattern.isNotBlank() && isValid(pattern)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Padrão") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text("Regex") },
                    isError = pattern.isNotBlank() && !isValid(pattern),
                    supportingText = {
                        if (pattern.isNotBlank() && !isValid(pattern))
                            Text("Regex inválida")
                    }
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Descrição (opcional)") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pattern, label) }, enabled = valid) {
                Text("Adicionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
