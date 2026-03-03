package com.example.smsfirewall

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.smsfirewall.data.DatabaseProvider
import com.example.smsfirewall.data.SpamRetentionPolicy
import com.example.smsfirewall.data.SmsRepository
import com.example.smsfirewall.data.local.SmsEntity
import com.example.smsfirewall.filter.SmsFilterEngine
import com.example.smsfirewall.filter.SmsStatus
import com.example.smsfirewall.notifications.MutedSenderStore
import com.example.smsfirewall.notifications.NotificationConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return

        val sender = messages.firstOrNull()?.displayOriginatingAddress.orEmpty()
        val body = messages.joinToString(separator = "") { message ->
            message.messageBody.orEmpty()
        }
        val receivedAt = messages.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()

        val filterEngine = SmsFilterEngine(
            blockedKeywords = setOf("free", "winner", "loan", "debt", "credit")
        )
        val decision = filterEngine.evaluate(sender = sender, body = body)

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val repository = SmsRepository(DatabaseProvider.getDatabase(context).smsDao())
                if (decision.status == SmsStatus.BLOCK) {
                    repository.insert(
                        SmsEntity(
                            sender = sender,
                            body = body,
                            receivedAt = receivedAt,
                            status = SmsStatus.BLOCK,
                            reason = decision.reason
                        )
                    )
                } else {
                    insertIntoSystemInbox(
                        context = context,
                        sender = sender,
                        body = body,
                        receivedAt = receivedAt
                    )
                }

                repository.deleteByStatusBefore(
                    status = SmsStatus.BLOCK,
                    beforeTimestamp = SpamRetentionPolicy.cutoffTimestamp()
                )

                val mutedSenderStore = MutedSenderStore(context)
                if (decision.status != SmsStatus.BLOCK && !mutedSenderStore.isMuted(sender)) {
                    showAllowedSmsNotification(context, sender, body)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun insertIntoSystemInbox(
        context: Context,
        sender: String,
        body: String,
        receivedAt: Long
    ) {
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, sender)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, receivedAt)
            put(Telephony.Sms.READ, 0)
            put(Telephony.Sms.SEEN, 0)
        }

        runCatching {
            context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
        }.onFailure { throwable ->
            Log.e(TAG, "Failed to insert allowed SMS into system provider", throwable)
        }
    }

    private fun showAllowedSmsNotification(context: Context, sender: String, body: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NotificationConstants.ALLOWED_SMS_CHANNEL_ID,
                "Allowed SMS",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationConstants.ALLOWED_SMS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(sender.ifBlank { "Unknown sender" })
            .setContentText(body.ifBlank { "New SMS received" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
    }

    private companion object {
        private const val TAG = "SmsReceiver"
    }
}
