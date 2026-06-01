package com.mochimoney.app.data.llm

import com.mochimoney.app.domain.model.LlmModel

/**
 * Curated list of small, on-device-friendly models for counterparty detection.
 *
 * Counterparty extraction is a short, structured task, so tiny instruction-tuned models are plenty.
 * These are non-gated LiteRT community `.task` bundles, so they download with no Hugging Face token.
 */
object LlmModelCatalog {

    val models: List<LlmModel> = listOf(
        LlmModel(
            id = "qwen2.5-0.5b-q8",
            displayName = "Qwen 2.5 0.5B",
            description = "Best accuracy for its size. Recommended.",
            approxSizeBytes = 521L * 1024 * 1024,
            downloadUrl = "https://huggingface.co/litert-community/Qwen2.5-0.5B-Instruct/resolve/main/Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
            fileName = "Qwen2.5-0.5B-Instruct_q8.task",
            license = "Apache-2.0",
        ),
        LlmModel(
            id = "qwen2.5-1.5b-q8",
            displayName = "Qwen 2.5 1.5B",
            description = "Most accurate. Larger and slower.",
            approxSizeBytes = 1524L * 1024 * 1024,
            downloadUrl = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
            fileName = "Qwen2.5-1.5B-Instruct_q8.task",
            license = "Apache-2.0",
        ),
        LlmModel(
            id = "smollm-135m-q8",
            displayName = "SmolLM 135M",
            description = "Tiny and fastest. Lowest accuracy.",
            approxSizeBytes = 159L * 1024 * 1024,
            downloadUrl = "https://huggingface.co/litert-community/SmolLM-135M-Instruct/resolve/main/SmolLM-135M-Instruct_multi-prefill-seq_q8_ekv1280.task",
            fileName = "SmolLM-135M-Instruct_q8.task",
            license = "Apache-2.0",
        ),
    )

    fun byId(id: String?): LlmModel? = id?.let { wanted -> models.firstOrNull { it.id == wanted } }
}
