package dev.albertus.expensms.ui.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.albertus.expensms.data.model.SmsMessage
import dev.albertus.expensms.data.model.SmsStatus
import dev.albertus.expensms.data.repository.SmsMessageRepository
import dev.albertus.expensms.ui.model.SelectionMode
import dev.albertus.expensms.utils.SimpleSmsForwardingService
import dev.albertus.expensms.utils.SimpleSmsSync
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SmsMainViewModel @Inject constructor(
    private val smsMessageRepository: SmsMessageRepository,
    private val simpleSmsForwardingService: SimpleSmsForwardingService,
    private val simpleSmsSync: SimpleSmsSync
) : ViewModel() {

    companion object {
        private const val TAG = "SmsMainViewModel"
    }

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()

    private val _selectedMonth = MutableStateFlow<YearMonth?>(null)
    val selectedMonth: StateFlow<YearMonth?> = _selectedMonth.asStateFlow()

    private val _selectedSmsMessages = MutableStateFlow<Set<String>>(emptySet())
    val selectedSmsMessages: StateFlow<Set<String>> = _selectedSmsMessages.asStateFlow()

    private val _deleteMode = MutableStateFlow(false)
    val deleteMode: StateFlow<Boolean> = _deleteMode.asStateFlow()

    private val _selectionMode = MutableStateFlow(SelectionMode.NONE)
    val selectionMode: StateFlow<SelectionMode> = _selectionMode.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        // Sync SMS messages when the ViewModel is created
        syncSmsMessages()
    }

    // Get all SMS messages
    val smsMessages: StateFlow<List<SmsMessage>> = smsMessageRepository.getAllSmsMessages()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Group SMS messages by date
    val groupedSmsMessages: StateFlow<Map<LocalDate, List<SmsMessage>>> = smsMessages
        .map { messages ->
            messages.groupBy { smsMessage ->
                smsMessage.timestamp.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    // Filter SMS messages based on selected date or month
    val filteredSmsMessages: StateFlow<Map<LocalDate, List<SmsMessage>>> = combine(
        groupedSmsMessages,
        selectedDate,
        selectedMonth
    ) { grouped, date, month ->
        when {
            date != null -> {
                grouped.filterKeys { it == date }
            }
            month != null -> {
                grouped.filterKeys { YearMonth.from(it) == month }
            }
            else -> grouped
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    fun setSelectedDate(date: LocalDate?) {
        _selectedDate.value = date
        _selectedMonth.value = null
        Log.i(TAG, "Selected date: $date")
    }

    fun setSelectedMonth(month: YearMonth?) {
        _selectedMonth.value = month
        _selectedDate.value = null
        Log.i(TAG, "Selected month: $month")
    }

    fun getSmsMessageById(id: String?): SmsMessage? {
        Log.d(TAG, "getSmsMessageById called with id: $id")
        Log.d(TAG, "Available SMS count: ${smsMessages.value.size}")
        return if (id != null) {
            val found = smsMessages.value.find { it.id == id }
            Log.d(TAG, "Found SMS in StateFlow: ${found?.id}")
            found
        } else {
            Log.d(TAG, "ID is null")
            null
        }
    }

    suspend fun getSmsMessageByIdFromDb(id: String?): SmsMessage? {
        Log.d(TAG, "getSmsMessageByIdFromDb called with id: $id")
        return if (id != null) {
            val found = smsMessageRepository.getSmsMessageById(id)
            Log.d(TAG, "Found SMS in database: ${found?.id}")
            found
        } else {
            Log.d(TAG, "ID is null")
            null
        }
    }

    // Selection management
    fun toggleSmsMessageSelection(smsMessageId: String) {
        val currentSelection = _selectedSmsMessages.value
        _selectedSmsMessages.value = if (smsMessageId in currentSelection) {
            currentSelection - smsMessageId
        } else {
            currentSelection + smsMessageId
        }
        Log.d(TAG, "Toggled selection for SMS $smsMessageId. Total selected: ${_selectedSmsMessages.value.size}")
    }

    fun selectAllSmsMessagesForDate(date: LocalDate) {
        val smsMessagesForDate = groupedSmsMessages.value[date] ?: emptyList()
        val currentSelection = _selectedSmsMessages.value
        _selectedSmsMessages.value = currentSelection + smsMessagesForDate.map { it.id }
        Log.i(TAG, "Selected all SMS messages for date $date: ${smsMessagesForDate.size} messages")
    }

    fun selectAllSmsMessagesForMonth(yearMonth: YearMonth) {
        val smsMessagesForMonth = groupedSmsMessages.value.filter { (date, _) ->
            YearMonth.from(date) == yearMonth
        }.values.flatten()
        val currentSelection = _selectedSmsMessages.value
        _selectedSmsMessages.value = currentSelection + smsMessagesForMonth.map { it.id }
        Log.i(TAG, "Selected all SMS messages for month $yearMonth: ${smsMessagesForMonth.size} messages")
    }

    fun selectAllFilteredSmsMessages() {
        val allFilteredSmsMessages = filteredSmsMessages.value.values.flatten()
        _selectedSmsMessages.value = allFilteredSmsMessages.map { it.id }.toSet()
        Log.i(TAG, "Selected all ${allFilteredSmsMessages.size} filtered SMS messages")
    }

    fun deselectAllSmsMessages() {
        _selectedSmsMessages.value = emptySet()
        Log.i(TAG, "Deselected all SMS messages")
    }

    fun getFilteredSmsMessagesCount(): Int {
        return filteredSmsMessages.value.values.flatten().size
    }

    fun areAllFilteredSmsMessagesSelected(): Boolean {
        val allFilteredSmsMessageIds = filteredSmsMessages.value.values.flatten().map { it.id }.toSet()
        return allFilteredSmsMessageIds.isNotEmpty() && selectedSmsMessages.value.containsAll(allFilteredSmsMessageIds)
    }

    fun getSelectedSmsMessages(): List<SmsMessage> {
        return smsMessages.value.filter { it.id in selectedSmsMessages.value }
    }

    fun getBankSmsMessagesCount(): Int {
        return getSelectedSmsMessages().count { it.bankSource != null }
    }

    // Selection mode management
    fun setSelectionMode(mode: SelectionMode) {
        _selectionMode.value = mode
        _deleteMode.value = (mode == SelectionMode.DELETE)
        if (mode == SelectionMode.NONE) {
            _selectedSmsMessages.value = emptySet()
        }
        Log.i(TAG, "Selection mode: $mode")
    }

    // Delete mode management (backward compatibility)
    fun setDeleteMode(enabled: Boolean) {
        setSelectionMode(if (enabled) SelectionMode.DELETE else SelectionMode.NONE)
    }

    // Enter selection mode and select the given SMS
    fun enterSelectionModeAndSelect(smsId: String) {
        _selectionMode.value = SelectionMode.MULTI_SELECT
        _selectedSmsMessages.value = setOf(smsId)
        Log.i(TAG, "Entered selection mode with SMS: $smsId")
    }

    // SMS message operations
    fun deleteSelectedSmsMessages() {
        Log.i(TAG, "=== DELETE SELECTED SMS MESSAGES ===")
        val selectedSms = getSelectedSmsMessages()
        Log.i(TAG, "Deleting ${selectedSms.size} SMS messages")

        viewModelScope.launch {
            try {
                smsMessageRepository.deleteSmsMessages(selectedSms.map { it.id })
                Log.i(TAG, "✅ Successfully deleted ${selectedSms.size} SMS messages")
                
                // Clear selection and exit delete mode
                _selectedSmsMessages.value = emptySet()
                _deleteMode.value = false
                _selectionMode.value = SelectionMode.NONE
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to delete SMS messages", e)
            }
        }
    }

    fun forwardSelectedSmsMessages() {
        Log.i(TAG, "=== FORWARD SELECTED SMS MESSAGES ===")
        val selectedSms = getSelectedSmsMessages()
        Log.i(TAG, "Total selected: ${selectedSms.size}")

        if (selectedSms.isEmpty()) {
            Log.w(TAG, "No SMS messages selected for forwarding")
            return
        }

        // Use batch forwarding for better performance and API efficiency
        Log.i(TAG, "Using batch SMS forwarding for ${selectedSms.size} messages")
        simpleSmsForwardingService.forwardSmsMessagesBatchIfEnabled(selectedSms)

        // Clear selection and exit delete mode after forwarding
        _selectedSmsMessages.value = emptySet()
        _deleteMode.value = false
        _selectionMode.value = SelectionMode.NONE

        Log.i(TAG, "=== FORWARD INITIATED ===")
    }

    // SMS sync functionality
    fun syncSmsMessages(fullSync: Boolean = false) {
        Log.i(TAG, "=== SYNC SMS MESSAGES ===")
        Log.i(TAG, "Full sync: $fullSync")

        if (_isSyncing.value) {
            Log.w(TAG, "SMS sync already in progress")
            return
        }

        viewModelScope.launch {
            try {
                _isSyncing.value = true
                Log.i(TAG, "Starting SMS sync...")

                simpleSmsSync.syncSmsMessages(
                    progressCallback = { progress ->
                        Log.d(TAG, "SMS sync progress: ${(progress * 100).toInt()}%")
                    },
                    fullSync = fullSync
                )

                Log.i(TAG, "✅ SMS sync completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "❌ SMS sync failed", e)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun refreshSmsMessages() {
        Log.i(TAG, "Refreshing SMS messages (full sync)")
        syncSmsMessages(fullSync = true)
    }

    fun markSelectedAsForwarded() {
        Log.i(TAG, "=== MARK SELECTED AS FORWARDED ===")
        Log.i(TAG, "Selected SMS count: ${_selectedSmsMessages.value.size}")

        viewModelScope.launch {
            try {
                val selectedIds = _selectedSmsMessages.value.toList()
                val smsToUpdate = smsMessages.value.filter { it.id in selectedIds }

                Log.i(TAG, "Marking ${smsToUpdate.size} SMS as forwarded")

                smsToUpdate.forEach { sms ->
                    val updatedSms = sms.copy(isForwarded = true)
                    smsMessageRepository.updateSmsMessage(updatedSms)
                    Log.d(TAG, "Marked SMS ${sms.id} as forwarded")
                }

                // Clear selection and exit delete mode
                _selectedSmsMessages.value = emptySet()
                _deleteMode.value = false

                Log.i(TAG, "✅ Successfully marked ${smsToUpdate.size} SMS as forwarded")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to mark SMS as forwarded", e)
            }
        }
    }

    fun markSelectedAsNotForwarded() {
        Log.i(TAG, "=== MARK SELECTED AS NOT FORWARDED ===")
        Log.i(TAG, "Selected SMS count: ${_selectedSmsMessages.value.size}")

        viewModelScope.launch {
            try {
                val selectedIds = _selectedSmsMessages.value.toList()
                val smsToUpdate = smsMessages.value.filter { it.id in selectedIds }

                Log.i(TAG, "Marking ${smsToUpdate.size} SMS as not forwarded")

                smsToUpdate.forEach { sms ->
                    val updatedSms = sms.copy(isForwarded = false)
                    smsMessageRepository.updateSmsMessage(updatedSms)
                    Log.d(TAG, "Marked SMS ${sms.id} as not forwarded")
                }

                // Clear selection and exit delete mode
                _selectedSmsMessages.value = emptySet()
                _deleteMode.value = false

                Log.i(TAG, "✅ Successfully marked ${smsToUpdate.size} SMS as not forwarded")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to mark SMS as not forwarded", e)
            }
        }
    }

}
