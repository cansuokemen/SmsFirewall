package com.example.smsfirewall.util

import java.util.Locale

const val PHONE_COMPARE_LENGTH = 10

fun normalizeSender(sender: String): String {
    val alphanumeric = sender
        .trim()
        .lowercase(Locale.ROOT)
        .filter { it.isLetterOrDigit() }
    if (alphanumeric.isBlank()) return ""

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
