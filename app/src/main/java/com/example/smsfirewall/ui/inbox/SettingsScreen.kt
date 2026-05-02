package com.example.smsfirewall.ui.inbox

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
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
                title = {
                    Text(
                        text       = stringResource(R.string.settings),
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
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
            // ── Tema ──────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader(
                    title       = stringResource(R.string.theme),
                    description = stringResource(R.string.theme_description),
                    icon        = Icons.Outlined.LightMode
                )
                ElevatedCard(
                    shape  = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.selectableGroup()) {
                        ThemeOption(
                            label    = stringResource(R.string.theme_system),
                            icon     = Icons.Outlined.SettingsBrightness,
                            selected = currentThemeMode == ThemeMode.SYSTEM,
                            onClick  = { onThemeModeChanged(ThemeMode.SYSTEM) }
                        )
                        SettingsDivider()
                        ThemeOption(
                            label    = stringResource(R.string.theme_light),
                            icon     = Icons.Outlined.LightMode,
                            selected = currentThemeMode == ThemeMode.LIGHT,
                            onClick  = { onThemeModeChanged(ThemeMode.LIGHT) }
                        )
                        SettingsDivider()
                        ThemeOption(
                            label    = stringResource(R.string.theme_dark),
                            icon     = Icons.Outlined.DarkMode,
                            selected = currentThemeMode == ThemeMode.DARK,
                            onClick  = { onThemeModeChanged(ThemeMode.DARK) }
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // ── Bildirim Tercihleri ───────────────────────
            item {
                SectionHeader(
                    title       = stringResource(R.string.notification_preferences),
                    description = stringResource(R.string.notification_preferences_description),
                    icon        = Icons.Outlined.Notifications
                )
                ElevatedCard(
                    shape  = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SwitchSettingItem(
                            icon           = if (soundEnabled) Icons.Outlined.VolumeUp else Icons.Outlined.VolumeOff,
                            title          = stringResource(R.string.notification_sound),
                            subtitle       = stringResource(R.string.notification_sound_description),
                            checked        = soundEnabled,
                            onCheckedChange = onSoundChanged
                        )
                        SettingsDivider()
                        SwitchSettingItem(
                            icon           = Icons.Outlined.Vibration,
                            title          = stringResource(R.string.notification_vibration),
                            subtitle       = stringResource(R.string.notification_vibration_description),
                            checked        = vibrationEnabled,
                            onCheckedChange = onVibrationChanged
                        )
                        SettingsDivider()
                        SwitchSettingItem(
                            icon           = if (quietHoursEnabled) Icons.Outlined.NotificationsOff else Icons.Outlined.Notifications,
                            title          = stringResource(R.string.notification_quiet_hours),
                            subtitle       = stringResource(R.string.notification_quiet_hours_description),
                            checked        = quietHoursEnabled,
                            onCheckedChange = onQuietHoursChanged
                        )
                        AnimatedVisibility(visible = quietHoursEnabled) {
                            Column {
                                SettingsDivider()
                                QuietHoursTimePicker(
                                    label        = stringResource(R.string.quiet_hours_start),
                                    hour         = quietHoursStart.first,
                                    minute       = quietHoursStart.second,
                                    onTimeSelected = onQuietHoursStartChanged
                                )
                                SettingsDivider()
                                QuietHoursTimePicker(
                                    label        = stringResource(R.string.quiet_hours_end),
                                    hour         = quietHoursEnd.first,
                                    minute       = quietHoursEnd.second,
                                    onTimeSelected = onQuietHoursEndChanged
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // ── Spam Saklama Süresi ───────────────────────
            item {
                SectionHeader(
                    title       = stringResource(R.string.spam_retention),
                    description = stringResource(R.string.spam_retention_description),
                    icon        = Icons.Outlined.Block
                )
                ElevatedCard(
                    shape  = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.selectableGroup()) {
                        SpamRetentionPreferenceStore.RETENTION_OPTIONS.forEachIndexed { idx, days ->
                            if (idx > 0) SettingsDivider()
                            RetentionOption(
                                label    = stringResource(R.string.spam_retention_days, days),
                                selected = spamRetentionDays == days,
                                onClick  = { onSpamRetentionChanged(days) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // ── Engellenen Numaralar ──────────────────────
            item {
                SectionHeader(
                    title       = stringResource(R.string.blocked_numbers),
                    description = stringResource(R.string.blocked_numbers_description),
                    icon        = Icons.Outlined.Block
                )
                BlockedSenderInput(
                    onAdd = { sender ->
                        val added = onAddBlockedSender(sender)
                        if (added) Toast.makeText(context, context.getString(R.string.blocked_number_added), Toast.LENGTH_SHORT).show()
                        else       Toast.makeText(context, context.getString(R.string.blocked_number_exists), Toast.LENGTH_SHORT).show()
                        added
                    }
                )
                Spacer(Modifier.height(12.dp))
            }

            if (blockedSenders.isEmpty()) {
                item {
                    ElevatedCard(
                        shape  = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector        = Icons.Outlined.Block,
                                contentDescription = null,
                                tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier           = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.padding(start = 8.dp))
                            Text(
                                text  = stringResource(R.string.no_blocked_numbers),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                item {
                    ElevatedCard(
                        shape  = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                    ) {
                        Column {
                            blockedSenders.sorted().forEachIndexed { idx, sender ->
                                if (idx > 0) SettingsDivider()
                                BlockedSenderItem(
                                    sender   = sender,
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

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ── Helper composables ────────────────────────────────────────────────────────

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
}

@Composable
private fun SectionHeader(title: String, description: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(18.dp)
            )
        }
        Column {
            Text(
                text       = title,
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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
        headlineContent    = { Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium) },
        supportingContent  = { Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent     = {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent    = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        colors             = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier           = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }
    )
}

@Composable
private fun QuietHoursTimePicker(
    label: String,
    hour: Int,
    minute: Int,
    onTimeSelected: (Int, Int) -> Unit
) {
    val context  = LocalContext.current
    val timeText = String.format("%02d:%02d", hour, minute)

    ListItem(
        headlineContent = { Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium) },
        trailingContent = {
            Text(
                text       = timeText,
                style      = MaterialTheme.typography.titleMedium,
                color      = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        },
        colors   = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth().clickable {
            TimePickerDialog(context, { _, h, m -> onTimeSelected(h, m) }, hour, minute, true).show()
        }
    )
}

@Composable
private fun BlockedSenderInput(onAdd: (String) -> Boolean) {
    var input by remember { mutableStateOf("") }
    val context = LocalContext.current

    fun submit() {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            Toast.makeText(context, context.getString(R.string.blocked_number_empty), Toast.LENGTH_SHORT).show()
            return
        }
        if (onAdd(trimmed)) input = ""
    }

    OutlinedTextField(
        value         = input,
        onValueChange = { input = it },
        label         = { Text(stringResource(R.string.add_blocked_number)) },
        placeholder   = { Text(stringResource(R.string.add_blocked_number_hint)) },
        singleLine    = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        trailingIcon  = {
            IconButton(onClick = { submit() }, enabled = input.isNotBlank()) {
                Icon(
                    imageVector        = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.cd_add_blocked_number),
                    tint               = if (input.isNotBlank()) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        shape    = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun BlockedSenderItem(sender: String, onRemove: () -> Unit) {
    ListItem(
        headlineContent = { Text(text = sender, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium) },
        leadingContent  = {
            Icon(imageVector = Icons.Outlined.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector        = Icons.Outlined.RemoveCircleOutline,
                    contentDescription = stringResource(R.string.cd_remove_blocked_number),
                    tint               = MaterialTheme.colorScheme.error
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun RetentionOption(label: String, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(
                text       = label,
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color      = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        },
        trailingContent = { RadioButton(selected = selected, onClick = null) },
        colors   = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth().selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
    )
}

@Composable
private fun ThemeOption(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(
                text       = label,
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color      = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        },
        leadingContent  = {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = { RadioButton(selected = selected, onClick = null) },
        colors   = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth().selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
    )
}
