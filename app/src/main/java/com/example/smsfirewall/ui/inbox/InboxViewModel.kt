package com.example.smsfirewall.ui.inbox

import android.app.Application
import android.app.NotificationManager
import android.database.ContentObserver
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.smsfirewall.data.SpamRetentionPolicy
import com.example.smsfirewall.data.SmsRepository
import com.example.smsfirewall.data.local.SmsEntity
import com.example.smsfirewall.filter.SmsStatus
import com.example.smsfirewall.notifications.MutedSenderStore
import com.example.smsfirewall.notifications.NotificationConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ScreenState {
    LIST, DETAIL, NEW_MESSAGE, SETTINGS
}

sealed interface UiEvent {
    data class ShowToast(val messageResId: Int) : UiEvent
}

class InboxViewModel(
    application: Application,
    val repository: SmsRepository
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

    var conversationSearchInput by mutableStateOf("")
    var conversationSearchQuery by mutableStateOf("")

    // --- Navigasyon ---
    var openedConversationKey by mutableStateOf<String?>(null)
        private set

    var isNewMessageScreenOpen by mutableStateOf(false)
        private set

    var isSettingsOpen by mutableStateOf(false)
        private set

    val currentScreen: ScreenState
        get() = when {
            isSettingsOpen -> ScreenState.SETTINGS
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

    // --- Diyalog ---
    var selectedForDelete by mutableStateOf<SmsEntity?>(null)
    var openedSpamMessage by mutableStateOf<SmsEntity?>(null)

    // --- Swipe aksiyonları ---
    var actionRevealedConversationKey by mutableStateOf<String?>(null)

    // --- Undo silme ---
    data class PendingDelete(
        val conversations: List<SmsConversation>,
        val tab: InboxTab
    )

    var pendingDelete by mutableStateOf<PendingDelete?>(null)
        private set
    private var pendingDeleteJob: Job? = null
    private companion object {
        const val MAX_CONTACT_CACHE_SIZE = 256
        const val UNDO_TIMEOUT_MS = 4000L
    }

    // --- Yenileme ---
    var isRefreshing by mutableStateOf(false)
        private set

    // --- MutedSenderStore ---
    val mutedSenderStore = MutedSenderStore(context)
    var mutedSendersChangeToken by mutableIntStateOf(0)
        private set

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
                if (channel == null) return true
                return channel.importance < NotificationManager.IMPORTANCE_HIGH
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
        if (tab == InboxTab.SPAM) {
            viewModelScope.launch {
                repository.deleteByStatusBefore(
                    status = SmsStatus.BLOCK,
                    beforeTimestamp = SpamRetentionPolicy.cutoffTimestamp()
                )
            }
        }
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
            schedulePendingDelete(PendingDelete(toDelete, selectedTab))
        }
    }

    // --- Konuşma aksiyonları ---

    fun deleteConversation(conversation: SmsConversation) {
        actionRevealedConversationKey = null
        schedulePendingDelete(PendingDelete(listOf(conversation), selectedTab))
    }

    fun undoDelete() {
        pendingDeleteJob?.cancel()
        pendingDelete = null
    }

    fun commitPendingDelete() {
        val delete = pendingDelete ?: return
        pendingDeleteJob?.cancel()
        pendingDelete = null
        viewModelScope.launch {
            performDelete(delete)
        }
    }

    private fun schedulePendingDelete(delete: PendingDelete) {
        // Eğer önceki bir pending varsa, önce onu commit et
        pendingDelete?.let { previous ->
            pendingDeleteJob?.cancel()
            viewModelScope.launch { performDelete(previous) }
        }
        pendingDelete = delete
        pendingDeleteJob = viewModelScope.launch {
            delay(UNDO_TIMEOUT_MS)
            pendingDelete = null
            performDelete(delete)
        }
    }

    private suspend fun performDelete(delete: PendingDelete) {
        for (conv in delete.conversations) {
            for (sms in conv.messages) {
                if (delete.tab == InboxTab.MESSAGES) {
                    deleteSmsFromSystemProvider(context, sms.id)
                } else {
                    repository.delete(sms)
                }
            }
        }
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

    fun deleteSpam(sms: SmsEntity) {
        viewModelScope.launch { repository.delete(sms) }
    }

    fun deleteSingleMessage(sms: SmsEntity) {
        viewModelScope.launch {
            if (selectedTab == InboxTab.MESSAGES) {
                deleteSmsFromSystemProvider(context, sms.id)
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
}

class InboxViewModelFactory(
    private val application: Application,
    private val repository: SmsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InboxViewModel::class.java)) {
            return InboxViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
