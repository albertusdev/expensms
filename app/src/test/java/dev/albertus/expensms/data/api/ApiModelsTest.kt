package dev.albertus.expensms.data.api

import dev.albertus.expensms.data.model.ApiLogType
import kotlinx.serialization.json.Json
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
        assertTrue(logTypes.contains(ApiLogType.SMS_BATCH_FORWARD))
        assertTrue(logTypes.contains(ApiLogType.CONNECTION_TEST))
        assertEquals(5, logTypes.size)
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

    @Test
    fun `SmsBatchForwardRequest should be created correctly`() {
        // Given
        val message1 = SmsForwardRequest("Test message 1", "OCBC", 1234567890L)
        val message2 = SmsForwardRequest("Test message 2", "OCBC", 1234567891L)
        val messages = listOf(message1, message2)

        // When
        val batchRequest = SmsBatchForwardRequest(messages)

        // Then
        assertEquals(messages, batchRequest.messages)
        assertEquals(2, batchRequest.messages.size)
    }

    @Test
    fun `SmsBatchForwardResponse should be created correctly`() {
        // Given
        val result1 = SmsBatchResult(success = true)
        val result2 = SmsBatchResult(success = false, error = "Test error")
        val results = listOf(result1, result2)
        val summary = SmsBatchSummary(total = 2, stored = 1, failed = 1, queued = 1)

        // When
        val response = SmsBatchForwardResponse(
            success = true,
            results = results,
            summary = summary
        )

        // Then
        assertTrue(response.success)
        assertEquals(results, response.results)
        assertEquals(summary, response.summary)
        assertEquals(2, response.results.size)
    }

    @Test
    fun `SmsBatchSummary should be created correctly`() {
        // Given
        val total = 10
        val stored = 8
        val failed = 2
        val queued = 8

        // When
        val summary = SmsBatchSummary(total, stored, failed, queued)

        // Then
        assertEquals(total, summary.total)
        assertEquals(stored, summary.stored)
        assertEquals(failed, summary.failed)
        assertEquals(queued, summary.queued)
    }

    @Test
    fun `SmsBatchResult should handle success case correctly`() {
        // Given
        val smsInfo = SmsInfo("sms_123", "OCBC", "2023-01-01T10:00:00Z", "2023-01-01T10:00:00Z")
        val processingAttempt = ProcessingAttemptInfo("attempt_123", "pending")
        val workflow = WorkflowInfo("workflow_123", "queued")

        // When
        val result = SmsBatchResult(
            success = true,
            sms = smsInfo,
            processingAttempt = processingAttempt,
            workflow = workflow
        )

        // Then
        assertTrue(result.success == true)
        assertEquals(smsInfo, result.sms)
        assertEquals(processingAttempt, result.processingAttempt)
        assertEquals(workflow, result.workflow)
        assertNull(result.error)
        assertNull(result.warning)
    }

    @Test
    fun `WorkflowInfo should handle missing instanceId correctly`() {
        // Given - API response without instanceId (like the actual API response)
        val workflowJson = """{"status":"queued"}"""

        // When
        val workflow = Json.decodeFromString<WorkflowInfo>(workflowJson)

        // Then
        assertNull(workflow.instanceId)
        assertEquals("queued", workflow.status)
    }

    @Test
    fun `SmsBatchResult should handle error case correctly`() {
        // Given
        val errorMessage = "Invalid SMS format"

        // When
        val result = SmsBatchResult(
            success = false,
            error = errorMessage
        )

        // Then
        assertTrue(result.success == false)
        assertEquals(errorMessage, result.error)
        assertNull(result.sms)
        assertNull(result.processingAttempt)
        assertNull(result.workflow)
        assertNull(result.warning)
    }
}
