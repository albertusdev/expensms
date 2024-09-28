package dev.albertus.expensms.utils

import java.util.Locale

object CurrencyUtils {
    private val indonesianLocale = Locale("id", "ID")

    fun Double.formatAsCurrency(): String {
        return String.format(indonesianLocale, "Rp%,.0f", this)
    }
}