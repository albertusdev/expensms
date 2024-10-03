package dev.albertus.expensms.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.text.SimpleDateFormat
import java.util.*


class SmsParserTest {

    @BeforeEach
    fun setup() {
        SmsParser.setLogger(TestLogger())
        CurrencyUtils.setLogger(TestLogger())
    }


    @Test
    fun testParseOcbcTransactionIDR() {
        val body = "Anda telah trx dgn KK OCBC 1234 28/09/24 di GOPAY Jakarta Selat IDR143,700.00. Cicilan bunga ringan s.d 24bln di ocbc.id/ocbcmobile. S&K. Info:1500999"
        val timestamp = System.currentTimeMillis()

        val result = SmsParser.parseTransaction(body, timestamp)

        assertNotNull(result, "OCBC transaction should be parsed")
        result?.let { transaction ->
            assertEquals("1234", transaction.cardLastFourDigits)
            assertEquals("GOPAY Jakarta Selat", transaction.merchant)
            assertEquals(143700.00, transaction.amount, 0.001)
            assertEquals(body, transaction.rawMessage)

            val expectedDate = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).parse("28/09/24")
            assertEquals(expectedDate, transaction.date)
        }
    }

    @Test
    fun testParseOcbcTransactionUSD() {
        val body = "Anda telah trx dgn KK OCBC 5678 30/09/24 di Amazon.com USD99.99. Cicilan bunga ringan s.d 24bln di ocbc.id/ocbcmobile. S&K. Info:1500999"
        val timestamp = System.currentTimeMillis()

        val result = SmsParser.parseTransaction(body, timestamp)

        assertNotNull(result, "OCBC transaction should be parsed")
        result?.let { transaction ->
            assertEquals("5678", transaction.cardLastFourDigits)
            assertEquals("Amazon.com", transaction.merchant)
            assertEquals(99.99, transaction.amount, 0.001)
            assertEquals(body, transaction.rawMessage)

            val expectedDate = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).parse("30/09/24")
            assertEquals(expectedDate, transaction.date)
        }
    }

    @Test
    fun testParseUobTransactionIDR() {
        val body = "Transaksi KK UOB Anda 5XXXXXXXXXXX6806 tgl 26-Sep-24 di EXPAT ROASTERS IDR 76.300,00 berhasil. RAHASIAKAN OTP Anda. Info 14008"
        val timestamp = System.currentTimeMillis()

        val result = SmsParser.parseTransaction(body, timestamp)

        assertNotNull(result, "UOB transaction should be parsed")
        result?.let { transaction ->
            assertEquals("6806", transaction.cardLastFourDigits)
            assertEquals("EXPAT ROASTERS", transaction.merchant)
            assertEquals(76300.00, transaction.amount, 0.001)
            assertEquals(body, transaction.rawMessage)

            val expectedDate = SimpleDateFormat("dd-MMM-yy", Locale.US).parse("26-Sep-24")
            assertEquals(expectedDate, transaction.date)
        }
    }

    @Test
    fun testParseUobTransactionSGD() {
        val body = "Transaksi KK UOB Anda 5XXXXXXXXXXX1234 tgl 27-Sep-24 di SINGAPORE AIRLINES SGD 500.00 berhasil. RAHASIAKAN OTP Anda. Info 14008"
        val timestamp = System.currentTimeMillis()

        val result = SmsParser.parseTransaction(body, timestamp)

        assertNotNull(result, "UOB transaction should be parsed", )
        result?.let { transaction ->
            assertEquals("1234", transaction.cardLastFourDigits)
            assertEquals("SINGAPORE AIRLINES", transaction.merchant)
            assertEquals(500.00, transaction.amount, 0.001)
            assertEquals(body, transaction.rawMessage)

            val expectedDate = SimpleDateFormat("dd-MMM-yy", Locale.US).parse("27-Sep-24")
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