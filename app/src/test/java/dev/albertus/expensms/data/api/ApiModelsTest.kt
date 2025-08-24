package dev.albertus.expensms.data.api

import dev.albertus.expensms.data.model.ApiLogType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ApiModelsTest {

    @Test
    fun `LoginRequest should be created correctly`() {
        // Given
        val email = "test@example.com"
        val password = "password123"

        // When
        val loginRequest = LoginRequest(email, password)

        // Then
        assertEquals(email, loginRequest.email)
        assertEquals(password, loginRequest.password)
    }

    @Test
    fun `LoginResponse should be created correctly`() {
        // Given
        val accessToken = "access_token_123"
        val refreshToken = "refresh_token_456"
        val user = UserInfo("user123", "test@example.com")

        // When
        val loginResponse = LoginResponse(accessToken, refreshToken, user)

        // Then
        assertEquals(accessToken, loginResponse.access_token)
        assertEquals(refreshToken, loginResponse.refresh_token)
        assertEquals(user, loginResponse.user)
    }

    @Test
    fun `UserInfo should be created correctly`() {
        // Given
        val id = "user123"
        val email = "test@example.com"

        // When
        val userInfo = UserInfo(id, email)

        // Then
        assertEquals(id, userInfo.id)
        assertEquals(email, userInfo.email)
    }

    @Test
    fun `SmsForwardRequest should be created correctly`() {
        // Given
        val rawMessage = "Test SMS message"
        val sender = "OCBC Info"
        val timestamp = System.currentTimeMillis()

        // When
        val smsForwardRequest = SmsForwardRequest(rawMessage, sender, timestamp)

        // Then
        assertEquals(rawMessage, smsForwardRequest.raw_message)
        assertEquals(sender, smsForwardRequest.sender)
        assertEquals(timestamp, smsForwardRequest.timestamp)
    }

    @Test
    fun `SmsForwardRequest should handle different senders correctly`() {
        // Given
        val rawMessage = "Anda telah trx dgn KK OCBC 1234 28/09/24 di GOPAY Jakarta IDR143,700.00"
        val sender1 = "OCBC"
        val sender2 = "OCBC Info"
        val timestamp = System.currentTimeMillis()

        // When
        val request1 = SmsForwardRequest(rawMessage, sender1, timestamp)
        val request2 = SmsForwardRequest(rawMessage, sender2, timestamp)

        // Then
        assertEquals(sender1, request1.sender)
        assertEquals(sender2, request2.sender)
        assertEquals(rawMessage, request1.raw_message)
        assertEquals(rawMessage, request2.raw_message)
    }

    @Test
    fun `ApiResult Success should work correctly`() {
        // Given
        val data = "Success data"

        // When
        val result = ApiResult.Success(data)

        // Then
        assertTrue(result is ApiResult.Success)
        assertEquals(data, result.data)
    }

    @Test
    fun `ApiResult Error should work correctly`() {
        // Given
        val message = "Error message"
        val code = "ERROR_CODE"

        // When
        val result = ApiResult.Error(message, code)

        // Then
        assertTrue(result is ApiResult.Error)
        assertEquals(message, result.message)
        assertEquals(code, result.code)
    }

    @Test
    fun `ApiResult NetworkError should work correctly`() {
        // When
        val result = ApiResult.NetworkError

        // Then
        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun `ApiLogType enum should have all expected values`() {
        // When/Then
        val logTypes = ApiLogType.values()
        
        assertTrue(logTypes.contains(ApiLogType.LOGIN))
        assertTrue(logTypes.contains(ApiLogType.TOKEN_REFRESH))
        assertTrue(logTypes.contains(ApiLogType.SMS_FORWARD))
        assertTrue(logTypes.contains(ApiLogType.CONNECTION_TEST))
        assertEquals(4, logTypes.size)
    }

    @Test
    fun `RefreshTokenRequest should be created correctly`() {
        // Given
        val refreshToken = "refresh_token_123"

        // When
        val request = RefreshTokenRequest(refreshToken)

        // Then
        assertEquals(refreshToken, request.refresh_token)
    }

    @Test
    fun `RefreshTokenResponse should be created correctly`() {
        // Given
        val accessToken = "new_access_token"
        val refreshToken = "new_refresh_token"

        // When
        val response = RefreshTokenResponse(accessToken, refreshToken)

        // Then
        assertEquals(accessToken, response.access_token)
        assertEquals(refreshToken, response.refresh_token)
    }

    @Test
    fun `ApiError should be created correctly`() {
        // Given
        val message = "API Error occurred"
        val code = "API_ERROR"

        // When
        val apiError = ApiError(message, code)

        // Then
        assertEquals(message, apiError.message)
        assertEquals(code, apiError.code)
    }
}
