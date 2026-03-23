package com.example.smsfirewall.ui.inbox

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.smsfirewall.R
import com.example.smsfirewall.filter.SmsStatus
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

    // Data
    val systemResult by viewModel.systemMessages.collectAsState()
    val regularMessages = systemResult.messages
    val unreadIds = systemResult.unreadIds
    val spamMessages by viewModel.repository.getByStatus(SmsStatus.BLOCK).collectAsState(initial = emptyList())

    val blockedSenders = viewModel.blockedSenders
    val filteredRegularMessages = remember(regularMessages, blockedSenders) {
        if (blockedSenders.isEmpty()) regularMessages
        else regularMessages.filter { msg ->
            val normalized = normalizeSender(msg.sender)
            blockedSenders.none { normalizeSender(it) == normalized }
        }
    }
    val visibleMessages = if (viewModel.selectedTab == InboxTab.MESSAGES) filteredRegularMessages else spamMessages
    val currentUnreadIds = if (viewModel.selectedTab == InboxTab.MESSAGES) unreadIds else emptySet()
    val unknownSenderLabel = stringResource(R.string.unknown_sender)

    val visibleConversations = remember(visibleMessages, currentUnreadIds, unknownSenderLabel) {
        buildConversations(visibleMessages, currentUnreadIds, unknownSenderLabel)
    }
    // Pending delete olan konuşmaları listeden gizle
    val pendingDeleteKeys = remember(viewModel.pendingDelete) {
        viewModel.pendingDelete?.conversations?.map { it.senderKey }?.toSet() ?: emptySet()
    }
    val displayConversations = remember(visibleConversations, pendingDeleteKeys) {
        if (pendingDeleteKeys.isEmpty()) visibleConversations
        else visibleConversations.filter { it.senderKey !in pendingDeleteKeys }
    }
    val filteredConversations = remember(displayConversations, viewModel.conversationSearchQuery) {
        val query = viewModel.conversationSearchQuery.trim()
        if (query.isBlank()) displayConversations
        else displayConversations.filter { it.matchesSearchQuery(query) }
    }
    val openedConversation = remember(viewModel.openedConversationKey, visibleConversations) {
        viewModel.openedConversationKey?.let { key -> visibleConversations.firstOrNull { it.senderKey == key } }
    }

    // Scroll state & bottom bar visibility
    val conversationListState = rememberLazyListState()
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

    // Sync effects
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

    // Screen navigation
    val currentScreen = when {
        viewModel.isSettingsOpen -> ScreenState.SETTINGS
        openedConversation != null -> ScreenState.DETAIL
        viewModel.isNewMessageScreenOpen -> ScreenState.NEW_MESSAGE
        else -> ScreenState.LIST
    }

    // Snackbar for undo
    val snackbarHostState = remember { SnackbarHostState() }
    val undoLabel = stringResource(R.string.undo)
    val deletedSingleLabel = stringResource(R.string.conversation_deleted)

    LaunchedEffect(viewModel.pendingDelete) {
        val pending = viewModel.pendingDelete ?: return@LaunchedEffect
        val count = pending.conversations.size
        val message = if (count == 1) deletedSingleLabel
        else context.getString(R.string.conversations_deleted, count)

        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = undoLabel,
            duration = SnackbarDuration.Short
        )
        when (result) {
            SnackbarResult.ActionPerformed -> viewModel.undoDelete()
            SnackbarResult.Dismissed -> viewModel.commitPendingDelete()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
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
                            contactName = viewModel.getContactName(conv.displaySender),
                            canSendMessage = viewModel.selectedTab == InboxTab.MESSAGES,
                            isSpamConversation = viewModel.selectedTab == InboxTab.SPAM,
                            callbacks = DetailScreenCallbacks(
                                onBack = { viewModel.closeConversation() },
                                onMessageLongPress = { sms -> viewModel.selectedForDelete = sms },
                                onSpamMessageClick = { sms -> viewModel.openedSpamMessage = sms },
                                onMarkAsNotSpam = { sms ->
                                    viewModel.markAsNotSpam(sms) { moved ->
                                        if (moved) {
                                            Toast.makeText(context, context.getString(R.string.message_moved), Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, context.getString(R.string.message_move_failed), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onDeleteSpam = { sms -> viewModel.deleteSpam(sms) },
                                onSendMessage = { messageBody ->
                                    viewModel.sendAndStoreMessage(conv.displaySender, messageBody) { sent ->
                                        if (!sent) {
                                            Toast.makeText(context, context.getString(R.string.message_send_failed), Toast.LENGTH_SHORT).show()
                                        }
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
                        onBack = { viewModel.closeNewMessage() },
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
                        currentThemeMode = currentThemeMode,
                        onThemeModeChanged = onThemeModeChanged,
                        blockedSenders = viewModel.blockedSenders,
                        onAddBlockedSender = { sender -> viewModel.addBlockedSender(sender) },
                        onRemoveBlockedSender = { sender -> viewModel.removeBlockedSender(sender) },
                        onBack = { viewModel.closeSettings() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                ScreenState.LIST -> {
                    ConversationListContent(
                        viewModel = viewModel,
                        filteredConversations = filteredConversations,
                        unreadIds = currentUnreadIds,
                        conversationListState = conversationListState,
                        isBottomBarVisible = isBottomBarVisible
                    )
                }
            }
        }
    }

    // Dialogs
    MessageOptionsDialog(
        sms = viewModel.selectedForDelete,
        onDismiss = { viewModel.selectedForDelete = null },
        onCopy = { sms ->
            val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboardManager.setPrimaryClip(android.content.ClipData.newPlainText("SMS", sms.body))
            Toast.makeText(context, context.getString(R.string.message_copied), Toast.LENGTH_SHORT).show()
            viewModel.selectedForDelete = null
        },
        onDelete = { sms ->
            viewModel.deleteSingleMessage(sms)
        },
        onBlockSender = { sms ->
            viewModel.addBlockedSender(sms.sender)
            Toast.makeText(context, context.getString(R.string.sender_blocked), Toast.LENGTH_SHORT).show()
            viewModel.selectedForDelete = null
        }
    )

    SpamMessageDialog(
        sms = viewModel.openedSpamMessage,
        onDismiss = { viewModel.openedSpamMessage = null }
    )

    BatchDeleteConfirmDialog(
        show = viewModel.showBatchDeleteConfirm,
        count = viewModel.selectedConversationKeys.size,
        onConfirm = { viewModel.batchDeleteSelected(filteredConversations) },
        onDismiss = { viewModel.showBatchDeleteConfirm = false }
    )
    } // Scaffold
}

