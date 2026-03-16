package com.example.smsfirewall

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Varsayılan SMS uygulaması olmak için gerekli MMS receiver.
 * MMS işleme şu an desteklenmiyor, ancak manifest kaydı zorunlu.
 */
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // MMS işleme henüz desteklenmiyor
    }
}
