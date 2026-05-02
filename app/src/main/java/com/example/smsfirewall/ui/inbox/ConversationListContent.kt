package com.example.smsfirewall.ui.inbox

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Deselect
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.smsfirewall.R
import com.example.smsfirewall.notifications.NotificationConstants

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationListContent(
    viewModel: InboxViewModel,
    filteredConversations: List<SmsConversation>,
    unreadIds: Set<Long>,
    conversationListState: LazyListState,
    isBottomBarVisible: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

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
        try {
            context.startActivity(intent)
        } catch (_: android.content.ActivityNotFoundException) {
            val fallback = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            try {
                context.startActivity(fallback)
            } catch (_: android.content.ActivityNotFoundException) {
                Toast.makeText(context, context.getString(R.string.open_settings), Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            AnimatedVisibility(
                visible = viewModel.isSelectionMode,
                enter   = slideInVertically(initialOffsetY = { -it }) + fadeIn(tween(200)),
                exit    = slideOutVertically(targetOffsetY = { -it }) + fadeOut(tween(200))
            ) {
                SelectionModeHeader(
                    selectedCount = viewModel.selectedConversationKeys.size,
                    allSelected = filteredConversations.isNotEmpty() &&
                        viewModel.selectedConversationKeys.size == filteredConversations.size,
                    onClose = { viewModel.exitSelectionMode() },
                    onToggleSelectAll = {
                        val allSelected = filteredConversations.isNotEmpty() &&
                            viewModel.selectedConversationKeys.size == filteredConversations.size
                        if (allSelected) viewModel.deselectAll()
                        else viewModel.selectAll(filteredConversations.map { it.senderKey }.toSet())
                    },
                    onBlock = {
                        viewModel.pendingBlockSenders = filteredConversations
                            .filter { it.senderKey in viewModel.selectedConversationKeys }
                            .map { it.displaySender }
                        viewModel.showBlockConfirm = true
                    },
                    onDelete = { viewModel.showBatchDeleteConfirm = true },
                    hasSelection = viewModel.selectedConversationKeys.isNotEmpty()
                )
            }
            AnimatedVisibility(
                visible = !viewModel.isSelectionMode,
                enter   = slideInVertically(initialOffsetY = { -it }) + fadeIn(tween(200)),
                exit    = slideOutVertically(targetOffsetY = { -it }) + fadeOut(tween(200))
            ) {
                NormalHeader(onOpenSettings = { viewModel.openSettings() })
            }

            if (viewModel.shouldShowNotificationWarning) {
                NotificationWarningCard(onOpenSettings = openNotificationSettings)
            }

            // Tab row with icons
            PrimaryTabRow(
                selectedTabIndex = viewModel.selectedTab.ordinal,
                modifier = Modifier.fillMaxWidth(),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                InboxTab.entries.forEach { tab ->
                    Tab(
                        selected = viewModel.selectedTab == tab,
                        onClick  = { viewModel.selectTab(tab) },
                        selectedContentColor   = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (tab) {
                                    InboxTab.MESSAGES -> Icons.Outlined.MailOutline
                                    InboxTab.SPAM     -> Icons.Outlined.Shield
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text  = stringResource(tab.titleResId),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (viewModel.selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            if (viewModel.selectedTab == InboxTab.SPAM) {
                SpamAutoDeleteWarningCard(retentionDays = viewModel.spamRetentionDays)
            }

            PullToRefreshBox(
                isRefreshing = viewModel.isRefreshing,
                onRefresh    = { viewModel.refresh() },
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication     = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        if (viewModel.actionRevealedConversationKey != null) {
                            viewModel.actionRevealedConversationKey = null
                        }
                    }
            ) {
                if (filteredConversations.isEmpty()) {
                    when {
                        viewModel.conversationSearchQuery.isNotBlank() -> SearchEmptyState()
                        viewModel.selectedTab == InboxTab.MESSAGES     -> MessagesEmptyState()
                        else                                            -> SpamEmptyState()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp)
                            .padding(top = 8.dp, bottom = 100.dp),
                        state = conversationListState,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredConversations, key = { it.senderKey }) { conversation ->
                            val isSenderMuted = remember(conversation.senderKey, viewModel.mutedSendersChangeToken) {
                                viewModel.mutedSenderStore.isMuted(conversation.displaySender)
                            }
                            val contactName = remember(conversation.displaySender) {
                                viewModel.getContactName(conversation.displaySender)
                            }
                            val itemState = ConversationItemState(
                                isSelectionMode       = viewModel.isSelectionMode,
                                isSelected            = conversation.senderKey in viewModel.selectedConversationKeys,
                                isActionsVisible      = viewModel.actionRevealedConversationKey == conversation.senderKey && !viewModel.isSelectionMode,
                                isNotificationsMuted  = isSenderMuted,
                                showReason            = viewModel.selectedTab == InboxTab.SPAM
                            )
                            val itemCallbacks = ConversationItemCallbacks(
                                onToggleSelection = { viewModel.toggleSelection(conversation.senderKey) },
                                onLongClick       = { viewModel.enterSelectionMode(conversation.senderKey) },
                                onOpenConversation = {
                                    if (viewModel.isSelectionMode) {
                                        viewModel.toggleSelection(conversation.senderKey)
                                    } else {
                                        viewModel.openConversation(conversation.senderKey)
                                        if (conversation.unreadCount > 0) {
                                            viewModel.markConversationRead(conversation, unreadIds)
                                        }
                                    }
                                },
                                onShowActions  = {
                                    if (!viewModel.isSelectionMode) {
                                        viewModel.actionRevealedConversationKey = conversation.senderKey
                                    }
                                },
                                onHideActions  = {
                                    if (viewModel.actionRevealedConversationKey == conversation.senderKey) {
                                        viewModel.actionRevealedConversationKey = null
                                    }
                                },
                                onSwipeDeleteConversation = { viewModel.deleteConversation(conversation) },
                                onMuteNotifications = {
                                    viewModel.muteNotifications(conversation.displaySender)
                                    Toast.makeText(context, context.getString(R.string.notifications_muted), Toast.LENGTH_SHORT).show()
                                },
                                onUnmuteNotifications = {
                                    viewModel.unmuteNotifications(conversation.displaySender)
                                    Toast.makeText(context, context.getString(R.string.notifications_unmuted), Toast.LENGTH_SHORT).show()
                                },
                                onBlockSender = {
                                    viewModel.pendingBlockSenders = listOf(conversation.displaySender)
                                    viewModel.showBlockConfirm = true
                                }
                            )
                            ConversationListItem(
                                modifier      = Modifier.animateItem(),
                                conversation  = conversation,
                                contactName   = contactName,
                                state         = itemState,
                                callbacks     = itemCallbacks
                            )
                        }
                    }
                }
            }
        }

        // Floating search bar + FAB
        BottomSearchBar(
            visible             = isBottomBarVisible && !viewModel.isSelectionMode,
            searchInput         = viewModel.conversationSearchInput,
            onSearchInputChanged = { value ->
                viewModel.conversationSearchInput = value
                viewModel.conversationSearchQuery = value.trim()
            },
            onSearchSubmit = {
                viewModel.conversationSearchQuery = viewModel.conversationSearchInput.trim()
                focusManager.clearFocus()
                keyboardController?.hide()
            },
            showNewMessageFab = viewModel.selectedTab == InboxTab.MESSAGES,
            onNewMessage      = { viewModel.openNewMessage() },
            modifier          = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun SelectionModeHeader(
    selectedCount: Int,
    allSelected: Boolean,
    onClose: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onBlock: () -> Unit,
    onDelete: () -> Unit,
    hasSelection: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 8.dp, end = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(imageVector = Icons.Filled.Close, contentDescription = stringResource(R.string.cd_close_selection))
        }
        Text(
            text       = stringResource(R.string.selected_count, selectedCount),
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.weight(1f)
        )
        IconButton(onClick = onToggleSelectAll) {
            Icon(
                imageVector = if (allSelected) Icons.Outlined.Deselect else Icons.Outlined.SelectAll,
                contentDescription = stringResource(if (allSelected) R.string.deselect_all else R.string.select_all)
            )
        }
        IconButton(onClick = onBlock, enabled = hasSelection) {
            Icon(
                imageVector = Icons.Outlined.Block,
                contentDescription = stringResource(R.string.block_sender),
                tint = if (hasSelection) MaterialTheme.colorScheme.error
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        }
        IconButton(onClick = onDelete, enabled = hasSelection) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.delete_selected),
                tint = if (hasSelection) MaterialTheme.colorScheme.error
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        }
    }
}

@Composable
private fun NormalHeader(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 20.dp, end = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text       = stringResource(R.string.app_name),
            style      = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color      = MaterialTheme.colorScheme.primary,
            modifier   = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onOpenSettings),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.cd_settings),
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun BottomSearchBar(
    visible: Boolean,
    searchInput: String,
    onSearchInputChanged: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    showNewMessageFab: Boolean,
    onNewMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter   = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit    = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(32.dp), clip = false)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(32.dp))
                .imePadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value          = searchInput,
                onValueChange  = onSearchInputChanged,
                modifier       = Modifier.weight(1f),
                singleLine     = true,
                shape          = RoundedCornerShape(28.dp),
                placeholder    = {
                    Text(
                        text  = stringResource(R.string.search_placeholder),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector        = Icons.Filled.Search,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.primary
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor    = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor  = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor       = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor     = Color.Transparent,
                    cursorColor              = MaterialTheme.colorScheme.primary,
                    focusedTextColor         = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor       = MaterialTheme.colorScheme.onSurface,
                    focusedPlaceholderColor  = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() })
            )
            if (showNewMessageFab) {
                FloatingActionButton(
                    onClick        = onNewMessage,
                    modifier       = Modifier.size(52.dp),
                    shape          = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor   = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        painter            = painterResource(id = R.drawable.ic_message_send),
                        contentDescription = stringResource(R.string.cd_new_message)
                    )
                }
            }
        }
    }
}
