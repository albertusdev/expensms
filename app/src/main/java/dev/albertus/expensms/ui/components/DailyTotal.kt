package dev.albertus.expensms.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.albertus.expensms.data.model.Transaction
import dev.albertus.expensms.ui.theme.ExpenseRed
import dev.albertus.expensms.utils.CurrencyUtils

@Composable
fun DailyTotal(transactions: List<Transaction>, isAmountVisible: Boolean) {
    val groupedByCurrency = transactions.groupBy { it.currency }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Daily Total",
            style = MaterialTheme.typography.bodyMedium
        )
        groupedByCurrency.forEach { (currency, transactionsForCurrency) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currency.currencyCode,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = if (isAmountVisible)
                        CurrencyUtils.format(
                            currency.currencyCode,
                            transactions.sumOf {
                                it.amount
                            }
                        )
                    else "****",
                    style = MaterialTheme.typography.titleSmall,
                    color = ExpenseRed
                )
            }
        }
    }
}