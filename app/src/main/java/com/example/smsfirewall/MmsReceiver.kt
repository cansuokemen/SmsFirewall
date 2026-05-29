package com.example.smsfirewall

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION) return
        if (intent.type != MMS_MIME_TYPE) return
        // Required for default SMS app eligibility. MMS parsing is not supported yet.
    }

    private companion object {
        const val MMS_MIME_TYPE = "application/vnd.wap.mms-message"
    }
}
