package com.example.smsfirewall.ui.inbox

import android.app.Application
import android.app.NotificationManager
import android.database.ContentObserver
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsfirewall.data.ConversationMetaStore
import com.example.smsfirewall.data.SpamRetentionPreferenceStore
import com.example.smsfirewall.data.SmsRepository
import com.example.smsfirewall.data.background.BackgroundImageRepository
import com.example.smsfirewall.data.background.BackgroundPreferenceStore
import com.example.smsfirewall.data.background.BackgroundSpec
import com.example.smsfirewall.data.local.SmsEntity
import com.example.smsfirewall.filter.FilterKeywordStore
import com.example.smsfirewall.filter.SmsStatus
import com.example.smsfirewall.notifications.MutedSenderStore
import com.example.smsfirewall.notifications.NotificationConstants
import com.example.smsfirewall.notifications.NotificationPreferenceStore
import com.example.smsfirewall.util.normalizeSender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class ScreenState {
    LIST, DETAIL, NEW_MESSAGE, SETTINGS, ARCHIVE
}

sealed interface UiEvent {
    data class ShowToast(val messageResId: Int) : UiEvent
}

@HiltViewModel
class InboxViewModel @Inject constructor(
    application: Application,
    val repository: SmsRepository,
    val mutedSenderStore: MutedSenderStore,
    val conversationMetaStore: ConversationMetaStore,
    val filterKeywordStore: FilterKeywordStore,
    val spamRetentionStore: SpamRetentionPreferenceStore,
    val notificationPreferenceStore: NotificationPreferenceStore,
    val backgroundPreferenceStore: BackgroundPreferenceStore,
    val backgroundImageRepository: BackgroundImageRepository
) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>()

    // --- UI Events (Toast vb.) ---
    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    // --- System messages (ContentObserver) ---
    private val _systemMessages = MutableStateFlow(SystemMessagesResult(emptyList(), emptySet()))
    val systemMessages: StateFlow<SystemMessagesResult> = _systemMessages.asStateFlow()

    private var manualRefreshKey = 0

    private val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = refreshSystemMessages()
        override fun onChange(selfChange: Boolean, uri: Uri?) = refreshSystemMessages()
    }

    // --- Tab & Arama ---
    var selectedTab by mutableStateOf(InboxTab.MESSAGES)
        private set

    var messagesFilter by mutableStateOf(MessagesFilter.ALL)
        private set

    var conversationMetaToken by mutableIntStateOf(0)
        private set

    var conversationSearchInput by mutableStateOf("")
    var conversationSearchQuery by mutableStateOf("")

    fun selectMessagesFilter(filter: MessagesFilter) {
        messagesFilter = filter
    }

    private fun bumpMeta() {
        conversationMetaToken++
    }

    // --- Navigasyon ---
    var openedConversationKey by mutableStateOf<String?>(null)
        private set

    var isNewMessageScreenOpen by mutableStateOf(false)
        private set

    var isSettingsOpen by mutableStateOf(false)
        private set

    var isArchiveOpen by mutableStateOf(false)
        private set

    val currentScreen: ScreenState
        get() = when {
            isSettingsOpen -> ScreenState.SETTINGS
            isArchiveOpen -> ScreenState.ARCHIVE
            openedConversationKey != null -> ScreenState.DETAIL
            isNewMessageScreenOpen -> ScreenState.NEW_MESSAGE
            else -> ScreenState.LIST
        }

    // --- Seçim modu ---
    var isSelectionMode by mutableStateOf(false)
        private set

    var selectedConversationKeys by mutableStateOf(setOf<String>())
        private set

    var showBatchDeleteConfirm by mutableStateOf(false)
    var showBlockConfirm by mutableStateOf(false)
    var pendingBlockSenders by mutableStateOf(listOf<String>())

    // --- Diyalog ---
    var selectedForDelete by mutableStateOf<SmsEntity?>(null)
    var openedSpamMessage by mutableStateOf<SmsEntity?>(null)

    // --- Silme animasyonu state ---
    var pendingCrumpleAnim by mutableStateOf<SmsEntity?>(null)
        private set
    var pendingShredderAnim by mutableStateOf<List<SmsConversation>?>(null)
        private set

    fun beginCrumpleAnim(sms: SmsEntity) { pendingCrumpleAnim = sms }
    fun finishCrumpleAnim() { pendingCrumpleAnim = null }

    fun beginShredderAnim(conversations: List<SmsConversation>) {
        if (conversations.isNotEmpty()) pendingShredderAnim = conversations
    }
    fun finishShredderAnim() { pendingShredderAnim = null }

    // --- Swipe aksiyonları ---
    var actionRevealedConversationKey by mutableStateOf<String?>(null)

    // --- Undo silme ---
    data class PendingDelete(
        val conversations: List<SmsConversation>,
        val tab: InboxTab
    )

    private companion object {
        const val MAX_CONTACT_CACHE_SIZE = 256
    }

    // --- Yenileme ---
    var isRefreshing by mutableStateOf(false)
        private set

    // --- MutedSenderStore ---
    var mutedSendersChangeToken by mutableIntStateOf(0)
        private set

    // --- Engellenen numaralar ---
    var blockedSenders by mutableStateOf(filterKeywordStore.getBlockedSenders())
        private set

    // --- Spam saklama süresi ---
    var spamRetentionDays by mutableStateOf(spamRetentionStore.getRetentionDays())
        private set

    fun setSpamRetention(days: Int) {
        spamRetentionStore.setRetentionDays(days)
        spamRetentionDays = days
    }

    // --- Bildirim tercihleri ---
    var notifSoundEnabled by mutableStateOf(notificationPreferenceStore.isSoundEnabled())
        private set

    var notifSoundUriString by mutableStateOf(notificationPreferenceStore.getSoundUriString())
        private set

    var notifVibrationEnabled by mutableStateOf(notificationPreferenceStore.isVibrationEnabled())
        private set

    var notifQuietHoursEnabled by mutableStateOf(notificationPreferenceStore.isQuietHoursEnabled())
        private set

    var notifQuietStart by mutableStateOf(notificationPreferenceStore.getQuietHoursStart())
        private set

    var notifQuietEnd by mutableStateOf(notificationPreferenceStore.getQuietHoursEnd())
        private set

    fun setNotifSound(enabled: Boolean) {
        notificationPreferenceStore.setSoundEnabled(enabled)
        notifSoundEnabled = enabled
        recreateNotificationChannel()
    }

    fun setNotifSoundUri(uri: Uri?) {
        notificationPreferenceStore.setSoundUri(uri)
        notifSoundUriString = notificationPreferenceStore.getSoundUriString()
        if (uri != null) setNotifSound(true) else recreateNotificationChannel()
    }

    fun setNotifVibration(enabled: Boolean) {
        notificationPreferenceStore.setVibrationEnabled(enabled)
        notifVibrationEnabled = enabled
        recreateNotificationChannel()
    }

    fun setNotifQuietHours(enabled: Boolean) {
        notificationPreferenceStore.setQuietHoursEnabled(enabled)
        notifQuietHoursEnabled = enabled
    }

    fun setNotifQuietStart(hour: Int, minute: Int) {
        notificationPreferenceStore.setQuietHoursStart(hour, minute)
        notifQuietStart = hour to minute
    }

    fun setNotifQuietEnd(hour: Int, minute: Int) {
        notificationPreferenceStore.setQuietHoursEnd(hour, minute)
        notifQuietEnd = hour to minute
    }

    private fun recreateNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.deleteNotificationChannel(NotificationConstants.ALLOWED_SMS_CHANNEL_ID)
        val channel = android.app.NotificationChannel(
            NotificationConstants.ALLOWED_SMS_CHANNEL_ID,
            context.getString(com.example.smsfirewall.R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            if (notificationPreferenceStore.isSoundEnabled()) {
                notificationPreferenceStore.getSoundUri()?.let { uri ->
                    setSound(
                        uri,
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                }
            } else {
                setSound(null, null)
            }
            enableVibration(notificationPreferenceStore.isVibrationEnabled())
            enableLights(true)
        }
        manager.createNotificationChannel(channel)
    }

    // --- Arka plan tercihleri ---
    var mainBackground by mutableStateOf<BackgroundSpec>(backgroundPreferenceStore.getMainBackground())
        private set

    private val conversationBackgrounds = mutableStateMapOf<String, BackgroundSpec>()

    fun applyMainBackground(spec: BackgroundSpec) {
        backgroundPreferenceStore.setMainBackground(spec)
        mainBackground = spec
        viewModelScope.launch {
            backgroundImageRepository.pruneUnused(backgroundPreferenceStore)
        }
    }

    fun resetMainBackground() {
        backgroundPreferenceStore.resetMainBackground()
        mainBackground = BackgroundSpec.Default
        viewModelScope.launch {
            backgroundImageRepository.pruneUnused(backgroundPreferenceStore)
        }
    }

    /** Bir sohbetin etkin arka planını döner: özel atama varsa onu, yoksa ana menüyü. */
    fun backgroundFor(address: String): BackgroundSpec {
        val key = normalizeSender(address)
        if (key.isBlank()) return mainBackground
        val cached = conversationBackgrounds[key]
        if (cached != null) return cached
        val stored = backgroundPreferenceStore.getConversationBackground(address)
        if (stored != null) {
            conversationBackgrounds[key] = stored
            return stored
        }
        return mainBackground
    }

    fun applyConversationBackground(address: String, spec: BackgroundSpec) {
        val key = normalizeSender(address)
        if (key.isBlank()) return
        backgroundPreferenceStore.setConversationBackground(address, spec)
        conversationBackgrounds[key] = spec
        viewModelScope.launch {
            backgroundImageRepository.pruneUnused(backgroundPreferenceStore)
        }
    }

    fun resetConversationBackground(address: String) {
        val key = normalizeSender(address)
        if (key.isBlank()) return
        backgroundPreferenceStore.resetConversationBackground(address)
        conversationBackgrounds.remove(key)
        viewModelScope.launch {
            backgroundImageRepository.pruneUnused(backgroundPreferenceStore)
        }
    }

    // --- Kişi adı cache ---
    val contactNameCache = mutableStateMapOf<String, String?>()
    private val pendingLookups = mutableSetOf<String>()

    // --- Notification warning ---
    val shouldShowNotificationWarning: Boolean
        get() {
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(NotificationManager::class.java)
                val channel = manager?.getNotificationChannel(NotificationConstants.ALLOWED_SMS_CHANNEL_ID)
                return channel != null && channel.importance < NotificationManager.IMPORTANCE_HIGH
            }
            return false
        }

    init {
        context.contentResolver.registerContentObserver(
            Telephony.Sms.CONTENT_URI,
            true,
            contentObserver
        )
        refreshSystemMessages()
        viewModelScope.launch {
            repository.deleteByStatusBefore(
                status = SmsStatus.BLOCK,
                beforeTimestamp = spamRetentionStore.cutoffTimestamp()
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        context.contentResolver.unregisterContentObserver(contentObserver)
    }

    // --- Data ---

    private fun refreshSystemMessages() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                querySystemMessagesWithReadStatus(context)
            }
            _systemMessages.value = result
        }
    }

    fun getContactName(phoneNumber: String): String? {
        if (phoneNumber in contactNameCache) return contactNameCache[phoneNumber]
        if (phoneNumber !in pendingLookups) {
            pendingLookups.add(phoneNumber)
            viewModelScope.launch {
                val name = withContext(Dispatchers.IO) {
                    resolveContactName(context, phoneNumber)
                }
                if (contactNameCache.size >= MAX_CONTACT_CACHE_SIZE) {
                    contactNameCache.keys.firstOrNull()?.let { contactNameCache.remove(it) }
                }
                contactNameCache[phoneNumber] = name
            }
        }
        return null
    }

    // --- Navigasyon aksiyonları ---

    fun selectTab(tab: InboxTab) {
        selectedTab = tab
        isSelectionMode = false
        selectedConversationKeys = emptySet()
    }

    fun openConversation(key: String) {
        isNewMessageScreenOpen = false
        actionRevealedConversationKey = null
        openedConversationKey = key
    }

    fun closeConversation() {
        openedConversationKey = null
    }

    fun openNewMessage() {
        openedConversationKey = null
        actionRevealedConversationKey = null
        isNewMessageScreenOpen = true
    }

    fun closeNewMessage() {
        isNewMessageScreenOpen = false
    }

    fun openSettings() {
        isSettingsOpen = true
    }

    fun closeSettings() {
        isSettingsOpen = false
    }

    // --- Yenileme ---

    fun refresh() {
        isRefreshing = true
        manualRefreshKey++
        refreshSystemMessages()
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            isRefreshing = false
        }
    }

    // --- Seçim modu aksiyonları ---

    fun enterSelectionMode(key: String) {
        if (!isSelectionMode) {
            isSelectionMode = true
            selectedConversationKeys = setOf(key)
            actionRevealedConversationKey = null
        }
    }

    fun toggleSelection(key: String) {
        val current = selectedConversationKeys
        selectedConversationKeys = if (key in current) {
            current - key
        } else {
            current + key
        }
        if (selectedConversationKeys.isEmpty()) {
            isSelectionMode = false
        }
    }

    fun selectAll(keys: Set<String>) {
        selectedConversationKeys = keys
    }

    fun deselectAll() {
        selectedConversationKeys = emptySet()
    }

    fun exitSelectionMode() {
        isSelectionMode = false
        selectedConversationKeys = emptySet()
    }

    fun batchDeleteSelected(conversations: List<SmsConversation>) {
        showBatchDeleteConfirm = false
        val keysToDelete = selectedConversationKeys.toSet()
        val toDelete = conversations.filter { it.senderKey in keysToDelete }
        isSelectionMode = false
        selectedConversationKeys = emptySet()
        if (toDelete.isNotEmpty()) {
            deleteConversationsNow(PendingDelete(toDelete, selectedTab))
        }
    }

    fun togglePinSelected(visibleConversations: List<SmsConversation>) {
        val targets = visibleConversations.filter { it.senderKey in selectedConversationKeys }
        if (targets.isEmpty()) return
        val anyUnpinned = targets.any { !it.isPinned }
        targets.forEach { conversationMetaStore.setPinned(it.displaySender, anyUnpinned) }
        bumpMeta()
        exitSelectionMode()
    }

    fun toggleFavoriteSelected(visibleConversations: List<SmsConversation>) {
        val targets = visibleConversations.filter { it.senderKey in selectedConversationKeys }
        if (targets.isEmpty()) return
        val anyUnfav = targets.any { !it.isFavorite }
        targets.forEach { conversationMetaStore.setFavorite(it.displaySender, anyUnfav) }
        bumpMeta()
        exitSelectionMode()
    }

    fun toggleMuteSelected(visibleConversations: List<SmsConversation>) {
        val targets = visibleConversations.filter { it.senderKey in selectedConversationKeys }
        if (targets.isEmpty()) return
        val anyUnmuted = targets.any { !mutedSenderStore.isMuted(it.displaySender) }
        targets.forEach {
            if (anyUnmuted) mutedSenderStore.mute(it.displaySender)
            else mutedSenderStore.unmute(it.displaySender)
        }
        mutedSendersChangeToken++
        exitSelectionMode()
    }

    fun archiveSelected(visibleConversations: List<SmsConversation>) {
        val targets = visibleConversations.filter { it.senderKey in selectedConversationKeys }
        if (targets.isEmpty()) return
        targets.forEach { conversationMetaStore.setArchived(it.displaySender, true) }
        bumpMeta()
        exitSelectionMode()
    }

    fun unarchiveSelected(visibleConversations: List<SmsConversation>) {
        val targets = visibleConversations.filter { it.senderKey in selectedConversationKeys }
        if (targets.isEmpty()) return
        targets.forEach { conversationMetaStore.setArchived(it.displaySender, false) }
        bumpMeta()
        exitSelectionMode()
    }

    fun restoreTrashSelected(visibleConversations: List<SmsConversation>) {
        val targets = visibleConversations.filter { it.senderKey in selectedConversationKeys }
        if (targets.isEmpty()) return
        exitSelectionMode()
        viewModelScope.launch {
            for (conv in targets) {
                for (sms in conv.messages) {
                    restoreStoredSmsToSystemInbox(context, repository, sms)
                }
            }
            refreshSystemMessages()
            selectTab(InboxTab.MESSAGES)
        }
    }

    fun openArchive() {
        exitSelectionMode()
        isArchiveOpen = true
    }

    fun closeArchive() {
        exitSelectionMode()
        isArchiveOpen = false
    }

    // --- Konuşma aksiyonları ---

    fun deleteConversation(conversation: SmsConversation) {
        actionRevealedConversationKey = null
        deleteConversationsNow(PendingDelete(listOf(conversation), selectedTab))
    }

    private fun deleteConversationsNow(delete: PendingDelete) {
        viewModelScope.launch {
            performDelete(delete)
        }
    }

    private suspend fun performDelete(delete: PendingDelete) {
        for (conv in delete.conversations) {
            for (sms in conv.messages) {
                if (delete.tab == InboxTab.MESSAGES || delete.tab == InboxTab.ARCHIVE) {
                    repository.insert(
                        SmsEntity(
                            id = 0,
                            sender = sms.sender,
                            body = sms.body,
                            receivedAt = sms.receivedAt,
                            status = SmsStatus.TRASH,
                            reason = sms.reason
                        )
                    )
                    deleteSmsFromSystemProvider(context, sms.id)
                    conversationMetaStore.setArchived(conv.displaySender, false)
                } else if (delete.tab == InboxTab.SPAM) {
                    repository.insert(
                        SmsEntity(
                            id = 0,
                            sender = sms.sender,
                            body = sms.body,
                            receivedAt = sms.receivedAt,
                            status = SmsStatus.TRASH,
                            reason = sms.reason
                        )
                    )
                    repository.delete(sms)
                } else {
                    repository.delete(sms)
                }
            }
        }
        bumpMeta()
    }

    fun muteNotifications(sender: String) {
        mutedSenderStore.mute(sender)
        mutedSendersChangeToken++
    }

    fun unmuteNotifications(sender: String) {
        mutedSenderStore.unmute(sender)
        mutedSendersChangeToken++
    }

    // --- Mesaj aksiyonları ---

    fun markAsNotSpam(sms: SmsEntity, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val moved = moveSpamToSystemInbox(context, repository, sms)
            onResult(moved)
        }
    }

    fun markSelectedAsSpam(visibleConversations: List<SmsConversation>) {
        val targets = visibleConversations.filter { it.senderKey in selectedConversationKeys }
        if (targets.isEmpty()) return
        exitSelectionMode()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                for (conv in targets) {
                    for (sms in conv.messages) {
                        repository.insert(
                            SmsEntity(
                                id         = 0,
                                sender     = sms.sender,
                                body       = sms.body,
                                receivedAt = sms.receivedAt,
                                status     = SmsStatus.BLOCK,
                                reason     = "Manually marked as spam"
                            )
                        )
                        if (sms.reason != SENT_MESSAGE_REASON) {
                            deleteSmsFromSystemProvider(context, sms.id)
                        }
                    }
                }
            }
            selectTab(InboxTab.SPAM)
        }
    }

    fun deleteSpam(sms: SmsEntity) {
        viewModelScope.launch { repository.delete(sms) }
    }

    fun deleteSingleMessage(sms: SmsEntity) {
        viewModelScope.launch {
            if (selectedTab == InboxTab.MESSAGES || selectedTab == InboxTab.ARCHIVE) {
                repository.insert(
                    SmsEntity(
                        id = 0,
                        sender = sms.sender,
                        body = sms.body,
                        receivedAt = sms.receivedAt,
                        status = SmsStatus.TRASH,
                        reason = sms.reason
                    )
                )
                deleteSmsFromSystemProvider(context, sms.id)
            } else if (selectedTab == InboxTab.SPAM) {
                repository.insert(
                    SmsEntity(
                        id = 0,
                        sender = sms.sender,
                        body = sms.body,
                        receivedAt = sms.receivedAt,
                        status = SmsStatus.TRASH,
                        reason = sms.reason
                    )
                )
                repository.delete(sms)
            } else {
                repository.delete(sms)
            }
            selectedForDelete = null
        }
    }

    fun sendAndStoreMessage(destinationAddress: String, messageBody: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val sent = sendSmsMessage(context, destinationAddress, messageBody)
            if (!sent) {
                onResult(false)
                return@launch
            }
            insertSentSmsIntoSystemProvider(context, destinationAddress, messageBody, System.currentTimeMillis())
            onResult(true)
        }
    }

    fun markConversationRead(conversation: SmsConversation, unreadIds: Set<Long>) {
        viewModelScope.launch {
            markConversationAsRead(context, conversation, unreadIds)
        }
    }

    // --- Engellenen numara aksiyonları ---

    fun addBlockedSender(sender: String): Boolean {
        val normalized = normalizeSender(sender)
        if (normalized.isBlank()) return false
        if (blockedSenders.any { normalizeSender(it) == normalized }) return false
        filterKeywordStore.addBlockedSender(sender)
        blockedSenders = filterKeywordStore.getBlockedSenders()
        // Sistem inbox mesajları silinmez — UI filtreleme ile gizlenir.
        // Engel kaldırıldığında otomatik geri gelir.
        return true
    }

    fun removeBlockedSender(sender: String) {
        filterKeywordStore.removeBlockedSender(sender)
        blockedSenders = filterKeywordStore.getBlockedSenders()
        // Engel kaldırıldığında Room DB'deki mesajları sistem inbox'a taşı
        val normalized = normalizeSender(sender)
        viewModelScope.launch {
            val allBlocked = repository.getAllByStatus(SmsStatus.BLOCK)
            val toRestore = allBlocked.filter { normalizeSender(it.sender) == normalized }
            for (sms in toRestore) {
                moveSpamToSystemInbox(context, repository, sms)
            }
            refreshSystemMessages()
        }
    }
}

