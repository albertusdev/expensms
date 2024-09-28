package dev.albertus.expensms

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "cardLastFourDigits") val cardLastFourDigits: String,
    @ColumnInfo(name = "date") val date: Date,
    @ColumnInfo(name = "merchant") val merchant: String,
    @ColumnInfo(name = "amount") val amount: Double,
    @ColumnInfo(name = "currency") val currency: String,
    @ColumnInfo(name = "rawMessage") val rawMessage: String
)