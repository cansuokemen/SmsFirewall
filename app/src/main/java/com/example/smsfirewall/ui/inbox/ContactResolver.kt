package com.example.smsfirewall.ui.inbox

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "ContactResolver"

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
