package com.mochimoney.app.data.repository

import com.mochimoney.app.domain.model.DefaultCategoryIds
import com.mochimoney.app.domain.model.UpiTransaction
import com.mochimoney.app.domain.repository.UpiTransactionRepository

internal object ParsedSmsTransactionImporter {
    fun import(
        transactions: List<UpiTransaction>,
        repository: UpiTransactionRepository,
    ): Int {
        val existingKeys = repository.getAll().map { it.dedupeKey }.toMutableSet()
        var newCount = 0
        transactions.forEach { parsed ->
            val existingByBody = repository.findBySmsBodyHash(parsed.smsBodyHash)
            if (existingByBody != null) {
                repairParsedSmsTransaction(repository, existingByBody, parsed)
            } else if (parsed.dedupeKey !in existingKeys) {
                repository.upsert(parsed)
                existingKeys += parsed.dedupeKey
                newCount += 1
            }
        }
        return newCount
    }

    private fun repairParsedSmsTransaction(
        repository: UpiTransactionRepository,
        existing: UpiTransaction,
        parsed: UpiTransaction,
    ) {
        val duplicate = repository.findByDedupeKey(parsed.dedupeKey)
        if (duplicate != null && duplicate.id != existing.id) {
            repository.delete(existing.id)
            return
        }

        val repaired = parsed.copy(
            id = existing.id,
            categoryId = if (existing.categoryId in ParserManagedCategoryIds) {
                parsed.categoryId
            } else {
                existing.categoryId
            },
            createdAtMillis = existing.createdAtMillis,
        )

        if (repaired != existing) {
            repository.updateParsedFields(repaired)
        }
    }

    private val ParserManagedCategoryIds = setOf(
        DefaultCategoryIds.UNCATEGORIZED,
        DefaultCategoryIds.OTHER,
        DefaultCategoryIds.INCOME,
    )
}
