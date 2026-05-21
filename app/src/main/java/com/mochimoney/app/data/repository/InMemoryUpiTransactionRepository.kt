package com.mochimoney.app.data.repository

import com.mochimoney.app.domain.model.UpiTransaction
import com.mochimoney.app.domain.repository.UpiTransactionRepository
import java.util.concurrent.atomic.AtomicLong

class InMemoryUpiTransactionRepository : UpiTransactionRepository {
    private val nextId = AtomicLong(1L)
    private val records = linkedMapOf<Long, UpiTransaction>()
    private val dedupeIndex = mutableMapOf<String, Long>()

    @Synchronized
    override fun upsert(transaction: UpiTransaction): UpiTransaction {
        val existingId = dedupeIndex[transaction.dedupeKey]
        if (existingId != null) {
            return records.getValue(existingId)
        }

        val id = transaction.id.takeIf { it > 0L } ?: nextId.getAndIncrement()
        val saved = transaction.copy(id = id)
        records[id] = saved
        dedupeIndex[saved.dedupeKey] = id
        return saved
    }

    @Synchronized
    override fun findByDedupeKey(dedupeKey: String): UpiTransaction? {
        val id = dedupeIndex[dedupeKey] ?: return null
        return records[id]
    }

    @Synchronized
    override fun getAll(): List<UpiTransaction> =
        records.values.sortedWith(compareByDescending<UpiTransaction> { it.occurredOn }.thenByDescending { it.id })

    @Synchronized
    override fun updateCategory(id: Long, categoryId: String) {
        val current = records[id] ?: return
        records[id] = current.copy(categoryId = categoryId)
    }

    @Synchronized
    override fun updateCategoryForCounterparty(counterparty: String, categoryId: String): Int {
        val normalized = counterparty.normalizedCounterpartyKey()
        var updated = 0
        records.entries.forEach { entry ->
            if (entry.value.counterparty.normalizedCounterpartyKey() == normalized) {
                entry.setValue(entry.value.copy(categoryId = categoryId))
                updated += 1
            }
        }
        return updated
    }

    @Synchronized
    override fun delete(id: Long) {
        val removed = records.remove(id) ?: return
        dedupeIndex.remove(removed.dedupeKey)
    }

    private fun String?.normalizedCounterpartyKey(): String =
        orEmpty().trim().lowercase().replace(Regex("\\s+"), " ")
}
