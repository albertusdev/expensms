package dev.albertus.expensms.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.albertus.expensms.data.model.Transaction
import dev.albertus.expensms.ui.viewModels.MainViewModel
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NarrowLayout(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    filteredTransactions: Map<LocalDate, List<Transaction>>,
    groupedTransactions: Map<LocalDate, List<Transaction>>,
    selectedDate: LocalDate?,
    selectedMonth: YearMonth,
    showMonthlyTotal: Boolean,
    isAmountVisible: Boolean,
    onTransactionClick: (String) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val headerHeight = if (showMonthlyTotal) 450.dp else 350.dp
    val headerHeightPx = with(LocalDensity.current) { headerHeight.toPx() }
    val lazyListState = rememberLazyListState()

    var headerOffset by remember { mutableStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val newOffset = headerOffset - delta
                headerOffset = newOffset.coerceIn(0f, headerHeightPx)
                return if (headerOffset > 0f && headerOffset < headerHeightPx) Offset(0f, -delta) else Offset.Zero
            }
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        item {
            Box(
                modifier = Modifier
                    .height(headerHeight)
                    .fillMaxWidth()
                    .offset { IntOffset(x = 0, y = -headerOffset.toInt()) }
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (showMonthlyTotal) {
                        MonthlyTotalSpending(
                            month = selectedMonth,
                            totalSpending = viewModel.getMonthlyTotalSpending(),
                            isWideLayout = false,
                            isAmountVisible = isAmountVisible,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TransactionCalendar(
                        availableDates = groupedTransactions.keys,
                        onDateSelected = { date -> viewModel.selectDate(date) },
                        selectedDate = selectedDate,
                        onMonthChanged = viewModel::setSelectedMonth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }

        filteredTransactions.forEach { (date, transactions) ->
            item {
                DayHeader(date)
            }
            item {
                DailyTotal(transactions, isAmountVisible = isAmountVisible)
            }
            items(transactions) { transaction ->
                TransactionItem(
                    transaction = transaction,
                    isAmountVisible = isAmountVisible,
                    onClick = { onTransactionClick(transaction.id) }
                )
            }
        }
    }
}