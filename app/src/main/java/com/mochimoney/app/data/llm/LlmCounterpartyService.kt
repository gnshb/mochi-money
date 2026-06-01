package com.mochimoney.app.data.llm

import android.content.Context
import com.mochimoney.app.data.AppPreferences
import com.mochimoney.app.domain.model.LlmBackend
import com.mochimoney.app.domain.model.TransactionDirection

/**
 * Picks the configured backend and runs counterparty extraction through it.
 *
 * Holds a single live [CounterpartyExtractor], rebuilt only when the user changes backend or
 * active model (loading a model is expensive, so we cache it). Not safe for concurrent extraction;
 * the caller serializes the "run on all" sweep.
 */
class LlmCounterpartyService(
    context: Context,
    private val preferences: AppPreferences,
    private val modelManager: LlmModelManager,
) {
    private val appContext = context.applicationContext

    private var extractor: CounterpartyExtractor? = null
    private var extractorKey: String? = null

    /** True when the user enabled the feature and a usable backend is configured. */
    @Synchronized
    fun isReady(): Boolean = preferences.llmCounterpartyEnabled && resolveExtractor() != null

    /** Human-readable reason the feature can't run, or null when it's ready. */
    @Synchronized
    fun unavailableReason(): String? {
        if (!preferences.llmCounterpartyEnabled) return "Turn on AI counterparty detection in Settings."
        return when (preferences.llmBackend) {
            LlmBackend.GeminiNano ->
                if (GeminiNanoCounterpartyExtractor.isSupported()) null
                else "Gemini Nano isn't supported on this device. Pick a downloadable model instead."
            LlmBackend.MediaPipe -> {
                val model = LlmModelCatalog.byId(preferences.activeLlmModelId)
                    ?: return "Choose and download a model in Settings."
                if (modelManager.isInstalled(model)) null
                else "Download \"${model.displayName}\" in Settings first."
            }
        }
    }

    suspend fun extract(
        smsBody: String,
        direction: TransactionDirection,
        currentGuess: String?,
    ): String? {
        val active = synchronized(this) { resolveExtractor() }
            ?: error(unavailableReason() ?: "No on-device model is configured.")
        return active.extract(smsBody, direction, currentGuess)
    }

    /** Drop the cached engine (e.g. after the user changes models or deletes one). */
    @Synchronized
    fun reset() {
        extractor?.close()
        extractor = null
        extractorKey = null
    }

    private fun resolveExtractor(): CounterpartyExtractor? {
        val backend = preferences.llmBackend
        val key = when (backend) {
            LlmBackend.GeminiNano -> "gemini-nano"
            LlmBackend.MediaPipe -> "mediapipe:${preferences.activeLlmModelId}"
        }
        if (extractor != null && extractorKey == key) return extractor

        reset()
        val built: CounterpartyExtractor? = when (backend) {
            LlmBackend.GeminiNano ->
                if (GeminiNanoCounterpartyExtractor.isSupported()) GeminiNanoCounterpartyExtractor(appContext) else null
            LlmBackend.MediaPipe -> {
                val model = LlmModelCatalog.byId(preferences.activeLlmModelId)
                if (model != null && modelManager.isInstalled(model)) {
                    MediaPipeCounterpartyExtractor(appContext, modelManager.modelFile(model))
                } else {
                    null
                }
            }
        }
        extractor = built
        extractorKey = if (built != null) key else null
        return built
    }
}
