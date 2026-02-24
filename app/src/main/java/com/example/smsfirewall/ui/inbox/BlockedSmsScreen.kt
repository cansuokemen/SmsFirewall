package com.example.smsfirewall.ui.inbox

import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.example.smsfirewall.data.SmsRepository
import com.example.smsfirewall.data.local.SmsEntity
import com.example.smsfirewall.filter.SmsStatus
import com.example.smsfirewall.notifications.MutedSenderStore
import com.example.smsfirewall.notifications.NotificationConstants
import com.example.smsfirewall.ui.theme.SmsFirewallTheme
import java.util.Locale
import kotlinx.coroutines.launch

private enum class InboxTab(val title: String) {
    MESSAGES("Mesajlar"),
    SPAM("Spam")
}

private data class SmsConversation(
    val senderKey: String,
    val displaySender: String,
    val messages: List<SmsEntity>,
    val latestReceivedAt: Long
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BlockedSmsScreen(repository: SmsRepository, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val regularMessages by repository.getByStatusNot(SmsStatus.BLOCK).collectAsState(initial = emptyList())
    val spamMessages by repository.getByStatus(SmsStatus.BLOCK).collectAsState(initial = emptyList())
    val mutedSenderStore = remember(context) { MutedSenderStore(context) }
    var mutedSendersChangeToken by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    var selectedForDelete by remember { mutableStateOf<SmsEntity?>(null) }
    var selectedTab by remember { mutableStateOf(InboxTab.MESSAGES) }
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

    val visibleMessages = if (selectedTab == InboxTab.MESSAGES) regularMessages else spamMessages
    val visibleConversations = remember(visibleMessages) { buildConversations(visibleMessages) }
    val emptyMessage = if (selectedTab == InboxTab.MESSAGES) {
        "Mesaj bulunamadi"
    } else {
        "Spam bulunamadi"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        if (shouldShowNotificationWarning) {
            NotificationWarningCard(onOpenSettings = openNotificationSettings)
        }

        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            modifier = Modifier.fillMaxWidth()
        ) {
            InboxTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(text = tab.title) }
                )
            }
        }

        if (visibleConversations.isEmpty()) {
            Text(
                text = emptyMessage,
                modifier = Modifier.padding(top = 12.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visibleConversations, key = { it.senderKey }) { conversation ->
                    val isSenderMuted = remember(conversation.senderKey, mutedSendersChangeToken) {
                        mutedSenderStore.isMuted(conversation.displaySender)
                    }
                    ConversationListItem(
                        conversation = conversation,
                        isNotificationsMuted = isSenderMuted,
                        showReason = selectedTab == InboxTab.SPAM,
                        onMessageLongPress = { sms -> selectedForDelete = sms },
                        onSwipeDeleteConversation = {
                            scope.launch {
                                conversation.messages.forEach { sms ->
                                    repository.delete(sms)
                                }
                            }
                        },
                        onMuteNotifications = {
                            mutedSenderStore.mute(conversation.displaySender)
                            mutedSendersChangeToken++
                            Toast.makeText(
                                context,
                                "Bu gonderici icin bildirim kapatildi",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onUnmuteNotifications = {
                            mutedSenderStore.unmute(conversation.displaySender)
                            mutedSendersChangeToken++
                            Toast.makeText(
                                context,
                                "Bu gonderici icin bildirim yeniden acildi",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }
    }

    if (selectedForDelete != null) {
        AlertDialog(
            onDismissRequest = { selectedForDelete = null },
            title = { Text(text = "Delete message") },
            text = { Text(text = "Delete selected message?") },
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

private fun buildConversations(messages: List<SmsEntity>): List<SmsConversation> {
    return messages
        .groupBy { normalizeSenderForGrouping(it.sender) }
        .mapNotNull { (senderKey, senderMessages) ->
            if (senderMessages.isEmpty()) {
                return@mapNotNull null
            }

            val sortedMessages = senderMessages.sortedBy { it.receivedAt }
            val latestMessage = sortedMessages.last()

            SmsConversation(
                senderKey = senderKey.ifBlank { "unknown_sender_${latestMessage.id}" },
                displaySender = latestMessage.sender.ifBlank { "Unknown sender" },
                messages = sortedMessages,
                latestReceivedAt = latestMessage.receivedAt
            )
        }
        .sortedByDescending { it.latestReceivedAt }
}

private fun normalizeSenderForGrouping(sender: String): String {
    val alphanumeric = sender
        .trim()
        .lowercase(Locale.ROOT)
        .filter { it.isLetterOrDigit() }

    if (alphanumeric.isBlank()) {
        return ""
    }

    return if (alphanumeric.all { it.isDigit() }) {
        if (alphanumeric.length > PHONE_COMPARE_LENGTH) {
            alphanumeric.takeLast(PHONE_COMPARE_LENGTH)
        } else {
            alphanumeric
        }
    } else {
        alphanumeric
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ConversationListItem(
    conversation: SmsConversation,
    isNotificationsMuted: Boolean,
    showReason: Boolean,
    onMessageLongPress: (SmsEntity) -> Unit,
    onSwipeDeleteConversation: () -> Unit,
    onMuteNotifications: () -> Unit,
    onUnmuteNotifications: () -> Unit
) {
    var actionsVisible by remember(conversation.senderKey) { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    if (actionsVisible) {
                        onSwipeDeleteConversation()
                        true
                    } else {
                        actionsVisible = true
                        false
                    }
                }

                SwipeToDismissBoxValue.StartToEnd -> {
                    actionsVisible = false
                    false
                }

                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {}
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CONVERSATION_CARD_HEIGHT)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sender: ${conversation.displaySender}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(CONVERSATION_MESSAGES_HEIGHT),
                        reverseLayout = true,
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(conversation.messages.asReversed(), key = { it.id }) { sms ->
                            MessageBubble(
                                sms = sms,
                                showReason = showReason,
                                onLongPress = { onMessageLongPress(sms) }
                            )
                        }
                    }
                }
            }

            if (actionsVisible) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isNotificationsMuted) {
                        SwipeActionButton(
                            iconRes = android.R.drawable.ic_lock_silent_mode_off,
                            contentDescription = "Bildirimi yeniden ac",
                            onClick = {
                                actionsVisible = false
                                onUnmuteNotifications()
                            }
                        )
                    } else {
                        SwipeActionButton(
                            iconRes = android.R.drawable.ic_lock_silent_mode,
                            contentDescription = "Bildirimleri kapat",
                            onClick = {
                                actionsVisible = false
                                onMuteNotifications()
                            }
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    SwipeActionButton(
                        iconRes = android.R.drawable.ic_menu_delete,
                        contentDescription = "Konusmayi sil",
                        onClick = onSwipeDeleteConversation
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    sms: SmsEntity,
    showReason: Boolean,
    onLongPress: () -> Unit
) {
    val bodyMaxLines = if (showReason) 1 else 2
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(MESSAGE_BUBBLE_HEIGHT)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPress
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Text(
            text = sms.body.ifBlank { "(Bos mesaj)" },
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 14.sp),
            maxLines = bodyMaxLines,
            overflow = TextOverflow.Ellipsis
        )
        if (showReason) {
            Text(
                text = "Reason: ${sms.reason}",
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SwipeActionButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        modifier = Modifier.size(44.dp),
        onClick = onClick
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConversationListItemPreview() {
    SmsFirewallTheme {
        ConversationListItem(
            conversation = SmsConversation(
                senderKey = "5551112233",
                displaySender = "+905551112233",
                messages = listOf(
                    SmsEntity(
                        id = 1,
                        sender = "+905551112233",
                        body = "Ilk mesaj",
                        receivedAt = 1_700_000_000_000,
                        status = SmsStatus.BLOCK,
                        reason = "Blocked keyword"
                    ),
                    SmsEntity(
                        id = 2,
                        sender = "+905551112233",
                        body = "Ikinci mesaj",
                        receivedAt = 1_700_000_100_000,
                        status = SmsStatus.BLOCK,
                        reason = "Blocked sender"
                    )
                ),
                latestReceivedAt = 1_700_000_100_000
            ),
            isNotificationsMuted = false,
            showReason = true,
            onMessageLongPress = {},
            onSwipeDeleteConversation = {},
            onMuteNotifications = {},
            onUnmuteNotifications = {}
        )
    }
}

private const val PHONE_COMPARE_LENGTH = 10
private val CONVERSATION_CARD_HEIGHT = 152.dp
private val CONVERSATION_MESSAGES_HEIGHT = 102.dp
private val MESSAGE_BUBBLE_HEIGHT = 44.dp
