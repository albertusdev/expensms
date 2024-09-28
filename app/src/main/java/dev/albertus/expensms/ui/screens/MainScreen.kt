package dev.albertus.expensms.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import dev.albertus.expensms.data.model.Transaction
import dev.albertus.expensms.ui.components.DayHeader
import dev.albertus.expensms.ui.viewModels.MainViewModel
import dev.albertus.expensms.ui.components.GroupedTransactionList
import dev.albertus.expensms.ui.components.TransactionCalendar
import dev.albertus.expensms.ui.theme.ExpenSMSTheme
import dev.albertus.expensms.ui.components.MonthlyTotalSpending
import dev.albertus.expensms.ui.components.TransactionItem
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, onNavigateToSettings: () -> Unit) {
    val groupedTransactions by viewModel.groupedTransactions.collectAsState(initial = emptyMap())
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val showMonthlyTotal by viewModel.showMonthlyTotal.collectAsState()

    ExpenSMSTheme {
        BoxWithConstraints {
            val isWideLayout = maxWidth >= 600.dp
            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    TopAppBar(
                        title = { Text("ExpenSMS") },
                        actions = {
                            IconButton(onClick = onNavigateToSettings) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        },
                        scrollBehavior = scrollBehavior
                    )
                }
            ) { paddingValues ->
                if (isWideLayout) {
                    WideLayout(
                        modifier = Modifier.padding(paddingValues),
                        viewModel = viewModel,
                        groupedTransactions = groupedTransactions,
                        selectedDate = selectedDate,
                        onDateSelected = { date ->
                            selectedDate = if (selectedDate == date) null else date
                            viewModel.setSelectedMonth(YearMonth.from(date))
                        },
                        selectedMonth = selectedMonth,
                        showMonthlyTotal = showMonthlyTotal
                    )
                } else {
                    NarrowLayout(
                        modifier = Modifier.padding(paddingValues),
                        viewModel = viewModel,
                        groupedTransactions = groupedTransactions,
                        selectedDate = selectedDate,
                        onDateSelected = { date ->
                            selectedDate = if (selectedDate == date) null else date
                            viewModel.setSelectedMonth(YearMonth.from(date))
                        },
                        selectedMonth = selectedMonth,
                        showMonthlyTotal = showMonthlyTotal
                    )
                }
            }
        }
    }
}

@Composable
fun WideLayout(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    groupedTransactions: Map<LocalDate, List<Transaction>>,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    selectedMonth: YearMonth,
    showMonthlyTotal: Boolean
) {
    Row(modifier = modifier) {
        Column(modifier = Modifier.weight(0.4f)) {
            TransactionCalendar(
                availableDates = groupedTransactions.keys,
                onDateSelected = onDateSelected,
                selectedDate = selectedDate,
                onMonthChanged = viewModel::setSelectedMonth,
                modifier = Modifier.fillMaxWidth()
            )
            if (showMonthlyTotal) {
                MonthlyTotalSpending(
                    month = selectedMonth,
                    totalSpending = viewModel.getMonthlyTotalSpending(),
                    isWideLayout = true
                )
            }
        }
        GroupedTransactionList(
            groupedTransactions = selectedDate?.let { date ->
                groupedTransactions.filter { it.key == date }
            } ?: groupedTransactions,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NarrowLayout(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    groupedTransactions: Map<LocalDate, List<Transaction>>,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    selectedMonth: YearMonth,
    showMonthlyTotal: Boolean
) {
    val lazyListState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0
        }
    }
    val headerAlpha by animateFloatAsState(if (isScrolled) 0f else 1f)

    Box(modifier = modifier) {
        LazyColumn(state = lazyListState) {
            item {
                Column(modifier = Modifier.animateItem(null, null)) {
                    if (showMonthlyTotal) {
                        MonthlyTotalSpending(
                            month = selectedMonth,
                            totalSpending = viewModel.getMonthlyTotalSpending(),
                            isWideLayout = false,
                            modifier = Modifier.alpha(headerAlpha)
                        )
                    }
                    TransactionCalendar(
                        availableDates = groupedTransactions.keys,
                        onDateSelected = onDateSelected,
                        selectedDate = selectedDate,
                        onMonthChanged = viewModel::setSelectedMonth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp)
                            .alpha(headerAlpha)
                    )
                }
            }
            groupedTransactions.forEach { (date, transactions) ->
                item {
                    DayHeader(date)
                }
                items(transactions.size) { transactionIndex ->
                    TransactionItem(transactions[transactionIndex])
                }
                item {
                    Divider(Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}