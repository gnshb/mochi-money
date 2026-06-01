package com.mochimoney.app.data.llm

import android.content.Context
import com.mochimoney.app.domain.model.LlmModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

/**
 * Manages the on-device model files the user downloads for [com.mochimoney.app.domain.model.LlmBackend.MediaPipe].
 *
 * Files live in the app's private storage so they never need extra permissions and are removed
 * when the app is uninstalled. Downloads stream to a `.part` file and atomically rename on success,
 * so a cancelled or failed download never looks like a complete model.
 */
class LlmModelManager(context: Context) {
    private val appContext = context.applicationContext
    private val modelsDir: File by lazy {
        File(appContext.filesDir, "llm_models").apply { mkdirs() }
    }

    fun modelFile(model: LlmModel): File = File(modelsDir, model.fileName)

    fun isInstalled(model: LlmModel): Boolean = modelFile(model).let { it.exists() && it.length() > 0L }

    fun installedModelIds(): Set<String> =
        LlmModelCatalog.models.filter(::isInstalled).map { it.id }.toSet()

    fun delete(model: LlmModel): Boolean {
        val file = modelFile(model)
        val part = partFile(model)
        if (part.exists()) part.delete()
        return if (file.exists()) file.delete() else false
    }

    /**
     * Clears MediaPipe's working cache. The runtime extracts the `.task` bundle into the app cache
     * on load (roughly doubling on-disk usage), so wiping it after removing a model reclaims that space.
     */
    fun clearInferenceCache() {
        appContext.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
    }

    /**
     * Downloads [model], reporting fractional progress (0f..1f) as bytes arrive. Cancelling the
     * coroutine aborts the download and removes the partial file. Returns the final model file.
     */
    suspend fun download(
        model: LlmModel,
        authToken: String? = null,
        onProgress: (Float) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val target = modelFile(model)
        if (isInstalled(model)) return@withContext target

        val part = partFile(model)
        if (part.exists()) part.delete()

        // Follow redirects manually: Hugging Face's resolve endpoint 302s to a pre-signed CDN URL,
        // and HttpURLConnection would otherwise forward our Authorization header to that CDN, which
        // rejects it. So we only attach the token while we're still on huggingface.co.
        val connection = openWithRedirects(model.downloadUrl, authToken)
        var total: Long
        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                val hfError = connection.getHeaderField("X-Error-Message")
                val repoPage = model.downloadUrl.substringBefore("/resolve/")
                val reason = when (code) {
                    401, 403 -> buildString {
                        append(hfError ?: "Access denied")
                        append(". This model is license-gated: open ")
                        append(repoPage)
                        append(" , sign in, click \"Agree and access repository\", then create a read token on that same account and paste it in Settings.")
                    }
                    404 -> "File not found at ${model.downloadUrl}"
                    else -> "HTTP $code ${connection.responseMessage.orEmpty()}".trim()
                }
                error(reason)
            }
            // Guard against silently downloading an HTML/JSON error page instead of the model.
            val contentType = connection.contentType.orEmpty()
            if (contentType.startsWith("text/") || contentType.contains("json", ignoreCase = true)) {
                error("Server returned a page, not a model file ($contentType). Check the token and license access.")
            }
            total = connection.contentLengthLong
            var downloaded = 0L
            connection.inputStream.use { input ->
                part.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var lastReported = -1
                    val totalForPct = total.takeIf { it > 0L } ?: model.approxSizeBytes
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        val pct = ((downloaded.toFloat() / totalForPct).coerceIn(0f, 1f) * 100).toInt()
                        if (pct != lastReported) {
                            lastReported = pct
                            onProgress(pct / 100f)
                        }
                    }
                }
            }
            // Detect a truncated transfer: server promised more bytes than we received.
            if (total > 0L && downloaded < total) {
                error("Download was incomplete (${downloaded / (1024 * 1024)} MB of ${total / (1024 * 1024)} MB). Check your connection and retry.")
            }
        } catch (t: Throwable) {
            part.delete()
            throw t
        } finally {
            connection.disconnect()
        }

        if (!part.renameTo(target)) {
            part.delete()
            error("Could not finalize the model file.")
        }
        onProgress(1f)
        target
    }

    private fun openWithRedirects(startUrl: String, authToken: String?): HttpURLConnection {
        var current = startUrl
        repeat(6) {
            val onHuggingFace = current.contains("huggingface.co")
            val conn = (URL(current).openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout = 30_000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "MochiMoney-Android")
                if (onHuggingFace) {
                    authToken?.takeIf { it.isNotBlank() }?.let {
                        setRequestProperty("Authorization", "Bearer $it")
                    }
                }
            }
            val code = conn.responseCode
            if (code in 300..399) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location.isNullOrBlank()) error("Redirect with no Location header.")
                current = URL(URL(current), location).toString()
                return@repeat
            }
            return conn
        }
        error("Too many redirects while downloading the model.")
    }

    private fun partFile(model: LlmModel): File = File(modelsDir, model.fileName + ".part")
}
