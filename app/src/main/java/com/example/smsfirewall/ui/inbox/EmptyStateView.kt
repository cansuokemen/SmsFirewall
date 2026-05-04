package com.example.smsfirewall.ui.inbox

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.smsfirewall.R

@Composable
internal fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val breathTransition = rememberInfiniteTransition(label = "iconBreathe")
    val breathScale by breathTransition.animateFloat(
        initialValue   = 1f,
        targetValue    = 1.08f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )
    val swayRotation by breathTransition.animateFloat(
        initialValue   = -6f,
        targetValue    = 6f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(2800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconSway"
    )

    val titleAlpha    = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    LaunchedEffect(title) {
        kotlinx.coroutines.delay(150L)
        titleAlpha.animateTo(1f, animationSpec = tween(durationMillis = 400))
    }
    LaunchedEffect(subtitle) {
        kotlinx.coroutines.delay(320L)
        subtitleAlpha.animateTo(1f, animationSpec = tween(durationMillis = 400))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Colored circle background for icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .graphicsLayer {
                    scaleX    = breathScale
                    scaleY    = breathScale
                    rotationZ = swayRotation
                }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                modifier           = Modifier.size(48.dp),
                tint               = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(
            text       = title,
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSurface,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.graphicsLayer { alpha = titleAlpha.value }
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text      = subtitle,
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier  = Modifier.graphicsLayer { alpha = subtitleAlpha.value }
        )
    }
}

@Composable
internal fun MessagesEmptyState(modifier: Modifier = Modifier) {
    EmptyStateView(
        icon     = Icons.Outlined.MailOutline,
        title    = stringResource(R.string.empty_messages_title),
        subtitle = stringResource(R.string.empty_messages_subtitle),
        modifier = modifier
    )
}

@Composable
internal fun SpamEmptyState(modifier: Modifier = Modifier) {
    EmptyStateView(
        icon     = Icons.Outlined.Shield,
        title    = stringResource(R.string.empty_spam_title),
        subtitle = stringResource(R.string.empty_spam_subtitle),
        modifier = modifier
    )
}

@Composable
internal fun SearchEmptyState(modifier: Modifier = Modifier) {
    EmptyStateView(
        icon     = Icons.Outlined.Search,
        title    = stringResource(R.string.empty_search_title),
        subtitle = stringResource(R.string.empty_search_subtitle),
        modifier = modifier
    )
}
