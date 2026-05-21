package com.mochimoney.app.domain.model

object DefaultCategoryIds {
    const val FOOD = "food"
    const val TRANSPORT = "transport"
    const val SHOPPING = "shopping"
    const val BILLS = "bills"
    const val GROCERIES = "groceries"
    const val HEALTH = "health"
    const val TRANSFERS = "transfers"
    const val INCOME = "income"
    const val OTHER = "other"
    const val UNCATEGORIZED = "uncategorized"
}

object DefaultCategories {
    val all: List<TransactionCategory> = listOf(
        TransactionCategory(
            id = DefaultCategoryIds.FOOD,
            name = "Food & Dining",
            colorHex = "#D97706",
            // Regex alternation as a worked example of the power available.
            keywords = setOf(
                "swiggy|zomato|dominos|kfc|mcdonald|pizza|burger",
                "cafe|coffee|tea|chai",
                "restaurant|dining|eatery|bistro",
            )
        ),
        TransactionCategory(
            id = DefaultCategoryIds.TRANSPORT,
            name = "Transport",
            colorHex = "#2563EB",
            keywords = setOf(
                "uber|ola|rapido|metro|irctc",
                "fuel|petrol|diesel|parking",
            )
        ),
        TransactionCategory(
            id = DefaultCategoryIds.SHOPPING,
            name = "Shopping",
            colorHex = "#7C3AED",
            keywords = setOf(
                "amazon|flipkart|myntra|ajio|meesho",
                "store|mart|retail|mall",
            )
        ),
        TransactionCategory(
            id = DefaultCategoryIds.BILLS,
            name = "Bills & Utilities",
            colorHex = "#0891B2",
            keywords = setOf(
                "airtel|jio|vodafone|vi|broadband",
                "electricity|water|gas|bill|recharge|wifi",
            )
        ),
        TransactionCategory(
            id = DefaultCategoryIds.GROCERIES,
            name = "Groceries",
            colorHex = "#16A34A",
            keywords = setOf(
                "bigbasket|blinkit|zepto|dmart|instamart",
                "grocery|groceries|super\\s*market|market",
            )
        ),
        TransactionCategory(
            id = DefaultCategoryIds.HEALTH,
            name = "Health",
            colorHex = "#E11D48",
            keywords = setOf(
                "apollo|medplus|pharmeasy|netmeds|1mg",
                "pharmacy|chemist|medicine|hospital|clinic|doctor",
            )
        ),
        TransactionCategory(
            id = DefaultCategoryIds.TRANSFERS,
            name = "Transfers",
            colorHex = "#475569",
            keywords = setOf("transfer|trf|self|^to\\s+self")
        ),
        TransactionCategory(
            id = DefaultCategoryIds.INCOME,
            name = "Income",
            colorHex = "#059669",
            keywords = setOf("salary|payroll|refund|cashback|interest")
        ),
        TransactionCategory(
            id = DefaultCategoryIds.OTHER,
            name = "Other",
            colorHex = "#64748B"
        ),
        TransactionCategory(
            id = DefaultCategoryIds.UNCATEGORIZED,
            name = "Uncategorized",
            colorHex = "#94A3B8"
        )
    )

    fun byId(id: String): TransactionCategory =
        all.firstOrNull { it.id == id } ?: all.first { it.id == DefaultCategoryIds.UNCATEGORIZED }
}
