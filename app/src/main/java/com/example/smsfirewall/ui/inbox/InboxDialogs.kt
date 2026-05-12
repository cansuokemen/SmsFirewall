package com.example.smsfirewall.ui.inbox

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.smsfirewall.R
import com.example.smsfirewall.data.local.SmsEntity
import com.example.smsfirewall.ui.animation.shake
import kotlinx.coroutines.launch

@Composable
internal fun MessageOptionsDialog(
    sms: SmsEntity?,
    onDismiss: () -> Unit,
    onCopy: (SmsEntity) -> Unit,
    onDelete: (SmsEntity) -> Unit,
    onBlockSender: ((SmsEntity) -> Unit)? = null,
    onStarToggle: ((SmsEntity) -> Unit)? = null
) {
    if (sms == null) return
    val haptic = LocalHapticFeedback.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.message_options)) },
        text = {
            Column {
                if (onStarToggle != null) {
                    DialogActionRow(index = 0) {
                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onStarToggle(sms)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (sms.isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = if (sms.isStarred) "Yıldızı kaldır" else "Yıldızla")
                            }
                        }
                    }
                }
                DialogActionRow(index = if (onStarToggle != null) 1 else 0) {
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCopy(sms)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = stringResource(R.string.copy_message))
                        }
                    }
                }
                DialogActionRow(index = if (onStarToggle != null) 2 else 1) {
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDelete(sms)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.delete_message),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                if (onBlockSender != null) {
                    DialogActionRow(index = if (onStarToggle != null) 3 else 2) {
                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onBlockSender(sms)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Block,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.block_sender),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun DialogActionRow(index: Int, content: @Composable () -> Unit) {
    val offset = remember { Animatable(40f) }
    val alpha  = remember { Animatable(0f) }
    val density = LocalDensity.current
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 60L)
        launch {
            offset.animateTo(
                targetValue   = 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
            )
        }
        launch { alpha.animateTo(1f, animationSpec = tween(durationMillis = 240)) }
    }
    Box(
        modifier = Modifier.graphicsLayer {
            translationX = offset.value * density.density
            this.alpha   = alpha.value
        }
    ) {
        content()
    }
}

@Composable
internal fun SpamMessageDialog(
    sms: SmsEntity?,
    onDismiss: () -> Unit
) {
    if (sms == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.spam_message)) },
        text = { Text(text = sms.body.ifBlank { stringResource(R.string.empty_message_placeholder) }) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.close))
            }
        }
    )
}

@Composable
internal fun BatchDeleteConfirmDialog(
    show: Boolean,
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return
    val haptic = LocalHapticFeedback.current
    var shakeKey by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.delete_selected)) },
        text = { Text(text = stringResource(R.string.delete_selected_confirm, count)) },
        confirmButton = {
            TextButton(
                modifier = Modifier.shake(trigger = if (shakeKey > 0) shakeKey else null),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    shakeKey++
                    onConfirm()
                }
            ) {
                Text(
                    text = stringResource(R.string.delete),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
internal fun BlockSenderConfirmDialog(
    show: Boolean,
    senderNames: List<String>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return
    val haptic = LocalHapticFeedback.current
    var shakeKey by remember { mutableIntStateOf(0) }

    val message = if (senderNames.size == 1) {
        stringResource(R.string.block_confirm_message, senderNames.first())
    } else {
        stringResource(R.string.block_confirm_message_multiple, senderNames.size)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.block_confirm_title)) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(
                modifier = Modifier.shake(trigger = if (shakeKey > 0) shakeKey else null),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    shakeKey++
                    onConfirm()
                }
            ) {
                Text(
                    text = stringResource(R.string.block),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}
