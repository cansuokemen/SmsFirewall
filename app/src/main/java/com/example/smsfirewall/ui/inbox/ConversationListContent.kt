package com.example.smsfirewall.ui.inbox

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Deselect
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.smsfirewall.R
import com.example.smsfirewall.notifications.NotificationConstants
import com.example.smsfirewall.ui.background.AppBackground

private sealed class SelectionArchiveAction(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val labelRes: Int,
    val contentDescriptionRes: Int,
    val onClick: () -> Unit
) {
    class Archive(onClick: () -> Unit) : SelectionArchiveAction(Icons.Outlined.Archive, R.string.action_archive, R.string.cd_archive, onClick)
    class Unarchive(onClick: () -> Unit) : SelectionArchiveAction(Icons.Outlined.Unarchive, R.string.action_unarchive, R.string.cd_unarchive, onClick)
    class Restore(onClick: () -> Unit) : SelectionArchiveAction(Icons.Outlined.RestoreFromTrash, R.string.restore_from_trash, R.string.cd_restore_from_trash, onClick)
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationListContent(
    viewModel: InboxViewModel,
    filteredConversations: List<SmsConversation>,
    unreadIds: Set<Long>,
    modifier: Modifier = Modifier,
    conversationListState: LazyListState,
    isBottomBarVisible: Boolean,
    messagesUnreadCount: Int = 0,
    messagesFavoriteCount: Int = 0,
    archivedCount: Int = 0,
    messagesCount: Int = 0,
    spamCount: Int = 0,
    trashCount: Int = 0,
    onOpenArchive: () -> Unit = {}
) {
    val context = LocalContext.current
    val openSettingsText = stringResource(R.string.open_settings)
    val notificationsMutedText = stringResource(R.string.notifications_muted)
    val notificationsUnmutedText = stringResource(R.string.notifications_unmuted)
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

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
                Toast.makeText(context, openSettingsText, Toast.LENGTH_SHORT).show()
            }
        }
    }

    AppBackground(spec = viewModel.mainBackground, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
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
                    onPin = { viewModel.togglePinSelected(filteredConversations) },
                    onFavorite = { viewModel.toggleFavoriteSelected(filteredConversations) },
                    onMute = { viewModel.toggleMuteSelected(filteredConversations) },
                    archiveAction = when (viewModel.selectedTab) {
                        InboxTab.MESSAGES -> SelectionArchiveAction.Archive { viewModel.archiveSelected(filteredConversations) }
                        InboxTab.ARCHIVE -> SelectionArchiveAction.Unarchive { viewModel.unarchiveSelected(filteredConversations) }
                        InboxTab.TRASH -> SelectionArchiveAction.Restore { viewModel.restoreTrashSelected(filteredConversations) }
                        InboxTab.SPAM -> null
                    },
                    onMarkAsSpam = if (viewModel.selectedTab == InboxTab.MESSAGES) {
                        { viewModel.markSelectedAsSpam(filteredConversations) }
                    } else null,
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

            AnimatedVisibility(
                visible = !viewModel.isSelectionMode,
                enter   = fadeIn(tween(200)),
                exit    = fadeOut(tween(200))
            ) {
                TopSearchBar(
                    searchInput          = viewModel.conversationSearchInput,
                    onSearchInputChanged = { value ->
                        viewModel.conversationSearchInput = value
                        viewModel.conversationSearchQuery = value.trim()
                    },
                    onSearchSubmit = {
                        viewModel.conversationSearchQuery = viewModel.conversationSearchInput.trim()
                    }
                )
            }

            if (viewModel.shouldShowNotificationWarning) {
                NotificationWarningCard(onOpenSettings = openNotificationSettings)
            }

            InboxSegmentedControl(
                selectedTab = viewModel.selectedTab,
                onSelect    = { viewModel.selectTab(it) },
                counts      = mapOf(
                    InboxTab.MESSAGES to messagesCount,
                    InboxTab.SPAM to spamCount,
                    InboxTab.ARCHIVE to archivedCount,
                    InboxTab.TRASH to trashCount
                ),
                modifier    = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )

            AnimatedVisibility(
                visible = viewModel.selectedTab == InboxTab.MESSAGES && !viewModel.isSelectionMode,
                enter   = fadeIn(tween(220)) + slideInVertically(initialOffsetY = { -it / 2 }),
                exit    = fadeOut(tween(160)) + slideOutVertically(targetOffsetY = { -it / 2 })
            ) {
                MessagesFilterChipRow(
                    current        = viewModel.messagesFilter,
                    onChange       = { viewModel.selectMessagesFilter(it) },
                    unreadCount    = messagesUnreadCount,
                    favoriteCount  = messagesFavoriteCount
                )
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
                        viewModel.selectedTab == InboxTab.ARCHIVE      -> ArchiveEmptyState()
                        viewModel.selectedTab == InboxTab.TRASH        -> TrashEmptyState()
                        else                                            -> SpamEmptyState()
                    }
                } else {
                    val firstAppearTimeMs = remember(viewModel.selectedTab) { System.currentTimeMillis() }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp)
                            .padding(top = 8.dp, bottom = 20.dp),
                        state = conversationListState,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(filteredConversations, key = { _, item -> item.senderKey }) { index, conversation ->
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
                                    Toast.makeText(context, notificationsMutedText, Toast.LENGTH_SHORT).show()
                                },
                                onUnmuteNotifications = {
                                    viewModel.unmuteNotifications(conversation.displaySender)
                                    Toast.makeText(context, notificationsUnmutedText, Toast.LENGTH_SHORT).show()
                                },
                                onBlockSender = {
                                    viewModel.pendingBlockSenders = listOf(conversation.displaySender)
                                    viewModel.showBlockConfirm = true
                                }
                            )
                            val isInitialLoad = remember { System.currentTimeMillis() - firstAppearTimeMs < 800L }
                            val staggerOffset = remember { Animatable(if (isInitialLoad) 60f else 0f) }
                            val staggerAlpha  = remember { Animatable(if (isInitialLoad) 0f else 1f) }
                            if (isInitialLoad) {
                                LaunchedEffect(Unit) {
                                    kotlinx.coroutines.delay((index.coerceAtMost(8)) * 45L)
                                    launch { staggerOffset.animateTo(0f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) }
                                    launch { staggerAlpha.animateTo(1f, animationSpec = tween(280)) }
                                }
                            }
                            ConversationListItem(
                                modifier      = Modifier
                                    .animateItem()
                                    .graphicsLayer {
                                        translationY = staggerOffset.value * this.density
                                        alpha        = staggerAlpha.value
                                    },
                                conversation  = conversation,
                                contactName   = contactName,
                                state         = itemState,
                                listDensity   = viewModel.listDensity,
                                textSize      = viewModel.appTextSize,
                                callbacks     = itemCallbacks
                            )
                        }
                        if (archivedCount > 0 && !viewModel.isSelectionMode &&
                            viewModel.selectedTab == InboxTab.MESSAGES) {
                            item(key = "archive_entry") {
                                Spacer(modifier = Modifier.height(4.dp).animateItem())
                                ArchiveEntryRow(
                                    count = archivedCount,
                                    onClick = onOpenArchive,
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }
        }

        // Standalone FAB
        AnimatedVisibility(
            visible = conversationListState.firstVisibleItemIndex > 1 &&
                !viewModel.isSelectionMode &&
                viewModel.selectedTab == InboxTab.MESSAGES,
            enter = slideInVertically(
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                initialOffsetY = { it }
            ) + fadeIn(tween(220)),
            exit = slideOutVertically(
                animationSpec = tween(220),
                targetOffsetY = { it }
            ) + fadeOut(tween(180)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    coroutineScope.launch { conversationListState.animateScrollToItem(0) }
                },
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.cd_scroll_to_top)
                )
            }
        }

        AnimatedVisibility(
            visible  = isBottomBarVisible && !viewModel.isSelectionMode && viewModel.selectedTab == InboxTab.MESSAGES,
            enter    = slideInVertically(
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                initialOffsetY = { it }
            ) + fadeIn(tween(220)),
            exit     = slideOutVertically(
                animationSpec = tween(220),
                targetOffsetY = { it }
            ) + fadeOut(tween(180)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 20.dp)
        ) {
            val fabInteractionSource = remember { MutableInteractionSource() }
            val isFabPressed by fabInteractionSource.collectIsPressedAsState()
            val fabScale by animateFloatAsState(
                targetValue   = if (isFabPressed) 0.88f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label         = "fabScale"
            )
            val fabRotation by animateFloatAsState(
                targetValue   = if (isFabPressed) 18f else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
                label         = "fabRotate"
            )
            FloatingActionButton(
                onClick           = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.openNewMessage()
                },
                interactionSource = fabInteractionSource,
                modifier          = Modifier
                    .size(56.dp)
                    .graphicsLayer {
                        scaleX    = fabScale
                        scaleY    = fabScale
                        rotationZ = fabRotation
                    },
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

@Composable
private fun SelectionModeHeader(
    selectedCount: Int,
    allSelected: Boolean,
    onClose: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onPin: () -> Unit,
    onFavorite: () -> Unit,
    onMute: () -> Unit,
    archiveAction: SelectionArchiveAction?,
    onMarkAsSpam: (() -> Unit)?,
    onBlock: () -> Unit,
    onDelete: () -> Unit,
    hasSelection: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 8.dp, end = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
            TextButton(onClick = onToggleSelectAll) {
                Icon(
                    imageVector = if (allSelected) Icons.Outlined.Deselect else Icons.Outlined.SelectAll,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text  = stringResource(if (allSelected) R.string.deselect_all else R.string.select_all),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = 4.dp, end = 4.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SelectionActionButton(Icons.Outlined.PushPin, stringResource(R.string.action_pin), stringResource(R.string.cd_pin), hasSelection, onPin)
            SelectionActionButton(Icons.Outlined.StarBorder, stringResource(R.string.action_favorite), stringResource(R.string.cd_favorite), hasSelection, onFavorite)
            SelectionActionButton(Icons.Outlined.NotificationsOff, stringResource(R.string.action_mute), stringResource(R.string.cd_mute_notifications), hasSelection, onMute)
            if (archiveAction != null) {
                SelectionActionButton(archiveAction.icon, stringResource(archiveAction.labelRes), stringResource(archiveAction.contentDescriptionRes), hasSelection, archiveAction.onClick)
            }
            if (onMarkAsSpam != null) {
                IconButton(onClick = onMarkAsSpam, enabled = hasSelection) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = "Spam olarak işaretle",
                        tint = if (hasSelection) MaterialTheme.colorScheme.error
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    )
                }
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
}

@Composable
private fun SelectionActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    val color = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        isDestructive -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    Card(
        modifier = Modifier
            .height(38.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) color.copy(alpha = 0.10f)
            else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = contentDescription, tint = color, modifier = Modifier.size(17.dp))
            Text(label, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessagesFilterChipRow(
    current: MessagesFilter,
    onChange: (MessagesFilter) -> Unit,
    unreadCount: Int = 0,
    favoriteCount: Int = 0
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MessagesFilter.entries.forEach { filter ->
            val selected = current == filter
            val badge = when (filter) {
                MessagesFilter.UNREAD    -> unreadCount
                MessagesFilter.FAVORITES -> favoriteCount
                MessagesFilter.ALL       -> 0
            }
            FilterChip(
                selected = selected,
                onClick  = {
                    if (current != filter) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onChange(filter)
                    }
                },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text       = stringResource(filter.titleResId),
                            style      = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                        if (badge > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f)
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 5.dp, vertical = 1.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text       = if (badge > 99) "99+" else badge.toString(),
                                    style      = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color      = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                                 else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                leadingIcon = null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.90f),
                    selectedLabelColor     = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor         = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f),
                    labelColor             = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled             = true,
                    selected            = selected,
                    borderColor         = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    selectedBorderColor = Color.Transparent,
                    borderWidth         = 0.5.dp,
                    selectedBorderWidth = 0.dp
                )
            )
        }
    }
}

