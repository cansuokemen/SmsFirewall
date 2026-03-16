# Add project specific ProGuard rules here.

# ─── Stack trace deobfuscation ───────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ─── Room Database ───────────────────────────────────────────────
# Room entities and DAOs
-keep class com.example.smsfirewall.data.local.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# ─── Kotlin Serialization / Coroutines ───────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ─── Compose ─────────────────────────────────────────────────────
-dontwarn androidx.compose.**

# ─── SmsFilterEngine (reflection-free, but keep for crash reporting) ─
-keep class com.example.smsfirewall.filter.FilterDecision { *; }
-keep class com.example.smsfirewall.filter.SmsStatus { *; }

# ─── BroadcastReceivers (referenced from manifest) ──────────────
-keep class com.example.smsfirewall.SmsReceiver { *; }
-keep class com.example.smsfirewall.MmsReceiver { *; }
-keep class com.example.smsfirewall.RespondViaMessageActivity { *; }
