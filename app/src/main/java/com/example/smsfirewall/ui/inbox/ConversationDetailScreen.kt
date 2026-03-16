package com.example.smsfirewall.ui.inbox

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.smsfirewall.data.local.SmsEntity
import java.util.Calendar

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationDetailScreen(
    conversation: SmsConversation,
    contactName: String?,
    canSendMessage: Boolean,
    isSpamConversation: Boolean,
    onBack: () -> Unit,
    onMessageLongPress: (SmsEntity) -> Unit,
    onSpamMessageClick: (SmsEntity) -> Unit,
    onMarkAsNotSpam: (SmsEntity) -> Unit,
    onDeleteSpam: (SmsEntity) -> Unit,
    onSendMessage: (String) -> Unit
) {
    var draftMessage by remember(conversation.senderKey) { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val displayMessages = remember(conversation.messages) { conversation.messages.asReversed() }
    val isKeyboardVisible = WindowInsets.ime.getBottom(density) > 0

    LaunchedEffect(displayMessages.size) {
        if (displayMessages.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible && displayMessages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    fun submitMessage() {
        if (!canSendMessage) return
        val text = draftMessage.trim()
        if (text.isBlank()) return
        onSendMessage(text)
        draftMessage = ""
    }

    val handleBack = {
        focusManager.clearFocus()
        keyboardController?.hide()
        onBack()
    }

    val displayName = contactName ?: conversation.displaySender

    Column(
        modifier = Modifier
            .fillMaxSize()
            .swipeBackGesture(onBack = handleBack)
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = displayName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (contactName != null) {
                        Text(
                            text = conversation.displaySender,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = handleBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Geri"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            state = listState,
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            var lastDateHeader: String? = null
            displayMessages.forEachIndexed { index, sms ->
                val dateHeader = formatDateHeader(sms.receivedAt)
                val nextSms = displayMessages.getOrNull(index + 1)
                val nextDateHeader = nextSms?.let { formatDateHeader(it.receivedAt) }

                item(key = sms.id) {
                    DetailMessageBubble(
                        sms = sms,
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            if (isSpamConversation) {
                                onSpamMessageClick(sms)
                            }
                        },
                        onLongPress = { onMessageLongPress(sms) }
                    )
                    if (isSpamConversation) {
                        SpamMessageActionRow(
                            onMarkAsNotSpam = { onMarkAsNotSpam(sms) },
                            onDelete = { onDeleteSpam(sms) }
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
                                text = dateHeader,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
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
                    .background(MaterialTheme.colorScheme.background)
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = draftMessage,
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
                            } else {
                                false
                            }
                        },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            submitMessage()
                            keyboardController?.hide()
                        }
                    ),
                    placeholder = {
                        Text(
                            text = "Mesaj yaz",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
                FilledIconButton(
                    onClick = {
                        submitMessage()
                        keyboardController?.hide()
                    },
                    enabled = draftMessage.isNotBlank(),
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Gonder"
                    )
                }
            }
        }
    }
}
