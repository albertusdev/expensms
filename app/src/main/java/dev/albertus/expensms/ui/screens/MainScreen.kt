package dev.albertus.expensms.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.albertus.expensms.ui.viewModels.MainViewModel
import dev.albertus.expensms.ui.theme.ExpenSMSTheme
import dev.albertus.expensms.ui.components.NarrowLayout
import dev.albertus.expensms.ui.components.WideLayout
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, onNavigateToSettings: () -> Unit, onNavigateToSmsDetail: (String) -> Unit) {
    val groupedTransactions by viewModel.groupedTransactions.collectAsState()
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val showMonthlyTotal by viewModel.showMonthlyTotal.collectAsState()
    val isAmountVisible by viewModel.isAmountVisible.collectAsState()
    val loadingProgress by viewModel.loadingProgress.collectAsState()

    ExpenSMSTheme {
        BoxWithConstraints {
            val isWideLayout = maxWidth >= 600.dp
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    Column {
                        TopAppBar(
                            title = { Text("ExpenSMS") },
                            actions = {
                                IconButton(onClick = { viewModel.setIsAmountVisible(!isAmountVisible) }) {
                                    Icon(
                                        if (isAmountVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = "Toggle amount visibility"
                                    )
                                }
                                IconButton(onClick = onNavigateToSettings) {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                                }
                            },
                            scrollBehavior = scrollBehavior
                        )
                        // Add LinearProgressIndicator here, directly under the TopAppBar
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
                }
            ) { paddingValues ->
                // Existing layout content
                if (isWideLayout) {
                    WideLayout(
                        modifier = Modifier.padding(paddingValues),
                        viewModel = viewModel,
                        groupedTransactions = groupedTransactions,
                        filteredTransactions = filteredTransactions,
                        selectedDate = selectedDate,
                        onDateSelected = { date -> viewModel.setSelectedDate(date) },
                        selectedMonth = selectedMonth,
                        showMonthlyTotal = showMonthlyTotal,
                        isAmountVisible = isAmountVisible,
                        onTransactionClick = onNavigateToSmsDetail
                    )
                } else {
                    NarrowLayout(
                        modifier = Modifier.padding(paddingValues),
                        viewModel = viewModel,
                        groupedTransactions = groupedTransactions,
                        filteredTransactions = filteredTransactions,
                        selectedDate = selectedDate,
                        selectedMonth = selectedMonth,
                        showMonthlyTotal = showMonthlyTotal,
                        isAmountVisible = isAmountVisible,
                        onTransactionClick = onNavigateToSmsDetail,
                        scrollBehavior = scrollBehavior
                    )
                }
            }
        }
    }
}
