package com.example.smsfirewall.ui.inbox

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.example.smsfirewall.R
import com.example.smsfirewall.data.SpamRetentionPolicy
import com.example.smsfirewall.data.SmsRepository
import com.example.smsfirewall.data.local.SmsEntity
import com.example.smsfirewall.filter.SmsStatus
import com.example.smsfirewall.notifications.MutedSenderStore
import com.example.smsfirewall.notifications.NotificationConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class ScreenState {
    LIST, DETAIL, NEW_MESSAGE
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BlockedSmsScreen(repository: SmsRepository, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var manualRefreshKey by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    val systemResult = rememberSystemMessages(context, manualRefreshKey)
    val regularMessages = systemResult.messages
    val unreadIds = systemResult.unreadIds
    val spamMessages by repository.getByStatus(SmsStatus.BLOCK).collectAsState(initial = emptyList())
    val mutedSenderStore = remember(context) { MutedSenderStore(context) }
    var mutedSendersChangeToken by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val conversationListState = rememberLazyListState()

    // FAB/bottom bar scroll'da gizleme
    var previousIndex by remember { mutableIntStateOf(0) }
    var previousOffset by remember { mutableIntStateOf(0) }
    val isBottomBarVisible by remember {
        derivedStateOf {
            if (conversationListState.firstVisibleItemIndex != previousIndex) {
                (previousIndex > conversationListState.firstVisibleItemIndex).also {
                    previousIndex = conversationListState.firstVisibleItemIndex
                    previousOffset = conversationListState.firstVisibleItemScrollOffset
                }
            } else {
                (previousOffset >= conversationListState.firstVisibleItemScrollOffset).also {
                    previousOffset = conversationListState.firstVisibleItemScrollOffset
                }
            }
        }
    }

    var selectedForDelete by remember { mutableStateOf<SmsEntity?>(null) }
    var openedSpamMessage by remember { mutableStateOf<SmsEntity?>(null) }
    var selectedTab by remember { mutableStateOf(InboxTab.MESSAGES) }
    var openedConversationKey by remember { mutableStateOf<String?>(null) }
    var isNewMessageScreenOpen by remember { mutableStateOf(false) }
    var actionRevealedConversationKey by remember { mutableStateOf<String?>(null) }
    var conversationSearchInput by remember { mutableStateOf("") }
    var conversationSearchQuery by remember { mutableStateOf("") }
    val shouldShowNotificationWarning = shouldShowNotificationPopupWarning(context)

    // Contact name cache
    val contactNameCache = remember { mutableMapOf<String, String?>() }
    fun getContactName(phoneNumber: String): String? {
        return contactNameCache.getOrPut(phoneNumber) {
            resolveContactName(context, phoneNumber)
        }
    }

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

    val visibleMessages = if (selectedTab == InboxTab.MESSAGES) {
        regularMessages
    } else {
        spamMessages
    }
    val currentUnreadIds = if (selectedTab == InboxTab.MESSAGES) unreadIds else emptySet()
    val visibleConversations = remember(visibleMessages, currentUnreadIds) {
        buildConversations(visibleMessages, currentUnreadIds)
    }
    val filteredConversations = remember(visibleConversations, conversationSearchQuery) {
        val query = conversationSearchQuery.trim()
        if (query.isBlank()) {
            visibleConversations
        } else {
            visibleConversations.filter { conversation ->
                conversation.matchesSearchQuery(query)
            }
        }
    }
    val openedConversation = remember(openedConversationKey, visibleConversations) {
        openedConversationKey?.let { key -> visibleConversations.firstOrNull { it.senderKey == key } }
    }

    val currentScreen = when {
        openedConversation != null -> ScreenState.DETAIL
        isNewMessageScreenOpen -> ScreenState.NEW_MESSAGE
        else -> ScreenState.LIST
    }

    LaunchedEffect(openedConversationKey, openedConversation) {
        if (openedConversationKey != null && openedConversation == null) {
            openedConversationKey = null
        }
    }

    LaunchedEffect(filteredConversations, actionRevealedConversationKey) {
        val activeKey = actionRevealedConversationKey ?: return@LaunchedEffect
        if (filteredConversations.none { it.senderKey == activeKey }) {
            actionRevealedConversationKey = null
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == InboxTab.SPAM) {
            conversationListState.scrollToItem(0)
            repository.deleteByStatusBefore(
                status = SmsStatus.BLOCK,
                beforeTimestamp = SpamRetentionPolicy.cutoffTimestamp()
            )
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

        insertSentSmsIntoSystemProvider(
            context = context,
            destinationAddress = destinationAddress,
            messageBody = messageBody,
            sentAt = System.currentTimeMillis()
        )
        return true
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                if (targetState == ScreenState.LIST) {
                    (slideInHorizontally { -it } + fadeIn()) togetherWith
                        (slideOutHorizontally { it } + fadeOut())
                } else {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it } + fadeOut())
                }
            },
            label = "screen_transition"
        ) { screen ->
            when (screen) {
                ScreenState.DETAIL -> {
                    val conv = openedConversation
                    if (conv != null) {
                        ConversationDetailScreen(
                            conversation = conv,
                            contactName = getContactName(conv.displaySender),
                            canSendMessage = selectedTab == InboxTab.MESSAGES,
                            isSpamConversation = selectedTab == InboxTab.SPAM,
                            onBack = { openedConversationKey = null },
                            onMessageLongPress = { sms -> selectedForDelete = sms },
                            onSpamMessageClick = { sms -> openedSpamMessage = sms },
                            onMarkAsNotSpam = { sms ->
                                scope.launch {
                                    val moved = moveSpamToSystemInbox(
                                        context = context,
                                        repository = repository,
                                        sms = sms
                                    )
                                    if (moved) {
                                        Toast.makeText(context, "Mesaj normal kutuya tasindi", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Mesaj tasinamadi", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onDeleteSpam = { sms ->
                                scope.launch { repository.delete(sms) }
                            },
                            onSendMessage = { messageBody ->
                                val sender = conv.displaySender
                                scope.launch {
                                    val sent = sendAndStoreMessage(sender, messageBody)
                                    if (!sent) {
                                        Toast.makeText(context, "Mesaj gonderilemedi", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }

                ScreenState.NEW_MESSAGE -> {
                    NewMessageScreen(
                        onBack = { isNewMessageScreenOpen = false },
                        onSendMessage = { destinationAddress, messageBody ->
                            scope.launch {
                                val sent = sendAndStoreMessage(destinationAddress, messageBody)
                                if (sent) {
                                    Toast.makeText(context, "Mesaj gonderildi", Toast.LENGTH_SHORT).show()
                                    isNewMessageScreenOpen = false
                                    selectedTab = InboxTab.MESSAGES
                                } else {
                                    Toast.makeText(context, "Mesaj gonderilemedi", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }

                ScreenState.LIST -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                            if (shouldShowNotificationWarning) {
                                NotificationWarningCard(onOpenSettings = openNotificationSettings)
                            }

                            SecondaryTabRow(
                                selectedTabIndex = selectedTab.ordinal,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                InboxTab.entries.forEach { tab ->
                                    Tab(
                                        selected = selectedTab == tab,
                                        onClick = { selectedTab = tab },
                                        text = {
                                            Text(
                                                text = tab.title,
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    )
                                }
                            }

                            if (selectedTab == InboxTab.SPAM) {
                                SpamAutoDeleteWarningCard()
                            }

                            PullToRefreshBox(
                                isRefreshing = isRefreshing,
                                onRefresh = {
                                    isRefreshing = true
                                    manualRefreshKey++
                                    scope.launch {
                                        delay(500)
                                        isRefreshing = false
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (filteredConversations.isEmpty()) {
                                    if (conversationSearchQuery.isNotBlank()) {
                                        SearchEmptyState()
                                    } else if (selectedTab == InboxTab.MESSAGES) {
                                        MessagesEmptyState()
                                    } else {
                                        SpamEmptyState()
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(top = 8.dp, bottom = 80.dp),
                                        state = conversationListState,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(filteredConversations, key = { it.senderKey }) { conversation ->
                                            val isSenderMuted = remember(conversation.senderKey, mutedSendersChangeToken) {
                                                mutedSenderStore.isMuted(conversation.displaySender)
                                            }
                                            val contactName = remember(conversation.displaySender) {
                                                getContactName(conversation.displaySender)
                                            }
                                            ConversationListItem(
                                                modifier = Modifier.animateItem(),
                                                conversation = conversation,
                                                contactName = contactName,
                                                isNotificationsMuted = isSenderMuted,
                                                showReason = selectedTab == InboxTab.SPAM,
                                                onOpenConversation = {
                                                    isNewMessageScreenOpen = false
                                                    actionRevealedConversationKey = null
                                                    openedConversationKey = conversation.senderKey
                                                    if (conversation.unreadCount > 0) {
                                                        scope.launch {
                                                            markConversationAsRead(
                                                                context, conversation, unreadIds
                                                            )
                                                        }
                                                    }
                                                },
                                                isActionsVisible = actionRevealedConversationKey == conversation.senderKey,
                                                onShowActions = {
                                                    actionRevealedConversationKey = conversation.senderKey
                                                },
                                                onHideActions = {
                                                    if (actionRevealedConversationKey == conversation.senderKey) {
                                                        actionRevealedConversationKey = null
                                                    }
                                                },
                                                onMessageLongPress = { sms -> selectedForDelete = sms },
                                                onSwipeDeleteConversation = {
                                                    scope.launch {
                                                        actionRevealedConversationKey = null
                                                        conversation.messages.forEach { sms ->
                                                            if (selectedTab == InboxTab.MESSAGES) {
                                                                deleteSmsFromSystemProvider(context, sms.id)
                                                            } else {
                                                                repository.delete(sms)
                                                            }
                                                        }
                                                    }
                                                },
                                                onMuteNotifications = {
                                                    mutedSenderStore.mute(conversation.displaySender)
                                                    mutedSendersChangeToken++
                                                    Toast.makeText(context, "Bildirim kapatildi", Toast.LENGTH_SHORT).show()
                                                },
                                                onUnmuteNotifications = {
                                                    mutedSenderStore.unmute(conversation.displaySender)
                                                    mutedSendersChangeToken++
                                                    Toast.makeText(context, "Bildirim acildi", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom bar with search and FAB
                        AnimatedVisibility(
                            visible = isBottomBarVisible,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                                .background(
                                    MaterialTheme.colorScheme.background,
                                    RoundedCornerShape(28.dp)
                                )
                                .imePadding(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = conversationSearchInput,
                                onValueChange = { value ->
                                    conversationSearchInput = value
                                    conversationSearchQuery = value.trim()
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(28.dp),
                                placeholder = {
                                    Text(
                                        text = "Kelime veya numara ara",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                                    cursorColor = MaterialTheme.colorScheme.primary,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        conversationSearchQuery = conversationSearchInput.trim()
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                    }
                                )
                            )
                            if (selectedTab == InboxTab.MESSAGES) {
                                FloatingActionButton(
                                    onClick = {
                                        selectedTab = InboxTab.MESSAGES
                                        openedConversationKey = null
                                        actionRevealedConversationKey = null
                                        isNewMessageScreenOpen = true
                                    },
                                    modifier = Modifier.size(56.dp),
                                    shape = CircleShape,
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_message_send),
                                        contentDescription = "Yeni mesaj"
                                    )
                                }
                            }
                        }
                        } // AnimatedVisibility
                    }
                }
            }
        }
    }

    if (selectedForDelete != null) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        AlertDialog(
            onDismissRequest = { selectedForDelete = null },
            title = { Text(text = "Mesaj secenekleri") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            val sms = selectedForDelete ?: return@TextButton
                            clipboardManager.setPrimaryClip(
                                android.content.ClipData.newPlainText("SMS", sms.body)
                            )
                            Toast.makeText(context, "Mesaj kopyalandi", Toast.LENGTH_SHORT).show()
                            selectedForDelete = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "Mesaji kopyala")
                        }
                    }
                    TextButton(
                        onClick = {
                            val sms = selectedForDelete ?: return@TextButton
                            scope.launch {
                                if (selectedTab == InboxTab.MESSAGES) {
                                    deleteSmsFromSystemProvider(context, sms.id)
                                } else {
                                    repository.delete(sms)
                                }
                                selectedForDelete = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Mesaji sil",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedForDelete = null }) {
                    Text(text = "Iptal")
                }
            }
        )
    }

    if (openedSpamMessage != null) {
        AlertDialog(
            onDismissRequest = { openedSpamMessage = null },
            title = { Text(text = "Spam mesaj") },
            text = { Text(text = openedSpamMessage?.body.orEmpty().ifBlank { "(Bos mesaj)" }) },
            confirmButton = {
                TextButton(onClick = { openedSpamMessage = null }) {
                    Text(text = "Kapat")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ConversationListItem(
    modifier: Modifier = Modifier,
    conversation: SmsConversation,
    contactName: String?,
    isNotificationsMuted: Boolean,
    showReason: Boolean,
    onOpenConversation: () -> Unit,
    isActionsVisible: Boolean,
    onShowActions: () -> Unit,
    onHideActions: () -> Unit,
    onMessageLongPress: (SmsEntity) -> Unit,
    onSwipeDeleteConversation: () -> Unit,
    onMuteNotifications: () -> Unit,
    onUnmuteNotifications: () -> Unit
) {
    val currentIsActionsVisible by rememberUpdatedState(isActionsVisible)
    val currentOnShowActions by rememberUpdatedState(onShowActions)
    val currentOnHideActions by rememberUpdatedState(onHideActions)
    val currentOnSwipeDeleteConversation by rememberUpdatedState(onSwipeDeleteConversation)

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    if (currentIsActionsVisible) {
                        currentOnSwipeDeleteConversation()
                        true
                    } else {
                        currentOnShowActions()
                        false
                    }
                }

                SwipeToDismissBoxValue.StartToEnd -> {
                    currentOnHideActions()
                    false
                }

                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    val displayName = contactName ?: conversation.displaySender
    val avatarLetter = displayName.firstOrNull()?.uppercase() ?: "?"
    val avatarColor = avatarColorForSender(conversation.displaySender)
    val hasUnread = conversation.unreadCount > 0

    SwipeToDismissBox(
        modifier = modifier,
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            if (direction == SwipeToDismissBoxValue.EndToStart) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(end = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Sil",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenConversation),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(avatarColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = avatarLetter,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (hasUnread) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = formatConversationTimestamp(conversation.latestReceivedAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hasUnread) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal
                                )
                                if (hasUnread) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        val latestMessage = conversation.messages.lastOrNull()
                        if (latestMessage != null) {
                            Text(
                                text = latestMessage.body.ifBlank { "(Bos mesaj)" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (hasUnread) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal,
                                maxLines = if (showReason) 1 else 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            if (showReason) {
                                Text(
                                    text = "Neden: ${latestMessage.reason}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (isActionsVisible) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isNotificationsMuted) {
                        SwipeActionButton(
                            icon = Icons.Outlined.Notifications,
                            contentDescription = "Bildirimi yeniden ac",
                            onClick = {
                                onHideActions()
                                onUnmuteNotifications()
                            }
                        )
                    } else {
                        SwipeActionButton(
                            icon = Icons.Outlined.NotificationsOff,
                            contentDescription = "Bildirimleri kapat",
                            onClick = {
                                onHideActions()
                                onMuteNotifications()
                            }
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    SwipeActionButton(
                        icon = Icons.Outlined.Delete,
                        contentDescription = "Konusmayi sil",
                        onClick = {
                            onHideActions()
                            onSwipeDeleteConversation()
                        }
                    )
                }
            }
        }
    }
}

private fun shouldShowNotificationPopupWarning(context: Context): Boolean {
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

internal fun Modifier.swipeBackGesture(onBack: () -> Unit): Modifier = composed {
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

private val SWIPE_BACK_THRESHOLD_DP = 96.dp
