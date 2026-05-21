package com.mochimoney.app.domain.model

import java.math.BigDecimal
import java.time.LocalDate

data class UpiTransaction(
    val id: Long = 0L,
    val dedupeKey: String,
    val direction: TransactionDirection,
    val amountPaise: Long,
    val currency: String = "INR",
    val occurredOn: LocalDate,
    val counterparty: String?,
    val referenceNumber: String?,
    val accountSuffix: String?,
    val sender: String?,
    val smsBodyHash: String,
    val smsBody: String? = null,
    val smsReceivedAtMillis: Long?,
    val categoryId: String = DefaultCategoryIds.UNCATEGORIZED,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val amount: BigDecimal
        get() = BigDecimal(amountPaise).movePointLeft(2)
}
