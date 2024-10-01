package dev.albertus.expensms.utils

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*
import dev.albertus.expensms.data.SupportedBank

class SmsParserTest {

    @Before
    fun setup() {
        SmsParser.setLogger(TestLogger())
    }

    @Test
    fun testParseOcbcTransaction() {
        val body = "Anda telah trx dgn KK OCBC 1234 28/09/24 di GOPAY Jakarta Selat IDR143,700.00. Cicilan bunga ringan s.d 24bln di ocbc.id/ocbcmobile. S&K. Info:1500999"
        val timestamp = System.currentTimeMillis()

        val result = SmsParser.parseTransaction(body, timestamp)

        assertNotNull("OCBC transaction should be parsed", result)
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
    fun testParseUobTransaction() {
        val body = "Transaksi KK UOB Anda 5XXXXXXXXXXX6806 tgl 26-Sep-24 di EXPAT ROASTERS IDR 76.300,00 berhasil. RAHASIAKAN OTP Anda. Info 14008"
        val timestamp = System.currentTimeMillis()

        // Test regex matching
        val matchResult = SupportedBank.UOB.regex.find(body)
        assertNotNull("UOB regex should match the SMS body", matchResult)

        matchResult?.let {
            assertEquals("5XXXXXXXXXXX6806", it.groups["cardNumber"]?.value)
            assertEquals("26-Sep-24", it.groups["date"]?.value)
            assertEquals("EXPAT ROASTERS", it.groups["merchant"]?.value)
            assertEquals("76.300,00", it.groups["amount"]?.value)
        }

        // Test amount parsing
        val amountStr = "76.300,00"
        val parsedAmount = SupportedBank.UOB.parseAmount(amountStr)
        assertNotNull("Amount should be parsed correctly", parsedAmount)
        assertEquals(76300.00, parsedAmount!!, 0.001)

        // Test date parsing
        val dateStr = "26-Sep-24"
        val parsedDate = SupportedBank.UOB.parseDate(dateStr)
        assertNotNull("Date should be parsed correctly", parsedDate)

        val result = SmsParser.parseTransaction(body, timestamp)

        assertNotNull("UOB transaction should be parsed", result)
        result?.let { transaction ->
            assertEquals("6806", transaction.cardLastFourDigits)
            assertEquals("EXPAT ROASTERS", transaction.merchant)
            assertEquals(76300.00, transaction.amount, 0.001)
            assertEquals("IDR", transaction.currency)
            assertEquals(body, transaction.rawMessage)

            val expectedDate = SimpleDateFormat("dd-MMM-yy", Locale.US).parse("26-Sep-24")
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