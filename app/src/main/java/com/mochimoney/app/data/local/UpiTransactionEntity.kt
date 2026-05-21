package com.mochimoney.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mochimoney.app.domain.model.TransactionDirection
import com.mochimoney.app.domain.model.UpiTransaction
import java.time.LocalDate

@Entity(
    tableName = "upi_transactions",
    indices = [Index(value = ["dedupeKey"], unique = true)]
)
data class UpiTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dedupeKey: String,
    val direction: String,
    val amountPaise: Long,
    val currency: String,
    val occurredOn: String,
    val counterparty: String?,
    val referenceNumber: String?,
    val accountSuffix: String?,
    val sender: String?,
    val smsBodyHash: String,
    val smsBody: String?,
    val smsReceivedAtMillis: Long?,
    val categoryId: String,
    val createdAtMillis: Long
)

fun UpiTransaction.toEntity(): UpiTransactionEntity =
    UpiTransactionEntity(
        id = id,
        dedupeKey = dedupeKey,
        direction = direction.name,
        amountPaise = amountPaise,
        currency = currency,
        occurredOn = occurredOn.toString(),
        counterparty = counterparty,
        referenceNumber = referenceNumber,
        accountSuffix = accountSuffix,
        sender = sender,
        smsBodyHash = smsBodyHash,
        smsBody = smsBody,
        smsReceivedAtMillis = smsReceivedAtMillis,
        categoryId = categoryId,
        createdAtMillis = createdAtMillis
    )

fun UpiTransactionEntity.toDomain(): UpiTransaction =
    UpiTransaction(
        id = id,
        dedupeKey = dedupeKey,
        direction = TransactionDirection.valueOf(direction),
        amountPaise = amountPaise,
        currency = currency,
        occurredOn = LocalDate.parse(occurredOn),
        counterparty = counterparty,
        referenceNumber = referenceNumber,
        accountSuffix = accountSuffix,
        sender = sender,
        smsBodyHash = smsBodyHash,
        smsBody = smsBody,
        smsReceivedAtMillis = smsReceivedAtMillis,
        categoryId = categoryId,
        createdAtMillis = createdAtMillis
    )
