package dev.albertus.expensms.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import android.icu.util.Currency
import android.icu.util.CurrencyAmount
import java.util.Date

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "bank") val bank: String,
    @ColumnInfo(name = "cardLastFourDigits") val cardLastFourDigits: String,
    @ColumnInfo(name = "date") val date: Date,
    @ColumnInfo(name = "merchant") val merchant: String,
    @ColumnInfo(name = "amount") val amount: Double,
    @ColumnInfo(name = "currency") val currencyCode: String,
    @ColumnInfo(name = "rawMessage") val rawMessage: String
) {
    val currency: Currency
        get() = Currency.getInstance((currencyCode))

    val currencyAmount: CurrencyAmount
        get() = CurrencyAmount(amount, Currency.getInstance(currencyCode))

    val formattedAmountWithCurrency: String
        get() = currencyAmount.toString()
}