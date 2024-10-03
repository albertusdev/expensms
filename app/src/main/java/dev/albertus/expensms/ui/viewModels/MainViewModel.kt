package dev.albertus.expensms.ui.viewModels

import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.albertus.expensms.data.SupportedBank
import dev.albertus.expensms.data.UserPreferences
import dev.albertus.expensms.data.model.Transaction
import dev.albertus.expensms.data.repository.TransactionRepository
import dev.albertus.expensms.utils.CurrencyUtils
import dev.albertus.expensms.utils.SmsSync
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.money.MonetaryAmount

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferencesDataStore: DataStore<UserPreferences>,
    private val transactionRepository: TransactionRepository,
    private val smsSync: SmsSync
) : ViewModel() {

    val enabledBanks: StateFlow<Map<String, Boolean>> = userPreferencesDataStore.data
        .map { it.enabledBanksMap }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyMap())

    private val _loadingProgress = MutableStateFlow(0f)
    val loadingProgress: StateFlow<Float> = _loadingProgress.asStateFlow()

    val transactions: StateFlow<List<Transaction>> = transactionRepository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _groupedTransactions = MutableStateFlow<Map<LocalDate, List<Transaction>>>(emptyMap())
    val groupedTransactions: StateFlow<Map<LocalDate, List<Transaction>>> = _groupedTransactions.asStateFlow()

    init {
        viewModelScope.launch {
            enabledBanks.collect {
                syncSmsMessages()
            }
        }

        viewModelScope.launch {
            transactions.collect { transactionList ->
                updateGroupedTransactions(transactionList)
            }
        }
    }

    fun setEnabledBank(bank: SupportedBank, enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesDataStore.updateData { preferences ->
                preferences.toBuilder()
                    .putEnabledBanks(bank.name, enabled)
                    .build()
            }
        }
    }

    private fun syncSmsMessages() {
        viewModelScope.launch {
            try {
                _loadingProgress.value = 0f
                val enabledBankNames = enabledBanks.value.filter { it.value }.keys
                val enabledBanks = SupportedBank.entries.filter { enabledBankNames.contains(it.name) }

                smsSync.syncSmsMessages(enabledBanks) { progress ->
                    _loadingProgress.value = progress
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _loadingProgress.value = 1f
            }
        }
    }

    private fun updateGroupedTransactions(transactionList: List<Transaction>) {
        _groupedTransactions.value = transactionList.groupBy { transaction ->
            transaction.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        }.toSortedMap(reverseOrder())
    }

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()

    private val _filteredTransactions =
        MutableStateFlow<Map<LocalDate, List<Transaction>>>(emptyMap())
    val filteredTransactions: StateFlow<Map<LocalDate, List<Transaction>>> =
        _filteredTransactions.asStateFlow()

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth = _selectedMonth.asStateFlow()

    fun setSelectedMonth(month: YearMonth) {
        _selectedDate.value = null
        _selectedMonth.value = month
    }

    init {
        viewModelScope.launch {
            combine(
                _groupedTransactions,
                _selectedDate,
                _selectedMonth
            ) { transactions, selectedDate, selectedMonth ->
                when {
                    selectedDate != null -> transactions.filter { it.key == selectedDate }
                    else -> transactions.filter { (date, _) ->
                        YearMonth.from(date) == selectedMonth
                    }
                }
            }.collect { filteredTransactions ->
                _filteredTransactions.value = filteredTransactions
            }
        }
    }

    fun setSelectedDate(date: LocalDate?) {
        _selectedDate.value = when {
            date == null -> null
            _selectedDate.value == null -> date
            ChronoUnit.DAYS.between(_selectedDate.value, date) == 0L -> null
            else -> date
        }
    }

    fun selectDate(date: LocalDate) {
        setSelectedDate(date)
    }

    fun getMonthlyTotalSpending(): Map<String, MonetaryAmount> {
        return filteredTransactions.value
            .values
            .flatten()
            .groupBy { it.money.currency.currencyCode }
            .mapValues { (currency, transactions) ->
                CurrencyUtils.sumAmounts(transactions)
            }
    }

    val showMonthlyTotal: StateFlow<Boolean> = userPreferencesDataStore.data
        .map { it.showMonthlyTotal }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    fun setShowMonthlyTotal(show: Boolean) {
        viewModelScope.launch {
            userPreferencesDataStore.updateData { preferences ->
                preferences.toBuilder().setShowMonthlyTotal(show).build()
            }
        }
    }

    val isAmountVisible: StateFlow<Boolean> = userPreferencesDataStore.data
        .map { it.isAmountVisible }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    fun setIsAmountVisible(isVisible: Boolean) {
        viewModelScope.launch {
            userPreferencesDataStore.updateData { preferences ->
                preferences.toBuilder().setIsAmountVisible(isVisible).build()
            }
        }
    }

    fun getTransactionById(transactionId: String?): Transaction? {
        return transactionId?.let { id ->
            transactions.value.find { it.id == id }
        }
    }

    fun getRawSmsForTransaction(transactionId: String?): String {
        return transactionId?.let { id ->
            transactions.value.find { it.id == id }?.rawMessage
        } ?: "Raw SMS not found"
    }

    fun loadSmsMessages() {
        viewModelScope.launch {
            syncSmsMessages()
        }
    }
}