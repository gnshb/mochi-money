package com.mochimoney.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mochimoney.app.data.AppPreferences
import com.mochimoney.app.data.splitwise.SplitwiseGroup
import com.mochimoney.app.domain.model.DefaultCategoryIds
import com.mochimoney.app.domain.model.TransactionCategory
import com.mochimoney.app.domain.model.TransactionDirection
import com.mochimoney.app.domain.model.UpiTransaction
import com.mochimoney.app.ui.components.KawaiiBackdrop
import com.mochimoney.app.ui.components.MochiIcons
import com.mochimoney.app.ui.components.rememberMochiHapticClick
import com.mochimoney.app.ui.screens.CategorizationScreen
import com.mochimoney.app.ui.screens.DashboardScreen
import com.mochimoney.app.ui.screens.OnboardingScreen
import com.mochimoney.app.ui.screens.SettingsScreen
import com.mochimoney.app.ui.screens.TransactionsScreen
import com.mochimoney.app.ui.theme.MochiMoneyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private const val MinRefreshMillis = 1200L

@Composable
fun MochiMoneyRoot(
    initialState: MochiMoneyUiState = sampleMochiMoneyUiState(),
) {
    var state by remember { mutableStateOf(initialState) }
    val actions = MochiMoneyActions(
        onCompleteOnboarding = {
            state = state.copy(hasCompletedOnboarding = true)
        },
        onRequestSmsPermission = {
            state = state.copy(smsPermissionGranted = true)
        },
        onCategorizeTransaction = { transactionId, categoryId ->
            val updatedTransactions = state.transactions.map { transaction ->
                if (transaction.id == transactionId) transaction.copy(categoryId = categoryId) else transaction
            }
            state = state.copy(
                transactions = updatedTransactions,
                uncategorizedCount = updatedTransactions.count { it.categoryId == null || it.categoryId == DefaultCategoryIds.UNCATEGORIZED },
            )
        },
        onUpsertCategory = { _, _, _ -> },
        onApplyCategoryKeywordRules = {},
        onSetMonthlyBudget = {},
        onToggleAutoScan = {
            state = state.copy(settings = state.settings.copy(scanSmsAutomatically = it))
        },
        onToggleBudgetAlerts = {
            state = state.copy(settings = state.settings.copy(budgetAlerts = it))
        },
        onToggleSplitwise = {
            state = state.copy(settings = state.settings.copy(splitwise = state.settings.splitwise.copy(enabled = it)))
        },
    )

    MochiMoneyTheme {
        MochiMoneyApp(state = state, actions = actions)
    }
}

