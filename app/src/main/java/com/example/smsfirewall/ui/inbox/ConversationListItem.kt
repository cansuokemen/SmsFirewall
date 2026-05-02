package com.example.smsfirewall.ui.inbox

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.smsfirewall.R

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationListItem(
    conversation: SmsConversation,
    contactName: String?,
    state: ConversationItemState,
    callbacks: ConversationItemCallbacks,
    modifier: Modifier = Modifier
) {
    val currentState by rememberUpdatedState(state)
    val currentCallbacks by rememberUpdatedState(callbacks)
    val haptic = LocalHapticFeedback.current

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    if (currentState.isActionsVisible) {
                        currentCallbacks.onSwipeDeleteConversation()
                        true
                    } else {
                        currentCallbacks.onShowActions()
                        false
                    }
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    currentCallbacks.onHideActions()
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    val displayName   = contactName ?: conversation.displaySender
    val avatarLetter  = displayName.firstOrNull()?.uppercase() ?: "?"
    val avatarColor   = avatarColorForSender(conversation.displaySender)
    val hasUnread     = conversation.unreadCount > 0

    val cardBgColor by animateColorAsState(
        targetValue = when {
            state.isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            else             -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 200),
        label = "cardBg"
    )

    val avatarBrush = remember(avatarColor) {
        Brush.linearGradient(
            colors = listOf(
                avatarColor,
                Color(
                    red   = (avatarColor.red   + (1f - avatarColor.red)   * 0.35f).coerceIn(0f, 1f),
                    green = (avatarColor.green + (1f - avatarColor.green) * 0.35f).coerceIn(0f, 1f),
                    blue  = (avatarColor.blue  + (1f - avatarColor.blue)  * 0.35f).coerceIn(0f, 1f),
                    alpha = 1f
                )
            ),
            start = Offset(0f, 0f),
            end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    SwipeToDismissBox(
        modifier = modifier,
        state = dismissState,
        enableDismissFromStartToEnd = !state.isSelectionMode,
        enableDismissFromEndToStart = !state.isSelectionMode,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            if (direction == SwipeToDismissBoxValue.EndToStart) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(end = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.cd_delete),
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
                    .then(
                        if (hasUnread && !state.isSelected)
                            Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        else
                            Modifier
                    )
                    .combinedClickable(
                        onClick = {
                            if (state.isActionsVisible) callbacks.onHideActions()
                            else callbacks.onOpenConversation()
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            callbacks.onLongClick()
                        }
                    ),
                shape  = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.isSelectionMode) {
                        Checkbox(
                            checked = state.isSelected,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                callbacks.onToggleSelection()
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    // Gradient avatar
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(avatarBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text  = avatarLetter,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text       = displayName,
                                    style      = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                                    color      = MaterialTheme.colorScheme.onSurface,
                                    maxLines   = 1,
                                    overflow   = TextOverflow.Ellipsis
                                )
                                val latestMessage = conversation.messages.lastOrNull()
                                if (latestMessage != null) {
                                    Text(
                                        text       = latestMessage.body.ifBlank { stringResource(R.string.empty_message_placeholder) },
                                        style      = MaterialTheme.typography.bodyMedium,
                                        color      = if (hasUnread) MaterialTheme.colorScheme.onSurface
                                                     else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal,
                                        maxLines   = if (state.showReason) 1 else 2,
                                        overflow   = TextOverflow.Ellipsis,
                                        modifier   = Modifier.padding(top = 2.dp)
                                    )
                                    if (state.showReason) {
                                        Text(
                                            text     = stringResource(R.string.reason_prefix, latestMessage.reason),
                                            style    = MaterialTheme.typography.bodySmall,
                                            color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text       = formatConversationTimestamp(LocalContext.current, conversation.latestReceivedAt),
                                    style      = MaterialTheme.typography.labelSmall,
                                    color      = if (hasUnread) MaterialTheme.colorScheme.primary
                                                 else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal
                                )
                                if (hasUnread) {
                                    // Pill-shaped unread badge
                                    Box(
                                        modifier = Modifier
                                            .wrapContentSize()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.primary)
                                            .padding(horizontal = 7.dp, vertical = 3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text       = if (conversation.unreadCount > UNREAD_BADGE_MAX) "${UNREAD_BADGE_MAX}+"
                                                         else conversation.unreadCount.toString(),
                                            style      = MaterialTheme.typography.labelSmall,
                                            color      = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Swipe action overlay
            AnimatedVisibility(
                visible = state.isActionsVisible,
                enter = slideInHorizontally(tween(200)) { it } + fadeIn(tween(200)),
                exit  = slideOutHorizontally(tween(180)) { it } + fadeOut(tween(180)),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.97f))
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.isNotificationsMuted) {
                        SwipeActionButton(
                            icon = Icons.Outlined.Notifications,
                            contentDescription = stringResource(R.string.cd_unmute_notifications),
                            onClick = { callbacks.onHideActions(); callbacks.onUnmuteNotifications() }
                        )
                    } else {
                        SwipeActionButton(
                            icon = Icons.Outlined.NotificationsOff,
                            contentDescription = stringResource(R.string.cd_mute_notifications),
                            onClick = { callbacks.onHideActions(); callbacks.onMuteNotifications() }
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    SwipeActionButton(
                        icon = Icons.Outlined.Block,
                        contentDescription = stringResource(R.string.cd_block_sender),
                        onClick = { callbacks.onHideActions(); callbacks.onBlockSender() }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    SwipeActionButton(
                        icon = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.cd_delete_conversation),
                        onClick = { callbacks.onHideActions(); callbacks.onSwipeDeleteConversation() }
                    )
                }
            }
        }
    }
}
