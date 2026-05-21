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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.mochimoney.app.ui.MochiMoneyActions
import com.mochimoney.app.ui.MochiMoneyUiState
import com.mochimoney.app.ui.components.CategoryIconBubble
import com.mochimoney.app.ui.components.MochiCardShape
import com.mochimoney.app.ui.components.MochiIcons
import com.mochimoney.app.ui.components.SectionTitle
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
            SectionTitle("Settings")
        }
        item {
            PermissionCard(state = state, actions = actions)
        }
        item {
            BudgetCard(state = state, actions = actions)
        }
        item {
            SenderFilterCard(state = state, actions = actions)
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
                Text("SMS permission", style = MaterialTheme.typography.titleLarge)
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
