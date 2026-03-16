package com.example.smsfirewall.ui.inbox

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.example.smsfirewall.data.SmsRepository
import com.example.smsfirewall.data.local.SmsEntity
import com.example.smsfirewall.filter.SmsStatus
import com.example.smsfirewall.ui.theme.AvatarColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal const val PHONE_COMPARE_LENGTH = 10
internal const val SENT_MESSAGE_REASON = "Sent by user"
internal const val SYSTEM_PROVIDER_REASON = "From system provider"
internal const val MESSAGE_TYPE_INBOX = 1
internal const val MESSAGE_TYPE_SENT = 2
private const val TAG = "SmsUtils"

internal enum class InboxTab(val title: String) {
    MESSAGES("Mesajlar"),
    SPAM("Spam")
}

internal data class SmsConversation(
    val senderKey: String,
    val displaySender: String,
    val messages: List<SmsEntity>,
    val latestReceivedAt: Long,
    val unreadCount: Int = 0
)

internal data class SystemMessagesResult(
    val messages: List<SmsEntity>,
    val unreadIds: Set<Long>
)

@Composable
internal fun rememberSystemMessages(
    context: Context,
    manualRefreshKey: Int = 0
): SystemMessagesResult {
    var refreshToken by remember { mutableIntStateOf(0) }

    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                refreshToken++
            }

            override fun onChange(selfChange: Boolean, uri: Uri?) {
                refreshToken++
            }
        }

        context.contentResolver.registerContentObserver(
            Telephony.Sms.CONTENT_URI,
            true,
            observer
        )

        onDispose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }

    val result by produceState(
        initialValue = SystemMessagesResult(emptyList(), emptySet()),
        context,
        refreshToken,
        manualRefreshKey
    ) {
        value = withContext(Dispatchers.IO) {
            querySystemMessagesWithReadStatus(context)
        }
    }

    return result
}

internal fun querySystemMessages(context: Context): List<SmsEntity> {
    return querySystemMessagesWithReadStatus(context).messages
}

private fun querySystemMessagesWithReadStatus(context: Context): SystemMessagesResult {
    val projection = arrayOf("_id", "address", "body", "date", "type", "read")
    val selection = "type IN (?, ?)"
    val selectionArgs = arrayOf(MESSAGE_TYPE_INBOX.toString(), MESSAGE_TYPE_SENT.toString())
    val sortOrder = "date ASC"

    return runCatching {
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow("_id")
            val addressColumn = cursor.getColumnIndexOrThrow("address")
            val bodyColumn = cursor.getColumnIndexOrThrow("body")
            val dateColumn = cursor.getColumnIndexOrThrow("date")
            val typeColumn = cursor.getColumnIndexOrThrow("type")
            val readColumn = cursor.getColumnIndexOrThrow("read")

            val messages = mutableListOf<SmsEntity>()
            val unreadIds = mutableSetOf<Long>()

            while (cursor.moveToNext()) {
                val smsType = cursor.getInt(typeColumn)
                val smsId = cursor.getLong(idColumn)
                val isRead = cursor.getInt(readColumn) == 1

                messages.add(
                    SmsEntity(
                        id = smsId,
                        sender = cursor.getString(addressColumn).orEmpty(),
                        body = cursor.getString(bodyColumn).orEmpty(),
                        receivedAt = cursor.getLong(dateColumn),
                        status = SmsStatus.ALLOW,
                        reason = if (smsType == MESSAGE_TYPE_SENT) {
                            SENT_MESSAGE_REASON
                        } else {
                            SYSTEM_PROVIDER_REASON
                        }
                    )
                )

                if (!isRead && smsType == MESSAGE_TYPE_INBOX) {
                    unreadIds.add(smsId)
                }
            }

            SystemMessagesResult(messages, unreadIds)
        } ?: SystemMessagesResult(emptyList(), emptySet())
    }.onFailure { throwable ->
        Log.e(TAG, "Failed to read SMS messages from provider", throwable)
    }.getOrDefault(SystemMessagesResult(emptyList(), emptySet()))
}

internal suspend fun deleteSmsFromSystemProvider(context: Context, smsId: Long) {
    if (smsId <= 0L) return

    withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.delete(
                Telephony.Sms.CONTENT_URI,
                "_id = ?",
                arrayOf(smsId.toString())
            )
        }.onFailure { throwable ->
            Log.e(TAG, "Failed to delete SMS from provider, id=$smsId", throwable)
        }
    }
}

