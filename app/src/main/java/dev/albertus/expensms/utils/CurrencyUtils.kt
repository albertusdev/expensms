package dev.albertus.expensms.utils

import dev.albertus.expensms.data.model.Transaction
import org.javamoney.moneta.Money
import org.javamoney.moneta.format.CurrencyStyle
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import javax.money.Monetary
import javax.money.MonetaryAmount
import javax.money.format.AmountFormatQueryBuilder
import javax.money.format.MonetaryFormats


object CurrencyUtils {
    private var logger: Logger = AndroidLogger()
    private val currencyToLocaleMap: MutableMap<String, Locale> = mutableMapOf()

    fun setLogger(newLogger: Logger) {
        logger = newLogger
    }

    fun sumAmounts(transactions: List<Transaction>): MonetaryAmount {
        return transactions
                .map { it.money }
                .reduce { acc, amount -> acc.add(amount) }
    }

    fun MonetaryAmount.format(): String {
        val currency = this.currency
        val locale = getPreferredLocaleForCurrency(currency)

        val formatQuery = AmountFormatQueryBuilder.of(locale)
            .set(CurrencyStyle.SYMBOL)
            .build()

        val formatter = MonetaryFormats.getAmountFormat(formatQuery)
        return formatter.format(this)
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

    private fun getPreferredLocaleForCurrency(currency: javax.money.CurrencyUnit): Locale {
        val currencyCode = currency.currencyCode
        return currencyToLocaleMap.getOrPut(currencyCode) {
            findPreferredLocale(currencyCode)
        }
    }

    private fun findPreferredLocale(currencyCode: String): Locale {
        val availableLocales = Locale.getAvailableLocales().toList()

        // Helper function to safely get currency for a locale
        fun safeGetCurrency(locale: Locale): Currency? {
            return try {
                Currency.getInstance(locale)
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        // First, try to find an exact match for language and country
        val exactMatch = availableLocales.find { locale ->
            locale.country.isNotEmpty() &&
                    safeGetCurrency(locale)?.currencyCode == currencyCode &&
                    locale.toString().lowercase() == locale.toString()
        }
        if (exactMatch != null) return exactMatch

        // If no exact match, try to find a match for the country
        val countryMatch = availableLocales.find { locale ->
            locale.country.isNotEmpty() &&
                    safeGetCurrency(locale)?.currencyCode == currencyCode
        }
        if (countryMatch != null) return countryMatch

        // If still no match, fall back to the first locale that uses this currency
        return availableLocales.find { locale ->
            safeGetCurrency(locale)?.currencyCode == currencyCode
        } ?: Locale.getDefault()
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