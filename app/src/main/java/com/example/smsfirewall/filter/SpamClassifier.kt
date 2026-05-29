package com.example.smsfirewall.filter

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpamClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private companion object {
        const val TAG = "SpamClassifier"
        const val MAX_LEN = 128
        const val SPAM_THRESHOLD = 0.5f
        const val UNK_ID = 1
        const val MODEL_FILE = "sms_spam_model.tflite"
        const val VOCAB_FILE = "sms_spam_vocab.json"
    }

    private val vocab: Map<String, Int> by lazy {
        runCatching { loadVocab() }.onFailure { Log.e(TAG, "Vocab yüklenemedi", it) }.getOrDefault(emptyMap())
    }

    private val interpreter: Interpreter? by lazy {
        runCatching { Interpreter(loadModelFile()) }.onFailure { Log.e(TAG, "Model yüklenemedi", it) }.getOrNull()
    }

    fun isSpam(text: String): Boolean = spamScore(text) >= SPAM_THRESHOLD

    fun spamScore(text: String): Float {
        val interp = interpreter ?: return 0f
        return try {
            val tokens  = preprocess(text)
            val input   = Array(1) { IntArray(MAX_LEN) }
            tokens.take(MAX_LEN).forEachIndexed { i, id -> input[0][i] = id }
            val output  = Array(1) { FloatArray(2) }
            interp.run(input, output)
            output[0][1]
        } catch (e: Exception) {
            Log.e(TAG, "İnference hatası", e)
            0f
        }
    }

    private fun preprocess(text: String): List<Int> {
        var t = text.lowercase(Locale.ROOT)
        t = t.replace(Regex("""https?://\S+|www\.\S+"""), "URL")
        t = t.replace(Regex("""\d+"""), "NUMBER")
        t = t.replace(Regex("""[!"#$%&'()*+,\-./:;<=>?@\[\\\]^_`{|}~]"""), " ")
        return t.split(Regex("""\s+"""))
            .filter { it.isNotBlank() }
            .map { vocab[it] ?: UNK_ID }
    }

    private fun loadVocab(): Map<String, Int> {
        val json  = context.assets.open(VOCAB_FILE).bufferedReader().readText()
        val array = JSONArray(json)
        val map   = HashMap<String, Int>(array.length())
        for (i in 0 until array.length()) map[array.getString(i)] = i
        return map
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fd = context.assets.openFd(MODEL_FILE)
        return FileInputStream(fd.fileDescriptor).channel.map(
            FileChannel.MapMode.READ_ONLY,
            fd.startOffset,
            fd.declaredLength
        )
    }
}
