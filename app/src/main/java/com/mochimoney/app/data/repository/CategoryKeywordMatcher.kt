package com.mochimoney.app.data.repository

import com.mochimoney.app.domain.model.DefaultCategoryIds
import com.mochimoney.app.domain.model.TransactionCategory
import com.mochimoney.app.domain.model.TransactionDirection
import com.mochimoney.app.domain.model.UpiTransaction

internal object CategoryKeywordMatcher {
    fun matchByKeywords(
        transaction: UpiTransaction,
        categories: List<TransactionCategory>,
    ): TransactionCategory? {
        if (transaction.direction == TransactionDirection.CREDIT) {
            return categories.firstOrNull { it.id == DefaultCategoryIds.INCOME }
        }
        val haystack = listOfNotNull(transaction.counterparty)
            .joinToString(separator = " ")
            .trim()
        if (haystack.isBlank()) return null
        return categories.firstOrNull { category ->
            category.id != DefaultCategoryIds.OTHER &&
                category.id != DefaultCategoryIds.UNCATEGORIZED &&
                category.keywords.any { pattern -> matches(haystack, pattern) }
        }
    }

    fun matches(haystack: String, pattern: String): Boolean {
        val trimmed = pattern.trim()
        if (trimmed.isEmpty()) return false
        val regex = runCatching { Regex(trimmed, RegexOption.IGNORE_CASE) }.getOrNull()
        return if (regex != null) {
            regex.containsMatchIn(haystack)
        } else {
            haystack.contains(trimmed, ignoreCase = true)
        }
    }
}
