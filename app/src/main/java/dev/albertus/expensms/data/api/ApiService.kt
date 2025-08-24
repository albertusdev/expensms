package dev.albertus.expensms.data.api

import android.util.Log
import androidx.datastore.core.DataStore
import dev.albertus.expensms.data.UserPreferences
import dev.albertus.expensms.data.model.ApiLogType
import dev.albertus.expensms.data.model.Transaction
import dev.albertus.expensms.utils.SecureStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiService @Inject constructor(
    private val apiClient: ApiClient,
    private val secureStorage: SecureStorage,
    private val userPreferencesDataStore: DataStore<UserPreferences>,
    private val apiLogger: ApiLogger
) {

    companion object {
        private const val TAG = "ApiService"
    }
    private val authMutex = Mutex()
    
    suspend fun testConnection(email: String, password: String): ApiResult<String> {
        // Log the connection test attempt
        apiLogger.logApiCall(
            logType = ApiLogType.CONNECTION_TEST,
            endpoint = "/auth/login",
            httpMethod = "POST",
            requestBody = "Connection test for: $email"
        )

        return when (val result = apiClient.login(email, password)) {
            is ApiResult.Success -> {
                // Store credentials and tokens
                secureStorage.saveApiPassword(password)
                secureStorage.saveTokens(
                    result.data.access_token,
                    result.data.refresh_token
                )
                ApiResult.Success("Connection successful")
            }
            is ApiResult.Error -> result
            is ApiResult.NetworkError -> result
        }
    }
    
    suspend fun isApiConfigured(): Boolean {
        val preferences = userPreferencesDataStore.data.first()
        return preferences.apiForwardingEnabled && 
               preferences.apiEmail.isNotEmpty() && 
               secureStorage.getApiPassword() != null
    }
    
    private suspend fun ensureValidToken(): String? = authMutex.withLock {
        val currentToken = secureStorage.getAccessToken()
        
        // If we have a valid token, return it
        if (currentToken != null && !secureStorage.isTokenExpired()) {
            return@withLock currentToken
        }
        
        // Try to refresh the token
        val refreshToken = secureStorage.getRefreshToken()
        if (refreshToken != null) {
            when (val refreshResult = apiClient.refreshToken(refreshToken)) {
                is ApiResult.Success -> {
                    secureStorage.saveTokens(
                        refreshResult.data.access_token,
                        refreshResult.data.refresh_token
                    )
                    return@withLock refreshResult.data.access_token
                }
                else -> {
                    // Refresh failed, clear tokens and try to re-login
                    secureStorage.clearTokens()
                }
            }
        }
        
        // Try to re-login with stored credentials
        val preferences = userPreferencesDataStore.data.first()
        val email = preferences.apiEmail
        val password = secureStorage.getApiPassword()
        
        if (email.isNotEmpty() && password != null) {
            when (val loginResult = apiClient.login(email, password)) {
                is ApiResult.Success -> {
                    secureStorage.saveTokens(
                        loginResult.data.access_token,
                        loginResult.data.refresh_token
                    )
                    return@withLock loginResult.data.access_token
                }
                else -> {
                    // Login failed, clear everything
                    secureStorage.clearAll()
                    return@withLock null
                }
            }
        }
        
        return@withLock null
    }
    
    suspend fun forwardTransaction(transaction: Transaction): ApiResult<String> {
        Log.i(TAG, "=== API FORWARD TRANSACTION ===")
        Log.i(TAG, "Transaction ID: ${transaction.id}")
        Log.i(TAG, "Bank: ${transaction.bank}")
        Log.i(TAG, "Merchant: ${transaction.merchant}")
        Log.i(TAG, "Amount: ${transaction.amount}")

        if (!isApiConfigured()) {
            Log.w(TAG, "❌ API forwarding not configured")
            return ApiResult.Error("API forwarding not configured")
        }

        Log.i(TAG, "✅ API is configured, getting access token...")
        val accessToken = ensureValidToken()
        if (accessToken == null) {
            Log.e(TAG, "❌ Failed to get valid access token")
            return ApiResult.Error("Unable to authenticate with API")
        }

        Log.i(TAG, "✅ Got valid access token, preparing SMS data...")
        val smsData = SmsForwardRequest(
            raw_message = transaction.rawMessage,
            sender = transaction.sender ?: transaction.bank, // Use original sender or fallback to bank
            timestamp = transaction.date.time
        )

        Log.i(TAG, "Simplified SMS data prepared:")
        Log.i(TAG, "  Raw message: ${transaction.rawMessage.take(50)}...")
        Log.i(TAG, "  Sender: ${transaction.sender ?: transaction.bank}")
        Log.i(TAG, "  Timestamp: ${transaction.date.time} (${transaction.date})")
        Log.i(TAG, "Calling API client...")

        val result = apiClient.forwardSms(accessToken, smsData, transaction.id)
        Log.i(TAG, "API client result: $result")
        return result
    }
    
    suspend fun clearApiConfiguration() {
        secureStorage.clearAll()
        userPreferencesDataStore.updateData { preferences ->
            preferences.toBuilder()
                .setApiForwardingEnabled(false)
                .setApiEmail("")
                .build()
        }
    }
}
