package com.mochimoney.app.domain.model

data class TransactionCategory(
    val id: String,
    val name: String,
    val colorHex: String,
    val keywords: Set<String> = emptySet()
)
