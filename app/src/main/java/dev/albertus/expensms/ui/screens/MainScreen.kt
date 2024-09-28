package dev.albertus.expensms.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import dev.albertus.expensms.ui.viewModels.MainViewModel
import dev.albertus.expensms.ui.components.GroupedTransactionList
import dev.albertus.expensms.ui.components.TransactionCalendar
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, onNavigateToSettings: () -> Unit) {
    val groupedTransactions by viewModel.groupedTransactions.collectAsState(initial = emptyMap())
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ExpenSMS") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TransactionCalendar(
                availableDates = groupedTransactions.keys,
                onDateSelected = { date ->
                    selectedDate = if (selectedDate == date) null else date
                }
            )
            GroupedTransactionList(
                groupedTransactions = selectedDate?.let { date ->
                    groupedTransactions.filter { it.key == date }
                } ?: groupedTransactions
            )
        }
    }
}