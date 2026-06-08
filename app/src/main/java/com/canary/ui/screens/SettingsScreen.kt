package com.canary.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.canary.data.PreferencesManager
import com.canary.service.CryptoService
import com.canary.service.NfcService
import com.canary.ui.theme.Green80
import com.canary.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    nfcService: NfcService,
    cryptoService: CryptoService,
    prefsManager: PreferencesManager,
) {
    val viewModel = remember { SettingsViewModel(prefsManager, cryptoService) }
    var showPublicKey by remember { mutableStateOf(false) }
    var showQrRecovery by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Key section
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Signing Key", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Fingerprint: ${viewModel.getFingerprint()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Green80,
                    )
                    Spacer(Modifier.height(8.dp))

                    if (showPublicKey) {
                        Text(
                            viewModel.getPublicKeyPem(),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    TextButton(onClick = { showPublicKey = !showPublicKey }) {
                        Text(if (showPublicKey) "Hide" else "Show Public Key")
                    }
                }
            }

            // GitHub section
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("GitHub", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Repo: ${viewModel.getRepoOwner()}/${viewModel.getRepoName()}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "PAT: ${viewModel.getPatStatus()}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            // Tag section
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("RFID Tag", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (viewModel.isTagPaired()) "Paired ✓" else "Not paired",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (viewModel.isTagPaired()) Green80 else MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { /* re-pair flow */ }) {
                        Text("Re-pair Tag")
                    }
                }
            }

            // Recovery section
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Recovery", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "If you lose your RFID sticker, use the QR backup to recover.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showQrRecovery = true }) {
                        Text("Scan Recovery QR")
                    }
                }
            }

            // Danger zone
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Reset", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            prefsManager.isSetupComplete = false
                            prefsManager.githubPat = ""
                            prefsManager.repoOwner = ""
                            prefsManager.repoName = ""
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text("Reset Setup")
                    }
                }
            }
        }
    }
}
