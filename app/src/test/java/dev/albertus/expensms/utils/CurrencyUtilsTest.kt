package dev.albertus.expensms.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal


class CurrencyUtilsTest {

    @BeforeEach
    fun setup() {
        CurrencyUtils.setLogger(TestLogger())
    }

    @ParameterizedTest
    @CsvSource(
        "IDR; 15.300,00; 15300",
        "IDR; 15,300.00; 15300",
        "IDR; 1,234; 1234",
        "IDR; 1.234; 1234",
        "IDR; 15300; 15300",
        "USD; 1,234.56; 1234.56",
        "EUR; 1,234.56; 1234.56",
        "INR; 1,00,000.00; 100000",
        delimiterString = ";"
    )
    fun `parse correctly parses IDR amount with various separators`(
        currencyCode: String,
        amount: String,
        expected: String,
    ) {
        val result = CurrencyUtils.parse(currencyCode, amount)!!
        Assertions.assertTrue(
            expected.toBigDecimal()
                .compareTo(result.number.numberValue(BigDecimal::class.java)) == 0
        )
    }

    @ParameterizedTest
    @CsvSource(
        "IDR; 1.000.000,00; 1000000",
        "IDR; 100.000.000,00; 100000000",
        "IDR; 1.000.000.000,00; 1000000000",
        "IDR; 1,000,000,000.00; 1000000000",
        "USD; 1,000,000; 1000000",
        delimiterString = ";"
    )
    fun `parse handles large numbers correctly`(
        currencyCode: String,
        amountStr: String,
        expected: Long
    ) {
        val result = CurrencyUtils.parse(currencyCode, amountStr)!!
        Assertions.assertEquals(expected, result.number.longValueExact())
    }

    @ParameterizedTest
    @CsvSource(
        "IDR; -15,300.00; -15300.00",
        "IDR; -150.300,00; -150300.00",
        "USD; -1,000.50; -1000.50",
        "EUR; -1,000.50; -1000.50",
        delimiterString = ";"
    )
    fun `parse handles negative numbers correctly`(
        currencyCode: String,
        amountStr: String,
        expected: String
    ) {
        val result = CurrencyUtils.parse(currencyCode, amountStr)!!
        Assertions.assertTrue(
            expected.toBigDecimal()
                .compareTo(result.number.numberValue(BigDecimal::class.java)) == 0
        )
    }

    @Test
    fun `parse throws exception for invalid amount string`() {
        Assertions.assertNull(CurrencyUtils.parse("USD", "invalid"))
    }

    @Test
    fun `parse throws exception for invalid currency code`() {
        Assertions.assertNull(CurrencyUtils.parse("XYZ", "1000"));
    }
}