package dev.albertus.expensms

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.runtime.livedata.observeAsState
import dev.albertus.expensms.ui.theme.ExpenSMSTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var permissionState: MutableState<Boolean>
    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        permissionState.value = isGranted
        if (isGranted) {
            viewModel.loadSmsMessages()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExpenSMSTheme {
                permissionState = remember { mutableStateOf(checkPermission()) }
                Surface(color = MaterialTheme.colorScheme.background) {
                    if (permissionState.value) {
                        MainScreen(viewModel)
                    } else {
                        PermissionScreen(
                            onRequestPermission = { requestSmsPermission() }
                        )
                    }
                }
            }
        }
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestSmsPermission() {
        requestPermissionLauncher.launch(Manifest.permission.READ_SMS)
    }
}


@Composable
fun PermissionScreen(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "SMS Permission Required",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "This app needs SMS permission to function properly. Please grant the permission to continue.",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val transactions by viewModel.transactions.observeAsState(initial = emptyList())
    val senderFilter by viewModel.senderFilter.observeAsState(initial = "")

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
            Text("Date: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(transaction.date)}")
            Text("Merchant: ${transaction.merchant}")
            Text("Amount: ${transaction.currency} ${transaction.amount}")
        }
    }
}