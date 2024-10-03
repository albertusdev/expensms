package dev.albertus.expensms.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.albertus.expensms.ui.components.GroupedTransactionList
import dev.albertus.expensms.ui.components.RestoreConfirmationDialog
import dev.albertus.expensms.ui.viewModels.MainViewModel
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashBinScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val ignoredTransactions by viewModel.ignoredTransactions.collectAsState()
    val selectedTransactions by viewModel.selectedTransactions.collectAsState()
    var showRestoreConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trash Bin") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedTransactions.isNotEmpty()) {
                        IconButton(onClick = { showRestoreConfirmation = true }) {
                            Icon(Icons.Default.Restore, contentDescription = "Restore selected")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            if (ignoredTransactions.isEmpty()) {
                Text(
                    "No deleted transactions",
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                GroupedTransactionList(
                    groupedTransactions = ignoredTransactions.groupBy { transaction ->
                        transaction.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    },
                    isAmountVisible = true,
                    onTransactionClick = { },
                    deleteMode = true,
                    selectedTransactions = selectedTransactions,
                    onTransactionSelect = viewModel::toggleTransactionSelection
                )
            }
        }
    }

    if (showRestoreConfirmation) {
        RestoreConfirmationDialog(
            count = selectedTransactions.size,
            onConfirm = {
                viewModel.restoreSelectedTransactions()
                showRestoreConfirmation = false
            },
            onDismiss = { showRestoreConfirmation = false }
        )
    }
}