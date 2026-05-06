package com.example.smsfirewall.ui.inbox

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.smsfirewall.R
import com.example.smsfirewall.data.background.BackgroundSpec
import com.example.smsfirewall.filter.SmsStatus
import com.example.smsfirewall.ui.animation.PaperCrumpleOverlay
import com.example.smsfirewall.ui.animation.ShredderBatchOverlay
import com.example.smsfirewall.ui.background.BackgroundPickerSheet
import com.example.smsfirewall.ui.theme.ThemeMode
import com.example.smsfirewall.util.normalizeSender

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedSmsScreen(
    viewModel: InboxViewModel,
    currentThemeMode: ThemeMode,
    onThemeModeChanged: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val systemResult by viewModel.systemMessages.collectAsState()
    val regularMessages = systemResult.messages
    val unreadIds       = systemResult.unreadIds
    val spamMessages by viewModel.repository.getByStatus(SmsStatus.BLOCK).collectAsState(initial = emptyList())

    val blockedSenders = viewModel.blockedSenders

    val filteredRegularMessages = remember(regularMessages, blockedSenders) {
        if (blockedSenders.isEmpty()) regularMessages
        else regularMessages.filter { msg ->
            val normalized = normalizeSender(msg.sender)
            blockedSenders.none { normalizeSender(it) == normalized }
        }
    }

    val filteredSpamMessages = remember(spamMessages, blockedSenders) {
        if (blockedSenders.isEmpty()) spamMessages
        else spamMessages.filter { msg ->
            val normalized = normalizeSender(msg.sender)
            blockedSenders.none { normalizeSender(it) == normalized }
        }
    }

    val visibleMessages    = if (viewModel.selectedTab == InboxTab.MESSAGES) filteredRegularMessages else filteredSpamMessages
    val currentUnreadIds   = if (viewModel.selectedTab == InboxTab.MESSAGES) unreadIds else emptySet()
    val unknownSenderLabel = stringResource(R.string.unknown_sender)

    val allConversations = remember(visibleMessages, currentUnreadIds, unknownSenderLabel, viewModel.conversationMetaToken) {
        buildConversations(visibleMessages, currentUnreadIds, unknownSenderLabel, viewModel.conversationMetaStore)
    }
    val archivedConversations = remember(allConversations) {
        allConversations.filter { it.isArchived }
    }
    val visibleConversations = remember(allConversations) {
        allConversations.filterNot { it.isArchived }
    }
    val pendingDeleteKeys = remember(viewModel.pendingDelete) {
        viewModel.pendingDelete?.conversations?.map { it.senderKey }?.toSet() ?: emptySet()
    }
    val displayConversations = remember(visibleConversations, pendingDeleteKeys) {
        if (pendingDeleteKeys.isEmpty()) visibleConversations
        else visibleConversations.filter { it.senderKey !in pendingDeleteKeys }
    }
    val tabFilteredConversations = remember(displayConversations, viewModel.selectedTab, viewModel.messagesFilter) {
        if (viewModel.selectedTab != InboxTab.MESSAGES) displayConversations
        else when (viewModel.messagesFilter) {
            MessagesFilter.ALL -> displayConversations
            MessagesFilter.UNREAD -> displayConversations.filter { it.unreadCount > 0 }
            MessagesFilter.FAVORITES -> displayConversations.filter { it.isFavorite }
        }
    }
    val filteredConversations = remember(tabFilteredConversations, viewModel.conversationSearchQuery) {
        val query = viewModel.conversationSearchQuery.trim()
        if (query.isBlank()) tabFilteredConversations
        else tabFilteredConversations.filter { it.matchesSearchQuery(query) }
    }
    val openedConversation = remember(viewModel.openedConversationKey, visibleConversations) {
        viewModel.openedConversationKey?.let { key -> visibleConversations.firstOrNull { it.senderKey == key } }
    }

    val conversationListState = rememberLazyListState()
    var previousIndex  by remember { mutableIntStateOf(0) }
    var previousOffset by remember { mutableIntStateOf(0) }
    val isBottomBarVisible by remember {
        derivedStateOf {
            if (conversationListState.firstVisibleItemIndex != previousIndex) {
                (previousIndex > conversationListState.firstVisibleItemIndex).also {
                    previousIndex  = conversationListState.firstVisibleItemIndex
                    previousOffset = conversationListState.firstVisibleItemScrollOffset
                }
            } else {
                (previousOffset >= conversationListState.firstVisibleItemScrollOffset).also {
                    previousOffset = conversationListState.firstVisibleItemScrollOffset
                }
            }
        }
    }

    LaunchedEffect(viewModel.openedConversationKey, openedConversation) {
        if (viewModel.openedConversationKey != null && openedConversation == null) {
            viewModel.closeConversation()
        }
    }

    LaunchedEffect(filteredConversations, viewModel.actionRevealedConversationKey) {
        val activeKey = viewModel.actionRevealedConversationKey ?: return@LaunchedEffect
        if (filteredConversations.none { it.senderKey == activeKey }) {
            viewModel.actionRevealedConversationKey = null
        }
    }

    LaunchedEffect(viewModel.selectedTab) {
        if (viewModel.selectedTab == InboxTab.SPAM) {
            conversationListState.scrollToItem(0)
        }
    }

    val currentScreen = when {
        viewModel.isSettingsOpen         -> ScreenState.SETTINGS
        viewModel.isArchiveOpen          -> ScreenState.ARCHIVE
        openedConversation != null       -> ScreenState.DETAIL
        viewModel.isNewMessageScreenOpen -> ScreenState.NEW_MESSAGE
        else                             -> ScreenState.LIST
    }

    BackHandler(enabled = currentScreen != ScreenState.LIST) {
        when (currentScreen) {
            ScreenState.SETTINGS    -> viewModel.closeSettings()
            ScreenState.DETAIL      -> viewModel.closeConversation()
            ScreenState.NEW_MESSAGE -> viewModel.closeNewMessage()
            ScreenState.ARCHIVE     -> viewModel.closeArchive()
            ScreenState.LIST        -> { }
        }
    }

    val snackbarHostState  = remember { SnackbarHostState() }
    val undoLabel          = stringResource(R.string.undo)
    val deletedSingleLabel = stringResource(R.string.conversation_deleted)

    var backgroundSheetTarget by remember { mutableStateOf<BackgroundSheetTarget?>(null) }

    LaunchedEffect(viewModel.pendingDelete) {
        val pending = viewModel.pendingDelete ?: return@LaunchedEffect
        val count   = pending.conversations.size
        val message = if (count == 1) deletedSingleLabel
                      else context.getString(R.string.conversations_deleted, count)

        val result = snackbarHostState.showSnackbar(
            message     = message,
            actionLabel = undoLabel,
            duration    = SnackbarDuration.Short
        )
        when (result) {
            SnackbarResult.ActionPerformed -> viewModel.undoDelete()
            SnackbarResult.Dismissed       -> viewModel.commitPendingDelete()
        }
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color.Transparent,
        modifier       = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            AnimatedContent(
                targetState  = currentScreen,
                transitionSpec = {
                    if (targetState == ScreenState.LIST) {
                        // Back → daha belirgin paralax kayma
                        (slideInHorizontally(
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                            initialOffsetX = { -it / 2 }
                        ) + fadeIn(tween(250))) togetherWith
                        (slideOutHorizontally(
                            animationSpec = tween(300),
                            targetOffsetX = { it }
                        ) + fadeOut(tween(220)))
                    } else {
                        // Forward → daha belirgin slide
                        (slideInHorizontally(
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                            initialOffsetX = { it }
                        ) + fadeIn(tween(250))) togetherWith
                        (slideOutHorizontally(
                            animationSpec = tween(300),
                            targetOffsetX = { -it / 2 }
                        ) + fadeOut(tween(220)))
                    }
                },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    ScreenState.DETAIL -> {
                        val conv = openedConversation
                        if (conv != null) {
                            ConversationDetailScreen(
                                conversation      = conv,
                                contactName       = viewModel.getContactName(conv.displaySender),
                                canSendMessage    = viewModel.selectedTab == InboxTab.MESSAGES,
                                isSpamConversation = viewModel.selectedTab == InboxTab.SPAM,
                                backgroundSpec     = viewModel.backgroundFor(conv.displaySender),
                                onChangeBackground = {
                                    backgroundSheetTarget = BackgroundSheetTarget.Conversation(conv.displaySender)
                                },
                                onResetBackground = {
                                    viewModel.resetConversationBackground(conv.displaySender)
                                },
                                callbacks = DetailScreenCallbacks(
                                    onBack            = { viewModel.closeConversation() },
                                    onMessageLongPress = { sms -> viewModel.selectedForDelete = sms },
                                    onSpamMessageClick = { sms -> viewModel.openedSpamMessage = sms },
                                    onMarkAsNotSpam   = { sms ->
                                        viewModel.markAsNotSpam(sms) { moved ->
                                            val msgRes = if (moved) R.string.message_moved else R.string.message_move_failed
                                            Toast.makeText(context, context.getString(msgRes), Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onDeleteSpam  = { sms -> viewModel.deleteSpam(sms) },
                                    onSendMessage = { messageBody ->
                                        viewModel.sendAndStoreMessage(conv.displaySender, messageBody) { sent ->
                                            if (!sent) Toast.makeText(context, context.getString(R.string.message_send_failed), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    }

                    ScreenState.NEW_MESSAGE -> {
                        NewMessageScreen(
                            onBack        = { viewModel.closeNewMessage() },
                            onSendMessage = { destinationAddress, messageBody ->
                                viewModel.sendAndStoreMessage(destinationAddress, messageBody) { sent ->
                                    if (sent) {
                                        Toast.makeText(context, context.getString(R.string.message_sent), Toast.LENGTH_SHORT).show()
                                        viewModel.closeNewMessage()
                                        viewModel.selectTab(InboxTab.MESSAGES)
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.message_send_failed), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }

                    ScreenState.SETTINGS -> {
                        SettingsScreen(
                            currentThemeMode        = currentThemeMode,
                            onThemeModeChanged      = onThemeModeChanged,
                            blockedSenders          = viewModel.blockedSenders,
                            onAddBlockedSender      = { sender -> viewModel.addBlockedSender(sender) },
                            onRemoveBlockedSender   = { sender -> viewModel.removeBlockedSender(sender) },
                            spamRetentionDays       = viewModel.spamRetentionDays,
                            onSpamRetentionChanged  = { days -> viewModel.setSpamRetention(days) },
                            soundEnabled            = viewModel.notifSoundEnabled,
                            onSoundChanged          = { viewModel.setNotifSound(it) },
                            vibrationEnabled        = viewModel.notifVibrationEnabled,
                            onVibrationChanged      = { viewModel.setNotifVibration(it) },
                            quietHoursEnabled       = viewModel.notifQuietHoursEnabled,
                            onQuietHoursChanged     = { viewModel.setNotifQuietHours(it) },
                            quietHoursStart         = viewModel.notifQuietStart,
                            onQuietHoursStartChanged = { h, m -> viewModel.setNotifQuietStart(h, m) },
                            quietHoursEnd           = viewModel.notifQuietEnd,
                            onQuietHoursEndChanged  = { h, m -> viewModel.setNotifQuietEnd(h, m) },
                            mainBackground          = viewModel.mainBackground,
                            onChangeMainBackground  = { backgroundSheetTarget = BackgroundSheetTarget.Main },
                            onResetMainBackground   = { viewModel.resetMainBackground() },
                            onBack                  = { viewModel.closeSettings() },
                            modifier                = Modifier.fillMaxSize()
                        )
                    }

                    ScreenState.ARCHIVE -> {
                        ArchiveListContent(
                            viewModel            = viewModel,
                            archivedConversations = archivedConversations,
                            unreadIds            = currentUnreadIds,
                            modifier             = Modifier.fillMaxSize()
                        )
                    }

                    ScreenState.LIST -> {
                        val msgUnreadCount = remember(displayConversations, viewModel.selectedTab) {
                            if (viewModel.selectedTab == InboxTab.MESSAGES)
                                displayConversations.count { it.unreadCount > 0 }
                            else 0
                        }
                        val msgFavoriteCount = remember(displayConversations, viewModel.selectedTab, viewModel.conversationMetaToken) {
                            if (viewModel.selectedTab == InboxTab.MESSAGES)
                                displayConversations.count { it.isFavorite }
                            else 0
                        }
                        ConversationListContent(
                            viewModel             = viewModel,
                            filteredConversations = filteredConversations,
                            unreadIds             = currentUnreadIds,
                            conversationListState = conversationListState,
                            isBottomBarVisible    = isBottomBarVisible,
                            messagesUnreadCount   = msgUnreadCount,
                            messagesFavoriteCount = msgFavoriteCount,
                            archivedCount         = archivedConversations.size,
                            onOpenArchive         = { viewModel.openArchive() }
                        )
                    }
                }
            }
        }

        // Dialogs
        MessageOptionsDialog(
            sms       = viewModel.selectedForDelete,
            onDismiss = { viewModel.selectedForDelete = null },
            onCopy    = { sms ->
                val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("SMS", sms.body))
                Toast.makeText(context, context.getString(R.string.message_copied), Toast.LENGTH_SHORT).show()
                viewModel.selectedForDelete = null
            },
            onDelete      = { sms ->
                viewModel.beginCrumpleAnim(sms)
                viewModel.deleteSingleMessage(sms)
            },
            onBlockSender = { sms ->
                viewModel.pendingBlockSenders = listOf(sms.sender)
                viewModel.showBlockConfirm    = true
                viewModel.selectedForDelete   = null
            }
        )

        SpamMessageDialog(
            sms       = viewModel.openedSpamMessage,
            onDismiss = { viewModel.openedSpamMessage = null }
        )

        BatchDeleteConfirmDialog(
            show      = viewModel.showBatchDeleteConfirm,
            count     = viewModel.selectedConversationKeys.size,
            onConfirm = {
                val targets = filteredConversations.filter { it.senderKey in viewModel.selectedConversationKeys }
                viewModel.beginShredderAnim(targets)
                viewModel.batchDeleteSelected(filteredConversations)
            },
            onDismiss = { viewModel.showBatchDeleteConfirm = false }
        )

        viewModel.pendingCrumpleAnim?.let {
            PaperCrumpleOverlay(onCompleted = { viewModel.finishCrumpleAnim() })
        }
        viewModel.pendingShredderAnim?.let { items ->
            ShredderBatchOverlay(
                itemCount   = items.size,
                onCompleted = { viewModel.finishShredderAnim() }
            )
        }

        BlockSenderConfirmDialog(
            show        = viewModel.showBlockConfirm,
            senderNames = viewModel.pendingBlockSenders,
            onConfirm   = {
                viewModel.pendingBlockSenders.forEach { sender -> viewModel.addBlockedSender(sender) }
                viewModel.showBlockConfirm    = false
                viewModel.pendingBlockSenders = emptyList()
                viewModel.exitSelectionMode()
                Toast.makeText(context, context.getString(R.string.sender_blocked), Toast.LENGTH_SHORT).show()
            },
            onDismiss = {
                viewModel.showBlockConfirm    = false
                viewModel.pendingBlockSenders = emptyList()
            }
        )

        backgroundSheetTarget?.let { target ->
            val initial = when (target) {
                is BackgroundSheetTarget.Main -> viewModel.mainBackground
                is BackgroundSheetTarget.Conversation -> viewModel.backgroundFor(target.address)
            }
            BackgroundPickerSheet(
                initial           = initial,
                imageRepository   = viewModel.backgroundImageRepository,
                onDismiss         = { backgroundSheetTarget = null },
                onApply           = { spec ->
                    when (target) {
                        is BackgroundSheetTarget.Main -> viewModel.applyMainBackground(spec)
                        is BackgroundSheetTarget.Conversation ->
                            viewModel.applyConversationBackground(target.address, spec)
                    }
                },
                onReset = {
                    when (target) {
                        is BackgroundSheetTarget.Main -> viewModel.resetMainBackground()
                        is BackgroundSheetTarget.Conversation ->
                            viewModel.resetConversationBackground(target.address)
                    }
                }
            )
        }
    }
}

internal sealed class BackgroundSheetTarget {
    object Main : BackgroundSheetTarget()
    data class Conversation(val address: String) : BackgroundSheetTarget()
}
