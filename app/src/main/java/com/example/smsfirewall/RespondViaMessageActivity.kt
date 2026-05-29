package com.example.smsfirewall

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log

class RespondViaMessageService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_RESPOND_VIA_MESSAGE) {
            sendRespondViaMessage(intent)
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }

    private fun sendRespondViaMessage(intent: Intent) {
        val recipients = intent.data?.schemeSpecificPart
            ?.split(';', ',')
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            .orEmpty()
        val message = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
            ?: intent.getCharSequenceExtra("sms_body")

        if (recipients.isEmpty() || message.isNullOrBlank()) return

        val smsManager = getSystemService(SmsManager::class.java) ?: return
        recipients.forEach { recipient ->
            runCatching {
                smsManager.sendTextMessage(recipient, null, message.toString(), null, null)
            }.onFailure { throwable ->
                Log.e(TAG, "Failed to respond via SMS", throwable)
            }
        }
    }

    private companion object {
        const val ACTION_RESPOND_VIA_MESSAGE = "android.intent.action.RESPOND_VIA_MESSAGE"
        const val TAG = "RespondViaMessage"
    }
}
