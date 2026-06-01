package com.mochimoney.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mochimoney.app.ui.GeminiNanoStatusUi
import com.mochimoney.app.ui.LlmBackendUi
import com.mochimoney.app.ui.LlmModelUi
import com.mochimoney.app.ui.LlmSettingsUi
import com.mochimoney.app.ui.MochiMoneyActions
import com.mochimoney.app.ui.MochiMoneyUiState
import com.mochimoney.app.ui.components.CategoryIconBubble
import com.mochimoney.app.ui.components.MochiCardShape
import com.mochimoney.app.ui.components.MochiIcons
import com.mochimoney.app.ui.components.formatCurrency
import com.mochimoney.app.ui.components.rememberMochiHapticClick
import com.mochimoney.app.ui.theme.MochiMint
import com.mochimoney.app.ui.theme.MochiRose
import com.mochimoney.app.ui.theme.MochiSky

@Composable
fun SettingsScreen(
    state: MochiMoneyUiState,
    actions: MochiMoneyActions,
    modifier: Modifier = Modifier,
) {
    com.mochimoney.app.ui.components.DismissFocusBox(modifier = modifier.fillMaxSize()) {
        SettingsContent(state = state, actions = actions)
    }
}

@Composable
private fun SettingsContent(
    state: MochiMoneyUiState,
    actions: MochiMoneyActions,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SplitwiseCard(state = state, actions = actions)
        }
        item {
            AiCounterpartyCard(state = state, actions = actions)
        }
        item {
            SettingsGroup {
                SettingsSwitchRow(
                    title = "Scan SMS on open",
                    subtitle = "Refresh from your SMS inbox when you open the app.",
                    checked = state.settings.scanSmsAutomatically,
                    onCheckedChange = actions.onToggleAutoScan,
                )
                SettingsSwitchRow(
                    title = "Monthly budget alerts",
                    subtitle = "Warn me as I approach my monthly budget.",
                    checked = state.settings.budgetAlerts,
                    onCheckedChange = actions.onToggleBudgetAlerts,
                )
            }
        }
        item {
            PermissionCard(state = state, actions = actions)
        }
        item {
            DataAssumptionsCard()
        }
        item {
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun BudgetCard(
    state: MochiMoneyUiState,
    actions: MochiMoneyActions,
) {
    var budgetText by rememberSaveable(state.monthlyBudget) {
        mutableStateOf((state.monthlyBudget / 100).takeIf { it > 0L }?.toString().orEmpty())
    }
    var saved by rememberSaveable { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MochiCardShape,
    ) {
        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
        val saveBudget = rememberMochiHapticClick {
            focusManager.clearFocus()
            actions.onSetMonthlyBudget((budgetText.toLongOrNull() ?: 0L) * 100L)
            saved = true
        }
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Monthly budget", style = MaterialTheme.typography.titleLarge)
            Text(
                "Current budget: ${formatCurrency(state.monthlyBudget)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = budgetText,
                onValueChange = {
                    budgetText = it.filter(Char::isDigit)
                    saved = false
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Budget in INR") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Button(
                onClick = saveBudget,
            ) {
                Text(if (saved) "Saved" else "Save budget")
            }
            AnimatedVisibility(visible = saved) {
                Text(
                    "Budget saved for this month.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MochiMint,
                )
            }
        }
    }
}

@Composable
private fun SenderFilterCard(
    state: MochiMoneyUiState,
    actions: MochiMoneyActions,
) {
    var pattern by rememberSaveable(state.senderPattern) { mutableStateOf(state.senderPattern) }
    var justSaved by rememberSaveable { mutableStateOf(false) }
    val invalid = pattern.isNotBlank() && runCatching { Regex(pattern) }.isFailure
    val dirty = pattern.trim() != state.senderPattern

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MochiCardShape,
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Sender filter", style = MaterialTheme.typography.titleLarge)
            Text(
                "Regex matched against the SMS sender shortcode. When set, only matching senders are scanned. Leave blank to scan every SMS.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = pattern,
                onValueChange = {
                    pattern = it
                    justSaved = false
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Sender pattern") },
                placeholder = { Text("any regex, e.g. SBI|HDFC|ICICI") },
                isError = invalid,
                supportingText = if (invalid) {
                    { Text("Invalid regex", color = MaterialTheme.colorScheme.error) }
                } else null,
            )
            val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = rememberMochiHapticClick(enabled = !invalid) {
                        focusManager.clearFocus()
                        actions.onSetSenderPattern(pattern.trim())
                        justSaved = true
                    },
                    enabled = !invalid && (dirty || !justSaved),
                ) {
                    Text(if (justSaved && !dirty) "Saved ✓" else "Save filter")
                }
            }
            ActiveFiltersList(
                pattern = state.senderPattern,
                onRemove = {
                    pattern = ""
                    justSaved = false
                    actions.onSetSenderPattern("")
                },
            )
        }
    }
}

