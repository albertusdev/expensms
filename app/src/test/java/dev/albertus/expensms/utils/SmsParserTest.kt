package dev.albertus.expensms.utils

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

class SmsParserTest {

    @Test
    fun testParseTransaction() {
        val body = "Anda telah trx dgn KK OCBC 1234 28/09/24 di GOPAY Jakarta Selat IDR143,700.00. Cicilan bunga ringan s.d 24bln di ocbc.id/ocbcmobile. S&K. Info:1500999"
        val timestamp = System.currentTimeMillis()

        val result = SmsParser.parseTransaction(body, timestamp)

        assertNotNull(result)
        result?.let { transaction ->
            assertEquals("1234", transaction.cardLastFourDigits)
            assertEquals("GOPAY Jakarta Selat", transaction.merchant)
            assertEquals(143700.00, transaction.amount, 0.001)
            assertEquals("IDR", transaction.currency)
            assertEquals(body, transaction.rawMessage)

            val expectedDate = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).parse("28/09/24")
            assertEquals(expectedDate, transaction.date)
        }
    }

    @Test
    fun testParseTransactionInvalidInput() {
        val body = "This is not a valid transaction SMS"
        val timestamp = System.currentTimeMillis()

        val result = SmsParser.parseTransaction(body, timestamp)

        assertNull(result)
    }
}