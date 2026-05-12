package com.example.smsfirewall

import android.Manifest
import android.app.NotificationManager
import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.smsfirewall.notifications.NotificationConstants
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManifestSecurityInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun backupIsDisabledForSensitiveSmsData() {
        val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)

        assertFalse(appInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP != 0)
    }

    @Test
    fun writeSmsPermissionIsNotRequested() {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val permissions = packageInfo.requestedPermissions.orEmpty()

        assertFalse(Manifest.permission.WRITE_SMS in permissions)
        assertTrue(Manifest.permission.READ_SMS in permissions)
        assertTrue(Manifest.permission.SEND_SMS in permissions)
    }

    @Test
    fun allowedSmsNotificationChannelExists() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)

        assertNotNull(manager.getNotificationChannel(NotificationConstants.ALLOWED_SMS_CHANNEL_ID))
    }
}
