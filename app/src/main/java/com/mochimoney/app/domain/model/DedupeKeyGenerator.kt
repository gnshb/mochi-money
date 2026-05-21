package com.mochimoney.app.domain.model

import java.security.MessageDigest
import java.time.LocalDate
import java.util.Locale

object DedupeKeyGenerator {
    fun generate(
        direction: TransactionDirection,
        amountPaise: Long,
        occurredOn: LocalDate,
        referenceNumber: String?,
        accountSuffix: String?,
        counterparty: String?
    ): String {
        val fingerprint = listOf(
            direction.name,
            amountPaise.toString(),
            occurredOn.toString(),
            referenceNumber.normalizedPart(),
            accountSuffix.normalizedPart(),
            counterparty.normalizedPart()
        ).joinToString(separator = "|")

        return sha256(fingerprint).take(32)
    }

    fun sha256(value: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { "%02x".format(it) }

    private fun String?.normalizedPart(): String =
        this
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?.replace(Regex("\\s+"), " ")
            .orEmpty()
}
