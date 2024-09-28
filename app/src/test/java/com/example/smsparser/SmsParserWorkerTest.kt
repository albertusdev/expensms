import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.text.SimpleDateFormat
import java.util.*

class SmsParserWorkerTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var workerParams: WorkerParameters

    @Mock
    private lateinit var repository: TransactionRepository

    private lateinit var worker: SmsParserWorker

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        worker = SmsParserWorker(context, workerParams, repository)
    }

    @Test
    fun testParseTransaction() = runBlocking {
        val sender = "OCBC"
        val body = "Anda telah trx dgn KK OCBC 1234 28/09/24 di GOPAY Jakarta Selat IDR143,700.00. Cicilan bunga ringan s.d 24bln di ocbc.id/ocbcmobile. S&K. Info:1500999"
        val timestamp = System.currentTimeMillis()

        `when`(workerParams.inputData).thenReturn(
            workDataOf(
                "sender" to sender,
                "body" to body,
                "timestamp" to timestamp
            )
        )

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)

        val expectedDate = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).parse("28/09/24")
        val expectedTransaction = Transaction(
            id = UUID.randomUUID().toString(), // This will be different each time
            cardLastFourDigits = "1234",
            date = expectedDate ?: Date(timestamp),
            merchant = "GOPAY Jakarta Selat",
            amount = 143700.00,
            currency = "IDR",
            rawMessage = body
        )

        // You might need to use ArgumentCaptor to capture the inserted transaction
        // and compare its fields with the expected transaction
    }
}