package com.mochimoney.app.data.llm

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import com.mochimoney.app.domain.model.TransactionDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Counterparty extractor backed by a user-downloaded model file run through MediaPipe LLM Inference.
 *
 * The [LlmInference] engine is created lazily on first use (loading is expensive) and reused across
 * transactions. It is NOT thread-safe, so callers must serialize requests — the orchestrating
 * service runs the "all transactions" sweep sequentially.
 */
class MediaPipeCounterpartyExtractor(
    context: Context,
    private val modelFile: File,
) : CounterpartyExtractor {
    private val appContext = context.applicationContext

    @Volatile
    private var engine: LlmInference? = null

    private fun engine(): LlmInference {
        engine?.let { return it }
        synchronized(this) {
            engine?.let { return it }
            require(modelFile.exists() && modelFile.length() > 0L) {
                "Model file is missing. Re-download the model in Settings."
            }
            // Force the CPU backend: the GPU LLM backend native-crashes on many Android GPUs
            // (including Tensor/Mali), which would take down the whole app uncatchably.
            val options = LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(1024)
                .setMaxTopK(40)
                .setPreferredBackend(LlmInference.Backend.CPU)
                .build()
            return LlmInference.createFromOptions(appContext, options).also { engine = it }
        }
    }

    override suspend fun extract(
        smsBody: String,
        direction: TransactionDirection,
        currentGuess: String?,
    ): String? = withContext(Dispatchers.Default) {
        // The litert-community Qwen/SmolLM .task bundles are instruction-tuned with the ChatML
        // template. Feeding raw text makes them ramble (e.g. echoing "UPI"); wrapping the prompt in
        // ChatML turn tokens makes them actually follow the instruction.
        val core = CounterpartyPrompt.build(smsBody, direction, currentGuess)
        val prompt = buildString {
            append("<|im_start|>system\nYou extract a single counterparty name from a bank SMS and reply with only that name.<|im_end|>\n")
            append("<|im_start|>user\n").append(core).append("<|im_end|>\n")
            append("<|im_start|>assistant\n")
        }
        val response = engine().generateResponse(prompt)
        CounterpartyPrompt.clean(response)
    }

    override fun close() {
        synchronized(this) {
            engine?.close()
            engine = null
        }
    }
}