@Composable
fun MochiMoneyApp(
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var refreshKey by remember { mutableStateOf(0) }
    var state by remember {
        mutableStateOf(
            MochiMoneyUiState(
                hasCompletedOnboarding = container.preferences.hasCompletedOnboarding,
                smsPermissionGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_SMS,
                ) == PackageManager.PERMISSION_GRANTED,
                senderPattern = container.preferences.senderPattern,
                monthlyBudget = container.preferences.monthlyBudgetPaise,
                settings = SettingsUi(
                    splitwise = container.preferences.toSplitwiseSettingsUi(),
                ),
            ),
        )
    }
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        state = state.copy(smsPermissionGranted = granted)
        if (granted) refreshKey += 1
    }

    LaunchedEffect(container, refreshKey) {
        val startMillis = System.currentTimeMillis()
        val loadedState = withContext(Dispatchers.IO) {
            val imported = if (state.smsPermissionGranted) {
                container.refreshFromSmsInbox()
            } else {
                0
            }
            val splitwiseResult = runCatching { container.refreshFromSplitwise() }
            val splitwiseImported = splitwiseResult.getOrDefault(0)
            loadStateFromRepositories(
                categories = container.categoryRepository.getCategories(),
                transactions = container.transactionRepository.getAll(),
                monthlyBudget = container.preferences.monthlyBudgetPaise,
                previousState = state,
            ).copy(
                isRefreshing = false,
                scanStatus = when {
                    splitwiseResult.isFailure -> "Splitwise sync failed: ${splitwiseResult.exceptionOrNull().userFacingMessage()}"
                    state.smsPermissionGranted -> "Scan complete. $imported new matching SMS and $splitwiseImported Splitwise expenses imported."
                    splitwiseImported > 0 -> "Splitwise sync complete. $splitwiseImported new expenses imported."
                    else -> state.scanStatus
                },
                settings = state.settings.copy(splitwise = container.preferences.toSplitwiseSettingsUi()),
            )
        }
        // Hold the refreshing state for at least 1.2s so the mochi pulse is visible
        // even when the scan completes in a few hundred ms.
        val elapsed = System.currentTimeMillis() - startMillis
        if (state.isRefreshing && elapsed < MinRefreshMillis) {
            kotlinx.coroutines.delay(MinRefreshMillis - elapsed)
        }
        state = loadedState
    }

    LaunchedEffect(state.scanStatus) {
        val status = state.scanStatus ?: return@LaunchedEffect
        if (status.startsWith("Scan complete") || status.startsWith("Saved") ||
            status.startsWith("Applied") || status.startsWith("Monthly budget") ||
            status.startsWith("Splitwise") || status.startsWith("Connected")
        ) {
            kotlinx.coroutines.delay(2500)
            if (state.scanStatus == status) {
                state = state.copy(scanStatus = null)
            }
        }
    }

    val actions = MochiMoneyActions(
        onCompleteOnboarding = {
            container.preferences.hasCompletedOnboarding = true
            state = state.copy(hasCompletedOnboarding = true)
        },
        onRequestSmsPermission = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    ),
                )
            } else {
                smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
            }
        },
        onRefresh = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
                state = state.copy(isRefreshing = true, scanStatus = "Scanning SMS inbox...")
                refreshKey += 1
            } else {
                state = state.copy(scanStatus = "SMS permission is needed before scanning.")
                smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
            }
        },
        onCategorizeTransaction = { transactionId, categoryId ->
            val selectedTransaction = state.transactions.firstOrNull { it.id == transactionId }
            selectedTransaction?.domainId?.let { domainId ->
                val counterparty = selectedTransaction.counterparty ?: selectedTransaction.title
                coroutineScope.launch(Dispatchers.IO) {
                    container.applyCategoryRule(domainId, counterparty, categoryId)
                    withContext(Dispatchers.Main) {
                        refreshKey += 1
                    }
                }
            }
            val updatedTransactions = state.transactions.map { transaction ->
                if (transaction.id == transactionId) transaction.copy(categoryId = categoryId) else transaction
            }
            state = state.copy(
                transactions = updatedTransactions,
                uncategorizedCount = updatedTransactions.count { it.categoryId == null || it.categoryId == DefaultCategoryIds.UNCATEGORIZED },
                scanStatus = "Saved category rule for ${selectedTransaction?.title ?: "merchant"}.",
            )
        },
        onUpsertCategory = { name, keywordsCsv, categoryId ->
            coroutineScope.launch(Dispatchers.IO) {
                container.upsertCategory(name, keywordsCsv, categoryId)
                val changed = container.applyKeywordRulesToExisting()
                withContext(Dispatchers.Main) {
                    state = state.copy(
                        scanStatus = if (changed > 0) {
                            "Saved. Re-categorized $changed transaction${if (changed == 1) "" else "s"}."
                        } else {
                            "Saved."
                        },
                    )
                    refreshKey += 1
                }
            }
        },
        onApplyCategoryKeywordRules = {
            coroutineScope.launch(Dispatchers.IO) {
                val changed = container.applyKeywordRulesToExisting()
                withContext(Dispatchers.Main) {
                    state = state.copy(scanStatus = "Applied keyword rules to $changed transactions.")
                    refreshKey += 1
                }
            }
        },
        onSetMonthlyBudget = { budgetPaise ->
            container.preferences.monthlyBudgetPaise = budgetPaise
            state = state.copy(
                monthlyBudget = budgetPaise,
                scanStatus = "Monthly budget updated.",
            )
        },
        onSetSenderPattern = { pattern ->
            container.preferences.senderPattern = pattern
            state = state.copy(
                senderPattern = pattern,
                scanStatus = if (pattern.isBlank()) "Sender filter cleared." else "Sender filter saved.",
            )
            refreshKey += 1
        },
        onToggleAutoScan = {
            state = state.copy(settings = state.settings.copy(scanSmsAutomatically = it))
        },
        onToggleBudgetAlerts = {
            state = state.copy(settings = state.settings.copy(budgetAlerts = it))
        },
        onToggleSplitwise = { enabled ->
            container.setSplitwiseEnabled(enabled)
            state = state.copy(
                settings = state.settings.copy(
                    splitwise = container.preferences.toSplitwiseSettingsUi(),
                ),
                scanStatus = if (enabled) "Splitwise enabled." else "Splitwise disabled.",
            )
        },
        onConnectSplitwise = { apiKey ->
            state = state.copy(scanStatus = "Connecting Splitwise...")
            coroutineScope.launch(Dispatchers.IO) {
                val keyToUse = apiKey.ifBlank { container.preferences.splitwiseApiKey }
                val result = runCatching { container.connectSplitwise(keyToUse) }
                withContext(Dispatchers.Main) {
                    state = if (result.isSuccess) {
                        val connection = result.getOrThrow()
                        state.copy(
                            settings = state.settings.copy(
                                splitwise = container.preferences.toSplitwiseSettingsUi(),
                            ),
                            scanStatus = "Connected Splitwise as ${connection.user.name}. Select groups to sync.",
                        )
                    } else {
                        state.copy(scanStatus = "Splitwise connection failed: ${result.exceptionOrNull().userFacingMessage()}")
                    }
                }
            }
        },
        onSelectSplitwiseGroup = { groupId, selected ->
            container.setSplitwiseGroupSelected(groupId, selected)
            state = state.copy(
                settings = state.settings.copy(
                    splitwise = container.preferences.toSplitwiseSettingsUi(),
                ),
            )
        },
        onSyncSplitwise = {
            state = state.copy(isRefreshing = true, scanStatus = "Syncing Splitwise...")
            coroutineScope.launch(Dispatchers.IO) {
                val result = runCatching { container.refreshFromSplitwise() }
                val categories = container.categoryRepository.getCategories()
                val transactions = container.transactionRepository.getAll()
                withContext(Dispatchers.Main) {
                    state = loadStateFromRepositories(
                        categories = categories,
                        transactions = transactions,
                        monthlyBudget = container.preferences.monthlyBudgetPaise,
                        previousState = state,
                    ).copy(
                        isRefreshing = false,
                        settings = state.settings.copy(splitwise = container.preferences.toSplitwiseSettingsUi()),
                        scanStatus = if (result.isSuccess) {
                            "Splitwise sync complete. ${result.getOrThrow()} new expenses imported."
                        } else {
                            "Splitwise sync failed: ${result.exceptionOrNull().userFacingMessage()}"
                        },
                    )
                }
            }
        },
    )

    MochiMoneyTheme {
        MochiMoneyApp(
            state = state,
            actions = actions,
            modifier = modifier,
        )
    }
}

