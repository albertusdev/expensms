package dev.albertus.expensms.ui.viewModels

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.util.Log
import android.provider.Telephony
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.albertus.expensms.data.UserPreferences
import dev.albertus.expensms.data.model.Transaction
import dev.albertus.expensms.data.repository.TransactionRepository
import dev.albertus.expensms.utils.SmsParser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferencesDataStore: DataStore<UserPreferences>,
    private val transactionRepository: TransactionRepository,
    private val contentResolver: ContentResolver
) : ViewModel() {

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())

    val senderFilter: StateFlow<String> = userPreferencesDataStore.data
        .map { it.senderFilter }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

    init {
        Log.d("MainViewModel", "ViewModel initialized")
        viewModelScope.launch {
            senderFilter.collect {
                loadSmsMessages()
            }
        }
    }

    fun setSenderFilter(sender: String) {
        viewModelScope.launch {
            userPreferencesDataStore.updateData { preferences ->
                preferences.toBuilder().setSenderFilter(sender).build()
            }
        }
    }

    private val _groupedTransactions = MutableStateFlow<Map<LocalDate, List<Transaction>>>(emptyMap())
    val groupedTransactions: StateFlow<Map<LocalDate, List<Transaction>>> = _groupedTransactions.asStateFlow()

    fun loadSmsMessages() {
        viewModelScope.launch {
            try {
                Log.d("MainViewModel", "Loading SMS messages")
                val messages = readSmsMessages()
                val parsedTransactions = messages.mapNotNull { (_, body, timestamp) ->
                    SmsParser.parseTransaction(body, timestamp)
                }
                Log.d("MainViewModel", "Parsed ${parsedTransactions.size} transactions")
                _transactions.value = parsedTransactions
                _groupedTransactions.value = parsedTransactions.groupBy { transaction ->
                    transaction.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                }.toSortedMap(reverseOrder())
                parsedTransactions.forEach { transaction ->
                    transactionRepository.insertTransaction(transaction)
                }
                Log.d("MainViewModel", "Transactions loaded and saved")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading SMS messages", e)
            }
        }
    }

    private fun readSmsMessages(): List<Triple<String, String, Long>> {
        val messages = mutableListOf<Triple<String, String, Long>>()
        val uri: Uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )
        val selection = if (senderFilter.value.isNotEmpty())
            "${Telephony.Sms.ADDRESS} LIKE ?" else null
        val selectionArgs = if (senderFilter.value.isNotEmpty())
            arrayOf("%${senderFilter.value}%") else null
        val cursor: Cursor? = contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            Telephony.Sms.DEFAULT_SORT_ORDER
        )

        cursor?.use {
            val senderIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
            val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)

            while (it.moveToNext()) {
                val sender = it.getString(senderIndex)
                val body = it.getString(bodyIndex)
                val timestamp = it.getLong(dateIndex)
                messages.add(Triple(sender, body, timestamp))
            }
        }

        return messages
    }

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()

    private val _filteredTransactions = MutableStateFlow<Map<LocalDate, List<Transaction>>>(emptyMap())
    val filteredTransactions: StateFlow<Map<LocalDate, List<Transaction>>> = _filteredTransactions.asStateFlow()

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth = _selectedMonth.asStateFlow()

    fun setSelectedMonth(month: YearMonth) {
        _selectedDate.value = null;
        _selectedMonth.value = month
    }

    init {
        viewModelScope.launch {
            combine(_groupedTransactions, _selectedDate, _selectedMonth) { transactions, selectedDate, selectedMonth ->
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

    fun getMonthlyTotalSpending(): Double {
        return filteredTransactions.value
            .values
            .flatten()
            .sumOf { it.amount }
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
            _transactions.value.find { it.id == id }
        }
    }

    fun getRawSmsForTransaction(transactionId: String?): String {
        return transactionId?.let { id ->
            _transactions.value.find { it.id == id }?.rawMessage
        } ?: "Raw SMS not found"
    }
}