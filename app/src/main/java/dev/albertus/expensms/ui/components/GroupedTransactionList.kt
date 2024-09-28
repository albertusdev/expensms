package dev.albertus.expensms.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.albertus.expensms.data.model.Transaction
import dev.albertus.expensms.ui.theme.ExpenseRed
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import dev.albertus.expensms.utils.CurrencyUtils.formatAsCurrency

@Composable
fun GroupedTransactionList(
    groupedTransactions: Map<LocalDate, List<Transaction>>,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        groupedTransactions.forEach { (date, transactions) ->
            item {
                DayHeader(date)
            }
            item {
                DailyTotal(transactions)
            }
            items(transactions) { transaction ->
                TransactionItem(transaction)
            }
            item {
                Divider(Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

@Composable
fun DayHeader(date: LocalDate) {
    Text(
        text = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
fun DailyTotal(transactions: List<Transaction>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Daily Total",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = transactions.sumOf { it.amount }.formatAsCurrency(),
            style = MaterialTheme.typography.titleSmall,
            color = ExpenseRed
        )
    }
}
