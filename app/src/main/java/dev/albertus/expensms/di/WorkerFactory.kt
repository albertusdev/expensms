package dev.albertus.expensms.di

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import dev.albertus.expensms.data.repository.SenderFilterRepository
import dev.albertus.expensms.data.repository.SmsMessageRepository
import dev.albertus.expensms.utils.SimpleSmsForwardingService
import dev.albertus.expensms.utils.NotificationService
import dev.albertus.expensms.worker.SimpleSmsWorker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenSMSWorkerFactory @Inject constructor(
    private val smsMessageRepository: SmsMessageRepository,
    private val senderFilterRepository: SenderFilterRepository,
    private val simpleSmsForwardingService: SimpleSmsForwardingService,
    private val notificationService: NotificationService
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            SimpleSmsWorker::class.java.name -> {
                SimpleSmsWorker(appContext, workerParameters, smsMessageRepository, senderFilterRepository, simpleSmsForwardingService, notificationService)
            }
            else -> null
        }
    }
}
