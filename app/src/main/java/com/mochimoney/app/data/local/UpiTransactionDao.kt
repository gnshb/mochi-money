package com.mochimoney.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UpiTransactionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnore(transaction: UpiTransactionEntity): Long

    @Query("SELECT * FROM upi_transactions WHERE id = :id LIMIT 1")
    fun findById(id: Long): UpiTransactionEntity?

    @Query("SELECT * FROM upi_transactions WHERE dedupeKey = :dedupeKey LIMIT 1")
    fun findByDedupeKey(dedupeKey: String): UpiTransactionEntity?

    @Query("SELECT * FROM upi_transactions WHERE smsBodyHash = :smsBodyHash LIMIT 1")
    fun findBySmsBodyHash(smsBodyHash: String): UpiTransactionEntity?

    @Query("SELECT * FROM upi_transactions ORDER BY occurredOn DESC, id DESC")
    fun getAll(): List<UpiTransactionEntity>

    @Query(
        """
        UPDATE upi_transactions
        SET dedupeKey = :dedupeKey,
            direction = :direction,
            amountPaise = :amountPaise,
            currency = :currency,
            occurredOn = :occurredOn,
            counterparty = :counterparty,
            referenceNumber = :referenceNumber,
            accountSuffix = :accountSuffix,
            sender = :sender,
            smsBody = :smsBody,
            smsReceivedAtMillis = :smsReceivedAtMillis,
            categoryId = :categoryId
        WHERE id = :id
        """
    )
    fun updateParsedFields(
        id: Long,
        dedupeKey: String,
        direction: String,
        amountPaise: Long,
        currency: String,
        occurredOn: String,
        counterparty: String?,
        referenceNumber: String?,
        accountSuffix: String?,
        sender: String?,
        smsBody: String?,
        smsReceivedAtMillis: Long?,
        categoryId: String,
    )

    @Query("UPDATE upi_transactions SET categoryId = :categoryId WHERE id = :id")
    fun updateCategory(id: Long, categoryId: String)

    @Query("UPDATE upi_transactions SET categoryId = :categoryId WHERE lower(trim(counterparty)) = lower(trim(:counterparty))")
    fun updateCategoryForCounterparty(counterparty: String, categoryId: String): Int

    @Query("UPDATE upi_transactions SET counterparty = :newName WHERE lower(trim(counterparty)) = lower(trim(:oldName))")
    fun renameCounterparty(oldName: String, newName: String): Int

    @Query("DELETE FROM upi_transactions WHERE id = :id")
    fun delete(id: Long)
}
