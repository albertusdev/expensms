package dev.albertus.expensms.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.albertus.expensms.data.model.Transaction
import dev.albertus.expensms.ui.viewModels.MainViewModel
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun WideLayout(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    groupedTransactions: Map<LocalDate, List<Transaction>>,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    selectedMonth: YearMonth,
    showMonthlyTotal: Boolean,
    isAmountVisible: Boolean
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
                    isWideLayout = true,
                    isAmountVisible = isAmountVisible
                )
            }
        }
        GroupedTransactionList(
            groupedTransactions = selectedDate?.let { date ->
                groupedTransactions.filter { it.key == date }
            } ?: groupedTransactions,
            modifier = Modifier.weight(0.6f),
            isAmountVisible = isAmountVisible
        )
    }
}