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
    /** 0f..1f while a "detect all" LLM run is in flight, null otherwise. */
    val inferProgress: Float? = null,
    /** True while any LLM detection (single or all) is running — keeps the popup pinned. */
    val inferBusy: Boolean = false,
    /** Non-null shows a manually-dismissed error dialog (e.g. a failed model download). */
    val downloadError: String? = null,
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
    /** True for user-entered transactions (no SMS/Splitwise backing); only these can be deleted. */
    val isManual: Boolean = false,
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
    val splitwise: SplitwiseSettingsUi = SplitwiseSettingsUi(),
    val llm: LlmSettingsUi = LlmSettingsUi(),
)

enum class LlmBackendUi { MediaPipe, GeminiNano }

/** Live result of probing whether Gemini Nano can actually run on this device. */
enum class GeminiNanoStatusUi { Unknown, Checking, Available, Unavailable }

@Immutable
data class LlmSettingsUi(
    val enabled: Boolean = false,
    val geminiNanoSupported: Boolean = false,
    val geminiNanoStatus: GeminiNanoStatusUi = GeminiNanoStatusUi.Unknown,
    val backend: LlmBackendUi = LlmBackendUi.MediaPipe,
    val models: List<LlmModelUi> = emptyList(),
    val hfTokenConfigured: Boolean = false,
)

@Immutable
data class LlmModelUi(
    val id: String,
    val displayName: String,
    val description: String,
    val sizeLabel: String,
    val license: String,
    val requiresLicenseAcceptance: Boolean,
    val isInstalled: Boolean,
    val isActive: Boolean,
    /** 0f..1f while a download is in flight, null otherwise. */
    val downloadProgress: Float? = null,
)

/** Replaces the download progress for [modelId] (null clears it). */
fun LlmSettingsUi.withProgress(modelId: String, progress: Float?): LlmSettingsUi =
    copy(models = models.map { if (it.id == modelId) it.copy(downloadProgress = progress) else it })

@Immutable
data class SplitwiseSettingsUi(
    val enabled: Boolean = false,
    val apiKeyConfigured: Boolean = false,
    val currentUserLabel: String = "",
    val groups: List<SplitwiseGroupUi> = emptyList(),
    val selectedGroupIds: Set<Long> = emptySet(),
)

@Immutable
data class SplitwiseGroupUi(
    val id: Long,
    val name: String,
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
    val onToggleSplitwise: (Boolean) -> Unit = {},
    val onConnectSplitwise: (apiKey: String) -> Unit = {},
    val onSelectSplitwiseGroup: (groupId: Long, selected: Boolean) -> Unit = { _, _ -> },
    val onSyncSplitwise: () -> Unit = {},
    val onToggleLlm: (Boolean) -> Unit = {},
    val onSelectLlmBackend: (LlmBackendUi) -> Unit = {},
    val onCheckGeminiNano: () -> Unit = {},
    val onSetHuggingFaceToken: (String) -> Unit = {},
    val onDownloadLlmModel: (modelId: String) -> Unit = {},
    val onDeleteLlmModel: (modelId: String) -> Unit = {},
    val onActivateLlmModel: (modelId: String) -> Unit = {},
    val onInferCounterparty: (transactionId: String) -> Unit = {},
    val onInferAllCounterparties: () -> Unit = {},
    val onEditTransaction: (transactionId: String, name: String, amountPaise: Long) -> Unit = { _, _, _ -> },
    val onDeleteTransaction: (transactionId: String) -> Unit = {},
    val onAddManualTransaction: (
        title: String,
        amountPaise: Long,
        occurredOn: LocalDate,
        isIncoming: Boolean,
        categoryId: String?,
        note: String?,
    ) -> Unit = { _, _, _, _, _, _ -> },
    val onDismissDownloadError: () -> Unit = {},
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
