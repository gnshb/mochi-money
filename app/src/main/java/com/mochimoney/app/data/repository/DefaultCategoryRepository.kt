package com.mochimoney.app.data.repository

import com.mochimoney.app.domain.model.DefaultCategories
import com.mochimoney.app.domain.model.DefaultCategoryIds
import com.mochimoney.app.domain.model.TransactionCategory
import com.mochimoney.app.domain.model.UpiTransaction
import com.mochimoney.app.domain.repository.CategoryRepository

class DefaultCategoryRepository : CategoryRepository {
    private var categories: List<TransactionCategory> = DefaultCategories.all

    override fun getCategories(): List<TransactionCategory> = categories

    override fun suggestFor(transaction: UpiTransaction): TransactionCategory =
        CategoryKeywordMatcher.matchByKeywords(transaction, categories)
            ?: DefaultCategories.byId(DefaultCategoryIds.UNCATEGORIZED)

    override fun upsertCategory(category: TransactionCategory) {
        categories = categories.filterNot { it.id == category.id } + category
    }
}
