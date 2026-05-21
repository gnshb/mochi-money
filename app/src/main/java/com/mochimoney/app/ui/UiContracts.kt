package com.mochimoney.app.ui

import androidx.compose.runtime.Immutable
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/*
 * Placeholder UI contracts because this repository currently has no repository
 * or ViewModel layer. A future data layer can map its models into these immutable
 * screen states and wire the callbacks from MochiMoneyApp into real actions.
 */
@Immutable
data class MochiMoneyUiState(
    val hasCompletedOnboarding: Boolean = false,
    val smsPermissionGranted: Boolean = false,
    val totalSpentThisMonth: Long = 0,
    val monthlyBudget: Long = 0,
    val incomingThisMonth: Long = 0,
    val uncategorizedCount: Int = 0,
    val isRefreshing: Boolean = false,
    val scanStatus: String? = null,
    val senderPattern: String = "",
    val transactions: List<UpiTransactionUi> = emptyList(),
    val categories: List<CategoryUi> = emptyList(),
    val settings: SettingsUi = SettingsUi(),
)

@Immutable
data class UpiTransactionUi(
    val id: String = UUID.randomUUID().toString(),
    val domainId: Long? = null,
    val title: String,
    val subtitle: String,
    val amountPaise: Long,
    val occurredOn: LocalDate,
    val occurredTime: LocalTime? = null,
    val counterparty: String? = title,
    val categoryId: String? = null,
    val note: String? = null,
    val smsBody: String? = null,
    val isIncoming: Boolean = false,
)

@Immutable
data class CategoryUi(
    val id: String,
    val label: String,
    val kind: CategoryKind,
    val spentPaise: Long,
    val budgetPaise: Long? = null,
    val keywordsCsv: String = "",
)

enum class CategoryKind {
    Food,
    Groceries,
    Travel,
    Bills,
    Shopping,
    Health,
    Salary,
    Savings,
    Other,
}

@Immutable
data class SettingsUi(
    val scanSmsAutomatically: Boolean = true,
    val budgetAlerts: Boolean = true,
)

data class MochiMoneyActions(
    val onCompleteOnboarding: () -> Unit = {},
    val onRequestSmsPermission: () -> Unit = {},
    val onOpenCategorization: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onTransactionSelected: (String) -> Unit = {},
    val onCategorySelected: (String) -> Unit = {},
    val onCategorizeTransaction: (transactionId: String, categoryId: String) -> Unit = { _, _ -> },
    val onUpsertCategory: (name: String, keywordsCsv: String, categoryId: String?) -> Unit = { _, _, _ -> },
    val onApplyCategoryKeywordRules: () -> Unit = {},
    val onSetMonthlyBudget: (budgetPaise: Long) -> Unit = {},
    val onSetSenderPattern: (pattern: String) -> Unit = {},
    val onToggleAutoScan: (Boolean) -> Unit = {},
    val onToggleBudgetAlerts: (Boolean) -> Unit = {},
)

fun sampleMochiMoneyUiState(): MochiMoneyUiState {
    val today = LocalDate.now()
    val categories = listOf(
        CategoryUi("food", "Food", CategoryKind.Food, 486_000, 700_000),
        CategoryUi("groceries", "Groceries", CategoryKind.Groceries, 312_500, 450_000),
        CategoryUi("travel", "Travel", CategoryKind.Travel, 221_000, 300_000),
        CategoryUi("bills", "Bills", CategoryKind.Bills, 610_000, 650_000),
        CategoryUi("shopping", "Shopping", CategoryKind.Shopping, 354_900, 500_000),
        CategoryUi("savings", "Savings", CategoryKind.Savings, 900_000, null),
    )

    val transactions = listOf(
        UpiTransactionUi(
            title = "Morning chai and snacks",
            subtitle = "Outgoing",
            amountPaise = 8_500,
            occurredOn = today, occurredTime = LocalTime.of(8, 35),
            categoryId = "food",
        ),
        UpiTransactionUi(
            title = "Grocery basket",
            subtitle = "Outgoing",
            amountPaise = 92_300,
            occurredOn = today.minusDays(1), occurredTime = LocalTime.of(19, 12),
            categoryId = "groceries",
        ),
        UpiTransactionUi(
            title = "Monthly rent transfer",
            subtitle = "Outgoing",
            amountPaise = 1_800_000,
            occurredOn = today.minusDays(2), occurredTime = LocalTime.of(10, 2),
            categoryId = "bills",
        ),
        UpiTransactionUi(
            title = "Metro card top up",
            subtitle = "Outgoing",
            amountPaise = 50_000,
            occurredOn = today.minusDays(3), occurredTime = LocalTime.of(9, 45),
            categoryId = "travel",
        ),
        UpiTransactionUi(
            title = "Salary",
            subtitle = "Incoming",
            amountPaise = 52_000_00,
            occurredOn = today.minusDays(4), occurredTime = LocalTime.of(11, 25),
            categoryId = "salary",
            isIncoming = true,
        ),
        UpiTransactionUi(
            title = "New transaction",
            subtitle = "Needs category",
            amountPaise = 37_000,
            occurredOn = today.minusDays(5), occurredTime = LocalTime.of(21, 5),
            categoryId = null,
        ),
    )

    return MochiMoneyUiState(
        hasCompletedOnboarding = false,
        smsPermissionGranted = false,
        totalSpentThisMonth = 4_204_000,
        monthlyBudget = 7_500_000,
        incomingThisMonth = 52_000_00,
        uncategorizedCount = transactions.count { it.categoryId == null },
        transactions = transactions,
        categories = categories,
    )
}
