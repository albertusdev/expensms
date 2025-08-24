package dev.albertus.expensms.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import dev.albertus.expensms.ui.viewModels.MainViewModel
import dev.albertus.expensms.ui.theme.ExpenSMSTheme
import dev.albertus.expensms.ui.components.NarrowLayout
import dev.albertus.expensms.ui.components.WideLayout
import dev.albertus.expensms.ui.components.DeleteConfirmationDialog
import dev.albertus.expensms.ui.components.ForwardConfirmationDialog
import dev.albertus.expensms.ui.props.LayoutProps
import dev.albertus.expensms.ui.model.SelectionMode
import androidx.compose.material.icons.filled.FileDownload

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, onNavigateToSettings: () -> Unit, onNavigateToSmsDetail: (String) -> Unit, onOpenDrawer: () -> Unit) {
    val groupedTransactions by viewModel.groupedTransactions.collectAsState()
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val showMonthlyTotal by viewModel.showMonthlyTotal.collectAsState()
    val isAmountVisible by viewModel.isAmountVisible.collectAsState()
    val loadingProgress by viewModel.loadingProgress.collectAsState()
    val transactionCounts by viewModel.transactionCounts.collectAsState()
    val deleteMode by viewModel.deleteMode.collectAsState()
    val selectedTransactions by viewModel.selectedTransactions.collectAsState()
    val selectionMode by viewModel.selectionMode.collectAsState()

    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showForwardConfirmation by remember { mutableStateOf(false) }

    val layoutProps = LayoutProps(
        viewModel = viewModel,
        groupedTransactions = groupedTransactions,
        filteredTransactions = filteredTransactions,
        selectedDate = selectedDate,
        selectedMonth = selectedMonth,
        showMonthlyTotal = showMonthlyTotal,
        isAmountVisible = isAmountVisible,
        onTransactionClick = onNavigateToSmsDetail,
        transactionCounts = transactionCounts,
        deleteMode = deleteMode,
        selectedTransactions = selectedTransactions,
        onTransactionSelect = viewModel::toggleTransactionSelection
    )

    ExpenSMSTheme {
        BoxWithConstraints {
            val isWideLayout = maxWidth >= 600.dp
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    TopAppBar(
                        title = { Text("ExpenSMS") },
                        navigationIcon = {
                            IconButton(onClick = onOpenDrawer) {
                                Icon(Icons.Default.Menu, contentDescription = "Open menu")
                            }
                        },
                        actions = {
                            if (deleteMode) {
                                val ocbcCount = viewModel.getSelectedOcbcTransactionsCount()
                                Text("${selectedTransactions.size} selected")

                                // Forward button (only show if OCBC transactions are selected)
                                if (ocbcCount > 0) {
                                    IconButton(onClick = { showForwardConfirmation = true }) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Forward selected OCBC transactions"
                                        )
                                    }
                                }

                                // Delete button
                                IconButton(onClick = { showDeleteConfirmation = true }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                                }
                            } else {
                                IconButton(onClick = { viewModel.setIsAmountVisible(!isAmountVisible) }) {
                                    Icon(
                                        if (isAmountVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = "Toggle amount visibility"
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.loadAllSmsMessages() }) {
                                Icon(
                                    Icons.Outlined.Refresh,
                                    contentDescription = "Sync all SMS"
                                )
                            }
                            IconButton(onClick = { viewModel.toggleDeleteMode() }) {
                                Icon(
                                    if (deleteMode) Icons.Default.Close else Icons.Default.Delete,
                                    contentDescription = "Toggle delete mode"
                                )
                            }
                            IconButton(onClick = onNavigateToSettings) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        },
                        scrollBehavior = scrollBehavior
                    )
                    if (loadingProgress < 1f) {
                        LinearProgressIndicator(
                            progress = { loadingProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                },
                floatingActionButton = {
                    if (selectionMode == SelectionMode.EXPORT_CSV && selectedTransactions.isNotEmpty()) {
                        FloatingActionButton(
                            onClick = { /* TODO: Implement CSV export */ }
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Export to CSV")
                        }
                    }
                }
            ) { paddingValues ->
                if (isWideLayout) {
                    WideLayout(
                        modifier = Modifier.padding(paddingValues),
                        props = layoutProps
                    )
                } else {
                    NarrowLayout(
                        modifier = Modifier.padding(paddingValues),
                        props = layoutProps,
                    )
                }
            }

            if (showDeleteConfirmation) {
                DeleteConfirmationDialog(
                    count = selectedTransactions.size,
                    onConfirm = {
                        viewModel.deleteSelectedTransactions()
                        showDeleteConfirmation = false
                    },
                    onDismiss = { showDeleteConfirmation = false }
                )
            }

            if (showForwardConfirmation) {
                ForwardConfirmationDialog(
                    count = viewModel.getSelectedOcbcTransactionsCount(),
                    onConfirm = {
                        viewModel.forwardSelectedTransactions()
                        showForwardConfirmation = false
                    },
                    onDismiss = { showForwardConfirmation = false }
                )
            }
        }
    }
}


