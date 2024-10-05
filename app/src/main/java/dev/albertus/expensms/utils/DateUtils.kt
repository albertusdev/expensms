package dev.albertus.expensms.utils

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

object DateUtils {
    private val dateFormat = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")
    private val fullDateTimeFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    fun LocalDate.formatToReadable(): String {
        return format(dateFormat)
    }

    fun Date.formatToTimeOnly(): String {
        val instant = toInstant()
        val zoneId = ZoneId.systemDefault()
        val locale = Locale.getDefault()

        val zoneName = when (zoneId.id) {
            "Asia/Jakarta" -> "WIB"
            "Asia/Makassar" -> "WITA"
            "Asia/Jayapura" -> "WIT"
            else -> zoneId.getDisplayName(java.time.format.TextStyle.SHORT, locale)

        }

        val formatter = DateTimeFormatter.ofPattern("hh:mm a '$zoneName'", locale)
        return instant.atZone(zoneId).format(formatter)
    }

    fun Date.formatFullDateTime(): String {
        return toInstant().atZone(ZoneId.systemDefault()).format(fullDateTimeFormat)
    }
}
