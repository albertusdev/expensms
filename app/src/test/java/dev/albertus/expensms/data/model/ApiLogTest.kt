package dev.albertus.expensms.data.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.util.*

class ApiLogTest {

    @Test
    fun `ApiLog should be created correctly`() {
        // Given
        val id = "log_123"
        val timestamp = Date()
        val transactionId = "txn_456"
        val logType = ApiLogType.SMS_FORWARD
        val endpoint = "/transactions/sms"
        val httpMethod = "POST"
        val requestBody = """{"raw_message": "test"}"""
        val responseCode = 201
        val responseBody = """{"id": "txn_123", "status": "processed"}"""
        val errorMessage = null
        val durationMs = 150L

        // When
        val apiLog = ApiLog(
            id = id,
            timestamp = timestamp,
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

        // Then
        assertEquals(id, apiLog.id)
        assertEquals(timestamp, apiLog.timestamp)
        assertEquals(transactionId, apiLog.transactionId)
        assertEquals(logType, apiLog.logType)
        assertEquals(endpoint, apiLog.endpoint)
        assertEquals(httpMethod, apiLog.httpMethod)
        assertEquals(requestBody, apiLog.requestBody)
        assertEquals(responseCode, apiLog.responseCode)
        assertEquals(responseBody, apiLog.responseBody)
        assertEquals(errorMessage, apiLog.errorMessage)
        assertEquals(durationMs, apiLog.durationMs)
    }

    @Test
    fun `ApiLog should handle null values correctly`() {
        // Given
        val id = "log_456"
        val timestamp = Date()
        val logType = ApiLogType.LOGIN
        val endpoint = "/auth/login"
        val httpMethod = "POST"

        // When
        val apiLog = ApiLog(
            id = id,
            timestamp = timestamp,
            transactionId = null,
            logType = logType,
            endpoint = endpoint,
            httpMethod = httpMethod,
            requestBody = null,
            responseCode = null,
            responseBody = null,
            errorMessage = "Network error",
            durationMs = null
        )

        // Then
        assertEquals(id, apiLog.id)
        assertEquals(timestamp, apiLog.timestamp)
        assertNull(apiLog.transactionId)
        assertEquals(logType, apiLog.logType)
        assertEquals(endpoint, apiLog.endpoint)
        assertEquals(httpMethod, apiLog.httpMethod)
        assertNull(apiLog.requestBody)
        assertNull(apiLog.responseCode)
        assertNull(apiLog.responseBody)
        assertEquals("Network error", apiLog.errorMessage)
        assertNull(apiLog.durationMs)
    }

    @Test
    fun `ApiLogType enum should have correct values`() {
        // When/Then
        assertEquals("LOGIN", ApiLogType.LOGIN.name)
        assertEquals("TOKEN_REFRESH", ApiLogType.TOKEN_REFRESH.name)
        assertEquals("SMS_FORWARD", ApiLogType.SMS_FORWARD.name)
        assertEquals("CONNECTION_TEST", ApiLogType.CONNECTION_TEST.name)
    }

    @Test
    fun `ApiLogWithTransaction should be created correctly`() {
        // Given
        val apiLog = ApiLog(
            id = "log_789",
            timestamp = Date(),
            transactionId = "txn_789",
            logType = ApiLogType.SMS_FORWARD,
            endpoint = "/transactions/sms",
            httpMethod = "POST",
            requestBody = null,
            responseCode = 201,
            responseBody = null,
            errorMessage = null,
            durationMs = 200L
        )

        val transaction = Transaction(
            id = "txn_789",
            bank = "OCBC",
            cardLastFourDigits = "1234",
            date = Date(),
            merchant = "Test Merchant",
            amount = 100.0,
            rawMessage = "Test SMS",
            money = org.javamoney.moneta.FastMoney.of(100, "IDR"),
            status = TransactionStatus.ACTIVE
        )

        // When
        val apiLogWithTransaction = ApiLogWithTransaction(apiLog, transaction)

        // Then
        assertEquals(apiLog, apiLogWithTransaction.apiLog)
        assertEquals(transaction, apiLogWithTransaction.transaction)
    }

    @Test
    fun `ApiLogWithTransaction should handle null transaction`() {
        // Given
        val apiLog = ApiLog(
            id = "log_no_txn",
            timestamp = Date(),
            transactionId = null,
            logType = ApiLogType.LOGIN,
            endpoint = "/auth/login",
            httpMethod = "POST",
            requestBody = null,
            responseCode = 200,
            responseBody = null,
            errorMessage = null,
            durationMs = 100L
        )

        // When
        val apiLogWithTransaction = ApiLogWithTransaction(apiLog, null)

        // Then
        assertEquals(apiLog, apiLogWithTransaction.apiLog)
        assertNull(apiLogWithTransaction.transaction)
    }
}
