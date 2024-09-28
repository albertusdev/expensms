package dev.albertus.expensms

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val id: String,
    val cardLastFourDigits: String,
    val date: Date,
    val merchant: String,
    val amount: Double,
    val currency: String,
    val rawMessage: String
)