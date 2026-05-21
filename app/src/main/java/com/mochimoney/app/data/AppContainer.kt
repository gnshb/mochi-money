package com.mochimoney.app.data

import android.content.Context
import androidx.room.Room
import com.mochimoney.app.data.local.MochiMoneyDatabase
import com.mochimoney.app.data.repository.RoomCategoryRepository
import com.mochimoney.app.data.repository.RoomUpiTransactionRepository
import com.mochimoney.app.domain.model.DefaultCategoryIds
import com.mochimoney.app.domain.model.TransactionCategory
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
