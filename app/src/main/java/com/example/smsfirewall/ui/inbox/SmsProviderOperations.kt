package com.example.smsfirewall.ui.inbox

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.smsfirewall.data.SmsRepository
import com.example.smsfirewall.data.local.SmsEntity
import com.example.smsfirewall.filter.SmsStatus
import com.example.smsfirewall.util.normalizeSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "SmsProviderOperations"

internal fun querySystemMessagesWithReadStatus(context: Context): SystemMessagesResult {
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

/**
 * Engellenen gönderenden gelen tüm mesajları sistem SMS sağlayıcısından siler.
 * normalizeSender ile karşılaştırma yaparak farklı formatlı numaraları da eşleştirir.
 */
internal suspend fun deleteSystemSmsBySender(context: Context, sender: String) {
    val normalizedTarget = normalizeSender(sender)
    if (normalizedTarget.isBlank()) return

    withContext(Dispatchers.IO) {
        runCatching {
            val projection = arrayOf("_id", "address")
            val idsToDelete = mutableListOf<Long>()

            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow("_id")
                val addressColumn = cursor.getColumnIndexOrThrow("address")

                while (cursor.moveToNext()) {
                    val address = cursor.getString(addressColumn).orEmpty()
                    if (normalizeSender(address) == normalizedTarget) {
                        idsToDelete.add(cursor.getLong(idColumn))
                    }
                }
            }

            idsToDelete.forEach { smsId ->
                context.contentResolver.delete(
                    Telephony.Sms.CONTENT_URI,
                    "_id = ?",
                    arrayOf(smsId.toString())
                )
            }
        }.onFailure { throwable ->
            Log.e(TAG, "Failed to delete system SMS by sender: $sender", throwable)
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

internal suspend fun restoreStoredSmsToSystemInbox(
    context: Context,
    repository: SmsRepository,
    sms: SmsEntity
): Boolean {
    val insertedIntoInbox = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, sms.sender)
            put(Telephony.Sms.BODY, sms.body)
            put(Telephony.Sms.DATE, sms.receivedAt)
            put(Telephony.Sms.READ, 1)
            put(Telephony.Sms.SEEN, 1)
        }

        runCatching {
            context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values) != null
        }.onFailure { throwable ->
            Log.e(TAG, "Failed to restore stored SMS into inbox", throwable)
        }.getOrDefault(false)
    }

    if (!insertedIntoInbox) return false

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
