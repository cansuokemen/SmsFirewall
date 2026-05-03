package com.example.smsfirewall.data.background

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max

/**
 * Galeriden seçilen arka plan görsellerini uygulamanın özel
 * dizinine kopyalar. Bellek baskısını sınırlamak için en uzun
 * kenar [MAX_DIMENSION] piksele indirilir ve JPEG olarak kaydedilir.
 */
class BackgroundImageRepository(context: Context) {

    private val appContext = context.applicationContext

    fun backgroundsDir(): File {
        val dir = File(appContext.filesDir, DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun fileFor(name: String): File = File(backgroundsDir(), name)

    /**
     * Verilen [uri] altındaki görseli özel dizine kopyalar ve
     * üretilen dosyanın yalnızca adını döner. Hata durumunda null.
     */
    suspend fun importImage(uri: Uri): String? = withContext(Dispatchers.IO) {
        val resolver = appContext.contentResolver
        val bitmap = resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: return@withContext null

        val scaled = downscaleIfNeeded(bitmap)
        if (scaled !== bitmap) bitmap.recycle()

        val fileName = "bg_${UUID.randomUUID().toString().take(12)}.jpg"
        val file = fileFor(fileName)
        try {
            FileOutputStream(file).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
        } finally {
            scaled.recycle()
        }
        fileName
    }

    /**
     * Artık hiçbir tercih tarafından kullanılmayan görsel
     * dosyalarını siler.
     */
    suspend fun pruneUnused(store: BackgroundPreferenceStore) = withContext(Dispatchers.IO) {
        val dir = backgroundsDir()
        dir.listFiles().orEmpty().forEach { file ->
            if (!store.isImageInUse(file.name)) file.delete()
        }
    }

    private fun downscaleIfNeeded(src: Bitmap): Bitmap {
        val longestEdge = max(src.width, src.height)
        if (longestEdge <= MAX_DIMENSION) return src
        val ratio = MAX_DIMENSION.toFloat() / longestEdge
        val newW = (src.width * ratio).toInt().coerceAtLeast(1)
        val newH = (src.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, newW, newH, true)
    }

    companion object {
        private const val DIR_NAME      = "backgrounds"
        private const val MAX_DIMENSION = 1920
        private const val JPEG_QUALITY  = 85
    }
}
