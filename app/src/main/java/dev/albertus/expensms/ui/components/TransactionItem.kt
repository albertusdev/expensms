package dev.albertus.expensms.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.albertus.expensms.data.model.Transaction
import dev.albertus.expensms.ui.theme.ExpenseRed
import dev.albertus.expensms.utils.CurrencyUtils.formatAsCurrency
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionItem(transaction: Transaction, isAmountVisible: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = transaction.merchant,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isAmountVisible) transaction.amount.formatAsCurrency() else "****",
                    style = MaterialTheme.typography.titleMedium,
                    color = ExpenseRed
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Card: **** ${transaction.cardLastFourDigits}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Date: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(transaction.date)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}