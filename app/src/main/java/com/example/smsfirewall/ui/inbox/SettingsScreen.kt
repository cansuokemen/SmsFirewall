package com.example.smsfirewall.ui.inbox

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.smsfirewall.R
import com.example.smsfirewall.data.SpamRetentionPreferenceStore
import com.example.smsfirewall.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentThemeMode: ThemeMode,
    onThemeModeChanged: (ThemeMode) -> Unit,
    blockedSenders: Set<String>,
    onAddBlockedSender: (String) -> Boolean,
    onRemoveBlockedSender: (String) -> Unit,
    spamRetentionDays: Int,
    onSpamRetentionChanged: (Int) -> Unit,
    soundEnabled: Boolean,
    onSoundChanged: (Boolean) -> Unit,
    vibrationEnabled: Boolean,
    onVibrationChanged: (Boolean) -> Unit,
    quietHoursEnabled: Boolean,
    onQuietHoursChanged: (Boolean) -> Unit,
    quietHoursStart: Pair<Int, Int>,
    onQuietHoursStartChanged: (Int, Int) -> Unit,
    quietHoursEnd: Pair<Int, Int>,
    onQuietHoursEndChanged: (Int, Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // --- Tema bölümü ---
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(
                    title = stringResource(R.string.theme),
                    description = stringResource(R.string.theme_description)
                )
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.selectableGroup()) {
                        ThemeOption(
                            label = stringResource(R.string.theme_system),
                            icon = Icons.Outlined.SettingsBrightness,
                            selected = currentThemeMode == ThemeMode.SYSTEM,
                            onClick = { onThemeModeChanged(ThemeMode.SYSTEM) }
                        )
                        ThemeOption(
                            label = stringResource(R.string.theme_light),
                            icon = Icons.Outlined.LightMode,
                            selected = currentThemeMode == ThemeMode.LIGHT,
                            onClick = { onThemeModeChanged(ThemeMode.LIGHT) }
                        )
                        ThemeOption(
                            label = stringResource(R.string.theme_dark),
                            icon = Icons.Outlined.DarkMode,
                            selected = currentThemeMode == ThemeMode.DARK,
                            onClick = { onThemeModeChanged(ThemeMode.DARK) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- Bildirim tercihleri bölümü ---
            item {
                SectionHeader(
                    title = stringResource(R.string.notification_preferences),
                    description = stringResource(R.string.notification_preferences_description)
                )
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Ses
                        SwitchSettingItem(
                            icon = if (soundEnabled) Icons.Outlined.VolumeUp else Icons.Outlined.VolumeOff,
                            title = stringResource(R.string.notification_sound),
                            subtitle = stringResource(R.string.notification_sound_description),
                            checked = soundEnabled,
                            onCheckedChange = onSoundChanged
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Titreşim
                        SwitchSettingItem(
                            icon = Icons.Outlined.Vibration,
                            title = stringResource(R.string.notification_vibration),
                            subtitle = stringResource(R.string.notification_vibration_description),
                            checked = vibrationEnabled,
                            onCheckedChange = onVibrationChanged
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Sessiz Saatler
                        SwitchSettingItem(
                            icon = if (quietHoursEnabled) Icons.Outlined.NotificationsOff else Icons.Outlined.Notifications,
                            title = stringResource(R.string.notification_quiet_hours),
                            subtitle = stringResource(R.string.notification_quiet_hours_description),
                            checked = quietHoursEnabled,
                            onCheckedChange = onQuietHoursChanged
                        )

                        // Sessiz saatler açıksa zaman seçicileri göster
                        AnimatedVisibility(visible = quietHoursEnabled) {
                            Column {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                QuietHoursTimePicker(
                                    label = stringResource(R.string.quiet_hours_start),
                                    hour = quietHoursStart.first,
                                    minute = quietHoursStart.second,
                                    onTimeSelected = onQuietHoursStartChanged
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                QuietHoursTimePicker(
                                    label = stringResource(R.string.quiet_hours_end),
                                    hour = quietHoursEnd.first,
                                    minute = quietHoursEnd.second,
                                    onTimeSelected = onQuietHoursEndChanged
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- Spam saklama süresi bölümü ---
            item {
                SectionHeader(
                    title = stringResource(R.string.spam_retention),
                    description = stringResource(R.string.spam_retention_description)
                )
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.selectableGroup()) {
                        SpamRetentionPreferenceStore.RETENTION_OPTIONS.forEach { days ->
                            RetentionOption(
                                label = stringResource(R.string.spam_retention_days, days),
                                selected = spamRetentionDays == days,
                                onClick = { onSpamRetentionChanged(days) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- Engellenen numaralar bölümü ---
            item {
                SectionHeader(
                    title = stringResource(R.string.blocked_numbers),
                    description = stringResource(R.string.blocked_numbers_description)
                )
                BlockedSenderInput(
                    onAdd = { sender ->
                        val added = onAddBlockedSender(sender)
                        if (added) {
                            Toast.makeText(context, context.getString(R.string.blocked_number_added), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, context.getString(R.string.blocked_number_exists), Toast.LENGTH_SHORT).show()
                        }
                        added
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (blockedSenders.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Block,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.padding(start = 8.dp))
                            Text(
                                text = stringResource(R.string.no_blocked_numbers),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                    ) {
                        Column {
                            blockedSenders.sorted().forEach { sender ->
                                BlockedSenderItem(
                                    sender = sender,
                                    onRemove = {
                                        onRemoveBlockedSender(sender)
                                        Toast.makeText(context, context.getString(R.string.blocked_number_removed), Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// --- Helper Composables ---

@Composable
private fun SectionHeader(title: String, description: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun SwitchSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
        },
        supportingContent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
    )
}

@Composable
private fun QuietHoursTimePicker(
    label: String,
    hour: Int,
    minute: Int,
    onTimeSelected: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    val timeText = String.format("%02d:%02d", hour, minute)

    ListItem(
        headlineContent = {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
        },
        trailingContent = {
            Text(
                text = timeText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                TimePickerDialog(
                    context,
                    { _, selectedHour, selectedMinute ->
                        onTimeSelected(selectedHour, selectedMinute)
                    },
                    hour,
                    minute,
                    true // 24-saat formatı
                ).show()
            }
    )
}

@Composable
private fun BlockedSenderInput(
    onAdd: (String) -> Boolean
) {
    var input by remember { mutableStateOf("") }
    val context = LocalContext.current

    fun submit() {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            Toast.makeText(context, context.getString(R.string.blocked_number_empty), Toast.LENGTH_SHORT).show()
            return
        }
        if (onAdd(trimmed)) {
            input = ""
        }
    }

    OutlinedTextField(
        value = input,
        onValueChange = { input = it },
        label = { Text(stringResource(R.string.add_blocked_number)) },
        placeholder = { Text(stringResource(R.string.add_blocked_number_hint)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        trailingIcon = {
            IconButton(
                onClick = { submit() },
                enabled = input.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.cd_add_blocked_number),
                    tint = if (input.isNotBlank()) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun BlockedSenderItem(
    sender: String,
    onRemove: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(text = sender, style = MaterialTheme.typography.bodyLarge)
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Outlined.Block,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Outlined.RemoveCircleOutline,
                    contentDescription = stringResource(R.string.cd_remove_blocked_number),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun RetentionOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
        },
        trailingContent = {
            RadioButton(selected = selected, onClick = null)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
    )
}

@Composable
private fun ThemeOption(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            RadioButton(selected = selected, onClick = null)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
    )
}
