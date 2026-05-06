package com.example.smsfirewall

import android.Manifest
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
import com.example.smsfirewall.data.SpamRetentionPreferenceStore
import com.example.smsfirewall.data.SmsRepository
import com.example.smsfirewall.data.local.SmsEntity
import com.example.smsfirewall.filter.FilterKeywordStore
import com.example.smsfirewall.filter.SmsFilterEngine
import com.example.smsfirewall.filter.SmsStatus
import com.example.smsfirewall.filter.SpamClassifier
import com.example.smsfirewall.notifications.MutedSenderStore
import com.example.smsfirewall.notifications.NotificationConstants
import com.example.smsfirewall.notifications.NotificationPreferenceStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: SmsRepository
    @Inject lateinit var filterKeywordStore: FilterKeywordStore
    @Inject lateinit var mutedSenderStore: MutedSenderStore
    @Inject lateinit var spamRetentionStore: SpamRetentionPreferenceStore
    @Inject lateinit var notificationPreferenceStore: NotificationPreferenceStore
    @Inject lateinit var spamClassifier: SpamClassifier

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
            blockedSenders  = filterKeywordStore.getBlockedSenders(),
            blockedKeywords = filterKeywordStore.getBlockedKeywords(),
            classifier      = spamClassifier
        )
        val decision = filterEngine.evaluate(sender = sender, body = body)

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
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
                    beforeTimestamp = spamRetentionStore.cutoffTimestamp()
                )

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
        // Sessiz saatler kontrolü — sessiz saatlerdeyse bildirim gösterme
        if (notificationPreferenceStore.isCurrentlyInQuietHours()) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
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

        val soundEnabled = notificationPreferenceStore.isSoundEnabled()
        val vibrationEnabled = notificationPreferenceStore.isVibrationEnabled()

        val builder = NotificationCompat.Builder(context, NotificationConstants.ALLOWED_SMS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(sender.ifBlank { context.getString(R.string.notification_unknown_sender) })
            .setContentText(body.ifBlank { context.getString(R.string.notification_new_sms) })
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // Ses ve titreşim ayarları (Android 7 ve altı için — Android 8+ kanal üzerinden yönetilir)
        if (!soundEnabled && !vibrationEnabled) {
            builder.setPriority(NotificationCompat.PRIORITY_LOW)
            builder.setDefaults(0)
            builder.setSound(null)
            builder.setVibrate(null)
        } else if (!soundEnabled) {
            builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
            builder.setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            builder.setSound(null)
        } else if (!vibrationEnabled) {
            builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
            builder.setDefaults(NotificationCompat.DEFAULT_SOUND)
            builder.setVibrate(null)
        } else {
            builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
            builder.setDefaults(NotificationCompat.DEFAULT_ALL)
        }

        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private companion object {
        private const val TAG = "SmsReceiver"
    }
}
