package com.example.smsfirewall.ui.inbox

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import android.content.Intent
import android.net.Uri
import android.util.Patterns
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.smsfirewall.R
import com.example.smsfirewall.data.AppTextSize
import com.example.smsfirewall.data.BubbleStyle
import com.example.smsfirewall.data.local.SmsEntity

private val OutgoingBubbleShape = RoundedCornerShape(
    topStart = 20.dp, topEnd = 6.dp, bottomStart = 20.dp, bottomEnd = 20.dp
)
private val IncomingBubbleShape = RoundedCornerShape(
    topStart = 6.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp
)
private val LeftOrigin  = TransformOrigin(0f, 1f)
private val RightOrigin = TransformOrigin(1f, 1f)

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DetailMessageBubble(
    sms: SmsEntity,
    textSize: AppTextSize,
    bubbleStyle: BubbleStyle,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongPress: () -> Unit
) {
    val isOutgoing = sms.reason == SENT_MESSAGE_REASON
    val bubbleShape = if (bubbleStyle.isSoft()) RoundedCornerShape(22.dp)
    else if (isOutgoing) OutgoingBubbleShape else IncomingBubbleShape
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val bodyScale = textSize.bodyScale()

    val scaleAnim       = remember(sms.id) { Animatable(0.85f) }
    val translateXAnim  = remember(sms.id) { Animatable(if (isOutgoing) 60f else -40f) }
    val translateYAnim  = remember(sms.id) { Animatable(24f) }
    val alphaAnim       = remember(sms.id) { Animatable(0f) }
    LaunchedEffect(sms.id) {
        alphaAnim.animateTo(1f, animationSpec = tween(durationMillis = 220))
    }
    LaunchedEffect(sms.id) {
        scaleAnim.animateTo(
            targetValue   = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
        )
    }
    LaunchedEffect(sms.id) {
        translateXAnim.animateTo(
            targetValue   = 0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
        )
    }
    LaunchedEffect(sms.id) {
        translateYAnim.animateTo(
            targetValue   = 0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Box {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .graphicsLayer {
                    val s = scaleAnim.value
                    scaleX            = s
                    scaleY            = s
                    alpha             = alphaAnim.value
                    translationX      = translateXAnim.value * density.density
                    translationY      = translateYAnim.value * density.density
                    transformOrigin   = if (isOutgoing) RightOrigin else LeftOrigin
                }
                .shadow(if (isOutgoing) 4.dp else 2.dp, bubbleShape, clip = false)
                .clip(bubbleShape)
                .background(
                    if (isOutgoing) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceContainerHigh
                )
                .then(
                    if (isOutgoing) Modifier
                    else Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                        shape = bubbleShape
                    )
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
            val bodyText = sms.body.ifBlank { stringResource(R.string.empty_message_placeholder) }
            LinkedText(
                text  = bodyText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize * bodyScale
                ),
                color = if (isOutgoing) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text     = formatMessageTimestamp(sms.receivedAt),
                style    = MaterialTheme.typography.labelSmall,
                color    = if (isOutgoing) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 3.dp)
            )
        }
        if (sms.isStarred) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color(0xFFFFC107),
                modifier = Modifier
                    .size(14.dp)
                    .align(if (isOutgoing) Alignment.TopStart else Alignment.TopEnd)
                    .offset(x = if (isOutgoing) (-2).dp else 2.dp, y = (-2).dp)
            )
        }
        } // Box
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
            .shadow(2.dp, IncomingBubbleShape, clip = false)
            .clip(IncomingBubbleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), IncomingBubbleShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text     = sms.body.ifBlank { stringResource(R.string.empty_message_placeholder) },
            style    = MaterialTheme.typography.bodyMedium,
            color    = MaterialTheme.colorScheme.onSurface,
            minLines = bodyMaxLines,
            maxLines = bodyMaxLines,
            overflow = TextOverflow.Ellipsis
        )
        if (showReason) {
            Text(
                text     = stringResource(R.string.reason_prefix, sms.reason),
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
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
    onClick: () -> Unit,
    index: Int = 0
) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 60L)
        scale.animateTo(
            targetValue   = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
        )
    }
    IconButton(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
        onClick  = onClick
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = contentDescription,
            tint               = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val URL_REGEX = Patterns.WEB_URL.toRegex()

@Composable
internal fun LinkedText(text: String, style: TextStyle, color: Color) {
    val context = LocalContext.current
    val annotated = buildAnnotatedString {
        var last = 0
        URL_REGEX.findAll(text).forEach { match ->
            append(text.substring(last, match.range.first))
            pushStringAnnotation("URL", match.value)
            withStyle(SpanStyle(color = Color(0xFF4FC3F7), textDecoration = TextDecoration.Underline)) {
                append(match.value)
            }
            pop()
            last = match.range.last + 1
        }
        if (last < text.length) append(text.substring(last))
    }
    if (annotated.getStringAnnotations("URL", 0, annotated.length).isEmpty()) {
        Text(text = text, style = style, color = color)
    } else {
        ClickableText(text = annotated, style = style.copy(color = color)) { offset ->
            annotated.getStringAnnotations("URL", offset, offset).firstOrNull()?.let { ann ->
                runCatching {
                    val uri = if (ann.item.startsWith("http")) ann.item else "https://${ann.item}"
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
                }
            }
        }
    }
}
