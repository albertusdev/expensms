package dev.albertus.expensms

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint

import dev.albertus.expensms.ui.theme.ExpenSMSTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExpenSMSTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val transactions by viewModel.transactions.collectAsState(initial = emptyList())
    val senderFilter by viewModel.senderFilter.collectAsState(initial = "")

    Column {
        SenderFilterInput(
            senderFilter = senderFilter,
            onSenderFilterChanged = { viewModel.setSenderFilter(it) }
        )
        TransactionList(transactions = transactions)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SenderFilterInput(senderFilter: String, onSenderFilterChanged: (String) -> Unit) {
    OutlinedTextField(
        value = senderFilter,
        onValueChange = onSenderFilterChanged,
        label = { Text("Sender Filter") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}

@Composable
fun TransactionList(transactions: List<Transaction>) {
    LazyColumn {
        items(transactions) { transaction ->
            TransactionItem(transaction)
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Card: **** ${transaction.cardLastFourDigits}")
            Text("Date: ${transaction.date}")
            Text("Merchant: ${transaction.merchant}")
            Text("Amount: ${transaction.currency} ${transaction.amount}")
        }
    }
}