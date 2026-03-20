package com.example.smsfirewall.ui.inbox

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.example.smsfirewall.R
import com.example.smsfirewall.ui.theme.AvatarColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Suppress("DEPRECATION") // Locale constructor required for Turkish locale support
private val trLocale = Locale("tr", "TR")

internal fun avatarColorForSender(sender: String): Color {
    val index = (sender.hashCode().and(0x7FFFFFFF)) % AvatarColors.size
    return AvatarColors[index]
}

internal fun formatConversationTimestamp(context: Context, timestampMs: Long): String {
    val now = Calendar.getInstance()
    val msgTime = Calendar.getInstance().apply { timeInMillis = timestampMs }

    if (isSameDay(now, msgTime)) {
        return SimpleDateFormat("HH:mm", trLocale).format(Date(timestampMs))
    }

    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    if (isSameDay(yesterday, msgTime)) {
        return context.getString(R.string.yesterday)
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

internal fun formatDateHeader(context: Context, timestampMs: Long): String {
    val now = Calendar.getInstance()
    val msgTime = Calendar.getInstance().apply { timeInMillis = timestampMs }

    if (isSameDay(now, msgTime)) return context.getString(R.string.today)

    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    if (isSameDay(yesterday, msgTime)) return context.getString(R.string.yesterday)

    val isSameYear = now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR)
    return if (isSameYear) {
        SimpleDateFormat("d MMMM", trLocale).format(Date(timestampMs))
    } else {
        SimpleDateFormat("d MMMM yyyy", trLocale).format(Date(timestampMs))
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
        cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
