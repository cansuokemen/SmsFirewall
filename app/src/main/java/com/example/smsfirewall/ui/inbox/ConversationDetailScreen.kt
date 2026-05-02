package com.example.smsfirewall.ui.inbox

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.smsfirewall.R
import com.example.smsfirewall.data.local.SmsEntity

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationDetailScreen(
    conversation: SmsConversation,
    contactName: String?,
    canSendMessage: Boolean,
    isSpamConversation: Boolean,
    callbacks: DetailScreenCallbacks
) {
    val context = LocalContext.current
    var draftMessage by remember(conversation.senderKey) { mutableStateOf("") }
    val listState       = rememberLazyListState()
    val focusManager    = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val haptic          = LocalHapticFeedback.current
    val density         = LocalDensity.current
    val displayMessages = remember(conversation.messages) { conversation.messages.asReversed() }
    val isKeyboardVisible = WindowInsets.ime.getBottom(density) > 0

    val sendButtonScale by animateFloatAsState(
        targetValue   = if (draftMessage.isNotBlank()) 1f else 0.88f,
        animationSpec = tween(200),
        label         = "sendScale"
    )

    val displayName   = contactName ?: conversation.displaySender
    val avatarLetter  = displayName.firstOrNull()?.uppercase() ?: "?"
    val avatarColor   = avatarColorForSender(conversation.displaySender)
    val avatarBrush   = remember(avatarColor) {
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

    LaunchedEffect(displayMessages.size) {
        if (displayMessages.isNotEmpty()) listState.scrollToItem(0)
    }

    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible && displayMessages.isNotEmpty()) listState.animateScrollToItem(0)
    }

    fun submitMessage() {
        if (!canSendMessage) return
        val text = draftMessage.trim()
        if (text.isBlank()) return
        callbacks.onSendMessage(text)
        draftMessage = ""
    }

    val handleBack = {
        focusManager.clearFocus()
        keyboardController?.hide()
        callbacks.onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .swipeBackGesture(onBack = handleBack)
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Mini gradient avatar
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(avatarBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = avatarLetter,
                            style      = MaterialTheme.typography.labelLarge,
                            color      = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text(
                            text     = displayName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style    = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (contactName != null) {
                            Text(
                                text     = conversation.displaySender,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style    = MaterialTheme.typography.bodySmall,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    handleBack()
                }) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            state   = listState,
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            displayMessages.forEachIndexed { index, sms ->
                val dateHeader = formatDateHeader(context, sms.receivedAt)
                val nextSms    = displayMessages.getOrNull(index + 1)
                val nextDateHeader = nextSms?.let { formatDateHeader(context, it.receivedAt) }

                item(key = sms.id) {
                    DetailMessageBubble(
                        sms       = sms,
                        onClick   = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            if (isSpamConversation) callbacks.onSpamMessageClick(sms)
                        },
                        onLongPress = { callbacks.onMessageLongPress(sms) }
                    )
                    if (isSpamConversation) {
                        SpamMessageActionRow(
                            onMarkAsNotSpam = { callbacks.onMarkAsNotSpam(sms) },
                            onDelete        = { callbacks.onDeleteSpam(sms) }
                        )
                    }
                }

                if (dateHeader != nextDateHeader) {
                    item(key = "date_header_$index") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text     = dateHeader,
                                style    = MaterialTheme.typography.labelMedium,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        if (canSendMessage) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value         = draftMessage,
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
                            } else false
                        },
                    singleLine  = true,
                    shape       = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor      = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor    = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = { submitMessage(); keyboardController?.hide() }
                    ),
                    placeholder = {
                        Text(
                            text  = stringResource(R.string.message_input_placeholder),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
                FilledIconButton(
                    onClick  = { submitMessage(); keyboardController?.hide() },
                    enabled  = draftMessage.isNotBlank(),
                    modifier = Modifier.size(48.dp).scale(sendButtonScale),
                    shape    = CircleShape,
                    colors   = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor   = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.cd_send)
                    )
                }
            }
        }
    }
}
