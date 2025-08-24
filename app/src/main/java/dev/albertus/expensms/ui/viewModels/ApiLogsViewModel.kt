package dev.albertus.expensms.ui.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.albertus.expensms.data.api.ApiLogger
import dev.albertus.expensms.data.local.ApiLogDao
import dev.albertus.expensms.data.model.ApiLogWithTransaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApiLogsViewModel @Inject constructor(
    private val apiLogDao: ApiLogDao,
    private val apiLogger: ApiLogger
) : ViewModel() {

    val apiLogs: StateFlow<List<ApiLogWithTransaction>> = apiLogDao.getRecentApiLogsWithTransactions(100)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    fun clearLogs() {
        viewModelScope.launch {
            apiLogger.clearAllLogs()
        }
    }
}
