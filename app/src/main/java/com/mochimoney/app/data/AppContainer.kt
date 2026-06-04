package com.mochimoney.app.data

import android.content.Context
import androidx.room.Room
import com.mochimoney.app.data.local.MochiMoneyDatabase
import com.mochimoney.app.data.repository.CategoryKeywordMatcher
import com.mochimoney.app.data.repository.ExistingTransactionCategoryRefresher
import com.mochimoney.app.data.repository.ParsedSmsTransactionImporter
import com.mochimoney.app.data.repository.RoomCategoryRepository
import com.mochimoney.app.data.repository.RoomUpiTransactionRepository
import com.mochimoney.app.data.llm.GeminiNanoCounterpartyExtractor
import com.mochimoney.app.data.llm.LlmCounterpartyService
import com.mochimoney.app.data.llm.LlmModelCatalog
import com.mochimoney.app.data.llm.LlmModelManager
import com.mochimoney.app.data.llm.titleCaseCounterparty
import com.mochimoney.app.data.splitwise.SplitwiseApiClient
import com.mochimoney.app.data.splitwise.SplitwiseGroup
import com.mochimoney.app.data.splitwise.SplitwiseUser
import com.mochimoney.app.domain.model.DefaultCategoryIds
import com.mochimoney.app.domain.model.DedupeKeyGenerator
import com.mochimoney.app.domain.model.LlmBackend
import com.mochimoney.app.domain.model.LlmModel
import com.mochimoney.app.domain.model.LlmModelStatus
import com.mochimoney.app.domain.model.TransactionCategory
import com.mochimoney.app.domain.model.TransactionDirection
import com.mochimoney.app.domain.model.UpiTransaction
import com.mochimoney.app.domain.repository.CategoryRepository
import com.mochimoney.app.domain.repository.UpiTransactionRepository
import com.mochimoney.app.sms.SmsInboxReader
import com.mochimoney.app.sms.SmsParseResult
import com.mochimoney.app.sms.UpiSmsParser
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val preferences: AppPreferences by lazy {
        AppPreferences(appContext)
    }

    val database: MochiMoneyDatabase by lazy {
        Room.databaseBuilder(
            appContext,
            MochiMoneyDatabase::class.java,
            "mochi_money.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    val categoryRepository: CategoryRepository by lazy {
        RoomCategoryRepository(database.categoryDao(), database.categoryRuleDao()).also {
            it.seedDefaults()
        }
    }

    val transactionRepository: UpiTransactionRepository by lazy {
        RoomUpiTransactionRepository(database.upiTransactionDao())
    }

    val smsInboxReader: SmsInboxReader by lazy {
        SmsInboxReader(appContext)
    }

    private val splitwiseApiClient: SplitwiseApiClient by lazy {
        SplitwiseApiClient()
    }

    val llmModelManager: LlmModelManager by lazy {
        LlmModelManager(appContext)
    }

    val llmCounterpartyService: LlmCounterpartyService by lazy {
        LlmCounterpartyService(appContext, preferences, llmModelManager)
    }

    fun refreshFromSmsInbox(limit: Int = 500): Int {
        val pattern = preferences.senderPattern
            .trim()
            .takeIf { it.isNotBlank() }
            ?.let { runCatching { Regex(it, RegexOption.IGNORE_CASE) }.getOrNull() }

        val rawMessages = smsInboxReader.readInbox(limit)
        val filtered = if (pattern != null) {
            rawMessages.filter { pattern.containsMatchIn(it.sender.orEmpty()) }
        } else {
            rawMessages
        }

        val parser = UpiSmsParser(
            categoryRepository = categoryRepository,
            trustSender = pattern != null,
        )
        val transactions = filtered.mapNotNull { rawSms ->
            (parser.parse(rawSms) as? SmsParseResult.Parsed)?.transaction
        }.map { it.copy(counterparty = applyCounterpartyAlias(it.counterparty)) }

        return ParsedSmsTransactionImporter.import(
            transactions = transactions,
            repository = transactionRepository,
        )
    }

    fun refreshFromSampleSms(body: String): Int {
        val parser = UpiSmsParser(categoryRepository = categoryRepository)
        val transaction = (parser.parse(com.mochimoney.app.sms.RawSms(body = body, sender = "sample.sms")) as? SmsParseResult.Parsed)
            ?.transaction
            ?: return 0

        transactionRepository.upsert(transaction)
        return 1
    }

    fun connectSplitwise(apiKey: String): SplitwiseConnection {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) error("Splitwise API key is required.")

        val user = splitwiseApiClient.getCurrentUser(cleanKey)
        val groups = splitwiseApiClient.getGroups(cleanKey)
        preferences.splitwiseApiKey = cleanKey
        preferences.splitwiseEnabled = true
        preferences.splitwiseCurrentUserId = user.id
        preferences.splitwiseCurrentUserName = user.name
        preferences.splitwiseGroups = groups

        val groupIds = groups.map { it.id }.toSet()
        preferences.splitwiseSelectedGroupIds = preferences.splitwiseSelectedGroupIds.intersect(groupIds)

        return SplitwiseConnection(user = user, groups = groups)
    }

    fun setSplitwiseEnabled(enabled: Boolean) {
        preferences.splitwiseEnabled = enabled
    }

    fun setSplitwiseGroupSelected(groupId: Long, selected: Boolean) {
        val updated = if (selected) {
            preferences.splitwiseSelectedGroupIds + groupId
        } else {
            preferences.splitwiseSelectedGroupIds - groupId
        }
        preferences.splitwiseSelectedGroupIds = updated
    }

    fun refreshFromSplitwise(): Int {
        if (!preferences.splitwiseEnabled) return 0
        val apiKey = preferences.splitwiseApiKey.takeIf { it.isNotBlank() } ?: return 0
        val currentUserId = preferences.splitwiseCurrentUserId.takeIf { it > 0L } ?: return 0
        val groupsById = preferences.splitwiseGroups.associateBy { it.id }
        val selectedGroups = preferences.splitwiseSelectedGroupIds.mapNotNull(groupsById::get)
        if (selectedGroups.isEmpty()) return 0

        val existingKeys = transactionRepository.getAll().map { it.dedupeKey }.toSet()
        val transactions = selectedGroups
            .flatMap { group ->
                splitwiseApiClient.getExpenseShares(
                    apiKey = apiKey,
                    currentUserId = currentUserId,
                    group = group,
                )
            }
            .map { share ->
                UpiTransaction(
                    dedupeKey = DedupeKeyGenerator.sha256("splitwise|${share.expenseId}|$currentUserId").take(32),
                    direction = share.direction,
                    amountPaise = share.amountPaise,
                    currency = share.currency,
                    occurredOn = share.date,
                    counterparty = share.groupName,
                    referenceNumber = "splitwise:${share.expenseId}",
                    accountSuffix = null,
                    sender = "Splitwise",
                    smsBodyHash = DedupeKeyGenerator.sha256("splitwise:${share.expenseId}"),
                    smsBody = "${share.description}\nGroup: ${share.groupName}",
                    smsReceivedAtMillis = null,
                    categoryId = DefaultCategoryIds.SPLITWISE,
                )
            }

        val newCount = transactions.count { it.dedupeKey !in existingKeys }
        transactionRepository.upsertAll(transactions)
        return newCount
    }

    /**
     * Stores a user-entered transaction (no SMS backing). When the caller doesn't pick a category,
     * outgoing payments fall back to keyword matching and incoming ones to Income.
     */
    fun addManualTransaction(
        direction: TransactionDirection,
        amountPaise: Long,
        occurredOn: java.time.LocalDate,
        counterparty: String?,
        note: String?,
        categoryId: String?,
    ): UpiTransaction {
        val cleanCounterparty = counterparty?.trim()?.takeIf { it.isNotBlank() }
            ?.let { titleCaseCounterparty(it) }
        val cleanNote = note?.trim()?.takeIf { it.isNotBlank() }
        val createdAt = System.currentTimeMillis()
        // A per-entry nonce keeps two otherwise-identical manual entries from colliding on the dedupe key.
        val nonce = DedupeKeyGenerator
            .sha256("manual|$createdAt|${cleanCounterparty.orEmpty()}|$amountPaise")
            .take(16)
        val reference = "manual:$nonce"
        val draft = UpiTransaction(
            dedupeKey = DedupeKeyGenerator.generate(
                direction = direction,
                amountPaise = amountPaise,
                occurredOn = occurredOn,
                referenceNumber = reference,
                accountSuffix = null,
                counterparty = cleanCounterparty,
            ),
            direction = direction,
            amountPaise = amountPaise,
            occurredOn = occurredOn,
            counterparty = cleanCounterparty,
            referenceNumber = reference,
            accountSuffix = null,
            sender = "Manual entry",
            smsBodyHash = DedupeKeyGenerator.sha256(reference),
            smsBody = cleanNote,
            smsReceivedAtMillis = createdAt,
            categoryId = DefaultCategoryIds.UNCATEGORIZED,
            createdAtMillis = createdAt,
        )
        val resolvedCategory = categoryId?.takeIf { it.isNotBlank() }
            ?: CategoryKeywordMatcher.matchByKeywords(draft, categoryRepository.getCategories())?.id
            ?: DefaultCategoryIds.UNCATEGORIZED
        return transactionRepository.upsert(draft.copy(categoryId = resolvedCategory))
    }

    fun applyCategoryRule(transactionId: Long, counterparty: String?, categoryId: String) {
        transactionRepository.updateCategory(transactionId, categoryId)
        val label = counterparty?.takeIf { it.isNotBlank() } ?: return
        categoryRepository.assignCounterpartyCategory(label, categoryId)
        transactionRepository.updateCategoryForCounterparty(label, categoryId)
        addKeywordToCategory(categoryId, label)
    }

    private fun addKeywordToCategory(categoryId: String, counterparty: String) {
        val normalized = counterparty.trim().replace(Regex("\\s+"), " ")
        if (normalized.isEmpty()) return
        val category = categoryRepository.getCategories().firstOrNull { it.id == categoryId } ?: return
        val merged = category.keywords + normalized
        if (merged == category.keywords) return
        categoryRepository.upsertCategory(category.copy(keywords = merged))
    }

    fun upsertCategory(name: String, keywordsCsv: String, categoryId: String? = null) {
        val cleanName = name.trim().takeIf { it.isNotBlank() } ?: return
        val id = categoryId ?: cleanName
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "custom_${System.currentTimeMillis()}" }

        val color = categoryRepository
            .getCategories()
            .firstOrNull { it.id == id }
            ?.colorHex
            ?: "#64748B"

        categoryRepository.upsertCategory(
            TransactionCategory(
                id = id,
                name = cleanName,
                colorHex = color,
                keywords = keywordsCsv.toKeywordSet(),
            ),
        )
    }

    fun applyKeywordRulesToExisting(): Int {
        return ExistingTransactionCategoryRefresher.refresh(
            categoryRepository = categoryRepository,
            transactionRepository = transactionRepository,
        )
    }

    // --- On-device LLM counterparty detection -------------------------------------------------

    fun geminiNanoSupported(): Boolean = GeminiNanoCounterpartyExtractor.isSupported()

    /** Why AI detection can't run right now, or null when it's ready. */
    fun llmUnavailableReason(): String? = llmCounterpartyService.unavailableReason()

    /** Install state of every catalog model (download progress is tracked by the UI). */
    fun llmModelStatuses(): List<LlmModelStatus> {
        val activeId = preferences.activeLlmModelId
        val backend = preferences.llmBackend
        return LlmModelCatalog.models.map { model ->
            val installed = llmModelManager.isInstalled(model)
            LlmModelStatus(
                model = model,
                isInstalled = installed,
                isActive = installed && backend == LlmBackend.MediaPipe && activeId == model.id,
            )
        }
    }

    fun setLlmEnabled(enabled: Boolean) {
        preferences.llmCounterpartyEnabled = enabled
        llmCounterpartyService.reset()
    }

    fun setLlmBackend(backend: LlmBackend) {
        preferences.llmBackend = backend
        llmCounterpartyService.reset()
    }

    fun setActiveLlmModel(modelId: String) {
        preferences.activeLlmModelId = modelId
        preferences.llmBackend = LlmBackend.MediaPipe
        llmCounterpartyService.reset()
    }

    fun setHuggingFaceToken(token: String) {
        preferences.huggingFaceToken = token
    }

    /** Real availability check for Gemini Nano (provisioned + can run a prompt), off the main thread. */
    suspend fun checkGeminiNano(): Boolean {
        val probe = GeminiNanoCounterpartyExtractor(appContext)
        return try {
            probe.isAvailable()
        } finally {
            probe.close()
        }
    }

    suspend fun downloadLlmModel(model: LlmModel, onProgress: (Float) -> Unit) {
        llmModelManager.download(model, preferences.huggingFaceToken.takeIf { it.isNotBlank() }, onProgress)
        // First model installed becomes the active one for a frictionless first run.
        if (preferences.activeLlmModelId == null) {
            preferences.activeLlmModelId = model.id
            preferences.llmBackend = LlmBackend.MediaPipe
        }
        llmCounterpartyService.reset()
    }

    fun deleteLlmModel(model: LlmModel) {
        llmModelManager.delete(model)
        if (preferences.activeLlmModelId == model.id) {
            preferences.activeLlmModelId = llmModelManager.installedModelIds().firstOrNull()
        }
        llmCounterpartyService.reset()
        // Release the model engine first (reset above), then reclaim the extracted-model cache.
        llmModelManager.clearInferenceCache()
    }

    /** Maps a detected counterparty through the user's learned corrections. */
    private fun applyCounterpartyAlias(name: String?): String? {
        val clean = name?.takeIf { it.isNotBlank() } ?: return name
        return preferences.counterpartyAliases[clean.trim().lowercase()] ?: clean
    }

    /**
     * Manually set a transaction's counterparty. Applies the new name to every other transaction
     * that currently shares the old name and remembers the correction for future imports/AI runs.
     */
    fun renameCounterparty(transactionId: Long, rawName: String): Boolean {
        val newName = titleCaseCounterparty(rawName).takeIf { it.isNotBlank() } ?: return false
        val txn = transactionRepository.findById(transactionId) ?: return false
        val old = txn.counterparty?.trim()
        transactionRepository.updateParsedFields(txn.copy(counterparty = newName))
        if (!old.isNullOrBlank() && !old.equals(newName, ignoreCase = true)) {
            transactionRepository.renameCounterparty(old, newName)
            preferences.counterpartyAliases = preferences.counterpartyAliases + (old.lowercase() to newName)
        }
        return true
    }

    /** Changes a transaction's amount (e.g. correcting a manual entry). Returns false if not found. */
    fun updateTransactionAmount(transactionId: Long, amountPaise: Long): Boolean {
        if (amountPaise <= 0L) return false
        val txn = transactionRepository.findById(transactionId) ?: return false
        if (txn.amountPaise == amountPaise) return true
        transactionRepository.updateParsedFields(txn.copy(amountPaise = amountPaise))
        return true
    }

    /** Permanently removes a transaction. */
    fun deleteTransaction(transactionId: Long) {
        transactionRepository.delete(transactionId)
    }

    /** Runs the model on one transaction's SMS, persisting an improved counterparty if found. */
    suspend fun inferCounterparty(transactionId: Long): CounterpartyInferenceResult {
        val txn = transactionRepository.findById(transactionId)
            ?: error("Transaction not found.")
        val body = txn.smsBody?.takeIf { it.isNotBlank() }
            ?: return CounterpartyInferenceResult(transactionId, txn.counterparty, changed = false)
        val detected = applyCounterpartyAlias(llmCounterpartyService.extract(body, txn.direction, txn.counterparty))
        val changed = !detected.isNullOrBlank() && detected != txn.counterparty
        if (changed) transactionRepository.updateParsedFields(txn.copy(counterparty = detected))
        return CounterpartyInferenceResult(
            transactionId = transactionId,
            counterparty = if (changed) detected else txn.counterparty,
            changed = changed,
        )
    }

    /** Sequentially runs the model over every SMS-backed transaction. Returns how many changed. */
    suspend fun inferAllCounterparties(onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }): Int {
        val candidates = transactionRepository.getAll().filter { !it.smsBody.isNullOrBlank() }
        var changedCount = 0
        candidates.forEachIndexed { index, txn ->
            coroutineContext.ensureActive()
            val detected = runCatching {
                applyCounterpartyAlias(llmCounterpartyService.extract(txn.smsBody!!, txn.direction, txn.counterparty))
            }.getOrNull()
            if (!detected.isNullOrBlank() && detected != txn.counterparty) {
                transactionRepository.updateParsedFields(txn.copy(counterparty = detected))
                changedCount++
            }
            onProgress(index + 1, candidates.size)
        }
        return changedCount
    }

    private fun String.toKeywordSet(): Set<String> =
        split("\n", ",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()

}

data class SplitwiseConnection(
    val user: SplitwiseUser,
    val groups: List<SplitwiseGroup>,
)

data class CounterpartyInferenceResult(
    val transactionId: Long,
    val counterparty: String?,
    val changed: Boolean,
)
