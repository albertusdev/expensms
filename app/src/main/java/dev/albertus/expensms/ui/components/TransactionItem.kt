package dev.albertus.expensms.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.albertus.expensms.data.model.Transaction
import dev.albertus.expensms.ui.theme.ExpenseRed
import dev.albertus.expensms.utils.CurrencyUtils.format
import dev.albertus.expensms.utils.DateUtils.formatToTimeOnly

@Composable
fun TransactionItem(
    transaction: Transaction,
    isAmountVisible: Boolean,
    onClick: () -> Unit,
    deleteMode: Boolean = false,
    isSelected: Boolean = false,
    onSelect: (String) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = if (deleteMode) { { onSelect(transaction.id) } } else onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = if (isSelected) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (deleteMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelect(transaction.id) },
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = transaction.merchant,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = transaction.bank,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isAmountVisible) transaction.money.format() else "****",
                            style = MaterialTheme.typography.titleMedium,
                            color = ExpenseRed
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = transaction.date.formatToTimeOnly(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}