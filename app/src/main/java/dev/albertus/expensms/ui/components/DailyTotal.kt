package dev.albertus.expensms.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.albertus.expensms.data.model.Transaction
import dev.albertus.expensms.ui.theme.ExpenseRed
import dev.albertus.expensms.utils.CurrencyUtils.formatAsCurrency

@Composable
fun DailyTotal(transactions: List<Transaction>, isAmountVisible: Boolean) {
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
            text = if (isAmountVisible)  transactions.sumOf { it.amount }.formatAsCurrency() else "****",
            style = MaterialTheme.typography.titleSmall,
            color = ExpenseRed
        )
    }
}