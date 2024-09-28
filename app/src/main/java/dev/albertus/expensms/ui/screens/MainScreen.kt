package dev.albertus.expensms.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.albertus.expensms.data.model.Transaction
import dev.albertus.expensms.ui.viewModels.MainViewModel
import dev.albertus.expensms.ui.components.GroupedTransactionList
import dev.albertus.expensms.ui.components.TransactionCalendar
import dev.albertus.expensms.ui.theme.ExpenSMSTheme
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, onNavigateToSettings: () -> Unit) {
    val groupedTransactions by viewModel.groupedTransactions.collectAsState(initial = emptyMap())
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    ExpenSMSTheme {
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
            AdaptiveLayout(
                modifier = Modifier.padding(paddingValues),
                availableDates = groupedTransactions.keys,
                selectedDate = selectedDate,
                onDateSelected = { date ->
                    selectedDate = if (selectedDate == date) null else date
                },
                transactions = selectedDate?.let { date ->
                    groupedTransactions.filter { it.key == date }
                } ?: groupedTransactions
            )
        }
    }
}

@Composable
fun AdaptiveLayout(
    modifier: Modifier = Modifier,
    availableDates: Set<LocalDate>,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    transactions: Map<LocalDate, List<Transaction>>
) {
    BoxWithConstraints(modifier = modifier) {
        if (maxWidth < 600.dp) {
            Column {
                TransactionCalendar(
                    availableDates = availableDates,
                    onDateSelected = onDateSelected,
                    selectedDate = selectedDate,
                    modifier = Modifier.height(300.dp)
                )
                GroupedTransactionList(
                    groupedTransactions = transactions,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Row {
                TransactionCalendar(
                    availableDates = availableDates,
                    onDateSelected = onDateSelected,
                    selectedDate = selectedDate,
                    modifier = Modifier.weight(0.4f)
                )
                GroupedTransactionList(
                    groupedTransactions = transactions,
                    modifier = Modifier.weight(0.6f)
                )
            }
        }
    }
}