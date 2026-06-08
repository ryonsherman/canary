package com.canary.ui.screens

import android.app.Application
import android.nfc.NfcAdapter
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.canary.MainActivity
import com.canary.data.PreferencesManager
import com.canary.service.CryptoService
import com.canary.service.NfcService
import com.canary.ui.theme.*
import com.canary.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    nfcService: NfcService,
    cryptoService: CryptoService,
    prefsManager: PreferencesManager,
    onNavigateToChain: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel = remember { HomeViewModel(context.applicationContext as Application, prefsManager, cryptoService) }
    var nfcDetected by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshChain()
    }

    LaunchedEffect(Unit) {
        MainActivity.nfcIntentHandler = { intent ->
            val tagHash = nfcService.readTagHash(intent)
            if (tagHash != null) {
                nfcDetected = true
                val result = viewModel.signAndPushCanary()
                isSuccess = result.isSuccess
                statusMessage = if (result.isSuccess) {
                    "Canary pushed for ${result.getOrNull()}"
                } else {
                    "Failed: ${result.exceptionOrNull()?.message}"
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Canary") },
                actions = {
                    TextButton(onClick = onNavigateToChain) {
                        Text("Chain")
                    }
                    TextButton(onClick = onNavigateToSettings) {
                        Text("Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.weight(0.3f))

            // NFC icon / pulsing indicator
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isSuccess == true -> GreenContainer.copy(alpha = 0.3f)
                            isSuccess == false -> RedContainer.copy(alpha = 0.3f)
                            nfcDetected -> BlueContainer.copy(alpha = 0.3f)
                            else -> MaterialTheme.colorScheme.surface
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when {
                        isSuccess == true -> "✓"
                        isSuccess == false -> "✗"
                        nfcDetected -> "···"
                        else -> "⟐"
                    },
                    style = MaterialTheme.typography.displayLarge,
                    color = when {
                        isSuccess == true -> Green40
                        isSuccess == false -> Red40
                        else -> Green80
                    },
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = when {
                    isSuccess == true -> "Alive"
                    isSuccess == false -> "Failed"
                    nfcDetected -> "Verifying..."
                    else -> "Tap your sticker"
                },
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(Modifier.height(8.dp))

            if (statusMessage.isNotBlank()) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(32.dp))

            // Status card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val chain = viewModel.chainState()
                    val lastTime = viewModel.lastCanaryTime()

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Last canary", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (lastTime.isNotBlank()) lastTime else "—",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Green80,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Chain length", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${chain?.totalCount ?: 0} days",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Green80,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Status", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (chain?.intact != false) "Healthy ✓" else "BREAK ✗",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (chain?.intact != false) Green80 else Red80,
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))
        }
    }
}
