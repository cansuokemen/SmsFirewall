package com.example.smsfirewall.data.background

/**
 * Bir ekranın arka planını tanımlayan dört temel kaynaktan biri:
 * varsayılan tema rengi, hazır gradient, hazır vector desen,
 * düz renk veya kullanıcının galerisinden seçtiği görsel.
 */
sealed class BackgroundSpec {
    object Default : BackgroundSpec()
    data class Gradient(val presetId: String) : BackgroundSpec()
    data class Pattern(val patternId: String) : BackgroundSpec()
    data class SolidColor(val argb: Long) : BackgroundSpec()
    data class CustomImage(val fileName: String) : BackgroundSpec()

    fun serialize(): String = when (this) {
        Default            -> TYPE_DEFAULT
        is Gradient        -> "$TYPE_GRADIENT$SEPARATOR$presetId"
        is Pattern         -> "$TYPE_PATTERN$SEPARATOR$patternId"
        is SolidColor      -> "$TYPE_SOLID$SEPARATOR$argb"
        is CustomImage     -> "$TYPE_IMAGE$SEPARATOR$fileName"
    }

    companion object {
        private const val SEPARATOR = ":"
        private const val TYPE_DEFAULT  = "default"
        private const val TYPE_GRADIENT = "gradient"
        private const val TYPE_PATTERN  = "pattern"
        private const val TYPE_SOLID    = "solid"
        private const val TYPE_IMAGE    = "image"

        fun deserialize(raw: String?): BackgroundSpec {
            if (raw.isNullOrBlank()) return Default
            val parts = raw.split(SEPARATOR, limit = 2)
            val type = parts[0]
            val payload = parts.getOrNull(1).orEmpty()
            return when (type) {
                TYPE_DEFAULT  -> Default
                TYPE_GRADIENT -> Gradient(payload)
                TYPE_PATTERN  -> Pattern(payload)
                TYPE_SOLID    -> payload.toLongOrNull()?.let { SolidColor(it) } ?: Default
                TYPE_IMAGE    -> if (payload.isNotBlank()) CustomImage(payload) else Default
                else          -> Default
            }
        }
    }
}
