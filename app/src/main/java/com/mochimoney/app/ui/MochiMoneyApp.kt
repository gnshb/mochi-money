package com.mochimoney.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.withStyle
import com.mochimoney.app.data.AppContainer
import com.mochimoney.app.data.AppPreferences
import com.mochimoney.app.data.llm.LlmModelCatalog
import com.mochimoney.app.data.splitwise.SplitwiseGroup
import com.mochimoney.app.domain.model.DefaultCategoryIds
import com.mochimoney.app.domain.model.LlmBackend
import com.mochimoney.app.domain.model.LlmModelStatus
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

private const val MinRefreshMillis = 1500L
private const val StatusVisibleMillis = 1500L

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
                    llm = container.toLlmSettingsUi(),
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

    // Gemini Nano availability is transient (probed at runtime), so it lives outside the
    // persisted prefs. currentLlmUi() rebuilds the LLM settings while preserving the probe result.
    var geminiNanoStatus by remember { mutableStateOf(GeminiNanoStatusUi.Unknown) }
    val currentLlmUi = { container.toLlmSettingsUi().copy(geminiNanoStatus = geminiNanoStatus) }
    val probeGeminiNano: (Boolean) -> Unit = { commit ->
        geminiNanoStatus = GeminiNanoStatusUi.Checking
        state = state.copy(settings = state.settings.copy(llm = currentLlmUi()))
        coroutineScope.launch(Dispatchers.IO) {
            val available = runCatching { container.checkGeminiNano() }.getOrDefault(false)
            if (commit && available) container.setLlmBackend(LlmBackend.GeminiNano)
            withContext(Dispatchers.Main) {
                geminiNanoStatus = if (available) GeminiNanoStatusUi.Available else GeminiNanoStatusUi.Unavailable
                state = state.copy(
                    settings = state.settings.copy(llm = currentLlmUi()),
                    scanStatus = when {
                        available && commit -> "Using Gemini Nano."
                        available -> "Gemini Nano is ready on this device."
                        else -> "Gemini Nano isn't available here (needs Google AICore access). Use a downloaded model instead."
                    },
                )
            }
        }
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
                settings = state.settings.copy(
                    splitwise = container.preferences.toSplitwiseSettingsUi(),
                    llm = currentLlmUi(),
                ),
            )
        }
        // Hold the refreshing state briefly so the mochi pulse is visible
        // even when the scan completes in a few hundred ms.
        val elapsed = System.currentTimeMillis() - startMillis
        if (state.isRefreshing && elapsed < MinRefreshMillis) {
            kotlinx.coroutines.delay(MinRefreshMillis - elapsed)
        }
        state = loadedState
    }

    LaunchedEffect(state.scanStatus, state.inferBusy) {
        val status = state.scanStatus ?: return@LaunchedEffect
        if (state.inferBusy) return@LaunchedEffect // keep the popup pinned until detection finishes
        kotlinx.coroutines.delay(StatusVisibleMillis)
        if (state.scanStatus == status && !state.inferBusy) {
            state = state.copy(scanStatus = null)
        }
    }

    // When AI is enabled on a device that could support Gemini Nano, probe once so Settings can
    // show a clear "available / not available" status instead of leaving the choice ambiguous.
    LaunchedEffect(state.settings.llm.enabled, state.settings.llm.geminiNanoSupported) {
        if (state.settings.llm.enabled &&
            state.settings.llm.geminiNanoSupported &&
            geminiNanoStatus == GeminiNanoStatusUi.Unknown
        ) {
            probeGeminiNano(false)
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
        onToggleLlm = { enabled ->
            container.setLlmEnabled(enabled)
            state = state.copy(
                settings = state.settings.copy(llm = currentLlmUi()),
                scanStatus = if (enabled) "LLM counterparty detection on." else "LLM counterparty detection off.",
            )
        },
        onSelectLlmBackend = { backend ->
            when (backend) {
                LlmBackendUi.MediaPipe -> {
                    container.setLlmBackend(LlmBackend.MediaPipe)
                    state = state.copy(settings = state.settings.copy(llm = currentLlmUi()))
                }
                // AICore being present doesn't guarantee Nano can run; probe and commit if it works.
                LlmBackendUi.GeminiNano -> probeGeminiNano(true)
            }
        },
        onCheckGeminiNano = { probeGeminiNano(false) },
        onSetHuggingFaceToken = { token ->
            container.setHuggingFaceToken(token)
            state = state.copy(
                settings = state.settings.copy(llm = currentLlmUi()),
                scanStatus = if (token.isBlank()) "Hugging Face token cleared." else "Hugging Face token saved.",
            )
        },
        onActivateLlmModel = { modelId ->
            container.setActiveLlmModel(modelId)
            state = state.copy(settings = state.settings.copy(llm = currentLlmUi()))
        },
        onDeleteLlmModel = { modelId ->
            LlmModelCatalog.byId(modelId)?.let { model ->
                container.deleteLlmModel(model)
                state = state.copy(
                    settings = state.settings.copy(llm = currentLlmUi()),
                    scanStatus = "Removed ${model.displayName}.",
                )
            }
        },
        onDownloadLlmModel = { modelId ->
            LlmModelCatalog.byId(modelId)?.let { model ->
                state = state.copy(settings = state.settings.copy(llm = state.settings.llm.withProgress(modelId, 0f)))
                coroutineScope.launch(Dispatchers.IO) {
                    val result = runCatching {
                        container.downloadLlmModel(model) { progress ->
                            coroutineScope.launch(Dispatchers.Main) {
                                state = state.copy(
                                    settings = state.settings.copy(llm = state.settings.llm.withProgress(modelId, progress)),
                                )
                            }
                        }
                    }
                    withContext(Dispatchers.Main) {
                        state = if (result.isSuccess) {
                            state.copy(
                                settings = state.settings.copy(llm = currentLlmUi()),
                                scanStatus = "Downloaded ${model.displayName}.",
                            )
                        } else {
                            // Surface failures in a manually-dismissed dialog (with any links intact).
                            state.copy(
                                settings = state.settings.copy(llm = currentLlmUi()),
                                downloadError = "Couldn't download ${model.displayName}.\n\n${result.exceptionOrNull().userFacingMessage()}",
                            )
                        }
                    }
                }
            }
        },
        onInferCounterparty = { transactionId ->
            val domainId = state.transactions.firstOrNull { it.id == transactionId }?.domainId
            val reason = container.llmUnavailableReason()
            when {
                domainId == null -> state = state.copy(scanStatus = "Can't analyze this transaction.")
                reason != null -> state = state.copy(scanStatus = reason)
                else -> {
                    state = state.copy(scanStatus = "Detecting counterparty…", inferBusy = true)
                    coroutineScope.launch(Dispatchers.IO) {
                        val result = runCatching { container.inferCounterparty(domainId) }
                        val categories = container.categoryRepository.getCategories()
                        val transactions = container.transactionRepository.getAll()
                        withContext(Dispatchers.Main) {
                            state = loadStateFromRepositories(
                                categories = categories,
                                transactions = transactions,
                                monthlyBudget = container.preferences.monthlyBudgetPaise,
                                previousState = state,
                            ).copy(
                                inferBusy = false,
                                scanStatus = result.fold(
                                    onSuccess = { r ->
                                        if (r.changed) "Counterparty set to ${r.counterparty}." else "No clearer counterparty found."
                                    },
                                    onFailure = { "LLM detection failed: ${it.userFacingMessage()}" },
                                ),
                            )
                        }
                    }
                }
            }
        },
        onInferAllCounterparties = {
            val reason = container.llmUnavailableReason()
            if (reason != null) {
                state = state.copy(scanStatus = reason)
            } else {
                state = state.copy(scanStatus = "Running LLM on all transactions…", inferProgress = 0f, inferBusy = true)
                coroutineScope.launch(Dispatchers.IO) {
                    val result = runCatching {
                        container.inferAllCounterparties { done, total ->
                            val fraction = if (total > 0) done.toFloat() / total else 1f
                            coroutineScope.launch(Dispatchers.Main) {
                                state = state.copy(
                                    inferProgress = fraction,
                                    scanStatus = "Analyzing $done of $total…",
                                )
                            }
                        }
                    }
                    val categories = container.categoryRepository.getCategories()
                    val transactions = container.transactionRepository.getAll()
                    withContext(Dispatchers.Main) {
                        state = loadStateFromRepositories(
                            categories = categories,
                            transactions = transactions,
                            monthlyBudget = container.preferences.monthlyBudgetPaise,
                            previousState = state,
                        ).copy(
                            inferProgress = null,
                            inferBusy = false,
                            scanStatus = result.fold(
                                onSuccess = { changed -> "LLM updated $changed transaction${if (changed == 1) "" else "s"}." },
                                onFailure = { "LLM run failed: ${it.userFacingMessage()}" },
                            ),
                        )
                    }
                }
            }
        },
        onRenameCounterparty = { transactionId, name ->
            val domainId = state.transactions.firstOrNull { it.id == transactionId }?.domainId
            if (domainId == null || name.isBlank()) {
                state = state.copy(scanStatus = "Enter a name first.")
            } else {
                coroutineScope.launch(Dispatchers.IO) {
                    val ok = runCatching { container.renameCounterparty(domainId, name) }.getOrDefault(false)
                    val categories = container.categoryRepository.getCategories()
                    val transactions = container.transactionRepository.getAll()
                    withContext(Dispatchers.Main) {
                        state = loadStateFromRepositories(
                            categories = categories,
                            transactions = transactions,
                            monthlyBudget = container.preferences.monthlyBudgetPaise,
                            previousState = state,
                        ).copy(
                            scanStatus = if (ok) "Renamed — saved for similar payments too." else "Couldn't rename.",
                        )
                    }
                }
            }
        },
        onDismissDownloadError = { state = state.copy(downloadError = null) },
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

    state.downloadError?.let { message ->
        ErrorDialog(message = message, onDismiss = actions.onDismissDownloadError)
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

@Composable
private fun SpendingHistoryDialog(state: MochiMoneyUiState, onDismiss: () -> Unit) {
    val budget = state.monthlyBudget
    val current = java.time.YearMonth.now()
    val rows = (0..2).map { back ->
        val ym = current.minusMonths(back.toLong())
        val txns = state.transactions.filter {
            it.occurredOn.year == ym.year && it.occurredOn.monthValue == ym.monthValue
        }
        val spent = txns.filterNot { it.isIncoming }.sumOf { it.amountPaise }
        val received = txns.filter { it.isIncoming }.sumOf { it.amountPaise }
        Triple(ym, spent, received)
    }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Last 3 months", style = MaterialTheme.typography.titleLarge) },
        text = {
            androidx.compose.foundation.layout.Column(
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
            ) {
                rows.forEach { (ym, spent, received) ->
                    val label = ym.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault()) +
                        " " + ym.year
                    val pace = if (budget > 0) (spent.toFloat() / budget).coerceIn(0f, 1f) else 0f
                    androidx.compose.foundation.layout.Column(
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                        ) {
                            Text(label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (budget > 0) {
                                    "${com.mochimoney.app.ui.components.formatCurrency(spent)} / ${com.mochimoney.app.ui.components.formatCurrency(budget)}"
                                } else {
                                    com.mochimoney.app.ui.components.formatCurrency(spent)
                                },
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                        if (budget > 0) {
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = { pace },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Text(
                            "Received ${com.mochimoney.app.ui.components.formatCurrency(received)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun ErrorDialog(message: String, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Download failed", style = MaterialTheme.typography.titleLarge) },
        text = { LinkifiedText(message) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

/** Renders text with any http(s) URLs as clickable links. */
@Composable
private fun LinkifiedText(text: String) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val urlRegex = Regex("https?://\\S+")
    val annotated = androidx.compose.ui.text.buildAnnotatedString {
        var last = 0
        urlRegex.findAll(text).forEach { match ->
            append(text.substring(last, match.range.first))
            val url = match.value.trimEnd('.', ',', ')', ';')
            pushStringAnnotation(tag = "url", annotation = url)
            withStyle(
                androidx.compose.ui.text.SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                ),
            ) { append(url) }
            pop()
            last = match.range.first + url.length
        }
        if (last < text.length) append(text.substring(last))
    }
    androidx.compose.foundation.text.ClickableText(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
    ) { offset ->
        annotated.getStringAnnotations(tag = "url", start = offset, end = offset)
            .firstOrNull()?.let { uriHandler.openUri(it.item) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactAppScaffold(
    state: MochiMoneyUiState,
    actions: MochiMoneyActions,
) {
    var selectedTab by rememberSaveable { mutableStateOf(MochiTab.Dashboard) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    val navigationActions = actions.copy(
        onOpenCategorization = { selectedTab = MochiTab.Categorize },
    )
    if (showHistory) SpendingHistoryDialog(state = state, onDismiss = { showHistory = false })

    Scaffold(
        topBar = {
            MochiTopBar(
                tab = selectedTab,
                showAiAction = selectedTab == MochiTab.Transactions && state.settings.llm.enabled,
                onRunAi = actions.onInferAllCounterparties,
                showHistoryAction = selectedTab == MochiTab.Transactions,
                onHistory = { showHistory = true },
            )
        },
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
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            KawaiiBackdrop()
            MochiTabContent(
                tab = selectedTab,
                state = state,
                actions = navigationActions,
                modifier = Modifier.fillMaxSize(),
            )
            ScanStatusPopup(
                message = state.scanStatus,
                progress = state.inferProgress,
                busy = state.inferBusy,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
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
    var showHistory by rememberSaveable { mutableStateOf(false) }
    val navigationActions = actions.copy(
        onOpenCategorization = { selectedTab = MochiTab.Categorize },
    )
    if (showHistory) SpendingHistoryDialog(state = state, onDismiss = { showHistory = false })

    Scaffold(
        topBar = {
            MochiTopBar(
                tab = selectedTab,
                showAiAction = selectedTab == MochiTab.Transactions && state.settings.llm.enabled,
                onRunAi = actions.onInferAllCounterparties,
                showHistoryAction = selectedTab == MochiTab.Transactions,
                onHistory = { showHistory = true },
            )
        },
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
                ScanStatusPopup(
                    message = state.scanStatus,
                    progress = state.inferProgress,
                    busy = state.inferBusy,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun ScanStatusPopup(
    message: String?,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    busy: Boolean = false,
) {
    AnimatedVisibility(
        visible = message != null || busy,
        modifier = modifier,
    ) {
        if (message != null || busy) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.96f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    if (message != null) Text(text = message, style = MaterialTheme.typography.bodyMedium)
                    if (busy || progress != null) {
                        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 6.dp))
                        if (progress != null) {
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            // Single detection has no determinate progress — show an indeterminate bar.
                            androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MochiTopBar(
    tab: MochiTab,
    showAiAction: Boolean = false,
    onRunAi: () -> Unit = {},
    showHistoryAction: Boolean = false,
    onHistory: () -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(
                text = tab.label,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        actions = {
            if (showHistoryAction) {
                androidx.compose.material3.IconButton(
                    onClick = com.mochimoney.app.ui.components.rememberMochiHapticClick(onClick = onHistory),
                ) {
                    Icon(
                        com.mochimoney.app.ui.components.MochiIcons.History,
                        contentDescription = "Spending history",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (showAiAction) {
                androidx.compose.material3.IconButton(
                    onClick = com.mochimoney.app.ui.components.rememberMochiHapticClick(onClick = onRunAi),
                ) {
                    Icon(
                        com.mochimoney.app.ui.components.MochiIcons.Ai,
                        contentDescription = "Detect all counterparties with LLM",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
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
            ?.let { com.mochimoney.app.data.llm.titleCaseCounterparty(it) }
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

private fun AppContainer.toLlmSettingsUi(): LlmSettingsUi =
    LlmSettingsUi(
        enabled = preferences.llmCounterpartyEnabled,
        geminiNanoSupported = geminiNanoSupported(),
        backend = when (preferences.llmBackend) {
            LlmBackend.GeminiNano -> LlmBackendUi.GeminiNano
            LlmBackend.MediaPipe -> LlmBackendUi.MediaPipe
        },
        models = llmModelStatuses().map { it.toUi() },
        hfTokenConfigured = preferences.huggingFaceToken.isNotBlank(),
    )

private fun LlmModelStatus.toUi(): LlmModelUi =
    LlmModelUi(
        id = model.id,
        displayName = model.displayName,
        description = model.description,
        sizeLabel = formatModelSize(model.approxSizeBytes),
        license = model.license,
        requiresLicenseAcceptance = model.requiresLicenseAcceptance,
        isInstalled = isInstalled,
        isActive = isActive,
    )

private fun formatModelSize(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) {
        String.format(java.util.Locale.US, "%.1f GB", mb / 1024)
    } else {
        String.format(java.util.Locale.US, "%.0f MB", mb)
    }
}

private fun Throwable?.userFacingMessage(): String =
    this?.message?.takeIf { it.isNotBlank() } ?: "Unknown error."
