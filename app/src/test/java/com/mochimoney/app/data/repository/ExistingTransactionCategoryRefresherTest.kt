package com.mochimoney.app.data.repository

import com.mochimoney.app.domain.model.DefaultCategoryIds
import com.mochimoney.app.domain.model.DedupeKeyGenerator
import com.mochimoney.app.domain.model.TransactionCategory
import com.mochimoney.app.domain.model.TransactionDirection
import com.mochimoney.app.domain.model.UpiTransaction
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ExistingTransactionCategoryRefresherTest {
    @Test
    fun removedKeywordUnmatchesExistingTransaction() {
        val categories = DefaultCategoryRepository()
        categories.upsertCategory(
            TransactionCategory(
                id = DefaultCategoryIds.FOOD,
                name = "Food & Dining",
                colorHex = "#D97706",
                keywords = setOf("cafe"),
            ),
        )
        val transactions = InMemoryUpiTransactionRepository()
        val saved = transactions.upsert(
            transaction(counterparty = "Corner Cafe", categoryId = DefaultCategoryIds.FOOD),
        )

        categories.upsertCategory(
            TransactionCategory(
                id = DefaultCategoryIds.FOOD,
                name = "Food & Dining",
                colorHex = "#D97706",
                keywords = emptySet(),
            ),
        )

        val changed = ExistingTransactionCategoryRefresher.refresh(categories, transactions)

        assertEquals(1, changed)
        assertEquals(
            DefaultCategoryIds.UNCATEGORIZED,
            transactions.getAll().first { it.id == saved.id }.categoryId,
        )
    }

    @Test
    fun splitwiseTransactionsAreNotReclassifiedByKeywordRefresh() {
        val categories = DefaultCategoryRepository()
        val transactions = InMemoryUpiTransactionRepository()
        val saved = transactions.upsert(
            transaction(counterparty = "Apartment group", categoryId = DefaultCategoryIds.SPLITWISE),
        )

        val changed = ExistingTransactionCategoryRefresher.refresh(categories, transactions)

        assertEquals(0, changed)
        assertEquals(
            DefaultCategoryIds.SPLITWISE,
            transactions.getAll().first { it.id == saved.id }.categoryId,
        )
    }

    private fun transaction(
        counterparty: String,
        categoryId: String,
    ): UpiTransaction =
        UpiTransaction(
            dedupeKey = DedupeKeyGenerator.sha256("$counterparty|$categoryId").take(32),
            direction = TransactionDirection.DEBIT,
            amountPaise = 12_300L,
            occurredOn = LocalDate.of(2026, 5, 22),
            counterparty = counterparty,
            referenceNumber = null,
            accountSuffix = null,
            sender = "test",
            smsBodyHash = DedupeKeyGenerator.sha256(counterparty),
            smsReceivedAtMillis = null,
            categoryId = categoryId,
        )
}
