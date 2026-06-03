package com.example.smsfirewall.ui.inbox

import android.app.TimePickerDialog
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.smsfirewall.R
import com.example.smsfirewall.data.AppTextSize
import com.example.smsfirewall.data.BubbleStyle
import com.example.smsfirewall.data.ListDensity
import com.example.smsfirewall.data.PrivacyPreferenceStore
import com.example.smsfirewall.data.SpamRetentionPreferenceStore
import com.example.smsfirewall.data.background.BackgroundSpec
import com.example.smsfirewall.ui.animation.SectionEntry
import com.example.smsfirewall.ui.background.AppBackground
import com.example.smsfirewall.ui.theme.ThemeMode
import java.util.Locale

private enum class SettingsTab(val label: String, val description: String, val icon: ImageVector) {
    GENERAL("Genel", "Tema ve ana ekran", Icons.Outlined.Tune),
    SECURITY("Guvenlik", "PIN ve biyometrik giris", Icons.Outlined.Lock),
    DATA("Veri", "Cop kutusu ve ozel alan", Icons.Outlined.Storage),
    APPEARANCE("Gorunum", "Liste ve sohbet stili", Icons.Outlined.Wallpaper),
    NOTIFICATIONS("Bildirimler", "Ses, titresim ve sessiz saatler", Icons.Outlined.Notifications),
    FILTER("Filtre", "Spam saklama tercihleri", Icons.Outlined.Block),
    BLOCKED("Engellenenler", "Engellenen numara listesi", Icons.Outlined.Block)
}

