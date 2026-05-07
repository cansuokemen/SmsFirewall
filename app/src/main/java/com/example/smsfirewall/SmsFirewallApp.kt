package com.example.smsfirewall

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.os.Build
import com.example.smsfirewall.notifications.NotificationConstants
import com.example.smsfirewall.notifications.NotificationPreferenceStore
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SmsFirewallApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationPreferenceStore = NotificationPreferenceStore(this)
            val channel = NotificationChannel(
                NotificationConstants.ALLOWED_SMS_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                if (notificationPreferenceStore.isSoundEnabled()) {
                    notificationPreferenceStore.getSoundUri()?.let { uri ->
                        setSound(
                            uri,
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                    }
                } else {
                    setSound(null, null)
                }
                enableVibration(true)
                enableLights(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
