package com.example.smsfirewall.ui.inbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.smsfirewall.R

@Composable
internal fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(R.string.cd_empty_state),
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
internal fun MessagesEmptyState(modifier: Modifier = Modifier) {
    EmptyStateView(
        icon = Icons.Outlined.MailOutline,
        title = stringResource(R.string.empty_messages_title),
        subtitle = stringResource(R.string.empty_messages_subtitle),
        modifier = modifier
    )
}

@Composable
internal fun SpamEmptyState(modifier: Modifier = Modifier) {
    EmptyStateView(
        icon = Icons.Outlined.Shield,
        title = stringResource(R.string.empty_spam_title),
        subtitle = stringResource(R.string.empty_spam_subtitle),
        modifier = modifier
    )
}

@Composable
internal fun SearchEmptyState(modifier: Modifier = Modifier) {
    EmptyStateView(
        icon = Icons.Outlined.Search,
        title = stringResource(R.string.empty_search_title),
        subtitle = stringResource(R.string.empty_search_subtitle),
        modifier = modifier
    )
}
