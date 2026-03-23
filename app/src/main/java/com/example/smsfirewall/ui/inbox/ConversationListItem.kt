package com.example.smsfirewall.ui.inbox

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

    val context = LocalContext.current
    val displayName = contactName ?: conversation.displaySender
    val avatarLetter = displayName.firstOrNull()?.uppercase() ?: "?"
    val avatarColor = avatarColorForSender(conversation.displaySender)
    val hasUnread = conversation.unreadCount > 0

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
                        .clip(RoundedCornerShape(12.dp))
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
                    .combinedClickable(
                        onClick = {
                            if (state.isActionsVisible) {
                                callbacks.onHideActions()
                            } else {
                                callbacks.onOpenConversation()
                            }
                        },
                        onLongClick = callbacks.onLongClick
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.isSelectionMode) {
                        Checkbox(
                            checked = state.isSelected,
                            onCheckedChange = { callbacks.onToggleSelection() },
                            modifier = Modifier.padding(end = 0.dp)
                        )
                    }
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
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val latestMessage = conversation.messages.lastOrNull()
                                if (latestMessage != null) {
                                    Text(
                                        text = latestMessage.body.ifBlank { stringResource(R.string.empty_message_placeholder) },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (hasUnread) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal,
                                        maxLines = if (state.showReason) 1 else 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    if (state.showReason) {
                                        Text(
                                            text = stringResource(R.string.reason_prefix, latestMessage.reason),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = formatConversationTimestamp(context, conversation.latestReceivedAt),
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
                                            text = if (conversation.unreadCount > UNREAD_BADGE_MAX) {
                                                "${UNREAD_BADGE_MAX}+"
                                            } else {
                                                conversation.unreadCount.toString()
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (state.isActionsVisible) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.isNotificationsMuted) {
                        SwipeActionButton(
                            icon = Icons.Outlined.Notifications,
                            contentDescription = stringResource(R.string.cd_unmute_notifications),
                            onClick = {
                                callbacks.onHideActions()
                                callbacks.onUnmuteNotifications()
                            }
                        )
                    } else {
                        SwipeActionButton(
                            icon = Icons.Outlined.NotificationsOff,
                            contentDescription = stringResource(R.string.cd_mute_notifications),
                            onClick = {
                                callbacks.onHideActions()
                                callbacks.onMuteNotifications()
                            }
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    SwipeActionButton(
                        icon = Icons.Outlined.Block,
                        contentDescription = stringResource(R.string.cd_block_sender),
                        onClick = {
                            callbacks.onHideActions()
                            callbacks.onBlockSender()
                        }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    SwipeActionButton(
                        icon = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.cd_delete_conversation),
                        onClick = {
                            callbacks.onHideActions()
                            callbacks.onSwipeDeleteConversation()
                        }
                    )
                }
            }
        }
    }
}
