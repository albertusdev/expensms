package dev.albertus.expensms.ui.props

import dev.albertus.expensms.data.model.Transaction
import dev.albertus.expensms.ui.viewModels.MainViewModel
import java.time.LocalDate
import java.time.YearMonth

data class LayoutProps(
    val viewModel: MainViewModel,
    val groupedTransactions: Map<LocalDate, List<Transaction>>,
    val filteredTransactions: Map<LocalDate, List<Transaction>>,
    val selectedDate: LocalDate?,
    val selectedMonth: YearMonth,
    val showMonthlyTotal: Boolean,
    val isAmountVisible: Boolean,
    val onTransactionClick: (String) -> Unit,
    val transactionCounts: Map<LocalDate, Int>,
    val deleteMode: Boolean,
    val selectedTransactions: Set<String>,
    val onTransactionSelect: (String) -> Unit
)