private data class PreferenceChoice(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentThemeMode: ThemeMode,
    onThemeModeChanged: (ThemeMode) -> Unit,
    blockedSenders: Set<String>,
    onAddBlockedSender: (String) -> Boolean,
    onRemoveBlockedSender: (String) -> Unit,
    blockedKeywords: Set<String>,
    onAddBlockedKeyword: (String) -> Boolean,
    onRemoveBlockedKeyword: (String) -> Unit,
    spamRetentionDays: Int,
    onSpamRetentionChanged: (Int) -> Unit,
    soundEnabled: Boolean,
    onSoundChanged: (Boolean) -> Unit,
    soundUriString: String?,
    onSoundUriChanged: (Uri?) -> Unit,
    vibrationEnabled: Boolean,
    onVibrationChanged: (Boolean) -> Unit,
    quietHoursEnabled: Boolean,
    onQuietHoursChanged: (Boolean) -> Unit,
    quietHoursStart: Pair<Int, Int>,
    onQuietHoursStartChanged: (Int, Int) -> Unit,
    quietHoursEnd: Pair<Int, Int>,
    onQuietHoursEndChanged: (Int, Int) -> Unit,
    mainBackground: BackgroundSpec,
    onChangeMainBackground: () -> Unit,
    onResetMainBackground: () -> Unit,
    appLockEnabled: Boolean,
    pinConfigured: Boolean,
    biometricLockEnabled: Boolean,
    onSetAppLockPin: (String) -> Unit,
    onClearAppLock: () -> Unit,
    onBiometricLockChanged: (Boolean) -> Unit,
    trashRetentionDays: Int,
    onTrashRetentionChanged: (Int) -> Unit,
    privateAreaEncryptionEnabled: Boolean,
    onPrivateAreaEncryptionChanged: (Boolean) -> Unit,
    trashCount: Int,
    spamCount: Int,
    onEmptyTrash: ((Int) -> Unit) -> Unit,
    onEmptySpam: ((Int) -> Unit) -> Unit,
    listDensity: ListDensity,
    onListDensityChanged: (ListDensity) -> Unit,
    appTextSize: AppTextSize,
    onAppTextSizeChanged: (AppTextSize) -> Unit,
    bubbleStyle: BubbleStyle,
    onBubbleStyleChanged: (BubbleStyle) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notificationSoundDefaultText = stringResource(R.string.notification_sound_default)
    val notificationSoundPickText = stringResource(R.string.notification_sound_pick)
    val haptic = LocalHapticFeedback.current
    var selectedTab by remember { mutableStateOf<SettingsTab?>(null) }
    val soundUri = remember(soundUriString) { soundUriString?.let(Uri::parse) }
    val soundTitle = remember(soundUriString, notificationSoundDefaultText) {
        soundUri?.let { uri ->
            runCatching { RingtoneManager.getRingtone(context, uri)?.getTitle(context) }.getOrNull()
        } ?: notificationSoundDefaultText
    }
    val soundPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        onSoundUriChanged(uri)
    }
    val openSoundPicker = {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, notificationSoundPickText)
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, soundUri)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        }
        soundPickerLauncher.launch(intent)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Text(
                        text = selectedTab?.label ?: stringResource(R.string.settings),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        if (selectedTab == null) onBack() else selectedTab = null
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            if (selectedTab == null) {
                item {
                    SettingsSection(index = 0) {
                        SettingsHomeHeader()
                    }
                }
                SettingsTab.entries.forEachIndexed { index, tab ->
                    item {
                        SettingsSection(index = index + 1) {
                            SettingsCategoryCard(
                                tab = tab,
                                summary = settingsCategorySummary(
                                    tab = tab,
                                    currentThemeMode = currentThemeMode,
                                    appLockEnabled = appLockEnabled,
                                    trashCount = trashCount,
                                    spamCount = spamCount,
                                    listDensity = listDensity,
                                    appTextSize = appTextSize,
                                    bubbleStyle = bubbleStyle,
                                    soundEnabled = soundEnabled,
                                    vibrationEnabled = vibrationEnabled,
                                    spamRetentionDays = spamRetentionDays,
                                    blockedCount = blockedSenders.size
                                ),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedTab = tab
                                }
                            )
                        }
                    }
                }
            } else {
                val activeTab = selectedTab ?: SettingsTab.GENERAL
                when (activeTab) {
                    SettingsTab.GENERAL -> {
                    item {
                        SettingsSection(index = 0) {
                            SettingsDetailHeader(
                                tab = SettingsTab.GENERAL,
                                summary = "Tema ve ana ekran gorunumu buradan yonetilir."
                            )
                        }
                    }
                    item {
                        SettingsSection(index = 1) {
                            ThemeCard(currentThemeMode, onThemeModeChanged)
                        }
                    }
                    item {
                        SettingsSection(index = 2) {
                            BackgroundCard(
                                mainBackground = mainBackground,
                                onChangeMainBackground = onChangeMainBackground,
                                onResetMainBackground = onResetMainBackground
                            )
                        }
                    }
                    }

                    SettingsTab.SECURITY -> {
                    item {
                        SettingsSection(index = 0) {
                            SettingsDetailHeader(
                                tab = SettingsTab.SECURITY,
                                summary = if (appLockEnabled) "Uygulama kilidi aktif." else "PIN belirleyerek uygulama acilisini koruyun."
                            )
                        }
                    }
                    item {
                        SettingsSection(index = 1) {
                            SecurityCard(
                                appLockEnabled = appLockEnabled,
                                pinConfigured = pinConfigured,
                                biometricLockEnabled = biometricLockEnabled,
                                onSetAppLockPin = onSetAppLockPin,
                                onClearAppLock = onClearAppLock,
                                onBiometricLockChanged = onBiometricLockChanged
                            )
                        }
                    }
                    }

                    SettingsTab.DATA -> {
                    item {
                        SettingsSection(index = 0) {
                            SettingsDetailHeader(
                                tab = SettingsTab.DATA,
                                summary = "Cop kutusu, spam ve ozel alan tercihleri."
                            )
                        }
                    }
                    item {
                        SettingsSection(index = 1) {
                            DataPrivacyCard(
                                trashRetentionDays = trashRetentionDays,
                                onTrashRetentionChanged = onTrashRetentionChanged,
                                privateAreaEncryptionEnabled = privateAreaEncryptionEnabled,
                                onPrivateAreaEncryptionChanged = onPrivateAreaEncryptionChanged,
                                trashCount = trashCount,
                                spamCount = spamCount,
                                onEmptyTrash = onEmptyTrash,
                                onEmptySpam = onEmptySpam
                            )
                        }
                    }
                    }

                    SettingsTab.APPEARANCE -> {
                    item {
                        SettingsSection(index = 0) {
                            SettingsDetailHeader(
                                tab = SettingsTab.APPEARANCE,
                                summary = "Liste sikligi, yazi boyutu ve sohbet balonlari."
                            )
                        }
                    }
                    item {
                        SettingsSection(index = 1) {
                            AppearanceCard(
                                listDensity = listDensity,
                                onListDensityChanged = onListDensityChanged,
                                appTextSize = appTextSize,
                                onAppTextSizeChanged = onAppTextSizeChanged,
                                bubbleStyle = bubbleStyle,
                                onBubbleStyleChanged = onBubbleStyleChanged
                            )
                        }
                    }
                    }

                    SettingsTab.NOTIFICATIONS -> {
                    item {
                        SettingsSection(index = 0) {
                            val soundText = if (soundEnabled) "Ses acik" else "Ses kapali"
                            val vibrationText = if (vibrationEnabled) "titresim acik" else "titresim kapali"
                            SettingsDetailHeader(
                                tab = SettingsTab.NOTIFICATIONS,
                                summary = "$soundText, $vibrationText"
                            )
                        }
                    }
                    item {
                        SettingsSection(index = 1) {
                            NotificationCard(
                                soundEnabled = soundEnabled,
                                onSoundChanged = onSoundChanged,
                                soundTitle = soundTitle,
                                openSoundPicker = openSoundPicker,
                                vibrationEnabled = vibrationEnabled,
                                onVibrationChanged = onVibrationChanged,
                                quietHoursEnabled = quietHoursEnabled,
                                onQuietHoursChanged = onQuietHoursChanged,
                                quietHoursStart = quietHoursStart,
                                onQuietHoursStartChanged = onQuietHoursStartChanged,
                                quietHoursEnd = quietHoursEnd,
                                onQuietHoursEndChanged = onQuietHoursEndChanged
                            )
                        }
                    }
                    }

                    SettingsTab.FILTER -> {
                    item {
                        SettingsSection(index = 0) {
                            SettingsDetailHeader(
                                tab = SettingsTab.FILTER,
                                summary = "Spam mesajlar $spamRetentionDays gun saklanir. ${blockedKeywords.size} anahtar kelime engelli."
                            )
                        }
                    }
                    item {
                        SettingsSection(index = 1) {
                            SpamRetentionCard(
                                spamRetentionDays = spamRetentionDays,
                                onSpamRetentionChanged = onSpamRetentionChanged
                            )
                        }
                    }
                    item {
                        SettingsSection(index = 2) {
                            BlockedKeywordsCard(
                                blockedKeywords = blockedKeywords,
                                onAddBlockedKeyword = onAddBlockedKeyword,
                                onRemoveBlockedKeyword = onRemoveBlockedKeyword
                            )
                        }
                    }
                    }

                    SettingsTab.BLOCKED -> {
                    item {
                        SettingsSection(index = 0) {
                            SettingsDetailHeader(
                                tab = SettingsTab.BLOCKED,
                                summary = "${blockedSenders.size} numara veya gonderici engelli."
                            )
                        }
                    }
                    item {
                        SettingsSection(index = 1) {
                            BlockedNumbersCard(
                                blockedSenders = blockedSenders,
                                onAddBlockedSender = onAddBlockedSender,
                                onRemoveBlockedSender = onRemoveBlockedSender
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

@Composable
private fun SettingsHomeHeader() {
    ElevatedCard(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 6.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Ayar merkezi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "SmsFirewall deneyimini guvenlik, veri, gorunum ve bildirim basliklari altinda yonetin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
            )
        }
    }
}

@Composable
private fun SettingsCategoryCard(
    tab: SettingsTab,
    summary: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = tab.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsDetailHeader(
    tab: SettingsTab,
    summary: String
) {
    SettingsSummaryCard(
        icon = tab.icon,
        title = tab.label,
        body = summary
    )
}

private fun settingsCategorySummary(
    tab: SettingsTab,
    currentThemeMode: ThemeMode,
    appLockEnabled: Boolean,
    trashCount: Int,
    spamCount: Int,
    listDensity: ListDensity,
    appTextSize: AppTextSize,
    bubbleStyle: BubbleStyle,
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    spamRetentionDays: Int,
    blockedCount: Int
): String = when (tab) {
    SettingsTab.GENERAL -> when (currentThemeMode) {
        ThemeMode.SYSTEM -> "Tema: sistem"
        ThemeMode.LIGHT -> "Tema: acik"
        ThemeMode.DARK -> "Tema: koyu"
    }
    SettingsTab.SECURITY -> if (appLockEnabled) "Uygulama kilidi aktif" else "Kilit kapali"
    SettingsTab.DATA -> "$trashCount cop, $spamCount spam mesaj"
    SettingsTab.APPEARANCE -> "${listDensityLabel(listDensity)}, ${textSizeLabel(appTextSize)}, ${bubbleStyleLabel(bubbleStyle)}"
    SettingsTab.NOTIFICATIONS -> "${if (soundEnabled) "Ses acik" else "Ses kapali"}, ${if (vibrationEnabled) "titresim acik" else "titresim kapali"}"
    SettingsTab.FILTER -> "Spam saklama: $spamRetentionDays gun"
    SettingsTab.BLOCKED -> "$blockedCount engellenen"
}

private fun listDensityLabel(value: ListDensity): String = when (value) {
    ListDensity.COMFORTABLE -> "rahat liste"
    ListDensity.COMPACT -> "kompakt liste"
}

private fun textSizeLabel(value: AppTextSize): String = when (value) {
    AppTextSize.SMALL -> "kucuk yazi"
    AppTextSize.NORMAL -> "normal yazi"
    AppTextSize.LARGE -> "buyuk yazi"
}

private fun bubbleStyleLabel(value: BubbleStyle): String = when (value) {
    BubbleStyle.MODERN -> "modern balon"
    BubbleStyle.SOFT -> "yumusak balon"
}

@Composable
private fun SettingsTabStrip(
    selectedTab: SettingsTab,
    onTabSelected: (SettingsTab) -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 18.dp)
    ) {
        PrimaryScrollableTabRow(
            selectedTabIndex = SettingsTab.entries.indexOf(selectedTab),
            edgePadding = 8.dp,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {}
        ) {
            SettingsTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    text = {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(index: Int, content: @Composable () -> Unit) {
    SectionEntry(index = index) {
        Column {
            content()
            Spacer(Modifier.height(22.dp))
        }
    }
}

@Composable
private fun SettingsSummaryCard(
    icon: ImageVector,
    title: String,
    body: String
) {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f)
                )
            }
        }
    }
}

@Composable
private fun ThemeCard(currentThemeMode: ThemeMode, onThemeModeChanged: (ThemeMode) -> Unit) {
    SectionHeader(
        title = stringResource(R.string.theme),
        description = stringResource(R.string.theme_description),
        icon = Icons.Outlined.LightMode
    )
    SettingsCard {
        Column(modifier = Modifier.selectableGroup()) {
            ThemeOption(
                label = stringResource(R.string.theme_system),
                icon = Icons.Outlined.SettingsBrightness,
                selected = currentThemeMode == ThemeMode.SYSTEM,
                onClick = { onThemeModeChanged(ThemeMode.SYSTEM) }
            )
            SettingsDivider()
            ThemeOption(
                label = stringResource(R.string.theme_light),
                icon = Icons.Outlined.LightMode,
                selected = currentThemeMode == ThemeMode.LIGHT,
                onClick = { onThemeModeChanged(ThemeMode.LIGHT) }
            )
            SettingsDivider()
            ThemeOption(
                label = stringResource(R.string.theme_dark),
                icon = Icons.Outlined.DarkMode,
                selected = currentThemeMode == ThemeMode.DARK,
                onClick = { onThemeModeChanged(ThemeMode.DARK) }
            )
        }
    }
}

@Composable
private fun BackgroundCard(
    mainBackground: BackgroundSpec,
    onChangeMainBackground: () -> Unit,
    onResetMainBackground: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    SectionHeader(
        title = stringResource(R.string.background_section_title),
        description = stringResource(R.string.background_section_description),
        icon = Icons.Outlined.Wallpaper
    )
    SettingsCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 84.dp, height = 56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        0.5.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    )
            ) {
                AppBackground(spec = mainBackground) {}
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.background_section_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.background_section_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onResetMainBackground()
            }) {
                Icon(
                    imageVector = Icons.Outlined.RemoveCircleOutline,
                    contentDescription = stringResource(R.string.background_reset),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onChangeMainBackground()
            }) {
                Text(text = stringResource(R.string.background_change))
            }
        }
    }
}

