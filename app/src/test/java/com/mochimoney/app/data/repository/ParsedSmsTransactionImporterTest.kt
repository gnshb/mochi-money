package com.mochimoney.app.data.repository

import com.mochimoney.app.domain.model.DefaultCategoryIds
import com.mochimoney.app.domain.model.DedupeKeyGenerator
import com.mochimoney.app.domain.model.TransactionDirection
import com.mochimoney.app.domain.model.UpiTransaction
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ParsedSmsTransactionImporterTest {
    @Test
    fun repairsExistingSmsImportedBeforeParserLearnedCounterparty() {
        val repository = InMemoryUpiTransactionRepository()
        val old = repository.upsert(
            transaction(
                dedupeSeed = "old",
                direction = TransactionDirection.CREDIT,
                counterparty = null,
                categoryId = DefaultCategoryIds.INCOME,
                smsBodyHash = SharedSmsBodyHash,
            ),
        )
        val parsed = transaction(
            dedupeSeed = "new",
            direction = TransactionDirection.DEBIT,
            counterparty = "Rajasri Fuels",
            categoryId = DefaultCategoryIds.UNCATEGORIZED,
            smsBodyHash = SharedSmsBodyHash,
        )

        val newCount = ParsedSmsTransactionImporter.import(listOf(parsed), repository)

        val repaired = repository.getAll().single()
        assertEquals(0, newCount)
        assertEquals(old.id, repaired.id)
        assertEquals(TransactionDirection.DEBIT, repaired.direction)
        assertEquals("Rajasri Fuels", repaired.counterparty)
        assertEquals(DefaultCategoryIds.UNCATEGORIZED, repaired.categoryId)
        assertEquals(parsed.dedupeKey, repaired.dedupeKey)
    }

    @Test
    fun preservesManualCategoryWhenRepairingParserFields() {
        val repository = InMemoryUpiTransactionRepository()
        repository.upsert(
            transaction(
                dedupeSeed = "old",
                direction = TransactionDirection.DEBIT,
                counterparty = null,
                categoryId = DefaultCategoryIds.GROCERIES,
                smsBodyHash = SharedSmsBodyHash,
            ),
        )
        val parsed = transaction(
            dedupeSeed = "new",
            direction = TransactionDirection.DEBIT,
            counterparty = "paytm-jiomartgrocery@ptybl",
            categoryId = DefaultCategoryIds.UNCATEGORIZED,
            smsBodyHash = SharedSmsBodyHash,
        )

        ParsedSmsTransactionImporter.import(listOf(parsed), repository)

        val repaired = repository.getAll().single()
        assertEquals("paytm-jiomartgrocery@ptybl", repaired.counterparty)
        assertEquals(DefaultCategoryIds.GROCERIES, repaired.categoryId)
    }

    private fun transaction(
        dedupeSeed: String,
        direction: TransactionDirection,
        counterparty: String?,
        categoryId: String,
        smsBodyHash: String,
    ): UpiTransaction =
        UpiTransaction(
            dedupeKey = DedupeKeyGenerator.sha256(dedupeSeed).take(32),
            direction = direction,
            amountPaise = 31_440L,
            occurredOn = LocalDate.of(2026, 5, 21),
            counterparty = counterparty,
            referenceNumber = "071210138665",
            accountSuffix = "2601",
            sender = "TM-BANK",
            smsBodyHash = smsBodyHash,
            smsBody = "test sms",
            smsReceivedAtMillis = null,
            categoryId = categoryId,
        )

    private companion object {
        const val SharedSmsBodyHash = "same-sms-body-hash"
    }
}
