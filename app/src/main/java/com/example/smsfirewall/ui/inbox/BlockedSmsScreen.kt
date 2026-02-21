package com.example.smsfirewall.ui.inbox

import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.example.smsfirewall.data.SmsRepository
import com.example.smsfirewall.data.local.SmsEntity
import com.example.smsfirewall.filter.SmsStatus
import com.example.smsfirewall.notifications.NotificationConstants
import com.example.smsfirewall.ui.theme.SmsFirewallTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BlockedSmsScreen(repository: SmsRepository, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val blockedMessages by repository.getByStatus(SmsStatus.BLOCK).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var selectedForDelete by remember { mutableStateOf<SmsEntity?>(null) }
    val shouldShowNotificationWarning = shouldShowNotificationPopupWarning(context)

    val openNotificationSettings: () -> Unit = {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, NotificationConstants.ALLOWED_SMS_CHANNEL_ID)
            }
        } else {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        }
        context.startActivity(intent)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        if (shouldShowNotificationWarning) {
            NotificationWarningCard(onOpenSettings = openNotificationSettings)
        }

        if (blockedMessages.isEmpty()) {
            Text(
                text = "Blocked messages not found",
                modifier = Modifier.padding(top = 12.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(blockedMessages, key = { it.id }) { sms ->
                    BlockedSmsItem(
                        sms = sms,
                        onLongPress = { selectedForDelete = sms }
                    )
                }
            }
        }
    }

    if (selectedForDelete != null) {
        AlertDialog(
            onDismissRequest = { selectedForDelete = null },
            title = { Text(text = "Delete message") },
            text = { Text(text = "Delete selected blocked message?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val sms = selectedForDelete ?: return@TextButton
                        scope.launch {
                            repository.delete(sms)
                            selectedForDelete = null
                        }
                    }
                ) {
                    Text(text = "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedForDelete = null }) {
                    Text(text = "Cancel")
                }
            }
        )
    }
}

@Composable
private fun NotificationWarningCard(onOpenSettings: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Notification popup might be disabled.")
            Text(text = "To see SMS as pop-up, open notification channel settings and enable pop on screen.")
            Button(onClick = onOpenSettings) {
                Text(text = "Open Settings")
            }
        }
    }
}

private fun shouldShowNotificationPopupWarning(context: android.content.Context): Boolean {
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
        return true
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = manager?.getNotificationChannel(NotificationConstants.ALLOWED_SMS_CHANNEL_ID)
        if (channel == null) {
            return true
        }
        return channel.importance < NotificationManager.IMPORTANCE_HIGH
    }

    return false
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BlockedSmsItem(sms: SmsEntity, onLongPress: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPress
            )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "Sender: ${sms.sender}")
            Text(text = sms.body)
            Text(text = "Reason: ${sms.reason}")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BlockedSmsItemPreview() {
    SmsFirewallTheme {
        BlockedSmsItem(
            sms = SmsEntity(
                id = 1,
                sender = "+905551112233",
                body = "You are a winner! Claim free gift now.",
                receivedAt = 0L,
                status = SmsStatus.BLOCK,
                reason = "Blocked keyword: winner"
            ),
            onLongPress = {}
        )
    }
}
