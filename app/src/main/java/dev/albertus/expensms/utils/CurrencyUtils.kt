package dev.albertus.expensms.utils

import android.icu.util.CurrencyAmount
import org.javamoney.moneta.Money
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import javax.money.Monetary
import javax.money.MonetaryAmount
import javax.money.format.AmountFormatQueryBuilder
import javax.money.format.MonetaryFormats


object CurrencyUtils {
    private var logger: Logger = AndroidLogger()

    fun setLogger(newLogger: Logger) {
        logger = newLogger
    }

    fun CurrencyAmount.format(): String {
        val locale = Locale.US
        val numberFormat = NumberFormat.getCurrencyInstance(locale);
        numberFormat.currency = Currency.getInstance(locale);
        return numberFormat.format(this.number);

    }

    fun format(currencyCode: String, amount: Double): String {
        val locale = Locale.US
        val numberFormat = NumberFormat.getCurrencyInstance(locale);
        numberFormat.currency = Currency.getInstance(locale);
        return numberFormat.format(amount);
    }

    fun parse(currencyStr: String, amountStr: String): MonetaryAmount? {
        try {
            val currency = Monetary.getCurrency(currencyStr)
            val relevantLocales = getRelevantLocales(currency)

            val parsedAmounts = relevantLocales.mapNotNull { locale ->
                try {
                    val format = MonetaryFormats.getAmountFormat(
                        AmountFormatQueryBuilder.of(locale).build()
                    )
                    Money.from(format.parse("$currencyStr $amountStr"))
                } catch (e: Exception) {
                    null
                }
            }

            return parsedAmounts.maxByOrNull {
                if (it.isPositiveOrZero)
                    it.number
                else
                    it.multiply(-1).number
            }
                ?: throw IllegalArgumentException("Unable to parse amount: $amountStr for currency: $currencyStr")
        } catch (e: Exception) {
            return null;
        }
    }

    private fun getRelevantLocales(currency: javax.money.CurrencyUnit): List<Locale> {
        return when (val currencyCode = currency.currencyCode) {
            "IDR" -> listOf(Locale("id", "ID"), Locale("en", "ID"))
            else -> listOf(
                Locale.getDefault(Locale.Category.FORMAT),
                Locale.US,
                Locale.forLanguageTag(currencyCode)
            )
        }
    }
}