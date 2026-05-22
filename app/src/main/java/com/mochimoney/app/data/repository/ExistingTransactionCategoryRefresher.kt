package com.mochimoney.app.data.repository

import com.mochimoney.app.domain.model.DefaultCategoryIds
import com.mochimoney.app.domain.repository.CategoryRepository
import com.mochimoney.app.domain.repository.UpiTransactionRepository

internal object ExistingTransactionCategoryRefresher {
    fun refresh(
        categoryRepository: CategoryRepository,
        transactionRepository: UpiTransactionRepository,
    ): Int {
        var changed = 0
        transactionRepository.getAll().forEach { transaction ->
            if (transaction.categoryId in InternalCategoryIds) return@forEach

            val suggested = categoryRepository.suggestFor(transaction)
            if (suggested.id != transaction.categoryId) {
                transactionRepository.updateCategory(transaction.id, suggested.id)
                changed += 1
            }
        }
        return changed
    }

    private val InternalCategoryIds = setOf(DefaultCategoryIds.SPLITWISE)
}
