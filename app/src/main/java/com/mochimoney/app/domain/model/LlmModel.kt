package com.mochimoney.app.domain.model

/**
 * Where counterparty inference runs.
 *
 * - [MediaPipe] runs a user-downloaded small model file on-device via MediaPipe LLM Inference.
 *   Works on any device but needs the model to be downloaded and stored first.
 * - [GeminiNano] runs the system-managed Gemini Nano model through AICore. No download needed,
 *   but only available on supported devices (Pixel 8+/select Samsung, API 31+).
 */
enum class LlmBackend {
    MediaPipe,
    GeminiNano,
}

/**
 * A downloadable small LLM the user can install for counterparty detection.
 *
 * Note on URLs: MediaPipe expects a `.task`/`.litertlm` bundle. Some hosts (e.g. Hugging Face
 * Gemma repos) gate downloads behind a license click, in which case the in-app download will
 * fail with an auth error and the user must accept the license / use a mirror. The catalog keeps
 * [requiresLicenseAcceptance] so the UI can warn up front.
 */
data class LlmModel(
    val id: String,
    val displayName: String,
    val description: String,
    val approxSizeBytes: Long,
    val downloadUrl: String,
    val fileName: String,
    val license: String,
    val requiresLicenseAcceptance: Boolean = false,
)

/** Runtime install state of a catalog model, surfaced to the UI. */
data class LlmModelStatus(
    val model: LlmModel,
    val isInstalled: Boolean,
    val isActive: Boolean,
    /** 0f..1f while a download is in flight, null otherwise. */
    val downloadProgress: Float? = null,
)
