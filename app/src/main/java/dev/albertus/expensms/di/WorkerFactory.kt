package dev.albertus.expensms.di

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import dev.albertus.expensms.data.repository.TransactionRepository
import dev.albertus.expensms.utils.SmsForwardingService
import dev.albertus.expensms.worker.SmsParserWorker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenSMSWorkerFactory @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val smsForwardingService: SmsForwardingService
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            SmsParserWorker::class.java.name -> {
                SmsParserWorker(appContext, workerParameters, transactionRepository, smsForwardingService)
            }
            else -> null
        }
    }
}