@Composable
private fun NormalHeader(onOpenSettings: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    var gearTapCount by remember { mutableStateOf(0) }
    var menuExpanded by remember { mutableStateOf(false) }
    val gearRotation by animateFloatAsState(
        targetValue   = gearTapCount * 45f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label         = "gearRotation"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 18.dp, end = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = stringResource(R.string.app_name),
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Güvenli, sade ve temiz SMS deneyimi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Box {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        gearTapCount++
                        onOpenSettings()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.cd_settings),
                    tint               = MaterialTheme.colorScheme.onSurface,
                    modifier           = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = gearRotation }
                )
            }
            DropdownMenu(
                expanded          = menuExpanded,
                onDismissRequest  = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text         = { Text("Bildirimler") },
                    leadingIcon  = { Icon(Icons.Outlined.Notifications, contentDescription = null) },
                    onClick      = { menuExpanded = false; onOpenSettings() }
                )
                DropdownMenuItem(
                    text         = { Text("Engellenen Numaralar") },
                    leadingIcon  = { Icon(Icons.Outlined.Block, contentDescription = null) },
                    onClick      = { menuExpanded = false; onOpenSettings() }
                )
                DropdownMenuItem(
                    text         = { Text("Tema") },
                    leadingIcon  = { Icon(Icons.Outlined.Tune, contentDescription = null) },
                    onClick      = { menuExpanded = false; onOpenSettings() }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text         = { Text("Tüm Ayarlar") },
                    leadingIcon  = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    onClick      = { menuExpanded = false; onOpenSettings() }
                )
            }
        }
    }
}

