package com.mochimoney.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mochimoney.app.domain.model.TransactionCategory

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorHex: String,
    val keywordsCsv: String
)

fun TransactionCategory.toEntity(): CategoryEntity =
    CategoryEntity(
        id = id,
        name = name,
        colorHex = colorHex,
        keywordsCsv = keywords.joinToString(separator = ",")
    )

fun CategoryEntity.toDomain(): TransactionCategory =
    TransactionCategory(
        id = id,
        name = name,
        colorHex = colorHex,
        keywords = keywordsCsv
            .split(",")
            .map(String::trim)
            .filter(String::isNotBlank)
            .toSet()
    )
