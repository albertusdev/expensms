package dev.albertus.expensms.data.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.util.Date

@Entity(tableName = "api_logs")
data class ApiLog(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "timestamp") val timestamp: Date,
    @ColumnInfo(name = "transaction_id") val transactionId: String?,
    @ColumnInfo(name = "log_type") val logType: ApiLogType,
    @ColumnInfo(name = "endpoint") val endpoint: String,
    @ColumnInfo(name = "http_method") val httpMethod: String,
    @ColumnInfo(name = "request_body") val requestBody: String?,
    @ColumnInfo(name = "response_code") val responseCode: Int?,
    @ColumnInfo(name = "response_body") val responseBody: String?,
    @ColumnInfo(name = "error_message") val errorMessage: String?,
    @ColumnInfo(name = "duration_ms") val durationMs: Long?
)

enum class ApiLogType {
    LOGIN,
    TOKEN_REFRESH,
    SMS_FORWARD,
    CONNECTION_TEST
}

data class ApiLogWithTransaction(
    @Embedded val apiLog: ApiLog,
    @Relation(
        parentColumn = "transaction_id",
        entityColumn = "id"
    )
    val transaction: Transaction?
)
