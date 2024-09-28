package dev.albertus.expensms.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.albertus.expensms.data.model.Transaction
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionItem(transaction: Transaction) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Card: **** ${transaction.cardLastFourDigits}")
            Text("Date: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(transaction.date)}")
            Text("Merchant: ${transaction.merchant}")
            Text("Amount: ${transaction.currency} ${transaction.amount}")
        }
    }
}