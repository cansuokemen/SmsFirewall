package com.example.smsfirewall.ui.inbox

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.smsfirewall.R
import com.example.smsfirewall.data.local.SmsEntity

private val OutgoingBubbleShape = RoundedCornerShape(
    topStart = 18.dp, topEnd = 6.dp, bottomStart = 18.dp, bottomEnd = 18.dp
)
private val IncomingBubbleShape = RoundedCornerShape(
    topStart = 6.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DetailMessageBubble(
    sms: SmsEntity,
    onClick: () -> Unit = {},
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOutgoing = sms.reason == SENT_MESSAGE_REASON
    val bubbleShape = if (isOutgoing) OutgoingBubbleShape else IncomingBubbleShape
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(bubbleShape)
                .background(
                    if (isOutgoing) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .combinedClickable(
                    onClick    = onClick,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress()
                    }
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text  = sms.body.ifBlank { stringResource(R.string.empty_message_placeholder) },
                style = MaterialTheme.typography.bodyMedium,
                color = if (isOutgoing) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text     = formatMessageTimestamp(sms.receivedAt),
                style    = MaterialTheme.typography.labelSmall,
                color    = if (isOutgoing) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f)
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 3.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MessageBubble(
    sms: SmsEntity,
    showReason: Boolean,
    onClick: () -> Unit = {},
    onLongPress: () -> Unit
) {
    val bodyMaxLines = if (showReason) 1 else 2
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(IncomingBubbleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text     = sms.body.ifBlank { stringResource(R.string.empty_message_placeholder) },
            style    = MaterialTheme.typography.bodyMedium,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            minLines = bodyMaxLines,
            maxLines = bodyMaxLines,
            overflow = TextOverflow.Ellipsis
        )
        if (showReason) {
            Text(
                text     = stringResource(R.string.reason_prefix, sms.reason),
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun SpamMessageActionRow(
    onMarkAsNotSpam: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onMarkAsNotSpam,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor   = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Text(text = stringResource(R.string.not_spam), style = MaterialTheme.typography.labelLarge)
        }
        Button(
            onClick = onDelete,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor   = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Text(text = stringResource(R.string.delete), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
internal fun SwipeActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        modifier = Modifier.size(44.dp),
        onClick  = onClick
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = contentDescription,
            tint               = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
