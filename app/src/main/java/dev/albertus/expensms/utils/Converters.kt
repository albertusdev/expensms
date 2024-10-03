package dev.albertus.expensms.utils

import androidx.room.TypeConverter
import org.javamoney.moneta.FastMoney
import java.util.Date
import javax.money.MonetaryAmount

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromMonetaryAmount(monetaryAmount: MonetaryAmount?): String? {
        return monetaryAmount?.let { "${it.currency.currencyCode}:${it.number}" }
    }

    @TypeConverter
    fun toMonetaryAmount(value: String?): MonetaryAmount? {
        return value?.let {
            val (currencyCode, amount) = it.split(":")
            FastMoney.of(amount.toBigDecimal(), currencyCode)
        }
    }
}