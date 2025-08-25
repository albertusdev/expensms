package dev.albertus.expensms.data.api

import dev.albertus.expensms.data.model.ApiLogType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiClient @Inject constructor(
    private val apiLogger: ApiLogger
) {
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
        )
        .build()
    
    companion object {
        private const val BASE_URL = "https://api.aiccountant.id"
        private const val CONTENT_TYPE_JSON = "application/json"
    }
    
    suspend fun login(email: String, password: String): ApiResult<LoginResponse> {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val endpoint = "/auth/login"
            val requestBody = json.encodeToString(
                LoginRequest.serializer(),
                LoginRequest(email, password)
            )

            try {
                val request = Request.Builder()
                    .url("$BASE_URL$endpoint")
                    .post(requestBody.toRequestBody(CONTENT_TYPE_JSON.toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                val duration = System.currentTimeMillis() - startTime
                val responseBody = response.body?.string()

                if (response.isSuccessful) {
                    if (responseBody != null) {
                        val loginResponse = json.decodeFromString<LoginResponse>(responseBody)

                        apiLogger.logApiCall(
                            logType = ApiLogType.LOGIN,
                            endpoint = endpoint,
                            httpMethod = "POST",
                            requestBody = requestBody,
                            responseCode = response.code,
                            responseBody = responseBody,
                            durationMs = duration
                        )

                        ApiResult.Success(loginResponse)
                    } else {
                        val errorMsg = "Empty response body"
                        apiLogger.logApiCall(
                            logType = ApiLogType.LOGIN,
                            endpoint = endpoint,
                            httpMethod = "POST",
                            requestBody = requestBody,
                            responseCode = response.code,
                            errorMessage = errorMsg,
                            durationMs = duration
                        )
                        ApiResult.Error(errorMsg)
                    }
                } else {
                    val errorMessage = try {
                        responseBody?.let { json.decodeFromString<ApiError>(it).message }
                    } catch (e: Exception) {
                        null
                    } ?: "Login failed with status ${response.code}"

                    apiLogger.logApiCall(
                        logType = ApiLogType.LOGIN,
                        endpoint = endpoint,
                        httpMethod = "POST",
                        requestBody = requestBody,
                        responseCode = response.code,
                        responseBody = responseBody,
                        errorMessage = errorMessage,
                        durationMs = duration
                    )

                    ApiResult.Error(errorMessage, response.code.toString())
                }
            } catch (e: IOException) {
                val duration = System.currentTimeMillis() - startTime
                apiLogger.logApiCall(
                    logType = ApiLogType.LOGIN,
                    endpoint = endpoint,
                    httpMethod = "POST",
                    requestBody = requestBody,
                    errorMessage = "Network error: ${e.message}",
                    durationMs = duration
                )
                ApiResult.NetworkError
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                val errorMsg = "Unexpected error: ${e.message}"
                apiLogger.logApiCall(
                    logType = ApiLogType.LOGIN,
                    endpoint = endpoint,
                    httpMethod = "POST",
                    requestBody = requestBody,
                    errorMessage = errorMsg,
                    durationMs = duration
                )
                ApiResult.Error(errorMsg)
            }
        }
    }
    
    suspend fun refreshToken(refreshToken: String): ApiResult<RefreshTokenResponse> {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val endpoint = "/auth/refresh"
            val requestBody = json.encodeToString(
                RefreshTokenRequest.serializer(),
                RefreshTokenRequest(refreshToken)
            )

            try {
                val request = Request.Builder()
                    .url("$BASE_URL$endpoint")
                    .post(requestBody.toRequestBody(CONTENT_TYPE_JSON.toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                val duration = System.currentTimeMillis() - startTime
                val responseBody = response.body?.string()

                if (response.isSuccessful) {
                    if (responseBody != null) {
                        val refreshResponse = json.decodeFromString<RefreshTokenResponse>(responseBody)

                        apiLogger.logApiCall(
                            logType = ApiLogType.TOKEN_REFRESH,
                            endpoint = endpoint,
                            httpMethod = "POST",
                            requestBody = requestBody,
                            responseCode = response.code,
                            responseBody = responseBody,
                            durationMs = duration
                        )

                        ApiResult.Success(refreshResponse)
                    } else {
                        val errorMsg = "Empty response body"
                        apiLogger.logApiCall(
                            logType = ApiLogType.TOKEN_REFRESH,
                            endpoint = endpoint,
                            httpMethod = "POST",
                            requestBody = requestBody,
                            responseCode = response.code,
                            errorMessage = errorMsg,
                            durationMs = duration
                        )
                        ApiResult.Error(errorMsg)
                    }
                } else {
                    val errorMsg = "Token refresh failed"
                    apiLogger.logApiCall(
                        logType = ApiLogType.TOKEN_REFRESH,
                        endpoint = endpoint,
                        httpMethod = "POST",
                        requestBody = requestBody,
                        responseCode = response.code,
                        responseBody = responseBody,
                        errorMessage = errorMsg,
                        durationMs = duration
                    )
                    ApiResult.Error(errorMsg, response.code.toString())
                }
            } catch (e: IOException) {
                val duration = System.currentTimeMillis() - startTime
                apiLogger.logApiCall(
                    logType = ApiLogType.TOKEN_REFRESH,
                    endpoint = endpoint,
                    httpMethod = "POST",
                    requestBody = requestBody,
                    errorMessage = "Network error: ${e.message}",
                    durationMs = duration
                )
                ApiResult.NetworkError
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                val errorMsg = "Unexpected error: ${e.message}"
                apiLogger.logApiCall(
                    logType = ApiLogType.TOKEN_REFRESH,
                    endpoint = endpoint,
                    httpMethod = "POST",
                    requestBody = requestBody,
                    errorMessage = errorMsg,
                    durationMs = duration
                )
                ApiResult.Error(errorMsg)
            }
        }
    }
    
    suspend fun forwardSms(
        accessToken: String,
        smsData: SmsForwardRequest,
        transactionId: String? = null,
        shouldLog: Boolean = true
    ): ApiResult<String> {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val endpoint = "/sms/"
            val requestBody = json.encodeToString(
                SmsForwardRequest.serializer(),
                smsData
            )

            try {
                val request = Request.Builder()
                    .url("$BASE_URL$endpoint")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .post(requestBody.toRequestBody(CONTENT_TYPE_JSON.toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                val duration = System.currentTimeMillis() - startTime
                val responseBody = response.body?.string()

                if (response.isSuccessful) {
                    // Always log successful API calls
                    apiLogger.logApiCall(
                        logType = ApiLogType.SMS_FORWARD,
                        endpoint = endpoint,
                        httpMethod = "POST",
                        requestBody = requestBody,
                        responseCode = response.code,
                        responseBody = responseBody,
                        durationMs = duration,
                        transactionId = transactionId
                    )
                    ApiResult.Success("SMS forwarded successfully")
                } else {
                    val errorMessage = try {
                        responseBody?.let { json.decodeFromString<ApiError>(it).message }
                    } catch (e: Exception) {
                        null
                    } ?: "SMS forward failed with status ${response.code}"

                    // Only log failures when shouldLog is true (to suppress retry failures)
                    if (shouldLog) {
                        apiLogger.logApiCall(
                            logType = ApiLogType.SMS_FORWARD,
                            endpoint = endpoint,
                            httpMethod = "POST",
                            requestBody = requestBody,
                            responseCode = response.code,
                            responseBody = responseBody,
                            errorMessage = errorMessage,
                            durationMs = duration,
                            transactionId = transactionId
                        )
                    }

                    ApiResult.Error(errorMessage, response.code.toString())
                }
            } catch (e: IOException) {
                val duration = System.currentTimeMillis() - startTime
                if (shouldLog) {
                    apiLogger.logApiCall(
                        logType = ApiLogType.SMS_FORWARD,
                        endpoint = endpoint,
                        httpMethod = "POST",
                        requestBody = requestBody,
                        errorMessage = "Network error: ${e.message}",
                        durationMs = duration,
                        transactionId = transactionId
                    )
                }
                ApiResult.NetworkError
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                val errorMsg = "Unexpected error: ${e.message}"
                if (shouldLog) {
                    apiLogger.logApiCall(
                        logType = ApiLogType.SMS_FORWARD,
                        endpoint = endpoint,
                        httpMethod = "POST",
                        requestBody = requestBody,
                        errorMessage = errorMsg,
                        durationMs = duration,
                        transactionId = transactionId
                    )
                }
                ApiResult.Error(errorMsg)
            }
        }
    }

    suspend fun forwardSmsBatch(
        accessToken: String,
        smsDataList: List<SmsForwardRequest>,
        transactionIds: List<String> = emptyList(),
        shouldLog: Boolean = true
    ): ApiResult<SmsBatchForwardResponse> {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val endpoint = "/sms/batch"
            val batchRequest = SmsBatchForwardRequest(messages = smsDataList)
            val requestBody = json.encodeToString(
                SmsBatchForwardRequest.serializer(),
                batchRequest
            )

            try {
                val request = Request.Builder()
                    .url("$BASE_URL$endpoint")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .post(requestBody.toRequestBody(CONTENT_TYPE_JSON.toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                val duration = System.currentTimeMillis() - startTime
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val batchResponse = json.decodeFromString<SmsBatchForwardResponse>(responseBody)
                    // Always log successful API calls
                    apiLogger.logApiCall(
                        logType = ApiLogType.SMS_BATCH_FORWARD,
                        endpoint = endpoint,
                        httpMethod = "POST",
                        requestBody = requestBody,
                        responseCode = response.code,
                        responseBody = responseBody,
                        durationMs = duration,
                        transactionId = transactionIds.joinToString(",")
                    )
                    ApiResult.Success(batchResponse)
                } else {
                    val errorMessage = try {
                        responseBody?.let { json.decodeFromString<ApiError>(it).message }
                    } catch (e: Exception) {
                        null
                    } ?: "SMS batch forward failed with status ${response.code}"

                    // Only log failures when shouldLog is true (to suppress retry failures)
                    if (shouldLog) {
                        apiLogger.logApiCall(
                            logType = ApiLogType.SMS_BATCH_FORWARD,
                            endpoint = endpoint,
                            httpMethod = "POST",
                            requestBody = requestBody,
                            responseCode = response.code,
                            responseBody = responseBody,
                            errorMessage = errorMessage,
                            durationMs = duration,
                            transactionId = transactionIds.joinToString(",")
                        )
                    }
                    ApiResult.Error(errorMessage, response.code.toString())
                }
            } catch (e: IOException) {
                val duration = System.currentTimeMillis() - startTime
                if (shouldLog) {
                    apiLogger.logApiCall(
                        logType = ApiLogType.SMS_BATCH_FORWARD,
                        endpoint = endpoint,
                        httpMethod = "POST",
                        requestBody = requestBody,
                        errorMessage = "Network error: ${e.message}",
                        durationMs = duration,
                        transactionId = transactionIds.joinToString(",")
                    )
                }
                ApiResult.NetworkError
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                val errorMsg = "Unexpected error: ${e.message}"
                if (shouldLog) {
                    apiLogger.logApiCall(
                        logType = ApiLogType.SMS_BATCH_FORWARD,
                        endpoint = endpoint,
                        httpMethod = "POST",
                        requestBody = requestBody,
                        errorMessage = errorMsg,
                        durationMs = duration,
                        transactionId = transactionIds.joinToString(",")
                    )
                }
                ApiResult.Error(errorMsg)
            }
        }
    }
}