@Composable
private fun AppearanceCard(
    listDensity: ListDensity,
    onListDensityChanged: (ListDensity) -> Unit,
    appTextSize: AppTextSize,
    onAppTextSizeChanged: (AppTextSize) -> Unit,
    bubbleStyle: BubbleStyle,
    onBubbleStyleChanged: (BubbleStyle) -> Unit
) {
    SectionHeader(
        title = "Gorunum",
        description = "Okunurluk ve sohbet yuzeyi tercihleri",
        icon = Icons.Outlined.Wallpaper
    )
    SettingsCard {
        Column {
            PreferenceChoiceGroup(
                title = "Liste sikligi",
                options = listOf(
                    PreferenceChoice("Rahat", listDensity == ListDensity.COMFORTABLE) { onListDensityChanged(ListDensity.COMFORTABLE) },
                    PreferenceChoice("Kompakt", listDensity == ListDensity.COMPACT) { onListDensityChanged(ListDensity.COMPACT) }
                )
            )
            SettingsDivider()
            PreferenceChoiceGroup(
                title = "Yazi boyutu",
                options = listOf(
                    PreferenceChoice("Kucuk", appTextSize == AppTextSize.SMALL) { onAppTextSizeChanged(AppTextSize.SMALL) },
                    PreferenceChoice("Normal", appTextSize == AppTextSize.NORMAL) { onAppTextSizeChanged(AppTextSize.NORMAL) },
                    PreferenceChoice("Buyuk", appTextSize == AppTextSize.LARGE) { onAppTextSizeChanged(AppTextSize.LARGE) }
                )
            )
            SettingsDivider()
            PreferenceChoiceGroup(
                title = "Sohbet balonu",
                options = listOf(
                    PreferenceChoice("Modern", bubbleStyle == BubbleStyle.MODERN) { onBubbleStyleChanged(BubbleStyle.MODERN) },
                    PreferenceChoice("Yumusak", bubbleStyle == BubbleStyle.SOFT) { onBubbleStyleChanged(BubbleStyle.SOFT) }
                )
            )
        }
    }
}

