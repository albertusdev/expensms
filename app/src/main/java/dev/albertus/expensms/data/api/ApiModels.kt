package dev.albertus.expensms.data.api

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val access_token: String,
    val refresh_token: String,
    val user: UserInfo
)

@Serializable
data class UserInfo(
    val id: String,
    val email: String
)

@Serializable
data class RefreshTokenRequest(
    val refresh_token: String
)

@Serializable
data class RefreshTokenResponse(
    val access_token: String,
    val refresh_token: String
)

@Serializable
data class SmsForwardRequest(
    val raw_message: String,
    val sender: String,
    val timestamp: Long
)

@Serializable
data class SmsBatchForwardRequest(
    val messages: List<SmsForwardRequest>
)

@Serializable
data class SmsForwardResponse(
    val success: Boolean,
    val result: SmsBatchResult
)

@Serializable
data class SmsBatchForwardResponse(
    val success: Boolean,
    val results: List<SmsBatchResult>,
    val summary: SmsBatchSummary
)

@Serializable
data class SmsBatchResult(
    val success: Boolean? = null,
    val error: String? = null,
    val message: String? = null,
    val warning: String? = null,
    val sms: SmsInfo? = null,
    val processingAttempt: ProcessingAttemptInfo? = null,
    val workflow: WorkflowInfo? = null
)

@Serializable
data class SmsInfo(
    val id: String,
    val sender: String,
    val timestamp: String,
    val created_at: String
)

@Serializable
data class ProcessingAttemptInfo(
    val id: String,
    val status: String
)

@Serializable
data class WorkflowInfo(
    val instanceId: String? = null,
    val status: String
)

@Serializable
data class SmsBatchSummary(
    val total: Int,
    val stored: Int,
    val failed: Int,
    val queued: Int
)

@Serializable
data class ApiError(
    val message: String,
    val code: String? = null
)

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: String? = null) : ApiResult<Nothing>()
    data object NetworkError : ApiResult<Nothing>()
}
