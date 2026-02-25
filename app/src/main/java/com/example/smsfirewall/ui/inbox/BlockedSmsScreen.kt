package com.example.smsfirewall.ui.inbox

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.telephony.SmsManager
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
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
    val allMessages by repository.getAll().collectAsState(initial = emptyList())
    val regularMessages by repository.getByStatusNot(SmsStatus.BLOCK).collectAsState(initial = emptyList())
    val spamMessages by repository.getByStatus(SmsStatus.BLOCK).collectAsState(initial = emptyList())
    val mutedSenderStore = remember(context) { MutedSenderStore(context) }
    var mutedSendersChangeToken by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    var selectedForDelete by remember { mutableStateOf<SmsEntity?>(null) }
    var selectedTab by remember { mutableStateOf(InboxTab.MESSAGES) }
    var openedConversationKey by remember { mutableStateOf<String?>(null) }
    var isNewMessageScreenOpen by remember { mutableStateOf(false) }
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
    val allConversations = remember(allMessages) { buildConversations(allMessages) }
    val openedConversation = remember(openedConversationKey, allConversations) {
        openedConversationKey?.let { key -> allConversations.firstOrNull { it.senderKey == key } }
    }
    val emptyMessage = if (selectedTab == InboxTab.MESSAGES) {
        "Mesaj bulunamadi"
    } else {
        "Spam bulunamadi"
    }

    LaunchedEffect(openedConversationKey, openedConversation) {
        if (openedConversationKey != null && openedConversation == null) {
            openedConversationKey = null
        }
    }

    suspend fun sendAndStoreMessage(destinationAddress: String, messageBody: String): Boolean {
        val sent = sendSmsMessage(
            context = context,
            destinationAddress = destinationAddress,
            messageBody = messageBody
        )
        if (!sent) {
            return false
        }

        repository.insert(
            SmsEntity(
                sender = destinationAddress,
                body = messageBody,
                receivedAt = System.currentTimeMillis(),
                status = SmsStatus.ALLOW,
                reason = SENT_MESSAGE_REASON
            )
        )
        return true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (openedConversation != null) {
                ConversationDetailScreen(
                    conversation = openedConversation,
                    onBack = { openedConversationKey = null },
                    onMessageLongPress = { sms -> selectedForDelete = sms },
                    onSendMessage = { messageBody ->
                        val sender = openedConversation.displaySender
                        scope.launch {
                            val sent = sendAndStoreMessage(
                                destinationAddress = sender,
                                messageBody = messageBody
                            )
                            if (!sent) {
                                Toast.makeText(
                                    context,
                                    "Mesaj gonderilemedi",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                )
            } else if (isNewMessageScreenOpen) {
                NewMessageScreen(
                    onBack = { isNewMessageScreenOpen = false },
                    onSendMessage = { destinationAddress, messageBody ->
                        scope.launch {
                            val sent = sendAndStoreMessage(
                                destinationAddress = destinationAddress,
                                messageBody = messageBody
                            )
                            if (sent) {
                                Toast.makeText(
                                    context,
                                    "Mesaj gonderildi",
                                    Toast.LENGTH_SHORT
                                ).show()
                                isNewMessageScreenOpen = false
                                selectedTab = InboxTab.MESSAGES
                            } else {
                                Toast.makeText(
                                    context,
                                    "Mesaj gonderilemedi",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                )
            } else {
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
                            .padding(top = 12.dp, bottom = 84.dp),
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
                                onOpenConversation = {
                                    isNewMessageScreenOpen = false
                                    openedConversationKey = conversation.senderKey
                                },
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
        }

        if (openedConversation == null && !isNewMessageScreenOpen) {
            FloatingActionButton(
                onClick = {
                    selectedTab = InboxTab.MESSAGES
                    openedConversationKey = null
                    isNewMessageScreenOpen = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_dialog_email),
                    contentDescription = "Yeni mesaj"
                )
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
    onOpenConversation: () -> Unit,
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
                    .clickable(onClick = onOpenConversation)
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

                    val latestMessage = conversation.messages.lastOrNull()
                    if (latestMessage != null) {
                        MessageBubble(
                            sms = latestMessage,
                            showReason = showReason,
                            onClick = onOpenConversation,
                            onLongPress = { onMessageLongPress(latestMessage) }
                        )
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
private fun ConversationDetailScreen(
    conversation: SmsConversation,
    onBack: () -> Unit,
    onMessageLongPress: (SmsEntity) -> Unit,
    onSendMessage: (String) -> Unit
) {
    var draftMessage by remember(conversation.senderKey) { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val displayMessages = remember(conversation.messages) { conversation.messages.asReversed() }

    LaunchedEffect(displayMessages.size) {
        if (displayMessages.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    fun submitMessage() {
        val text = draftMessage.trim()
        if (text.isBlank()) return
        onSendMessage(text)
        draftMessage = ""
    }

    val handleBack = {
        focusManager.clearFocus()
        keyboardController?.hide()
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .swipeBackGesture(onBack = handleBack)
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
            .padding(top = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = handleBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Geri"
                )
            }
            Text(
                text = conversation.displaySender,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            state = listState,
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(displayMessages, key = { it.id }) { sms ->
                DetailMessageBubble(
                    sms = sms,
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    },
                    onLongPress = { onMessageLongPress(sms) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draftMessage,
                onValueChange = { draftMessage = it },
                modifier = Modifier
                    .weight(1f)
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyUp &&
                            (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                        ) {
                            submitMessage()
                            keyboardController?.hide()
                            true
                        } else {
                            false
                        }
                    },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        submitMessage()
                        keyboardController?.hide()
                    }
                ),
                placeholder = { Text(text = "Mesaj yaz") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    submitMessage()
                    keyboardController?.hide()
                },
                enabled = draftMessage.isNotBlank()
            ) {
                Text(text = "Gonder")
            }
        }
    }
}

@Composable
private fun NewMessageScreen(
    onBack: () -> Unit,
    onSendMessage: (destinationAddress: String, messageBody: String) -> Unit
) {
    var destinationAddress by remember { mutableStateOf("") }
    var draftMessage by remember { mutableStateOf("") }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun submitMessage() {
        val recipient = destinationAddress.trim()
        val messageBody = draftMessage.trim()

        if (recipient.isBlank()) {
            Toast.makeText(context, "Numara girin", Toast.LENGTH_SHORT).show()
            return
        }
        if (messageBody.isBlank()) {
            Toast.makeText(context, "Mesaj bos olamaz", Toast.LENGTH_SHORT).show()
            return
        }

        onSendMessage(recipient, messageBody)
    }

    val handleBack = {
        focusManager.clearFocus()
        keyboardController?.hide()
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .swipeBackGesture(onBack = handleBack)
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
            .padding(top = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = handleBack
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Geri"
                )
            }
            Text(text = "Yeni Mesaj")
        }

        OutlinedTextField(
            value = destinationAddress,
            onValueChange = { destinationAddress = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            singleLine = true,
            label = { Text(text = "Numara") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next
            )
        )

        OutlinedTextField(
            value = draftMessage,
            onValueChange = { draftMessage = it },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 10.dp),
            label = { Text(text = "Mesaj") },
            placeholder = { Text(text = "Mesajinizi yazin") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    submitMessage()
                    keyboardController?.hide()
                }
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = {
                    submitMessage()
                    keyboardController?.hide()
                },
                enabled = destinationAddress.isNotBlank() && draftMessage.isNotBlank()
            ) {
                Text(text = "Gonder")
            }
        }
    }
}

private fun Modifier.swipeBackGesture(onBack: () -> Unit): Modifier = composed {
    val swipeBackThresholdPx = with(LocalDensity.current) { SWIPE_BACK_THRESHOLD_DP.toPx() }
    pointerInput(onBack, swipeBackThresholdPx) {
        var draggedDistance = 0f
        var backTriggered = false

        detectHorizontalDragGestures(
            onDragStart = {
                draggedDistance = 0f
                backTriggered = false
            },
            onHorizontalDrag = { change, dragAmount ->
                if (dragAmount > 0f) {
                    draggedDistance += dragAmount
                }

                if (!backTriggered && draggedDistance >= swipeBackThresholdPx) {
                    backTriggered = true
                    change.consume()
                    onBack()
                }
            },
            onDragEnd = {
                draggedDistance = 0f
                backTriggered = false
            },
            onDragCancel = {
                draggedDistance = 0f
                backTriggered = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DetailMessageBubble(
    sms: SmsEntity,
    onClick: () -> Unit = {},
    onLongPress: () -> Unit
) {
    val isOutgoing = sms.reason == SENT_MESSAGE_REASON
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(MaterialTheme.shapes.medium)
                .background(
                    if (isOutgoing) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongPress
                )
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = sms.body.ifBlank { "(Bos mesaj)" },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    sms: SmsEntity,
    showReason: Boolean,
    onClick: () -> Unit = {},
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
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Text(
            text = sms.body.ifBlank { "(Bos mesaj)" },
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 14.sp),
            minLines = bodyMaxLines,
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

private fun sendSmsMessage(
    context: Context,
    destinationAddress: String,
    messageBody: String
): Boolean {
    if (destinationAddress.isBlank() || messageBody.isBlank()) {
        return false
    }

    val hasPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.SEND_SMS
    ) == PackageManager.PERMISSION_GRANTED
    if (!hasPermission) {
        return false
    }

    return runCatching {
        SmsManager.getDefault().sendTextMessage(
            destinationAddress,
            null,
            messageBody,
            null,
            null
        )
    }.isSuccess
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
            onOpenConversation = {},
            onMessageLongPress = {},
            onSwipeDeleteConversation = {},
            onMuteNotifications = {},
            onUnmuteNotifications = {}
        )
    }
}

private const val PHONE_COMPARE_LENGTH = 10
private const val SENT_MESSAGE_REASON = "Sent by user"
private val SWIPE_BACK_THRESHOLD_DP = 96.dp
private val CONVERSATION_CARD_HEIGHT = 116.dp
private val MESSAGE_BUBBLE_HEIGHT = 46.dp
