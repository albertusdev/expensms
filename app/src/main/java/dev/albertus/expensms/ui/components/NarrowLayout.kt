package dev.albertus.expensms.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.albertus.expensms.ui.props.LayoutProps

@Composable
fun NarrowLayout(
    modifier: Modifier = Modifier,
    props: LayoutProps,
) {
    val lazyListState = rememberLazyListState()

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(top = 4.dp)
        ) {
           item {
               HeaderContent(
                   props = props,
                   modifier = Modifier
                       .fillMaxWidth()
               )
           }

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
