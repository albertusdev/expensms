package dev.albertus.expensms.data.api

import android.util.Log
import androidx.datastore.core.DataStore
import dev.albertus.expensms.data.UserPreferences
import dev.albertus.expensms.data.model.ApiLogType
import dev.albertus.expensms.data.model.SmsMessage
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
        private const val MAX_RETRY_ATTEMPTS = 2
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
        Log.d(TAG, "=== ENSURING VALID TOKEN ===")

        val currentToken = secureStorage.getAccessToken()

        // If we have a valid token, return it
        if (currentToken != null && !secureStorage.isTokenExpired()) {
            Log.d(TAG, "✅ Current token is valid, using existing token")
            return@withLock currentToken
        }

        Log.i(TAG, "🔄 Current token is expired or missing, attempting refresh...")

        // Step 1: Try to refresh the token using refresh token
        val refreshToken = secureStorage.getRefreshToken()
        if (refreshToken != null) {
            Log.i(TAG, "📱 Attempting token refresh with refresh token...")
            when (val refreshResult = apiClient.refreshToken(refreshToken)) {
                is ApiResult.Success -> {
                    Log.i(TAG, "✅ Token refresh successful!")
                    secureStorage.saveTokens(
                        refreshResult.data.access_token,
                        refreshResult.data.refresh_token
                    )
                    return@withLock refreshResult.data.access_token
                }
                is ApiResult.Error -> {
                    Log.w(TAG, "❌ Token refresh failed: ${refreshResult.message}")
                    Log.i(TAG, "🔄 Falling back to re-login with stored credentials...")
                    secureStorage.clearTokens()
                }
                is ApiResult.NetworkError -> {
                    Log.w(TAG, "❌ Token refresh failed due to network error")
                    Log.i(TAG, "🔄 Falling back to re-login with stored credentials...")
                    secureStorage.clearTokens()
                }
            }
        } else {
            Log.w(TAG, "❌ No refresh token available, falling back to re-login...")
        }

        // Step 2: Fallback - Try to re-login with stored credentials
        val preferences = userPreferencesDataStore.data.first()
        val email = preferences.apiEmail
        val password = secureStorage.getApiPassword()

        if (email.isNotEmpty() && password != null) {
            Log.i(TAG, "🔑 Attempting re-login with stored credentials for: $email")
            when (val loginResult = apiClient.login(email, password)) {
                is ApiResult.Success -> {
                    Log.i(TAG, "✅ Re-login successful! New tokens obtained")
                    secureStorage.saveTokens(
                        loginResult.data.access_token,
                        loginResult.data.refresh_token
                    )
                    return@withLock loginResult.data.access_token
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "❌ Re-login failed: ${loginResult.message}")
                    Log.e(TAG, "🚨 All authentication methods exhausted - clearing stored credentials")
                    secureStorage.clearAll()
                    return@withLock null
                }
                is ApiResult.NetworkError -> {
                    Log.e(TAG, "❌ Re-login failed due to network error")
                    Log.w(TAG, "⚠️ Network issue - keeping credentials for later retry")
                    return@withLock null
                }
            }
        } else {
            Log.e(TAG, "❌ No stored credentials available for re-login")
            Log.e(TAG, "   Email: ${if (email.isNotEmpty()) "✅ Available" else "❌ Missing"}")
            Log.e(TAG, "   Password: ${if (password != null) "✅ Available" else "❌ Missing"}")
            return@withLock null
        }
    }

    /**
     * Check if the error message indicates an expired token or authentication failure
     */
    private fun isTokenExpiredError(errorMessage: String): Boolean {
        val expiredTokenKeywords = listOf(
            "expired token",
            "token expired",
            "token has expired",
            "access token expired",
            "jwt expired",
            "token invalid",
            "unauthorized",
            "authentication failed",
            "invalid token",
            "token not found",
            "access denied",
            "forbidden",
            "401", // HTTP 401 Unauthorized
            "403"  // HTTP 403 Forbidden
        )

        return expiredTokenKeywords.any { keyword ->
            errorMessage.contains(keyword, ignoreCase = true)
        }
    }

    /**
     * Check if we have the necessary credentials for fallback login
     */
    private suspend fun hasStoredCredentials(): Boolean {
        val preferences = userPreferencesDataStore.data.first()
        val email = preferences.apiEmail
        val password = secureStorage.getApiPassword()

        return email.isNotEmpty() && password != null
    }

    /**
     * Execute API call with automatic token refresh and retry logic
     */
    private suspend fun <T> executeWithRetry(
        operation: suspend (accessToken: String, shouldLog: Boolean) -> ApiResult<T>,
        operationName: String
    ): ApiResult<T> {
        var lastError: ApiResult.Error? = null

        for (attempt in 0..MAX_RETRY_ATTEMPTS) {
            Log.d(TAG, "=== $operationName - Attempt ${attempt + 1}/${MAX_RETRY_ATTEMPTS + 1} ===")

            // Get valid token (this handles refresh token + fallback login automatically)
            val accessToken = ensureValidToken()
            if (accessToken == null) {
                val hasCredentials = hasStoredCredentials()
                val errorMessage = if (hasCredentials) {
                    "Authentication failed - please check your credentials"
                } else {
                    "No valid credentials available - please reconfigure API settings"
                }

                Log.e(TAG, "❌ Failed to get valid access token on attempt ${attempt + 1}")
                Log.e(TAG, "   Has stored credentials: $hasCredentials")
                return ApiResult.Error(errorMessage)
            }

            // Only log on the final attempt (after all retries)
            val shouldLog = attempt == MAX_RETRY_ATTEMPTS

            // Execute the operation
            val result = operation(accessToken, shouldLog)

            when (result) {
                is ApiResult.Success -> {
                    if (attempt > 0) {
                        Log.i(TAG, "✅ $operationName succeeded on retry attempt ${attempt + 1}")
                    }
                    return result
                }
                is ApiResult.Error -> {
                    Log.w(TAG, "❌ $operationName failed on attempt ${attempt + 1}: ${result.message}")
                    lastError = result

                    // Check if this is a token expiration error
                    if (isTokenExpiredError(result.message)) {
                        Log.i(TAG, "🔄 Detected token/auth error: ${result.message}")
                        Log.i(TAG, "🔄 Will retry with fresh token (refresh token → fallback login)")

                        // Clear current tokens to force refresh on next attempt
                        secureStorage.clearTokens()

                        // Don't return yet, continue to next retry attempt
                        if (attempt < MAX_RETRY_ATTEMPTS) {
                            val hasCredentials = hasStoredCredentials()
                            Log.i(TAG, "⏳ Retrying $operationName with fresh authentication...")
                            Log.i(TAG, "   Available fallback: ${if (hasCredentials) "✅ Stored credentials" else "❌ No credentials"}")
                            continue
                        } else {
                            Log.w(TAG, "❌ Max retries reached, authentication recovery failed")
                        }
                    }

                    // For non-token errors, return immediately
                    if (!isTokenExpiredError(result.message)) {
                        Log.w(TAG, "❌ $operationName failed with non-token error, not retrying: ${result.message}")
                        return result
                    }
                }
                is ApiResult.NetworkError -> {
                    Log.w(TAG, "❌ $operationName failed with network error on attempt ${attempt + 1}")
                    return result // Don't retry network errors
                }
            }
        }

        // All retries exhausted
        Log.e(TAG, "❌ $operationName failed after ${MAX_RETRY_ATTEMPTS + 1} attempts")
        return lastError ?: ApiResult.Error("Operation failed after maximum retry attempts")
    }
    


    /**
     * Forward SMS message directly without transaction parsing
     */
    suspend fun forwardSmsMessage(smsMessage: SmsMessage): ApiResult<String> {
        Log.i(TAG, "=== FORWARD SMS MESSAGE ===")
        Log.i(TAG, "SMS ID: ${smsMessage.id}")
        Log.i(TAG, "Sender: ${smsMessage.sender}")
        Log.i(TAG, "Bank source: ${smsMessage.bankSource}")
        Log.i(TAG, "Message length: ${smsMessage.rawMessage.length}")

        if (!isApiConfigured()) {
            Log.w(TAG, "❌ API forwarding not configured")
            return ApiResult.Error("API forwarding not configured")
        }

        Log.i(TAG, "✅ API is configured, preparing SMS data...")
        val smsData = SmsForwardRequest(
            raw_message = smsMessage.rawMessage,
            sender = smsMessage.sender,
            timestamp = smsMessage.timestamp.time
        )

        Log.i(TAG, "SMS data prepared:")
        Log.i(TAG, "  Raw message: ${smsMessage.rawMessage.take(50)}...")
        Log.i(TAG, "  Sender: ${smsMessage.sender}")
        Log.i(TAG, "  Timestamp: ${smsMessage.timestamp.time} (${smsMessage.timestamp})")

        // Use retry logic with automatic token refresh
        return executeWithRetry(
            operation = { accessToken, shouldLog ->
                Log.d(TAG, "Calling API client with token... (shouldLog: $shouldLog)")
                apiClient.forwardSms(accessToken, smsData, smsMessage.id, shouldLog)
            },
            operationName = "Forward SMS Message"
        )
    }

    /**
     * Forward multiple SMS messages in batch
     */
    suspend fun forwardSmsMessagesBatch(smsMessages: List<SmsMessage>): ApiResult<SmsBatchForwardResponse> {
        Log.i(TAG, "=== FORWARD SMS MESSAGES BATCH ===")
        Log.i(TAG, "Batch size: ${smsMessages.size}")

        if (!isApiConfigured()) {
            Log.w(TAG, "❌ API forwarding not configured")
            return ApiResult.Error("API forwarding not configured")
        }

        Log.i(TAG, "✅ API is configured, preparing batch SMS data...")
        val smsDataList = smsMessages.map { smsMessage ->
            SmsForwardRequest(
                raw_message = smsMessage.rawMessage,
                sender = smsMessage.sender,
                timestamp = smsMessage.timestamp.time
            )
        }

        Log.i(TAG, "Batch SMS data prepared:")
        Log.i(TAG, "  Total messages: ${smsDataList.size}")
        Log.i(TAG, "  First message sender: ${smsMessages.firstOrNull()?.sender}")
        Log.i(TAG, "  Last message sender: ${smsMessages.lastOrNull()?.sender}")

        // Use retry logic with automatic token refresh
        return executeWithRetry(
            operation = { accessToken, shouldLog ->
                Log.d(TAG, "Calling batch API client with token... (shouldLog: $shouldLog)")
                apiClient.forwardSmsBatch(accessToken, smsDataList, emptyList(), shouldLog)
            },
            operationName = "Forward SMS Batch"
        )
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
