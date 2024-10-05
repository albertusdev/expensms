package dev.albertus.expensms.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.albertus.expensms.ui.props.LayoutProps
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NarrowLayout(
    modifier: Modifier = Modifier,
    props: LayoutProps,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var headerHeightPx by remember { mutableStateOf(0f) }
    val headerHeightDp = with(LocalDensity.current) { headerHeightPx.toDp() }

    var headerOffset by remember { mutableStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val newOffset = headerOffset + delta
                headerOffset = newOffset.coerceIn(-headerHeightPx, 0f)
                return Offset.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(top = headerHeightDp)
        ) {
            props.filteredTransactions.forEach { (date, transactions) ->
                item {
                    DayHeader(date)
                }
                item {
                    DailyTotal(transactions, isAmountVisible = props.isAmountVisible)
                }
                items(transactions) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        isAmountVisible = props.isAmountVisible,
                        onClick = { props.onTransactionClick(transaction.id) },
                        deleteMode = props.deleteMode,
                        isSelected = props.selectedTransactions.contains(transaction.id),
                        onSelect = props.onTransactionSelect
                    )
                }
            }
        }

        HeaderContent(
            props = props,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = with(LocalDensity.current) { headerOffset.toDp() })
                .onSizeChanged { headerHeightPx = it.height.toFloat() }
        )
    }
}

@Composable
private fun HeaderContent(
    props: LayoutProps,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (props.showMonthlyTotal) {
            MonthlyTotalSpending(
                month = props.selectedMonth,
                totalSpending = props.viewModel.getMonthlyTotalSpending(),
                isWideLayout = false,
                isAmountVisible = props.isAmountVisible,
                modifier = Modifier.fillMaxWidth()
            )
        }
        TransactionCalendar(
            availableDates = props.groupedTransactions.keys,
            onDateSelected = { date -> props.viewModel.selectDate(date) },
            selectedDate = props.selectedDate,
            onMonthChanged = props.viewModel::setSelectedMonth,
            modifier = Modifier.fillMaxWidth(),
            transactionCounts = props.transactionCounts
        )
    }
}
