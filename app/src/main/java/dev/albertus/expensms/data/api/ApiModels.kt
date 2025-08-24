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
data class ApiError(
    val message: String,
    val code: String? = null
)

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: String? = null) : ApiResult<Nothing>()
    data object NetworkError : ApiResult<Nothing>()
}
