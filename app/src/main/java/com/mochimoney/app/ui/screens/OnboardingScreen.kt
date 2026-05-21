package com.mochimoney.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mochimoney.app.ui.MochiMoneyActions
import com.mochimoney.app.ui.MochiMoneyUiState
import com.mochimoney.app.ui.components.CategoryIconBubble
import com.mochimoney.app.ui.components.KawaiiBackdrop
import com.mochimoney.app.ui.components.MochiCardShape
import com.mochimoney.app.ui.components.MochiIcons
import com.mochimoney.app.ui.components.MochiMascot
import com.mochimoney.app.ui.components.MochiMood
import com.mochimoney.app.ui.components.rememberMochiHapticClick
import com.mochimoney.app.ui.theme.MochiMint
import com.mochimoney.app.ui.theme.MochiRose
import com.mochimoney.app.ui.theme.MochiSky

@Composable
fun OnboardingScreen(
    state: MochiMoneyUiState,
    actions: MochiMoneyActions,
    modifier: Modifier = Modifier,
) {
    val completeOnboarding = rememberMochiHapticClick(onClick = actions.onCompleteOnboarding)
    val requestSmsPermission = rememberMochiHapticClick(onClick = actions.onRequestSmsPermission)

    Box(modifier = modifier.fillMaxSize()) {
        KawaiiBackdrop()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                MochiMascot(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(156.dp),
                    size = 156.dp,
                    mood = MochiMood.Happy,
                )
                Text("Mochi Money", style = MaterialTheme.typography.displaySmall)
                Text(
                    "A calm SMS tracker for spending, categories, and monthly budgets.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PermissionExplainerCard(
                    granted = state.smsPermissionGranted,
                    onRequestPermission = requestSmsPermission,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = completeOnboarding,
                ) {
                    Text(if (state.smsPermissionGranted) "Start tracking" else "Continue without SMS")
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = requestSmsPermission,
                ) {
                    Text(if (state.smsPermissionGranted) "Permission ready" else "Allow SMS import")
                }
            }
        }
    }
}

@Composable
private fun PermissionExplainerCard(
    granted: Boolean,
    onRequestPermission: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MochiCardShape,
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryIconBubble(
                    icon = MochiIcons.Transactions,
                    color = if (granted) MochiMint else MochiRose,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("SMS access", style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (granted) "SMS import is available." else "Import needs SMS permission.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    if (granted) "Ready" else "Needed",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (granted) MochiMint else MochiRose,
                    fontWeight = FontWeight.Black,
                )
            }
            PermissionPoint(
                title = "Private by design",
                body = "The UI contract leaves parsing and storage to the app layer.",
                color = MochiSky,
            )
            PermissionPoint(
                title = "Generic categories",
                body = "Payments use neutral labels and icons.",
                color = MochiMint,
            )
            Button(onClick = onRequestPermission, enabled = !granted) {
                Text(if (granted) "SMS allowed" else "Grant SMS permission")
            }
        }
    }
}

@Composable
private fun PermissionPoint(
    title: String,
    body: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        CategoryIconBubble(icon = MochiIcons.Categories, color = color)
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
