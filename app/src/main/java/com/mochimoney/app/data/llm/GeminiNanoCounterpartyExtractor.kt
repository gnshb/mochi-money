package com.mochimoney.app.data.llm

import android.content.Context
import android.os.Build
import com.google.ai.edge.aicore.GenerativeModel
import com.google.ai.edge.aicore.generationConfig
import com.mochimoney.app.domain.model.TransactionDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Counterparty extractor backed by the system Gemini Nano model through AICore.
 *
 * No model download is needed — the OS manages the weights — but it only works on supported
 * devices (Pixel 8+/select Samsung, API 31+). [isSupported] is a cheap pre-check; actual
 * availability (AICore present, model provisioned) only surfaces when inference is attempted,
 * so callers should treat failures here as "Gemini Nano unavailable on this device".
 */
class GeminiNanoCounterpartyExtractor(context: Context) : CounterpartyExtractor {
    private val appContext = context.applicationContext

    @Volatile
    private var model: GenerativeModel? = null

    private fun model(): GenerativeModel {
        model?.let { return it }
        synchronized(this) {
            model?.let { return it }
            val config = generationConfig {
                context = appContext
                temperature = 0.2f
                topK = 16
                maxOutputTokens = 64
            }
            return GenerativeModel(generationConfig = config).also { model = it }
        }
    }

    override suspend fun extract(
        smsBody: String,
        direction: TransactionDirection,
        currentGuess: String?,
    ): String? = withContext(Dispatchers.Default) {
        check(isSupported()) { "Gemini Nano is not supported on this device." }
        val prompt = CounterpartyPrompt.build(smsBody, direction, currentGuess)
        val response = try {
            model().generateContent(prompt)
        } catch (t: Throwable) {
            throw IllegalStateException(friendlyError(t), t)
        }
        CounterpartyPrompt.clean(response.text)
    }

    /**
     * Probes whether Gemini Nano can actually run a prompt on this device. AICore being installed
     * doesn't guarantee the model/feature is provisioned, so we do a tiny real generation and treat
     * any failure as "unavailable". Safe to call off the main thread.
     */
    suspend fun isAvailable(): Boolean = withContext(Dispatchers.Default) {
        if (!isSupported()) return@withContext false
        runCatching { model().generateContent("Reply with OK.") }.isSuccess
    }

    private fun friendlyError(t: Throwable): String {
        val raw = t.message.orEmpty()
        return if (raw.contains("NOT_AVAILABLE", ignoreCase = true) || raw.contains("feature not found", ignoreCase = true)) {
            "Gemini Nano isn't available on this device. Switch to a downloaded model in Settings."
        } else {
            "Gemini Nano failed. Switch to a downloaded model in Settings."
        }
    }

    override fun close() {
        synchronized(this) {
            model?.close()
            model = null
        }
    }

    companion object {
        /** Cheap pre-check: AICore's GenerativeModel API requires API 31+. */
        fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }
}
