package dev.albertus.expensms.ui.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.albertus.expensms.data.local.ApiLogDao
import dev.albertus.expensms.data.model.ApiLogWithSmsMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SmsDetailViewModel @Inject constructor(
    private val apiLogDao: ApiLogDao
) : ViewModel() {

    companion object {
        private const val TAG = "SmsDetailViewModel"
    }

    private val _apiLogs = MutableStateFlow<List<ApiLogWithSmsMessage>>(emptyList())
    val apiLogs: StateFlow<List<ApiLogWithSmsMessage>> = _apiLogs.asStateFlow()

    fun loadApiLogs(smsMessageId: String) {
        Log.i(TAG, "=== LOADING API LOGS FOR SMS ===")
        Log.i(TAG, "SMS Message ID: $smsMessageId")

        viewModelScope.launch {
            try {
                apiLogDao.getApiLogsBySmsMessageId(smsMessageId).collect { logs ->
                    Log.i(TAG, "Found ${logs.size} API logs for SMS $smsMessageId")
                    logs.forEach { logWithSms ->
                        val log = logWithSms.apiLog
                        Log.d(TAG, "API Log: ${log.logType} ${log.endpoint} - ${log.responseCode ?: "No response"}")
                    }
                    _apiLogs.value = logs
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load API logs for SMS $smsMessageId", e)
                _apiLogs.value = emptyList()
            }
        }
    }
}
