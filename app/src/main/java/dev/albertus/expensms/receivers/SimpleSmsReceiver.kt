package dev.albertus.expensms.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dev.albertus.expensms.worker.SimpleSmsWorker

class SimpleSmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SimpleSmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            Log.i(TAG, "=== SMS RECEIVED IN BACKGROUND ===")
            Log.i(TAG, "App process state: ${if (isAppInForeground(context)) "FOREGROUND" else "BACKGROUND/KILLED"}")

            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            Log.i(TAG, "Number of SMS messages: ${messages.size}")

            messages.forEachIndexed { index, smsMessage ->
                val sender = smsMessage.displayOriginatingAddress
                val body = smsMessage.messageBody
                val timestamp = smsMessage.timestampMillis

                Log.i(TAG, "=== SMS Message ${index + 1} ===")
                Log.i(TAG, "  Raw sender: '$sender'")
                Log.i(TAG, "  Sender length: ${sender?.length ?: 0}")
                Log.i(TAG, "  Sender bytes: ${sender?.toByteArray()?.joinToString { it.toString() } ?: "null"}")
                Log.i(TAG, "  Timestamp: ${java.util.Date(timestamp)}")
                Log.i(TAG, "  Body preview: ${body?.take(100)}${if ((body?.length ?: 0) > 100) "..." else ""}")
                Log.i(TAG, "  Body length: ${body?.length ?: 0}")

                val workRequest = OneTimeWorkRequestBuilder<SimpleSmsWorker>()
                    .setInputData(
                        workDataOf(
                            "sender" to sender,
                            "body" to body,
                            "timestamp" to timestamp,
                            "is_background" to true // Mark as background processing
                        )
                    )
                    .build()

                Log.i(TAG, "  ✅ Queuing SMS for processing with WorkManager (ID: ${workRequest.id})")
                WorkManager.getInstance(context).enqueue(workRequest)
            }
            Log.i(TAG, "=== SMS RECEIVER COMPLETED ===")
        } else {
            Log.w(TAG, "Received non-SMS intent: ${intent.action}")
        }
    }

    private fun isAppInForeground(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        val packageName = context.packageName
        for (appProcess in appProcesses) {
            if (appProcess.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                appProcess.processName == packageName) {
                return true
            }
        }
        return false
    }
}
