package dev.albertus.expensms.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.albertus.expensms.MainActivity
import dev.albertus.expensms.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "NotificationService"

        // Notification Channels
        private const val CHANNEL_ID_SMS_PROCESSING = "sms_processing"
        private const val CHANNEL_ID_SMS_FORWARDING = "sms_forwarding"
        private const val CHANNEL_ID_SYNC = "sms_sync"
        private const val CHANNEL_ID_ERRORS = "errors"

        // Notification IDs
        private const val NOTIFICATION_ID_SMS_PROCESSING = 1001
        private const val NOTIFICATION_ID_SMS_FORWARDING = 1002
        private const val NOTIFICATION_ID_BATCH_FORWARDING = 1003
        private const val NOTIFICATION_ID_SYNC_PROGRESS = 1004
        private const val NOTIFICATION_ID_ERROR = 1005
        private const val NOTIFICATION_ID_FILTER_ADDED = 1006
        private const val NOTIFICATION_ID_BACKGROUND_STATUS = 1007
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // SMS Processing Channel
            val smsProcessingChannel = NotificationChannel(
                CHANNEL_ID_SMS_PROCESSING,
                "SMS Processing",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when SMS messages are received and processed"
                setShowBadge(true)
                enableVibration(true)
            }

            // SMS Forwarding Channel
            val smsForwardingChannel = NotificationChannel(
                CHANNEL_ID_SMS_FORWARDING,
                "SMS Forwarding",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for SMS forwarding to API (success/failure)"
                setShowBadge(true)
                enableVibration(true)
                enableLights(true)
            }

            // Sync Progress Channel
            val syncChannel = NotificationChannel(
                CHANNEL_ID_SYNC,
                "SMS Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Progress notifications for SMS synchronization"
                setShowBadge(false)
                enableVibration(false)
            }

            // Error Channel
            val errorChannel = NotificationChannel(
                CHANNEL_ID_ERRORS,
                "Errors",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important error notifications"
                setShowBadge(true)
                enableVibration(true)
                enableLights(true)
            }

            notificationManager.createNotificationChannels(listOf(
                smsProcessingChannel,
                smsForwardingChannel,
                syncChannel,
                errorChannel
            ))

            Log.i(TAG, "Created notification channels")
        }
    }

    fun showSmsProcessingNotification(sender: String, messagePreview: String, bankSource: String? = null, isBackground: Boolean = true) {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "Notification permission not granted - skipping SMS processing notification")
            return
        }

        Log.i(TAG, "Showing SMS processing notification - Sender: $sender, Background: $isBackground")

        val title = if (bankSource != null) {
            "SMS Processed ($bankSource)"
        } else {
            "SMS Processed"
        }

        val contentText = "From $sender: ${messagePreview.take(50)}${if (messagePreview.length > 50) "..." else ""}"

        val bigText = buildString {
            append("SMS from $sender processed and stored")
            if (bankSource != null) {
                append(" as $bankSource")
            }
            append(".\n\n")
            if (isBackground) {
                append("📱 Processed in background\n")
            }
            append("Message: $messagePreview")
        }

        // Create intent to open app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SMS_PROCESSING)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_SMS_PROCESSING, notification)
            Log.i(TAG, "✅ SMS processing notification shown")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Failed to show SMS processing notification", e)
        }
    }

    fun showSmsForwardingNotification(sender: String, success: Boolean, errorMessage: String? = null, bankSource: String? = null) {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "Notification permission not granted - skipping SMS forwarding notification")
            return
        }

        Log.i(TAG, "Showing SMS forwarding notification - Sender: $sender, Success: $success")

        val title = if (success) {
            "✅ SMS Forwarded${if (bankSource != null) " ($bankSource)" else ""}"
        } else {
            "❌ SMS Forward Failed${if (bankSource != null) " ($bankSource)" else ""}"
        }

        val content = if (success) {
            "SMS from $sender successfully forwarded to aiccountant API"
        } else {
            "Failed to forward SMS from $sender: ${errorMessage ?: "Unknown error"}"
        }

        val bigText = if (success) {
            buildString {
                append("✅ SUCCESS: SMS forwarded to aiccountant API\n\n")
                append("Sender: $sender\n")
                if (bankSource != null) {
                    append("Bank: $bankSource\n")
                }
                append("Time: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}\n")
                append("Status: Processing in aiccountant system")
            }
        } else {
            buildString {
                append("❌ FAILED: Could not forward SMS to API\n\n")
                append("Sender: $sender\n")
                if (bankSource != null) {
                    append("Bank: $bankSource\n")
                }
                append("Error: ${errorMessage ?: "Unknown error"}\n")
                append("Time: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
            }
        }

        // Create intent to open app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SMS_FORWARDING)
            .setSmallIcon(if (success) android.R.drawable.ic_menu_send else android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(if (success) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setColor(if (success) 0xFF4CAF50.toInt() else 0xFFF44336.toInt()) // Green for success, red for error
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_SMS_FORWARDING, notification)
            Log.i(TAG, "✅ SMS forwarding notification shown")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Failed to show SMS forwarding notification", e)
        }
    }

    fun showBatchForwardingNotification(successCount: Int, totalCount: Int, failedCount: Int = totalCount - successCount) {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "Notification permission not granted - skipping batch forwarding notification")
            return
        }

        Log.i(TAG, "Showing batch forwarding notification - Success: $successCount, Total: $totalCount")

        val title = when {
            successCount == totalCount -> "✅ Batch Forward Complete"
            successCount == 0 -> "❌ Batch Forward Failed"
            else -> "⚠️ Batch Forward Partial"
        }

        val content = "$successCount of $totalCount SMS messages forwarded successfully"

        val bigText = buildString {
            append("Batch SMS forwarding completed\n\n")
            append("✅ Successful: $successCount\n")
            if (failedCount > 0) {
                append("❌ Failed: $failedCount\n")
            }
            append("📊 Total: $totalCount\n")
            append("Time: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
        }

        // Create intent to open app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SMS_FORWARDING)
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setProgress(totalCount, successCount, false)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BATCH_FORWARDING, notification)
            Log.i(TAG, "✅ Batch forwarding notification shown")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Failed to show batch forwarding notification", e)
        }
    }

    fun showSyncProgressNotification(progress: Int, total: Int, isUpdatingExisting: Boolean = false) {
        if (!hasNotificationPermission()) return

        val phase = if (isUpdatingExisting) "Updating existing SMS" else "Processing new SMS"
        val title = "SMS Sync in Progress"
        val content = "$phase: $progress of $total"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SYNC)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle(title)
            .setContentText(content)
            .setProgress(total, progress, false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_SYNC_PROGRESS, notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Failed to show sync progress notification", e)
        }
    }

    fun hideSyncProgressNotification() {
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_SYNC_PROGRESS)
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Failed to hide sync progress notification", e)
        }
    }

    fun showFilterAddedNotification(filterName: String, pattern: String, updatedSmsCount: Int) {
        if (!hasNotificationPermission()) return

        Log.i(TAG, "Showing filter added notification - Filter: $filterName, Updated SMS: $updatedSmsCount")

        val title = "✅ Filter Added: $filterName"
        val content = "Pattern '$pattern' - Updated $updatedSmsCount existing SMS"

        val bigText = buildString {
            append("New sender filter added successfully\n\n")
            append("Filter Name: $filterName\n")
            append("Pattern: '$pattern'\n")
            append("Updated SMS: $updatedSmsCount existing messages\n")
            append("Future SMS from this sender will be automatically processed")
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "senderFilters")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SMS_PROCESSING)
            .setSmallIcon(android.R.drawable.ic_menu_add)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(0xFF4CAF50.toInt()) // Green
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_FILTER_ADDED, notification)
            Log.i(TAG, "✅ Filter added notification shown")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Failed to show filter added notification", e)
        }
    }

    fun showBackgroundStatusNotification(isActive: Boolean, processedCount: Int = 0) {
        if (!hasNotificationPermission()) return

        val title = if (isActive) "📱 ExpenSMS Active" else "⏸️ ExpenSMS Inactive"
        val content = if (isActive) {
            "Background SMS processing active${if (processedCount > 0) " - $processedCount processed today" else ""}"
        } else {
            "Background SMS processing inactive - check permissions"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SMS_PROCESSING)
            .setSmallIcon(if (isActive) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(isActive)
            .setAutoCancel(!isActive)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BACKGROUND_STATUS, notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Failed to show background status notification", e)
        }
    }

    fun showErrorNotification(title: String, message: String, throwable: Throwable? = null) {
        if (!hasNotificationPermission()) return

        Log.e(TAG, "Showing error notification - Title: $title, Message: $message", throwable)

        val bigText = buildString {
            append("❌ ERROR: $message\n\n")
            append("Time: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}\n")
            if (throwable != null) {
                append("Details: ${throwable.message ?: throwable.javaClass.simpleName}")
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ERRORS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("❌ $title")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(0xFFF44336.toInt()) // Red
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_ERROR, notification)
            Log.i(TAG, "✅ Error notification shown")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Failed to show error notification", e)
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // No runtime permission needed for older versions
        }
    }
}
