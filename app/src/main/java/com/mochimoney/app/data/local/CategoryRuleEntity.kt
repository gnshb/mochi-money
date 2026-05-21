package com.mochimoney.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

@Entity(tableName = "category_rules")
data class CategoryRuleEntity(
    @PrimaryKey val counterpartyKey: String,
    val categoryId: String,
    val counterpartyLabel: String,
    val updatedAtMillis: Long = System.currentTimeMillis(),
)

fun String.toCounterpartyKey(): String =
    trim()
        .lowercase(Locale.ROOT)
        .replace(Regex("\\s+"), " ")
