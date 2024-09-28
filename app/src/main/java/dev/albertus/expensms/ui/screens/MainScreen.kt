package dev.albertus.expensms.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import dev.albertus.expensms.ui.viewModels.MainViewModel
import dev.albertus.expensms.ui.components.SenderFilterInput
import dev.albertus.expensms.ui.components.TransactionList

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