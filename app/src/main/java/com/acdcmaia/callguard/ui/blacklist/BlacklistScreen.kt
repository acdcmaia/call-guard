package com.acdcmaia.callguard.ui.blacklist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.acdcmaia.callguard.callGuardApp
import com.acdcmaia.callguard.data.db.BlacklistPattern
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlacklistScreen() {
    val context = LocalContext.current
    val vm: BlacklistViewModel = viewModel(
        factory = BlacklistViewModel.factory(context.callGuardApp)
    )
    val patterns by vm.patterns.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<BlacklistPattern?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Lista Negra") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar")
            }
        }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            items(patterns) { p ->
                PatternItem(
                    p,
                    onEdit = { editTarget = p },
                    onDelete = { vm.deletePattern(p) }
                )
                HorizontalDivider()
            }
        }
    }

    if (showAddDialog) {
        PatternDialog(
            title = "Novo Padrão",
            initialPattern = "",
            initialLabel = "",
            isValid = vm::isValidPattern,
            onDismiss = { showAddDialog = false },
            onConfirm = { pattern, label ->
                vm.addPattern(pattern, label)
                showAddDialog = false
            }
        )
    }

    editTarget?.let { target ->
        PatternDialog(
            title = "Editar Padrão",
            initialPattern = target.pattern,
            initialLabel = target.label,
            isValid = vm::isValidPattern,
            onDismiss = { editTarget = null },
            onConfirm = { pattern, label ->
                vm.updatePattern(target, pattern, label)
                editTarget = null
            }
        )
    }
}

@Composable
private fun PatternItem(pattern: BlacklistPattern, onEdit: () -> Unit, onDelete: () -> Unit) {
    ListItem(
        headlineContent = { Text(pattern.pattern) },
        supportingContent = { if (pattern.label.isNotBlank()) Text(pattern.label) },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Remover")
            }
        },
        modifier = Modifier.clickable { onEdit() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PatternDialog(
    title: String,
    initialPattern: String,
    initialLabel: String,
    isValid: (String) -> Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var pattern by remember { mutableStateOf(initialPattern) }
    var label by remember { mutableStateOf(initialLabel) }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TooltipBox(
                    positionProvider = belowAnchor,
                    tooltip = {
                        RichTooltip {
                            Text("Sequência de dígitos que apareça em qualquer parte do número chamador. Ex.: '91234' bloqueia chamadas de '021912345678'")
                        }
                    },
                    state = tooltipState
                ) {
                    OutlinedTextField(
                        value = pattern,
                        onValueChange = { pattern = it.filter { c -> c.isDigit() } },
                        label = { Text("Sequência de dígitos") },
                        placeholder = { Text("ex: 91234") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .focusable()
                            .onFocusChanged { if (it.isFocused) scope.launch { tooltipState.show() } }
                    )
                }
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Descrição (opcional)") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pattern, label) }, enabled = isValid(pattern)) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
