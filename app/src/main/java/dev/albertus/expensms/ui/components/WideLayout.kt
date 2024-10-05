package dev.albertus.expensms.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.albertus.expensms.ui.props.LayoutProps

@Composable
fun WideLayout(
    modifier: Modifier = Modifier,
    props: LayoutProps
) {
    Row(modifier = modifier) {
        Column(modifier = Modifier.weight(0.4f)) {
            TransactionCalendar(
                availableDates = props.groupedTransactions.keys,
                onDateSelected = { date -> props.viewModel.selectDate(date) },
                selectedDate = props.selectedDate,
                onMonthChanged = props.viewModel::setSelectedMonth,
                modifier = Modifier.fillMaxWidth(),
                transactionCounts = props.transactionCounts
            )
            if (props.showMonthlyTotal) {
                MonthlyTotalSpending(
                    month = props.selectedMonth,
                    totalSpending = props.viewModel.getMonthlyTotalSpending(),
                    isWideLayout = true,
                    isAmountVisible = props.isAmountVisible
                )
            }
        }
        GroupedTransactionList(
            groupedTransactions = props.filteredTransactions,
            modifier = Modifier.weight(0.6f),
            isAmountVisible = props.isAmountVisible,
            onTransactionClick = props.onTransactionClick,
            deleteMode = props.deleteMode,
            selectedTransactions = props.selectedTransactions,
            onTransactionSelect = props.onTransactionSelect
        )
    }
}