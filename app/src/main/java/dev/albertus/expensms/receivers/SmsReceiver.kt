package dev.albertus.expensms.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dev.albertus.expensms.worker.SmsParserWorker

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            Log.i(TAG, "=== SMS RECEIVED ===")
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            Log.i(TAG, "Number of SMS messages: ${messages.size}")

            messages.forEachIndexed { index, smsMessage ->
                val sender = smsMessage.displayOriginatingAddress
                val body = smsMessage.messageBody
                val timestamp = smsMessage.timestampMillis

                Log.i(TAG, "SMS Message ${index + 1}:")
                Log.i(TAG, "  Sender: '$sender'")
                Log.i(TAG, "  Timestamp: ${java.util.Date(timestamp)}")
                Log.i(TAG, "  Body: ${body?.take(100)}${if ((body?.length ?: 0) > 100) "..." else ""}")

                val workRequest = OneTimeWorkRequestBuilder<SmsParserWorker>()
                    .setInputData(
                        workDataOf(
                            "sender" to sender,
                            "body" to body,
                            "timestamp" to timestamp
                        )
                    )
                    .build()

                Log.i(TAG, "  Queuing SMS for parsing with WorkManager")
                WorkManager.getInstance(context).enqueue(workRequest)
            }
        }
    }
}