@Composable
fun MochiMoneyApp(
    state: MochiMoneyUiState,
    actions: MochiMoneyActions = MochiMoneyActions(),
    modifier: Modifier = Modifier,
) {
    if (!state.hasCompletedOnboarding) {
        OnboardingScreen(
            state = state,
            actions = actions,
            modifier = modifier,
        )
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useRail = maxWidth >= 720.dp
        if (useRail) {
            WideAppScaffold(state = state, actions = actions)
        } else {
            CompactAppScaffold(state = state, actions = actions)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactAppScaffold(
    state: MochiMoneyUiState,
    actions: MochiMoneyActions,
) {
    var selectedTab by rememberSaveable { mutableStateOf(MochiTab.Dashboard) }
    val navigationActions = actions.copy(
        onOpenCategorization = { selectedTab = MochiTab.Categorize },
    )

    Scaffold(
        topBar = { MochiTopBar(selectedTab) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                tonalElevation = 4.dp,
            ) {
                MochiTab.entries.forEach { tab ->
                    val selectTab = rememberMochiHapticClick(enabled = selectedTab != tab) {
                        selectedTab = tab
                    }
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = selectTab,
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            KawaiiBackdrop()
            MochiTabContent(
                tab = selectedTab,
                state = state,
                actions = navigationActions,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WideAppScaffold(
    state: MochiMoneyUiState,
    actions: MochiMoneyActions,
) {
    var selectedTab by rememberSaveable { mutableStateOf(MochiTab.Dashboard) }
    val navigationActions = actions.copy(
        onOpenCategorization = { selectedTab = MochiTab.Categorize },
    )

    Scaffold(
        topBar = { MochiTopBar(selectedTab) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            NavigationRail(
                modifier = Modifier.fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            ) {
                MochiTab.entries.forEach { tab ->
                    val selectTab = rememberMochiHapticClick(enabled = selectedTab != tab) {
                        selectedTab = tab
                    }
                    NavigationRailItem(
                        selected = selectedTab == tab,
                        onClick = selectTab,
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
            Box(Modifier.fillMaxSize()) {
                KawaiiBackdrop()
                MochiTabContent(
                    tab = selectedTab,
                    state = state,
                    actions = navigationActions,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MochiTopBar(tab: MochiTab) {
    TopAppBar(
        title = {
            Text(
                text = tab.label,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@Composable
private fun MochiTabContent(
    tab: MochiTab,
    state: MochiMoneyUiState,
    actions: MochiMoneyActions,
    modifier: Modifier = Modifier,
) {
    when (tab) {
        MochiTab.Dashboard -> DashboardScreen(state, actions, modifier)
        MochiTab.Transactions -> TransactionsScreen(state, actions, modifier)
        MochiTab.Categorize -> CategorizationScreen(state, actions, modifier)
        MochiTab.Settings -> SettingsScreen(state, actions, modifier)
    }
}

private enum class MochiTab(
    val label: String,
    val icon: ImageVector,
) {
    Dashboard("Dashboard", MochiIcons.Dashboard),
    Transactions("Transactions", MochiIcons.Transactions),
    Categorize("Categorize", MochiIcons.Categories),
    Settings("Settings", MochiIcons.Settings),
}

private fun loadStateFromRepositories(
    categories: List<TransactionCategory>,
    transactions: List<UpiTransaction>,
    monthlyBudget: Long,
    previousState: MochiMoneyUiState,
): MochiMoneyUiState {
    val today = LocalDate.now()
    val monthTransactions = transactions.filter {
        it.occurredOn.year == today.year && it.occurredOn.month == today.month
    }
    val uiCategories = categories.map { category ->
        val spent = monthTransactions
            .filter { it.categoryId == category.id && it.direction == TransactionDirection.DEBIT }
            .sumOf { it.amountPaise }
        CategoryUi(
            id = category.id,
            label = category.name,
            kind = category.kind(),
            spentPaise = spent,
            budgetPaise = estimatedBudgetFor(category.id),
            keywordsCsv = category.keywords.joinToString("\n"),
        )
    }

    val zone = java.time.ZoneId.systemDefault()
    val uiTransactions = transactions.map { transaction ->
        val counterpartyLabel = transaction.counterparty?.takeIf { it.isNotBlank() }
        val fallbackTitle = if (transaction.direction == TransactionDirection.CREDIT) "Received" else "Sent"
        val derivedTime = transaction.smsReceivedAtMillis
            ?.let { java.time.Instant.ofEpochMilli(it).atZone(zone) }
            ?.takeIf { it.toLocalDate() == transaction.occurredOn }
            ?.toLocalTime()
        UpiTransactionUi(
            id = transaction.id.toString(),
            domainId = transaction.id,
            title = counterpartyLabel ?: fallbackTitle,
            subtitle = if (transaction.direction == TransactionDirection.CREDIT) "Incoming" else "Outgoing",
            amountPaise = transaction.amountPaise,
            occurredOn = transaction.occurredOn,
            occurredTime = derivedTime,
            counterparty = transaction.counterparty,
            categoryId = transaction.categoryId,
            smsBody = transaction.smsBody,
            isIncoming = transaction.direction == TransactionDirection.CREDIT,
        )
    }

    val spent = monthTransactions
        .filter { it.direction == TransactionDirection.DEBIT }
        .sumOf { it.amountPaise }
    val incoming = monthTransactions
        .filter { it.direction == TransactionDirection.CREDIT }
        .sumOf { it.amountPaise }
    return previousState.copy(
        totalSpentThisMonth = spent,
        monthlyBudget = monthlyBudget,
        incomingThisMonth = incoming,
        uncategorizedCount = uiTransactions.count { it.categoryId == null || it.categoryId == DefaultCategoryIds.UNCATEGORIZED },
        transactions = uiTransactions,
        categories = uiCategories,
    )
}

private fun TransactionCategory.kind(): CategoryKind = when (id) {
    DefaultCategoryIds.FOOD -> CategoryKind.Food
    DefaultCategoryIds.GROCERIES -> CategoryKind.Groceries
    DefaultCategoryIds.HEALTH -> CategoryKind.Health
    DefaultCategoryIds.TRANSPORT -> CategoryKind.Travel
    DefaultCategoryIds.BILLS -> CategoryKind.Bills
    DefaultCategoryIds.SHOPPING -> CategoryKind.Shopping
    DefaultCategoryIds.INCOME -> CategoryKind.Salary
    DefaultCategoryIds.TRANSFERS -> CategoryKind.Savings
    else -> CategoryKind.Other
}

private fun estimatedBudgetFor(categoryId: String): Long? = when (categoryId) {
    DefaultCategoryIds.FOOD -> 700_000L
    DefaultCategoryIds.GROCERIES -> 450_000L
    DefaultCategoryIds.HEALTH -> 250_000L
    DefaultCategoryIds.TRANSPORT -> 300_000L
    DefaultCategoryIds.BILLS -> 650_000L
    DefaultCategoryIds.SHOPPING -> 500_000L
    DefaultCategoryIds.TRANSFERS -> 300_000L
    else -> null
}

private fun AppPreferences.toSplitwiseSettingsUi(): SplitwiseSettingsUi =
    SplitwiseSettingsUi(
        enabled = splitwiseEnabled,
        apiKeyConfigured = splitwiseApiKey.isNotBlank(),
        currentUserLabel = splitwiseCurrentUserName,
        groups = splitwiseGroups.map { it.toUi() },
        selectedGroupIds = splitwiseSelectedGroupIds,
    )

private fun SplitwiseGroup.toUi(): SplitwiseGroupUi =
    SplitwiseGroupUi(id = id, name = name)

private fun Throwable?.userFacingMessage(): String =
    this?.message?.takeIf { it.isNotBlank() } ?: "Unknown error."