@Composable
private fun TopSearchBar(
    searchInput: String,
    onSearchInputChanged: (String) -> Unit,
    onSearchSubmit: () -> Unit
) {
    val focusManager     = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value         = searchInput,
        onValueChange = onSearchInputChanged,
        modifier      = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        singleLine    = true,
        shape         = RoundedCornerShape(28.dp),
        placeholder   = {
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
        trailingIcon = if (searchInput.isNotEmpty()) {
            {
                IconButton(onClick = { onSearchInputChanged("") }) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                }
            }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor     = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.90f),
            unfocusedContainerColor   = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f),
            focusedBorderColor        = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor      = Color.Transparent,
            cursorColor               = MaterialTheme.colorScheme.primary,
            focusedTextColor          = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor        = MaterialTheme.colorScheme.onSurface,
            focusedPlaceholderColor   = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            onSearchSubmit()
            focusManager.clearFocus()
            keyboardController?.hide()
        })
    )
}

@Composable
private fun InboxSegmentedControl(
    selectedTab: InboxTab,
    onSelect: (InboxTab) -> Unit,
    counts: Map<InboxTab, Int>,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val tabs = InboxTab.entries

    Box(
        modifier = modifier
            .height(46.dp)
            .shadow(2.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f))
    ) {
        val selectedIndex = tabs.indexOf(selectedTab)
        val pillOffsetFraction by animateFloatAsState(
            targetValue   = selectedIndex.toFloat() / tabs.size.toFloat(),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness    = Spring.StiffnessMediumLow
            ),
            label = "segPill"
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(1f / tabs.size)
                .graphicsLayer { translationX = pillOffsetFraction * size.width * tabs.size }
                .padding(4.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
        )
        Row(modifier = Modifier.fillMaxSize()) {
            tabs.forEach { tab ->
                val isSelected = tab == selectedTab
                val count = if (tab == InboxTab.MESSAGES) 0 else counts[tab].orZero()
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            indication        = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(tab)
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (tab) {
                            InboxTab.MESSAGES -> Icons.Outlined.MailOutline
                            InboxTab.SPAM     -> Icons.Outlined.Shield
                            InboxTab.ARCHIVE  -> Icons.Outlined.Archive
                            InboxTab.TRASH    -> Icons.Outlined.DeleteSweep
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text       = stringResource(tab.titleResId),
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color      = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                     else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (count > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.16f)
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                )
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (count > 99) "99+" else count.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Int?.orZero(): Int = this ?: 0

@Composable
internal fun ArchiveEntryRow(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.80f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Outlined.Archive,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier           = Modifier.size(22.dp)
                )
            }
            Text(
                text       = stringResource(R.string.archived_conversations),
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface,
                modifier   = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = count.toString(),
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Icon(
                imageVector        = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun ArchiveListContent(
    viewModel: InboxViewModel,
    archivedConversations: List<SmsConversation>,
    unreadIds: Set<Long>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notificationsMutedText = stringResource(R.string.notifications_muted)
    val notificationsUnmutedText = stringResource(R.string.notifications_unmuted)
    val haptic  = LocalHapticFeedback.current

    AppBackground(spec = viewModel.mainBackground, modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = viewModel.isSelectionMode,
                enter   = slideInVertically(initialOffsetY = { -it }) + fadeIn(tween(200)),
                exit    = slideOutVertically(targetOffsetY = { -it }) + fadeOut(tween(200))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 8.dp, end = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.exitSelectionMode() }) {
                            Icon(
                                imageVector        = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.cd_close_selection)
                            )
                        }
                        Text(
                            text       = stringResource(R.string.selected_count, viewModel.selectedConversationKeys.size),
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier   = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            val allSelected = archivedConversations.isNotEmpty() &&
                                viewModel.selectedConversationKeys.size == archivedConversations.size
                            if (allSelected) viewModel.deselectAll()
                            else viewModel.selectAll(archivedConversations.map { it.senderKey }.toSet())
                        }) {
                            val allSelected = archivedConversations.isNotEmpty() &&
                                viewModel.selectedConversationKeys.size == archivedConversations.size
                            Icon(
                                imageVector        = if (allSelected) Icons.Outlined.Deselect else Icons.Outlined.SelectAll,
                                contentDescription = stringResource(if (allSelected) R.string.deselect_all else R.string.select_all)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(start = 4.dp, end = 4.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val hasSelection = viewModel.selectedConversationKeys.isNotEmpty()
                        IconButton(
                            onClick  = { viewModel.unarchiveSelected(archivedConversations) },
                            enabled  = hasSelection
                        ) {
                            Icon(
                                imageVector        = Icons.Outlined.Unarchive,
                                contentDescription = stringResource(R.string.cd_unarchive),
                                tint               = if (hasSelection) MaterialTheme.colorScheme.primary
                                                     else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                        }
                        IconButton(
                            onClick  = { viewModel.showBatchDeleteConfirm = true },
                            enabled  = hasSelection
                        ) {
                            Icon(
                                imageVector        = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.delete_selected),
                                tint               = if (hasSelection) MaterialTheme.colorScheme.error
                                                     else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = !viewModel.isSelectionMode,
                enter   = slideInVertically(initialOffsetY = { -it }) + fadeIn(tween(200)),
                exit    = slideOutVertically(targetOffsetY = { -it }) + fadeOut(tween(200))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 12.dp, end = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.closeArchive() }) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                    Text(
                        text       = stringResource(R.string.archived_conversations),
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.weight(1f)
                    )
                    if (archivedConversations.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text       = archivedConversations.size.toString(),
                                style      = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            if (archivedConversations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.Archive,
                            contentDescription = stringResource(R.string.cd_empty_state),
                            tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier           = Modifier.size(64.dp)
                        )
                        Text(
                            text   = stringResource(R.string.archive_empty_title),
                            style  = MaterialTheme.typography.titleMedium,
                            color  = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text   = stringResource(R.string.archive_empty_subtitle),
                            style  = MaterialTheme.typography.bodyMedium,
                            color  = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                val firstAppearTimeMs = remember { System.currentTimeMillis() }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                        .padding(top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(archivedConversations, key = { _, item -> item.senderKey }) { index, conversation ->
                        val isSenderMuted = remember(conversation.senderKey, viewModel.mutedSendersChangeToken) {
                            viewModel.mutedSenderStore.isMuted(conversation.displaySender)
                        }
                        val contactName = remember(conversation.displaySender) {
                            viewModel.getContactName(conversation.displaySender)
                        }
                        val itemState = ConversationItemState(
                            isSelectionMode      = viewModel.isSelectionMode,
                            isSelected           = conversation.senderKey in viewModel.selectedConversationKeys,
                            isActionsVisible     = false,
                            isNotificationsMuted = isSenderMuted,
                            showReason           = false
                        )
                        val itemCallbacks = ConversationItemCallbacks(
                            onToggleSelection    = { viewModel.toggleSelection(conversation.senderKey) },
                            onLongClick          = { viewModel.enterSelectionMode(conversation.senderKey) },
                            onOpenConversation   = {
                                if (viewModel.isSelectionMode) viewModel.toggleSelection(conversation.senderKey)
                            },
                            onShowActions        = {},
                            onHideActions        = {},
                            onSwipeDeleteConversation = { viewModel.deleteConversation(conversation) },
                            onMuteNotifications  = {
                                viewModel.muteNotifications(conversation.displaySender)
                                Toast.makeText(context, notificationsMutedText, Toast.LENGTH_SHORT).show()
                            },
                            onUnmuteNotifications = {
                                viewModel.unmuteNotifications(conversation.displaySender)
                                Toast.makeText(context, notificationsUnmutedText, Toast.LENGTH_SHORT).show()
                            },
                            onBlockSender = {
                                viewModel.pendingBlockSenders = listOf(conversation.displaySender)
                                viewModel.showBlockConfirm = true
                            }
                        )
                        val isInitialLoad = remember { System.currentTimeMillis() - firstAppearTimeMs < 800L }
                        val staggerOffset = remember { Animatable(if (isInitialLoad) 60f else 0f) }
                        val staggerAlpha  = remember { Animatable(if (isInitialLoad) 0f else 1f) }
                        if (isInitialLoad) {
                            LaunchedEffect(Unit) {
                                kotlinx.coroutines.delay((index.coerceAtMost(8)) * 45L)
                                launch { staggerOffset.animateTo(0f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) }
                                launch { staggerAlpha.animateTo(1f, animationSpec = tween(280)) }
                            }
                        }
                        ConversationListItem(
                            modifier     = Modifier
                                .animateItem()
                                .graphicsLayer {
                                    translationY = staggerOffset.value * this.density
                                    alpha        = staggerAlpha.value
                                },
                            conversation = conversation,
                            contactName  = contactName,
                            state        = itemState,
                            listDensity  = viewModel.listDensity,
                            textSize     = viewModel.appTextSize,
                            callbacks    = itemCallbacks
                        )
                    }
                }
            }
        }

        BatchDeleteConfirmDialog(
            show      = viewModel.showBatchDeleteConfirm,
            count     = viewModel.selectedConversationKeys.size,
            onConfirm = {
                val targets = archivedConversations.filter { it.senderKey in viewModel.selectedConversationKeys }
                viewModel.beginShredderAnim(targets)
                viewModel.batchDeleteSelected(archivedConversations)
            },
            onDismiss = { viewModel.showBatchDeleteConfirm = false }
        )
    }
}
