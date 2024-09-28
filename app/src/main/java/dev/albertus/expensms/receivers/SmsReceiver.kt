package dev.albertus.expensms.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dev.albertus.expensms.worker.SmsParserWorker

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            messages.forEach { smsMessage ->
                val workRequest = OneTimeWorkRequestBuilder<SmsParserWorker>()
                    .setInputData(
                        workDataOf(
                            "sender" to smsMessage.displayOriginatingAddress,
                            "body" to smsMessage.messageBody,
                            "timestamp" to smsMessage.timestampMillis
                        )
                    )
                    .build()
                WorkManager.getInstance(context).enqueue(workRequest)
            }
        }
    }
}