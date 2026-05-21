package com.mochimoney.app.domain.repository

import com.mochimoney.app.domain.model.TransactionCategory
import com.mochimoney.app.domain.model.UpiTransaction

interface CategoryRepository {
    fun getCategories(): List<TransactionCategory>

    fun suggestFor(transaction: UpiTransaction): TransactionCategory

    fun assignCounterpartyCategory(counterparty: String, categoryId: String) = Unit

    fun upsertCategory(category: TransactionCategory) = Unit
}