internal suspend fun insertSentSmsIntoSystemProvider(
    context: Context,
    destinationAddress: String,
    messageBody: String,
    sentAt: Long
) {
    withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("address", destinationAddress)
            put("body", messageBody)
            put("date", sentAt)
            put("type", MESSAGE_TYPE_SENT)
            put("read", 1)
            put("seen", 1)
        }

        runCatching {
            context.contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values)
        }.onFailure { throwable ->
            Log.e(TAG, "Failed to insert sent SMS into provider", throwable)
        }
    }
}

internal suspend fun moveSpamToSystemInbox(
    context: Context,
    repository: SmsRepository,
    sms: SmsEntity
): Boolean {
    val insertedIntoInbox = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, sms.sender)
            put(Telephony.Sms.BODY, sms.body)
            put(Telephony.Sms.DATE, sms.receivedAt)
            put(Telephony.Sms.READ, 0)
            put(Telephony.Sms.SEEN, 0)
        }

        runCatching {
            context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values) != null
        }.onFailure { throwable ->
            Log.e(TAG, "Failed to move spam SMS into inbox", throwable)
        }.getOrDefault(false)
    }

    if (!insertedIntoInbox) {
        return false
    }

    repository.delete(sms)
    return true
}

internal fun sendSmsMessage(
    context: Context,
    destinationAddress: String,
    messageBody: String
): Boolean {
    if (destinationAddress.isBlank() || messageBody.isBlank()) {
        return false
    }

    val hasPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.SEND_SMS
    ) == PackageManager.PERMISSION_GRANTED
    if (!hasPermission) {
        return false
    }

    val smsManager = context.getSystemService(SmsManager::class.java) ?: return false

    return runCatching {
        smsManager.sendTextMessage(
            destinationAddress,
            null,
            messageBody,
            null,
            null
        )
    }.isSuccess
}

internal suspend fun readPhoneNumberFromPickerUri(
    context: Context,
    contactUri: Uri
): String? {
    return withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.query(
                contactUri,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                null,
                null,
                null
            )?.use { cursor ->
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (numberIndex == -1 || !cursor.moveToFirst()) {
                    null
                } else {
                    cursor.getString(numberIndex)?.trim()
                }
            }
        }.onFailure { throwable ->
            Log.e(TAG, "Failed to read selected contact", throwable)
        }.getOrNull()
    }
}

internal fun buildConversations(
    messages: List<SmsEntity>,
    unreadIds: Set<Long> = emptySet()
): List<SmsConversation> {
    return messages
        .groupBy { normalizeSenderForGrouping(it.sender) }
        .mapNotNull { (senderKey, senderMessages) ->
            if (senderMessages.isEmpty()) {
                return@mapNotNull null
            }

            val sortedMessages = senderMessages.sortedBy { it.receivedAt }
            val latestMessage = sortedMessages.last()
            val unread = if (unreadIds.isEmpty()) 0 else {
                senderMessages.count { it.id in unreadIds }
            }

            SmsConversation(
                senderKey = senderKey.ifBlank { "unknown_sender_${latestMessage.id}" },
                displaySender = latestMessage.sender.ifBlank { "Bilinmeyen gonderici" },
                messages = sortedMessages,
                latestReceivedAt = latestMessage.receivedAt,
                unreadCount = unread
            )
        }
        .sortedByDescending { it.latestReceivedAt }
}

internal fun normalizeSenderForGrouping(sender: String): String {
    val alphanumeric = sender
        .trim()
        .lowercase(Locale.ROOT)
        .filter { it.isLetterOrDigit() }

    if (alphanumeric.isBlank()) {
        return ""
    }

    return if (alphanumeric.all { it.isDigit() }) {
        if (alphanumeric.length > PHONE_COMPARE_LENGTH) {
            alphanumeric.takeLast(PHONE_COMPARE_LENGTH)
        } else {
            alphanumeric
        }
    } else {
        alphanumeric
    }
}

internal fun normalizeForSearch(value: String): String {
    return value
        .trim()
        .lowercase(Locale.ROOT)
        .filter { it.isLetterOrDigit() }
}

internal fun normalizeDigits(value: String): String {
    return value.filter { it.isDigit() }
}

internal fun numberLikeMatch(candidate: String, queryDigits: String): Boolean {
    if (queryDigits.isBlank()) return false

    val candidateDigits = normalizeDigits(candidate)
    if (candidateDigits.isBlank()) return false

    if (candidateDigits.contains(queryDigits) || queryDigits.contains(candidateDigits)) {
        return true
    }

    val candidateLastDigits = if (candidateDigits.length > PHONE_COMPARE_LENGTH) {
        candidateDigits.takeLast(PHONE_COMPARE_LENGTH)
    } else {
        candidateDigits
    }
    val queryLastDigits = if (queryDigits.length > PHONE_COMPARE_LENGTH) {
        queryDigits.takeLast(PHONE_COMPARE_LENGTH)
    } else {
        queryDigits
    }

    return candidateLastDigits.contains(queryLastDigits) || queryLastDigits.contains(candidateLastDigits)
}

