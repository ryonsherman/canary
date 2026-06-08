package com.canary.ui.screens

import android.app.Application
import android.content.Intent
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
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
import kotlinx.coroutines.launch
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

    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        val handler: (Intent) -> Unit = { intent ->
            statusMessage = ""
            isSuccess = null
            if (viewModel.isPushing()) {
                statusMessage = "Already pushing a canary, please wait..."
            } else {
                scope.launch {
                    try {
                        val tagSecret = nfcService.readTagHash(intent)
                        val storedSecret = prefsManager.tagRawSecret
                        val isOurTag = storedSecret != null && tagSecret != null &&
                            storedSecret.contentEquals(tagSecret)
                        if (isOurTag) {
                            val result = viewModel.signAndPushCanary()
                            if (result.isSuccess) {
                                viewModel.refreshChain()
                            }
                            nfcDetected = true
                            isSuccess = result.isSuccess
                            statusMessage = if (result.isSuccess) {
                                "Canary pushed for ${result.getOrNull()}"
                            } else {
                                "Failed: ${result.exceptionOrNull()?.message ?: result.exceptionOrNull().toString()}"
                            }
                        } else {
                            statusMessage = if (tagSecret != null) "Wrong tag — not paired with this device"
                                else "Tag read failed — is this your paired tag?"
                        }
                    } catch (e: Exception) {
                        statusMessage = "NFC error: ${e.message ?: e.toString()}"
                    }
                }
            }
        }
        MainActivity.nfcIntentHandler.set(handler)
        onDispose {
            if (MainActivity.nfcIntentHandler.get() === handler) {
                MainActivity.nfcIntentHandler.set(null)
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
                        else -> MaterialTheme.colorScheme.primary
                    },
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = when {
                    isSuccess == true -> "Alive"
                    isSuccess == false -> "Failed"
                    nfcDetected -> "Verifying..."
                    else -> "Tap your RFID tag"
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
                        Text("Last Canary", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (lastTime.isNotBlank()) {
                                try {
                                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmmss'Z'", Locale.US)
                                    val instant = java.time.Instant.from(formatter.parse(lastTime))
                                    val local = instant.atZone(ZoneId.systemDefault())
                                    val outFormat = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm", Locale.US)
                                    outFormat.format(local)
                                } catch (_: Exception) { lastTime }
                            } else "—",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Chain Length", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${if (chain != null) "${chain.totalCount} Links" else "--"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Integrity", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            when {
                                chain == null -> "—"
                                chain.intact -> "Healthy ✓"
                                else -> "BREAK ✗"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                chain == null -> MaterialTheme.colorScheme.onSurfaceVariant
                                chain.intact -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.error
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))
        }
    }
}
