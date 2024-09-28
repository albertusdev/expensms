package dev.albertus.expensms.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.albertus.expensms.data.model.Transaction
import java.time.LocalDate

@Composable
fun GroupedTransactionList(
    groupedTransactions: Map<LocalDate, List<Transaction>>,
    modifier: Modifier = Modifier,
    isAmountVisible: Boolean = true
) {
    LazyColumn(modifier = modifier) {
        groupedTransactions.forEach { (date, transactions) ->
            item {
                DayHeader(date)
            }
            item {
                DailyTotal(transactions, isAmountVisible = isAmountVisible)
            }
            items(transactions) { transaction ->
                TransactionItem(transaction, isAmountVisible = isAmountVisible)
            }
            item {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

