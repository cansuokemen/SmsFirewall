package com.example.smsfirewall

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import com.example.smsfirewall.ui.inbox.BlockedSmsScreen
import com.example.smsfirewall.ui.inbox.AppLockScreen
import com.example.smsfirewall.ui.inbox.InboxViewModel
import com.example.smsfirewall.ui.theme.ThemePreferenceStore
import com.example.smsfirewall.ui.theme.ThemeMode
import com.example.smsfirewall.ui.theme.SmsFirewallTheme
import com.example.smsfirewall.data.AppLockPreferenceStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.fragment.app.FragmentActivity

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private var uiStarted = false
    private var requestInFlight = false
    private var requestAttempted = false

    private val inboxViewModel: InboxViewModel by viewModels()

    @Inject
    lateinit var themePreferenceStore: ThemePreferenceStore

    @Inject
    lateinit var appLockPreferenceStore: AppLockPreferenceStore

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
        setContent {
            var themeMode by remember { mutableStateOf(themePreferenceStore.getThemeMode()) }
            var isUnlocked by remember { mutableStateOf(!appLockPreferenceStore.isLockEnabled()) }
            val systemInDarkTheme = isSystemInDarkTheme()
            val useDarkSystemBars = when (themeMode) {
                ThemeMode.SYSTEM -> systemInDarkTheme
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            val launchScale = remember { Animatable(1.06f) }
            val launchAlpha = remember { Animatable(0f) }
            SideEffect {
                window.statusBarColor = Color.TRANSPARENT
                window.navigationBarColor = Color.TRANSPARENT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isStatusBarContrastEnforced = false
                    window.isNavigationBarContrastEnforced = false
                }
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !useDarkSystemBars
                    isAppearanceLightNavigationBars = !useDarkSystemBars
                }
            }
            LaunchedEffect(Unit) {
                launch { launchScale.animateTo(1f, animationSpec = tween(durationMillis = 460)) }
                launch { launchAlpha.animateTo(1f, animationSpec = tween(durationMillis = 380)) }
            }
            SmsFirewallTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = launchScale.value
                            scaleY = launchScale.value
                            alpha  = launchAlpha.value
                        }
                ) {
                    if (isUnlocked) {
                        BlockedSmsScreen(
                            viewModel = inboxViewModel,
                            currentThemeMode = themeMode,
                            onThemeModeChanged = { newMode ->
                                themePreferenceStore.setThemeMode(newMode)
                                themeMode = newMode
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        AppLockScreen(
                            biometricAvailable = canUseBiometricUnlock(),
                            onVerifyPin = { pin -> appLockPreferenceStore.verifyPin(pin) },
                            onUnlocked = { isUnlocked = true },
                            onBiometricClick = {
                                showBiometricPrompt(
                                    onSuccess = { isUnlocked = true },
                                    onFailure = { }
                                )
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
        requestNotificationPermissionIfNeeded()
    }

    private fun canUseBiometricUnlock(): Boolean {
        if (!appLockPreferenceStore.isBiometricEnabled()) return false
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return BiometricManager.from(this).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun showBiometricPrompt(
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("SmsFirewall kilitli")
            .setSubtitle("Devam etmek icin kimliginizi dogrulayin")
            .setAllowedAuthenticators(authenticators)
            .build()
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onFailure()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailure()
                }
            }
        )
        prompt.authenticate(promptInfo)
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