@Composable
private fun SecurityCard(
    appLockEnabled: Boolean,
    pinConfigured: Boolean,
    biometricLockEnabled: Boolean,
    onSetAppLockPin: (String) -> Unit,
    onClearAppLock: () -> Unit,
    onBiometricLockChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var pinAgain by remember { mutableStateOf("") }
    val canSavePin = pin.length >= 4 && pin == pinAgain
    SectionHeader(
        title = "Uygulama kilidi",
        description = "Uygulamaya girerken PIN veya biyometri iste",
        icon = Icons.Outlined.Lock
    )
    SettingsCard {
        Column {
            ListItem(
                headlineContent = { Text(if (appLockEnabled) "Kilit aktif" else "Kilit kapali") },
                supportingContent = {
                    Text(
                        if (pinConfigured) "PIN ayarlandi. Isterseniz yeni PIN belirleyebilir veya kilidi kapatabilirsiniz."
                        else "En az 4 haneli bir PIN belirleyin."
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            SettingsDivider()
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.take(32) },
                    label = { Text(if (pinConfigured) "Yeni PIN / sifre" else "PIN / sifre") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = pinAgain,
                    onValueChange = { pinAgain = it.take(32) },
                    label = { Text("Tekrar gir") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (canSavePin) {
                                onSetAppLockPin(pin)
                                pin = ""
                                pinAgain = ""
                                Toast.makeText(context, "Uygulama kilidi kaydedildi", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                AnimatedVisibility(visible = pin.isNotBlank() || pinAgain.isNotBlank()) {
                    Text(
                        text = when {
                            pin.length in 1..3 -> "PIN en az 4 karakter olmali."
                            pinAgain.isNotBlank() && pin != pinAgain -> "Girdiginiz degerler ayni degil."
                            else -> "Hazir."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (canSavePin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onSetAppLockPin(pin)
                            pin = ""
                            pinAgain = ""
                            Toast.makeText(context, "Uygulama kilidi kaydedildi", Toast.LENGTH_SHORT).show()
                        },
                        enabled = canSavePin,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (pinConfigured) "PIN degistir" else "Kilidi ac")
                    }
                    TextButton(
                        onClick = {
                            onClearAppLock()
                            pin = ""
                            pinAgain = ""
                            Toast.makeText(context, "Uygulama kilidi kapatildi", Toast.LENGTH_SHORT).show()
                        },
                        enabled = appLockEnabled,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Kapat")
                    }
                }
            }
            SettingsDivider()
            SwitchSettingItem(
                icon = Icons.Outlined.Fingerprint,
                title = "Biyometrik giris",
                subtitle = if (appLockEnabled) "Kilit ekraninda parmak izi veya yuz tanima kullan."
                else "Biyometri icin once PIN belirleyin.",
                checked = biometricLockEnabled,
                onCheckedChange = onBiometricLockChanged,
                enabled = appLockEnabled
            )
        }
    }
}

@Composable
private fun DataPrivacyCard(
    trashRetentionDays: Int,
    onTrashRetentionChanged: (Int) -> Unit,
    privateAreaEncryptionEnabled: Boolean,
    onPrivateAreaEncryptionChanged: (Boolean) -> Unit,
    trashCount: Int,
    spamCount: Int,
    onEmptyTrash: ((Int) -> Unit) -> Unit,
    onEmptySpam: ((Int) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val blockedNumberAddedText = stringResource(R.string.blocked_number_added)
    val blockedNumberExistsText = stringResource(R.string.blocked_number_exists)
    val blockedNumberRemovedText = stringResource(R.string.blocked_number_removed)
    SectionHeader(
        title = "Veri yonetimi",
        description = "Silinen ve engellenen mesajlarin saklanma bicimi",
        icon = Icons.Outlined.Storage
    )
    SettingsCard {
        Column {
            PreferenceChoiceGroup(
                title = "Cop kutusu saklama",
                options = PrivacyPreferenceStore.TRASH_RETENTION_OPTIONS.map { days ->
                    PreferenceChoice(
                        label = trashRetentionLabel(days),
                        selected = trashRetentionDays == days
                    ) {
                        onTrashRetentionChanged(days)
                    }
                }
            )
            SettingsDivider()
            SwitchSettingItem(
                icon = Icons.Outlined.Lock,
                title = "Ozel alani sifrele",
                subtitle = "Gizli mesajlar ve cop kutusu icin sifreli saklama hazirligi.",
                checked = privateAreaEncryptionEnabled,
                onCheckedChange = onPrivateAreaEncryptionChanged
            )
            SettingsDivider()
            DataActionItem(
                icon = Icons.Outlined.DeleteSweep,
                title = "Cop kutusunu temizle",
                subtitle = "$trashCount mesaj kalici olarak silinecek.",
                buttonText = "Temizle",
                enabled = trashCount > 0,
                onClick = {
                    onEmptyTrash { deleted ->
                        Toast.makeText(context, "$deleted mesaj cop kutusundan silindi", Toast.LENGTH_SHORT).show()
                    }
                }
            )
            SettingsDivider()
            DataActionItem(
                icon = Icons.Outlined.Block,
                title = "Spam listesini temizle",
                subtitle = "$spamCount spam mesaj kalici olarak silinecek.",
                buttonText = "Temizle",
                enabled = spamCount > 0,
                onClick = {
                    onEmptySpam { deleted ->
                        Toast.makeText(context, "$deleted spam mesaj silindi", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

private fun trashRetentionLabel(days: Int): String = when (days) {
    PrivacyPreferenceStore.RETENTION_NEVER -> "Sinirsiz"
    else -> "$days gun"
}

@Composable
private fun DataActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    buttonText: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        },
        supportingContent = {
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        },
        trailingContent = {
            TextButton(
                onClick = onClick,
                enabled = enabled
            ) {
                Text(buttonText)
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun NotificationCard(
    soundEnabled: Boolean,
    onSoundChanged: (Boolean) -> Unit,
    soundTitle: String,
    openSoundPicker: () -> Unit,
    vibrationEnabled: Boolean,
    onVibrationChanged: (Boolean) -> Unit,
    quietHoursEnabled: Boolean,
    onQuietHoursChanged: (Boolean) -> Unit,
    quietHoursStart: Pair<Int, Int>,
    onQuietHoursStartChanged: (Int, Int) -> Unit,
    quietHoursEnd: Pair<Int, Int>,
    onQuietHoursEndChanged: (Int, Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    SectionHeader(
        title = stringResource(R.string.notification_preferences),
        description = stringResource(R.string.notification_preferences_description),
        icon = Icons.Outlined.Notifications
    )
    SettingsCard {
        Column {
            SwitchSettingItem(
                icon = if (soundEnabled) Icons.AutoMirrored.Outlined.VolumeUp else Icons.AutoMirrored.Outlined.VolumeOff,
                title = stringResource(R.string.notification_sound),
                subtitle = stringResource(R.string.notification_sound_description),
                checked = soundEnabled,
                onCheckedChange = onSoundChanged
            )
            AnimatedVisibility(visible = soundEnabled) {
                Column {
                    SettingsDivider()
                    ListItem(
                        modifier = Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            openSoundPicker()
                        },
                        headlineContent = { Text(stringResource(R.string.notification_sound_custom)) },
                        supportingContent = { Text(soundTitle) },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            TextButton(onClick = openSoundPicker) {
                                Text(stringResource(R.string.notification_sound_pick_short))
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
            SettingsDivider()
            SwitchSettingItem(
                icon = Icons.Outlined.Vibration,
                title = stringResource(R.string.notification_vibration),
                subtitle = stringResource(R.string.notification_vibration_description),
                checked = vibrationEnabled,
                onCheckedChange = onVibrationChanged
            )
            SettingsDivider()
            SwitchSettingItem(
                icon = if (quietHoursEnabled) Icons.Outlined.NotificationsOff else Icons.Outlined.Notifications,
                title = stringResource(R.string.notification_quiet_hours),
                subtitle = stringResource(R.string.notification_quiet_hours_description),
                checked = quietHoursEnabled,
                onCheckedChange = onQuietHoursChanged
            )
            AnimatedVisibility(visible = quietHoursEnabled) {
                Column {
                    SettingsDivider()
                    QuietHoursTimePicker(
                        label = stringResource(R.string.quiet_hours_start),
                        hour = quietHoursStart.first,
                        minute = quietHoursStart.second,
                        onTimeSelected = onQuietHoursStartChanged
                    )
                    SettingsDivider()
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
}

@Composable
private fun SpamRetentionCard(
    spamRetentionDays: Int,
    onSpamRetentionChanged: (Int) -> Unit
) {
    SectionHeader(
        title = stringResource(R.string.spam_retention),
        description = stringResource(R.string.spam_retention_description),
        icon = Icons.Outlined.Block
    )
    SettingsCard {
        Column(modifier = Modifier.selectableGroup()) {
            SpamRetentionPreferenceStore.RETENTION_OPTIONS.forEachIndexed { idx, days ->
                if (idx > 0) SettingsDivider()
                RetentionOption(
                    label = stringResource(R.string.spam_retention_days, days),
                    selected = spamRetentionDays == days,
                    onClick = { onSpamRetentionChanged(days) }
                )
            }
        }
    }
}

@Composable
private fun BlockedNumbersCard(
    blockedSenders: Set<String>,
    onAddBlockedSender: (String) -> Boolean,
    onRemoveBlockedSender: (String) -> Unit
) {
    val context = LocalContext.current
    val blockedNumberAddedText = stringResource(R.string.blocked_number_added)
    val blockedNumberExistsText = stringResource(R.string.blocked_number_exists)
    val blockedNumberRemovedText = stringResource(R.string.blocked_number_removed)
    SectionHeader(
        title = stringResource(R.string.blocked_numbers),
        description = stringResource(R.string.blocked_numbers_description),
        icon = Icons.Outlined.Block
    )
    BlockedSenderInput(
        onAdd = { sender ->
            val added = onAddBlockedSender(sender)
            Toast.makeText(
                context,
                if (added) blockedNumberAddedText else blockedNumberExistsText,
                Toast.LENGTH_SHORT
            ).show()
            added
        }
    )
    Spacer(Modifier.height(12.dp))
    SettingsCard(modifier = Modifier.animateContentSize()) {
        if (blockedSenders.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Block,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.padding(start = 8.dp))
                Text(
                    text = stringResource(R.string.no_blocked_numbers),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column {
                blockedSenders.sorted().forEachIndexed { idx, sender ->
                    if (idx > 0) SettingsDivider()
                    BlockedSenderItem(
                        sender = sender,
                        onRemove = {
                            onRemoveBlockedSender(sender)
                            Toast.makeText(
                                context,
                                blockedNumberRemovedText,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth()
    ) {
        content()
    }
}

@Composable
private fun PreferenceChoiceGroup(
    title: String,
    options: List<PreferenceChoice>
) {
    Column(modifier = Modifier.padding(vertical = 10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                PreferenceChoiceChip(
                    option = option,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PreferenceChoiceChip(
    option: PreferenceChoice,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val background = if (option.selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f)
    val foreground = if (option.selected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                option.onClick()
            }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = option.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (option.selected) FontWeight.Bold else FontWeight.Medium,
            color = foreground
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
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
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
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
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val contentAlpha = if (enabled) 1f else 0.48f
    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked && enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCheckedChange(it)
                }
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(!checked)
            }
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
    val timeText = String.format(Locale.ROOT, "%02d:%02d", hour, minute)

    ListItem(
        headlineContent = { Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium) },
        trailingContent = {
            Text(
                text = timeText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                TimePickerDialog(context, { _, h, m -> onTimeSelected(h, m) }, hour, minute, true).show()
            }
    )
}

@Composable
private fun BlockedSenderInput(onAdd: (String) -> Boolean) {
    var input by remember { mutableStateOf("") }
    val context = LocalContext.current
    val blockedNumberEmptyText = stringResource(R.string.blocked_number_empty)

    fun submit() {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            Toast.makeText(context, blockedNumberEmptyText, Toast.LENGTH_SHORT).show()
            return
        }
        if (onAdd(trimmed)) input = ""
    }

    OutlinedTextField(
        value = input,
        onValueChange = { input = it },
        label = { Text(stringResource(R.string.add_blocked_number)) },
        placeholder = { Text(stringResource(R.string.add_blocked_number_hint)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        trailingIcon = {
            IconButton(onClick = { submit() }, enabled = input.isNotBlank()) {
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
private fun BlockedSenderItem(sender: String, onRemove: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    ListItem(
        headlineContent = { Text(text = sender, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium) },
        leadingContent = {
            Icon(imageVector = Icons.Outlined.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        },
        trailingContent = {
            IconButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onRemove()
            }) {
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
private fun BlockedKeywordsCard(
    blockedKeywords: Set<String>,
    onAddBlockedKeyword: (String) -> Boolean,
    onRemoveBlockedKeyword: (String) -> Unit
) {
    val context = LocalContext.current
    val blockedKeywordAddedText = stringResource(R.string.blocked_keyword_added)
    val blockedKeywordExistsText = stringResource(R.string.blocked_keyword_exists)
    val blockedKeywordRemovedText = stringResource(R.string.blocked_keyword_removed)
    SectionHeader(
        title = stringResource(R.string.blocked_keywords),
        description = stringResource(R.string.blocked_keywords_description),
        icon = Icons.Outlined.TextFields
    )
    BlockedKeywordInput(
        blockedKeywords = blockedKeywords,
        onAdd = { keyword ->
            val added = onAddBlockedKeyword(keyword)
            Toast.makeText(
                context,
                if (added) blockedKeywordAddedText else blockedKeywordExistsText,
                Toast.LENGTH_SHORT
            ).show()
            added
        },
        onRemove = { keyword ->
            onRemoveBlockedKeyword(keyword)
            Toast.makeText(
                context,
                blockedKeywordRemovedText,
                Toast.LENGTH_SHORT
            ).show()
        }
    )
}

@Composable
private fun BlockedKeywordInput(
    blockedKeywords: Set<String>,
    onAdd: (String) -> Boolean,
    onRemove: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val blockedKeywordEmptyText = stringResource(R.string.blocked_keyword_empty)
    val cdRemoveText = stringResource(R.string.cd_remove_blocked_keyword)

    fun submit() {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            Toast.makeText(context, blockedKeywordEmptyText, Toast.LENGTH_SHORT).show()
            return
        }
        if (onAdd(trimmed)) input = ""
    }

    Column {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text(stringResource(R.string.add_blocked_keyword)) },
            placeholder = { Text(stringResource(R.string.add_blocked_keyword_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
                autoCorrect = false
            ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            trailingIcon = {
                IconButton(onClick = { submit() }, enabled = input.isNotBlank()) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.cd_add_blocked_keyword),
                        tint = if (input.isNotBlank()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )
        if (blockedKeywords.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                blockedKeywords.sorted().forEach { keyword ->
                    InputChip(
                        selected = false,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRemove(keyword)
                        },
                        label = {
                            Text(
                                text = keyword,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = cdRemoveText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        } else {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.no_blocked_keywords),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun RetentionOption(label: String, selected: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val textColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(200),
        label = "retentionTextColor"
    )
    ListItem(
        headlineContent = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor
            )
        },
        trailingContent = { RadioButton(selected = selected, onClick = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                role = Role.RadioButton
            )
    )
}

@Composable
private fun ThemeOption(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val textColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(200),
        label = "themeTextColor"
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "themeIconColor"
    )
    ListItem(
        headlineContent = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor
            )
        },
        leadingContent = {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor)
        },
        trailingContent = { RadioButton(selected = selected, onClick = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                role = Role.RadioButton
            )
    )
}
