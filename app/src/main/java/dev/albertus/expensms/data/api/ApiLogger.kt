package dev.albertus.expensms.data.api

import dev.albertus.expensms.data.local.ApiLogDao
import dev.albertus.expensms.data.model.ApiLog
import dev.albertus.expensms.data.model.ApiLogType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiLogger @Inject constructor(
    private val apiLogDao: ApiLogDao,
    private val coroutineScope: CoroutineScope
) {
    
    fun logApiCall(
        logType: ApiLogType,
        endpoint: String,
        httpMethod: String,
        requestBody: String? = null,
        responseCode: Int? = null,
        responseBody: String? = null,
        errorMessage: String? = null,
        durationMs: Long? = null,
        transactionId: String? = null
    ) {
        coroutineScope.launch {
            try {
                val apiLog = ApiLog(
                    id = UUID.randomUUID().toString(),
                    timestamp = Date(),
                    transactionId = transactionId,
                    logType = logType,
                    endpoint = endpoint,
                    httpMethod = httpMethod,
                    requestBody = requestBody,
                    responseCode = responseCode,
                    responseBody = responseBody,
                    errorMessage = errorMessage,
                    durationMs = durationMs
                )
                apiLogDao.insertApiLog(apiLog)
                
                // Clean up old logs (keep only last 500 entries)
                val logCount = apiLogDao.getLogCount()
                if (logCount > 500) {
                    val cutoffDate = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L) // 7 days ago
                    apiLogDao.deleteOldLogs(cutoffDate)
                }
            } catch (e: Exception) {
                android.util.Log.e("ApiLogger", "Failed to log API call", e)
            }
        }
    }
    
    suspend fun clearAllLogs() {
        apiLogDao.clearAllLogs()
    }
}