@Composable
private fun ActiveFiltersList(pattern: String, onRemove: () -> Unit) {
    if (pattern.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Active",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        androidx.compose.material3.AssistChip(
            onClick = rememberMochiHapticClick(onClick = onRemove),
            label = { Text(pattern) },
            trailingIcon = { Text("✕", style = MaterialTheme.typography.labelLarge) },
        )
    }
}

@Composable
private fun SplitwiseCard(
    state: MochiMoneyUiState,
    actions: MochiMoneyActions,
) {
    val splitwise = state.settings.splitwise
    var apiKey by rememberSaveable(splitwise.apiKeyConfigured) { mutableStateOf("") }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val connectEnabled = apiKey.isNotBlank() || splitwise.apiKeyConfigured

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MochiCardShape,
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CategoryIconBubble(
                    icon = MochiIcons.Settings,
                    color = if (splitwise.enabled) MochiMint else MaterialTheme.colorScheme.outline,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Splitwise", style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (splitwise.apiKeyConfigured) {
                            "Connected${splitwise.currentUserLabel.takeIf { it.isNotBlank() }?.let { " as $it" }.orEmpty()}."
                        } else {
                            "Import your net share from selected groups."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = splitwise.enabled,
                    onCheckedChange = actions.onToggleSplitwise,
                )
            }

            AnimatedVisibility(visible = splitwise.enabled) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(if (splitwise.apiKeyConfigured) "New API key" else "API key") },
                        placeholder = { Text(if (splitwise.apiKeyConfigured) "Leave blank to reconnect with saved key" else "Splitwise personal API key") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = rememberMochiHapticClick(enabled = connectEnabled) {
                                focusManager.clearFocus()
                                actions.onConnectSplitwise(apiKey)
                                apiKey = ""
                            },
                            enabled = connectEnabled,
                        ) {
                            Text(if (splitwise.apiKeyConfigured) "Reconnect" else "Connect")
                        }
                        OutlinedButton(
                            onClick = rememberMochiHapticClick(
                                enabled = splitwise.apiKeyConfigured && splitwise.selectedGroupIds.isNotEmpty(),
                                onClick = actions.onSyncSplitwise,
                            ),
                            enabled = splitwise.apiKeyConfigured && splitwise.selectedGroupIds.isNotEmpty(),
                        ) {
                            Text("Sync")
                        }
                    }

                    if (splitwise.groups.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Groups",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            splitwise.groups.forEach { group ->
                                SplitwiseGroupRow(
                                    name = group.name,
                                    selected = group.id in splitwise.selectedGroupIds,
                                    onSelectedChange = { selected ->
                                        actions.onSelectSplitwiseGroup(group.id, selected)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SplitwiseGroupRow(
    name: String,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = onSelectedChange,
        )
        Text(
            text = name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AiCounterpartyCard(
    state: MochiMoneyUiState,
    actions: MochiMoneyActions,
) {
    val llm = state.settings.llm
    var showManage by rememberSaveable { mutableStateOf(false) }
    val activeModel = llm.models.firstOrNull { it.isActive }
    val usingNano = llm.backend == LlmBackendUi.GeminiNano && llm.geminiNanoStatus == GeminiNanoStatusUi.Available

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MochiCardShape,
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CategoryIconBubble(
                    icon = MochiIcons.Ai,
                    color = if (llm.enabled) MochiSky else MaterialTheme.colorScheme.outline,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("LLM counterparty detection", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Detect who a payment was with, on-device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = llm.enabled, onCheckedChange = actions.onToggleLlm)
            }

            AnimatedVisibility(visible = llm.enabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        when {
                            usingNano -> "Using Gemini Nano"
                            activeModel != null -> "Using ${activeModel.displayName}"
                            else -> "No model chosen yet"
                        },
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = rememberMochiHapticClick { showManage = true }) { Text("Manage") }
                }
            }
        }
    }

    if (showManage) {
        LlmManageDialog(llm = llm, actions = actions, onDismiss = { showManage = false })
    }
}

