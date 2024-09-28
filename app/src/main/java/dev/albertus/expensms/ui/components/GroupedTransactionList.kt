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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun GroupedTransactionList(groupedTransactions: Map<LocalDate, List<Transaction>>) {
    LazyColumn {
        groupedTransactions.forEach { (date, transactions) ->
            item {
                DayHeader(date, transactions)
            }
            items(transactions) { transaction ->
                TransactionItem(transaction)
            }
            item {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

@Composable
fun DayHeader(date: LocalDate, transactions: List<Transaction>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Total: ${transactions.sumOf { it.amount }.formatAsCurrency()}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

fun Double.formatAsCurrency(): String {
    return String.format("IDR %.2f", this)
}