package com.example.smsfirewall

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.smsfirewall.data.DatabaseProvider
import com.example.smsfirewall.data.SmsRepository
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.smsfirewall.ui.inbox.BlockedSmsScreen
import com.example.smsfirewall.ui.theme.ThemePreferenceStore
import com.example.smsfirewall.ui.theme.SmsFirewallTheme

class MainActivity : ComponentActivity() {
    private var uiStarted = false
    private var requestInFlight = false
    private var requestAttempted = false
    private lateinit var smsRepository: SmsRepository

    private val defaultSmsRequestLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            requestInFlight = false
            if (isDefaultSmsApp()) {
                startUiIfNeeded()
            } else {
                finish()
            }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        smsRepository = SmsRepository(DatabaseProvider.getDatabase(this).smsDao())
        continueOnlyIfDefaultSms()
    }

    override fun onResume() {
        super.onResume()
        if (!uiStarted && !requestInFlight) {
            continueOnlyIfDefaultSms()
        }
    }

    private fun continueOnlyIfDefaultSms() {
        if (isDefaultSmsApp()) {
            startUiIfNeeded()
            return
        }

        requestDefaultSmsRoleOrFinish()
    }

    private fun isDefaultSmsApp(): Boolean {
        val hasTelephony =
            packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
        if (!hasTelephony) return true

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_SMS) == true
        } else {
            Telephony.Sms.getDefaultSmsPackage(this) == packageName
        }
    }

    private fun requestDefaultSmsRoleOrFinish() {
        val hasTelephony =
            packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
        if (!hasTelephony) {
            finish()
            return
        }

        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager == null || !roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                null
            } else {
                roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
            }
        } else {
            Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            }
        }

        if (intent == null) {
            finish()
            return
        }

        if (requestAttempted) {
            finish()
            return
        }

        requestAttempted = true
        requestInFlight = true
        defaultSmsRequestLauncher.launch(intent)
    }

    private fun startUiIfNeeded() {
        if (uiStarted) return
        uiStarted = true
        enableEdgeToEdge()
        val themePreferenceStore = ThemePreferenceStore(this)
        setContent {
            var themeMode by remember { mutableStateOf(themePreferenceStore.getThemeMode()) }
            SmsFirewallTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    BlockedSmsScreen(
                        repository = smsRepository,
                        currentThemeMode = themeMode,
                        onThemeModeChanged = { newMode ->
                            themePreferenceStore.setThemeMode(newMode)
                            themeMode = newMode
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        requestNotificationPermissionIfNeeded()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