@Composable
private fun LlmManageDialog(
    llm: LlmSettingsUi,
    actions: MochiMoneyActions,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("On-device LLM", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (llm.geminiNanoSupported) {
                    Text("Engine", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val nanoStatus = when (llm.geminiNanoStatus) {
                        GeminiNanoStatusUi.Checking -> "Checking…"
                        GeminiNanoStatusUi.Available -> "✓ Ready on this device."
                        GeminiNanoStatusUi.Unavailable -> "✕ Not available — needs AICore access."
                        GeminiNanoStatusUi.Unknown -> "Tap Check to test."
                    }
                    val nanoColor = when (llm.geminiNanoStatus) {
                        GeminiNanoStatusUi.Available -> MochiMint
                        GeminiNanoStatusUi.Unavailable -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    BackendRow(
                        title = "Gemini Nano",
                        subtitle = nanoStatus,
                        subtitleColor = nanoColor,
                        selected = llm.backend == LlmBackendUi.GeminiNano,
                        onSelect = { actions.onSelectLlmBackend(LlmBackendUi.GeminiNano) },
                        trailing = {
                            if (llm.geminiNanoStatus != GeminiNanoStatusUi.Checking) {
                                TextButton(onClick = rememberMochiHapticClick(onClick = actions.onCheckGeminiNano)) { Text("Check") }
                            }
                        },
                    )
                    BackendRow(
                        title = "Downloaded model",
                        subtitle = "Pick one below.",
                        selected = llm.backend == LlmBackendUi.MediaPipe,
                        onSelect = { actions.onSelectLlmBackend(LlmBackendUi.MediaPipe) },
                    )
                }

                Text("Models", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                llm.models.forEach { model ->
                    LlmModelRow(
                        model = model,
                        backendSelectable = !llm.geminiNanoSupported || llm.backend == LlmBackendUi.MediaPipe,
                        onDownload = { actions.onDownloadLlmModel(model.id) },
                        onDelete = { actions.onDeleteLlmModel(model.id) },
                        onActivate = { actions.onActivateLlmModel(model.id) },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun BackendRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onSelect: () -> Unit,
    subtitleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = selected, onClick = rememberMochiHapticClick(onClick = onSelect))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = subtitleColor,
            )
        }
        trailing?.invoke()
    }
}

@Composable
private fun LlmModelRow(
    model: LlmModelUi,
    backendSelectable: Boolean,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onActivate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${model.displayName} · ${model.sizeLabel}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    model.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (model.requiresLicenseAcceptance) {
                    Text(
                        "License: ${model.license} — may require accepting terms on the host before download.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (model.isActive) {
                    Text(
                        "Active model",
                        style = MaterialTheme.typography.labelMedium,
                        color = MochiMint,
                    )
                }
            }
        }

        val progress = model.downloadProgress
        when {
            progress != null -> {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Downloading… ${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            model.isInstalled -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (backendSelectable && !model.isActive) {
                    Button(onClick = rememberMochiHapticClick(onClick = onActivate)) { Text("Use") }
                }
                OutlinedButton(onClick = rememberMochiHapticClick(onClick = onDelete)) { Text("Remove") }
            }
            else -> Button(onClick = rememberMochiHapticClick(onClick = onDownload)) {
                Text("Download")
            }
        }
    }
}

@Composable
private fun DataAssumptionsCard() {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MochiCardShape,
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Note", style = MaterialTheme.typography.titleLarge)
            Text(
                "Mochi Money runs completely offline. No telemetry/analytics: all SMS parsing and storage stays on your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Open source under GPL v3!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            androidx.compose.material3.TextButton(
                onClick = rememberMochiHapticClick {
                    uriHandler.openUri("https://github.com/gnshb/mochi-money")
                },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            ) {
                Text("github.com/gnshb/mochi-money")
            }
        }
    }
}

@Composable
private fun PermissionCard(
    state: MochiMoneyUiState,
    actions: MochiMoneyActions,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MochiCardShape,
    ) {
        val requestPermission = rememberMochiHapticClick(onClick = actions.onRequestSmsPermission)
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CategoryIconBubble(
                icon = MochiIcons.Settings,
                color = if (state.smsPermissionGranted) MochiMint else MochiRose,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("SMS permission", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (state.smsPermissionGranted) {
                        "Ready to read your SMS inbox."
                    } else {
                        "Needed to import payment alerts from SMS."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = requestPermission) {
                Text(if (state.smsPermissionGranted) "Review" else "Allow")
            }
        }
    }
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MochiCardShape,
    ) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CategoryIconBubble(
            icon = MochiIcons.Settings,
            color = if (checked) MochiSky else MaterialTheme.colorScheme.outline,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCheckedChange(it)
            },
        )
    }
}
