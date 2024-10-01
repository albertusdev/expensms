package dev.albertus.expensms.data

import java.text.SimpleDateFormat
import java.util.*

enum class SupportedBank(
    val displayName: String,
    val senderFilter: String,
    val sampleSms: String,
    val regex: Regex,
    val parseAmount: (String) -> Double?,
    val parseDate: (String) -> Date?
) {
    OCBC(
        displayName = "OCBC",
        senderFilter = "OCBC",
        sampleSms = "Anda telah trx dgn KK OCBC 1234 28/09/24 di GOPAY Jakarta Selat IDR143,700.00. Cicilan bunga ringan s.d 24bln di ocbc.id/ocbcmobile. S&K. Info:1500999",
        regex = Regex("Anda telah trx dgn KK OCBC (?<cardNumber>\\d{4}) (?<date>\\d{2}/\\d{2}/\\d{2}) di (?<merchant>.+) IDR(?<amount>[\\d,]+\\.\\d{2})\\."),
        parseAmount = { it.replace(",", "").toDoubleOrNull() },
        parseDate = { SimpleDateFormat("dd/MM/yy", Locale.getDefault()).parse(it) }
    ),
    UOB(
        displayName = "UOB",
        senderFilter = "UOB",
        sampleSms = "Transaksi KK UOB Anda 5XXXXXXXXXXX6806 tgl 26-Sep-24 di EXPAT ROASTERS IDR 76.300,00 berhasil. RAHASIAKAN OTP Anda. Info 14008",
        regex = Regex("Transaksi KK UOB Anda (?<cardNumber>.+?\\d{4}) tgl (?<date>\\d{2}-\\w{3}-\\d{2}) di (?<merchant>.+) IDR (?<amount>[\\d.,]+) berhasil\\."),
        parseAmount = { it.replace(".", "").replace(",", ".").toDoubleOrNull() },
        parseDate = { SimpleDateFormat("dd-MMM-yy", Locale.US).parse(it) }
    )
}