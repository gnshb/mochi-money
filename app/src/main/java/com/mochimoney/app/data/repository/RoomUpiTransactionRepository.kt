package com.mochimoney.app.data.repository

import com.mochimoney.app.data.local.UpiTransactionDao
import com.mochimoney.app.data.local.toDomain
import com.mochimoney.app.data.local.toEntity
import com.mochimoney.app.domain.model.UpiTransaction
import com.mochimoney.app.domain.repository.UpiTransactionRepository

class RoomUpiTransactionRepository(
    private val dao: UpiTransactionDao
) : UpiTransactionRepository {
    override fun upsert(transaction: UpiTransaction): UpiTransaction {
        val insertedId = dao.insertIgnore(transaction.toEntity())
        val saved = if (insertedId == -1L) {
            dao.findByDedupeKey(transaction.dedupeKey)
        } else {
            dao.findById(insertedId)
        }

        return saved?.toDomain() ?: transaction.copy(id = insertedId.takeIf { it > 0L } ?: transaction.id)
    }

    override fun findByDedupeKey(dedupeKey: String): UpiTransaction? =
        dao.findByDedupeKey(dedupeKey)?.toDomain()

    override fun getAll(): List<UpiTransaction> =
        dao.getAll().map { it.toDomain() }

    override fun updateCategory(id: Long, categoryId: String) {
        dao.updateCategory(id, categoryId)
    }

    override fun updateCategoryForCounterparty(counterparty: String, categoryId: String): Int =
        dao.updateCategoryForCounterparty(counterparty, categoryId)

    override fun delete(id: Long) {
        dao.delete(id)
    }
}
