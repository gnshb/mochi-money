package com.mochimoney.app.domain.repository

import com.mochimoney.app.domain.model.UpiTransaction

interface UpiTransactionRepository {
    fun upsert(transaction: UpiTransaction): UpiTransaction

    fun upsertAll(transactions: List<UpiTransaction>): List<UpiTransaction> =
        transactions.map(::upsert)

    fun findById(id: Long): UpiTransaction?

    fun findByDedupeKey(dedupeKey: String): UpiTransaction?

    fun findBySmsBodyHash(smsBodyHash: String): UpiTransaction? = null

    fun getAll(): List<UpiTransaction>

    fun updateParsedFields(transaction: UpiTransaction)

    fun updateCategory(id: Long, categoryId: String)

    fun updateCategoryForCounterparty(counterparty: String, categoryId: String): Int

    fun renameCounterparty(oldName: String, newName: String): Int

    fun delete(id: Long)
}