internal fun SmsConversation.matchesSearchQuery(query: String): Boolean {
    val trimmedQuery = query.trim()
    if (trimmedQuery.isBlank()) return true

    val normalizedQuery = normalizeForSearch(trimmedQuery)
    val queryDigits = normalizeDigits(trimmedQuery)

    if (displaySender.contains(trimmedQuery, ignoreCase = true)) {
        return true
    }

    if (normalizedQuery.isNotBlank() && normalizeForSearch(displaySender).contains(normalizedQuery)) {
        return true
    }

    if (numberLikeMatch(displaySender, queryDigits)) {
        return true
    }

    return messages.any { sms ->
        sms.body.contains(trimmedQuery, ignoreCase = true) ||
            (normalizedQuery.isNotBlank() && normalizeForSearch(sms.body).contains(normalizedQuery)) ||
            sms.sender.contains(trimmedQuery, ignoreCase = true) ||
            (normalizedQuery.isNotBlank() && normalizeForSearch(sms.sender).contains(normalizedQuery)) ||
            numberLikeMatch(sms.sender, queryDigits) ||
            numberLikeMatch(sms.body, queryDigits)
    }
}

// Mark conversation messages as read in system provider
internal suspend fun markConversationAsRead(
    context: Context,
    conversation: SmsConversation,
    unreadIds: Set<Long>
) {
    val idsToMark = conversation.messages
        .map { it.id }
        .filter { it in unreadIds }

    if (idsToMark.isEmpty()) return

    withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("read", 1)
            put("seen", 1)
        }

        idsToMark.forEach { smsId ->
            runCatching {
                context.contentResolver.update(
                    Telephony.Sms.CONTENT_URI,
                    values,
                    "_id = ?",
                    arrayOf(smsId.toString())
                )
            }.onFailure { throwable ->
                Log.e(TAG, "Failed to mark SMS as read, id=$smsId", throwable)
            }
        }
    }
}

// Contact name resolution
internal fun resolveContactName(context: Context, phoneNumber: String): String? {
    if (phoneNumber.isBlank()) return null
    val uri = Uri.withAppendedPath(
        ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
        Uri.encode(phoneNumber)
    )
    return runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(0)
            } else {
                null
            }
        }
    }.getOrNull()
}

// Avatar color from sender string
internal fun avatarColorForSender(sender: String): Color {
    val index = (sender.hashCode().and(0x7FFFFFFF)) % AvatarColors.size
    return AvatarColors[index]
}

// Timestamp formatting
@Suppress("DEPRECATION")
private val trLocale = Locale("tr", "TR")

internal fun formatConversationTimestamp(timestampMs: Long): String {
    val now = Calendar.getInstance()
    val msgTime = Calendar.getInstance().apply { timeInMillis = timestampMs }

    val isToday = now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == msgTime.get(Calendar.DAY_OF_YEAR)

    if (isToday) {
        return SimpleDateFormat("HH:mm", trLocale).format(Date(timestampMs))
    }

    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = yesterday.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) &&
        yesterday.get(Calendar.DAY_OF_YEAR) == msgTime.get(Calendar.DAY_OF_YEAR)

    if (isYesterday) {
        return "Dun"
    }

    val isSameYear = now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR)

    return if (isSameYear) {
        SimpleDateFormat("d MMM", trLocale).format(Date(timestampMs))
    } else {
        SimpleDateFormat("d MMM yyyy", trLocale).format(Date(timestampMs))
    }
}

internal fun formatMessageTimestamp(timestampMs: Long): String {
    return SimpleDateFormat("HH:mm", trLocale).format(Date(timestampMs))
}

internal fun formatDateHeader(timestampMs: Long): String {
    val now = Calendar.getInstance()
    val msgTime = Calendar.getInstance().apply { timeInMillis = timestampMs }

    val isToday = now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == msgTime.get(Calendar.DAY_OF_YEAR)
    if (isToday) return "Bugun"

    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = yesterday.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) &&
        yesterday.get(Calendar.DAY_OF_YEAR) == msgTime.get(Calendar.DAY_OF_YEAR)
    if (isYesterday) return "Dun"

    val isSameYear = now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR)
    return if (isSameYear) {
        SimpleDateFormat("d MMMM", trLocale).format(Date(timestampMs))
    } else {
        SimpleDateFormat("d MMMM yyyy", trLocale).format(Date(timestampMs))
    }
}
