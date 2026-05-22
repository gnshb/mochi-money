package com.mochimoney.app.data

import android.content.Context
import androidx.room.Room
import com.mochimoney.app.data.local.MochiMoneyDatabase
import com.mochimoney.app.data.repository.RoomCategoryRepository
import com.mochimoney.app.data.repository.RoomUpiTransactionRepository
import com.mochimoney.app.data.splitwise.SplitwiseApiClient
import com.mochimoney.app.data.splitwise.SplitwiseGroup
import com.mochimoney.app.data.splitwise.SplitwiseUser
import com.mochimoney.app.domain.model.DefaultCategoryIds
import com.mochimoney.app.domain.model.DedupeKeyGenerator
import com.mochimoney.app.domain.model.TransactionCategory
import com.mochimoney.app.domain.model.UpiTransaction
import com.mochimoney.app.domain.repository.CategoryRepository
import com.mochimoney.app.domain.repository.UpiTransactionRepository
import com.mochimoney.app.sms.SmsInboxReader
import com.mochimoney.app.sms.SmsParseResult
import com.mochimoney.app.sms.UpiSmsParser

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
        }

        val existingKeys = transactionRepository.getAll().map { it.dedupeKey }.toSet()
        val newCount = transactions.count { it.dedupeKey !in existingKeys }
        transactionRepository.upsertAll(transactions)
        return newCount
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
        val transactions = transactionRepository.getAll()
        var changed = 0
        transactions.forEach { transaction ->
            val suggested = categoryRepository.suggestFor(transaction)
            if (
                suggested.id != DefaultCategoryIds.UNCATEGORIZED &&
                suggested.id != transaction.categoryId &&
                transaction.categoryId in setOf(DefaultCategoryIds.UNCATEGORIZED, DefaultCategoryIds.OTHER)
            ) {
                transactionRepository.updateCategory(transaction.id, suggested.id)
                changed += 1
            }
        }
        return changed
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
