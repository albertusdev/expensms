package dev.albertus.expensms.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import dev.albertus.expensms.ui.props.LayoutProps

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

    var showDeleteConfirmation by remember { mutableStateOf(false) }

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
                                Text("${selectedTransactions.size} selected")
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
                        scrollBehavior = scrollBehavior
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
        }
    }
}


