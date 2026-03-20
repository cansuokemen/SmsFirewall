package com.example.smsfirewall.ui.inbox

import com.example.smsfirewall.R
import com.example.smsfirewall.data.local.SmsEntity

internal const val SENT_MESSAGE_REASON = "Sent by user"
internal const val SYSTEM_PROVIDER_REASON = "From system provider"
internal const val MESSAGE_TYPE_INBOX = 1
internal const val MESSAGE_TYPE_SENT = 2
internal const val UNREAD_BADGE_MAX = 99

enum class InboxTab(val titleResId: Int) {
    MESSAGES(R.string.tab_messages),
    SPAM(R.string.tab_spam)
}

data class SmsConversation(
    val senderKey: String,
    val displaySender: String,
    val messages: List<SmsEntity>,
    val latestReceivedAt: Long,
    val unreadCount: Int = 0
)

data class SystemMessagesResult(
    val messages: List<SmsEntity>,
    val unreadIds: Set<Long>
)

data class ConversationItemState(
    val isSelectionMode: Boolean = false,
    val isSelected: Boolean = false,
    val isActionsVisible: Boolean = false,
    val isNotificationsMuted: Boolean = false,
    val showReason: Boolean = false
)

data class ConversationItemCallbacks(
    val onToggleSelection: () -> Unit = {},
    val onLongClick: () -> Unit = {},
    val onOpenConversation: () -> Unit,
    val onShowActions: () -> Unit = {},
    val onHideActions: () -> Unit = {},
    val onMessageLongPress: (SmsEntity) -> Unit = {},
    val onSwipeDeleteConversation: () -> Unit = {},
    val onMuteNotifications: () -> Unit = {},
    val onUnmuteNotifications: () -> Unit = {}
)

data class DetailScreenCallbacks(
    val onBack: () -> Unit,
    val onMessageLongPress: (SmsEntity) -> Unit = {},
    val onSpamMessageClick: (SmsEntity) -> Unit = {},
    val onMarkAsNotSpam: (SmsEntity) -> Unit = {},
    val onDeleteSpam: (SmsEntity) -> Unit = {},
    val onSendMessage: (String) -> Unit = {}
)
