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

    override fun findBySmsBodyHash(smsBodyHash: String): UpiTransaction? =
        dao.findBySmsBodyHash(smsBodyHash)?.toDomain()

    override fun getAll(): List<UpiTransaction> =
        dao.getAll().map { it.toDomain() }

    override fun updateParsedFields(transaction: UpiTransaction) {
        dao.updateParsedFields(
            id = transaction.id,
            dedupeKey = transaction.dedupeKey,
            direction = transaction.direction.name,
            amountPaise = transaction.amountPaise,
            currency = transaction.currency,
            occurredOn = transaction.occurredOn.toString(),
            counterparty = transaction.counterparty,
            referenceNumber = transaction.referenceNumber,
            accountSuffix = transaction.accountSuffix,
            sender = transaction.sender,
            smsBody = transaction.smsBody,
            smsReceivedAtMillis = transaction.smsReceivedAtMillis,
            categoryId = transaction.categoryId,
        )
    }

    override fun updateCategory(id: Long, categoryId: String) {
        dao.updateCategory(id, categoryId)
    }

    override fun updateCategoryForCounterparty(counterparty: String, categoryId: String): Int =
        dao.updateCategoryForCounterparty(counterparty, categoryId)

    override fun delete(id: Long) {
        dao.delete(id)
    }
}
