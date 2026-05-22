package com.mochimoney.app.data.repository

import com.mochimoney.app.data.local.CategoryDao
import com.mochimoney.app.data.local.CategoryRuleDao
import com.mochimoney.app.data.local.CategoryRuleEntity
import com.mochimoney.app.data.local.toDomain
import com.mochimoney.app.data.local.toEntity
import com.mochimoney.app.data.local.toCounterpartyKey
import com.mochimoney.app.domain.model.DefaultCategories
import com.mochimoney.app.domain.model.DefaultCategoryIds
import com.mochimoney.app.domain.model.TransactionCategory
import com.mochimoney.app.domain.model.UpiTransaction
import com.mochimoney.app.domain.repository.CategoryRepository

class RoomCategoryRepository(
    private val dao: CategoryDao,
    private val ruleDao: CategoryRuleDao,
    private val fallback: CategoryRepository = DefaultCategoryRepository()
) : CategoryRepository {
    fun seedDefaults() {
        val existingCategories = dao.getAll()
        val existingIds = existingCategories.map { it.id }.toSet()
        val missingDefaults = DefaultCategories.all
            .filterNot { it.id in existingIds }
            .map { it.toEntity() }
        if (missingDefaults.isNotEmpty()) {
            dao.upsertAll(missingDefaults)
        }

        existingCategories
            .firstOrNull { it.id == DefaultCategoryIds.INCOME && it.name == "Income" }
            ?.let { dao.upsert(it.copy(name = "Credit")) }
    }

    override fun getCategories(): List<TransactionCategory> =
        dao.getAll().map { it.toDomain() }.ifEmpty { fallback.getCategories() }

    override fun suggestFor(transaction: UpiTransaction): TransactionCategory {
        val live = getCategories()
        val counterparty = transaction.counterparty
        val rule = counterparty
            ?.toCounterpartyKey()
            ?.let(ruleDao::findByCounterpartyKey)
        if (rule != null) {
            live.firstOrNull { it.id == rule.categoryId }?.let { return it }
        }
        return CategoryKeywordMatcher.matchByKeywords(transaction, live)
            ?: fallback.suggestFor(transaction)
    }

    override fun assignCounterpartyCategory(counterparty: String, categoryId: String) {
        val key = counterparty.toCounterpartyKey()
        if (key.isBlank()) return

        ruleDao.upsert(
            CategoryRuleEntity(
                counterpartyKey = key,
                categoryId = categoryId,
                counterpartyLabel = counterparty.trim(),
            ),
        )
    }

    override fun upsertCategory(category: TransactionCategory) {
        dao.upsert(category.toEntity())
    }
}
