package com.canary.ui.screens

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.canary.MainActivity
import com.canary.data.PreferencesManager
import com.canary.service.CryptoService
import com.canary.service.NfcService
import com.canary.ui.theme.*
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
    var pairingTag by remember { mutableStateOf(false) }
    var regenerating by remember { mutableStateOf(false) }
    var showRegenConfirm by remember { mutableStateOf(false) }
    var nfcMessage by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }
    var showMnemonicDialog by remember { mutableStateOf(false) }
    var githubRepoFull by remember { mutableStateOf("${viewModel.getRepoOwner()}/${viewModel.getRepoName()}") }
    var githubPat by remember { mutableStateOf("") }

    if (pairingTag || regenerating) {
        val isRegen = regenerating
        DisposableEffect(pairingTag, regenerating) {
            val handler: (Intent) -> Unit = { intent ->
                try {
                    val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
                    if (tag != null) {
                        if (isRegen) {
                            val secret = cryptoService.generateTagSecret()
                            prefsManager.tagRawSecret = secret
                            val success = nfcService.writeTagSecret(tag, secret)
                            if (success) {
                                regenerating = false
                                nfcMessage = "New secret written — old tags invalidated"
                            } else {
                                nfcMessage = "Failed to write to tag"
                            }
                        } else {
                            val secret = prefsManager.tagRawSecret
                            if (secret != null) {
                                val success = nfcService.writeTagSecret(tag, secret)
                                if (success) {
                                    pairingTag = false
                                    nfcMessage = "Tag paired!"
                                } else {
                                    nfcMessage = "Failed to write to tag"
                                }
                            } else {
                                nfcMessage = "No tag secret saved — pair a tag first"
                            }
                        }
                    } else {
                        nfcMessage = "No NFC tag in intent"
                    }
                } catch (e: Exception) {
                    nfcMessage = "NFC error: ${e.message}"
                }
            }
            MainActivity.nfcIntentHandler.set(handler)
            onDispose {
                if (MainActivity.nfcIntentHandler.get() === handler) {
                    MainActivity.nfcIntentHandler.set(null)
                }
            }
        }
    }

    if (showRegenConfirm) {
        AlertDialog(
            onDismissRequest = { showRegenConfirm = false },
            title = { Text("Regenerate Secret?") },
            text = {
                Text("This will generate a new tag secret. All previously paired tags will stop working. This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRegenConfirm = false
                        regenerating = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Regenerate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegenConfirm = false }) { Text("Cancel") }
            },
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Setup") },
            text = {
                Text("This will erase all configuration and return to the setup wizard. Are you sure?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        prefsManager.isSetupComplete = false
                        prefsManager.githubPat = ""
                        prefsManager.repoOwner = ""
                        prefsManager.repoName = ""
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showMnemonicDialog) {
        val mnemonic = prefsManager.recoveryMnemonic
        var generatedMnemonic by remember { mutableStateOf("") }
        var generatedQrBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

        AlertDialog(
            onDismissRequest = { showMnemonicDialog = false },
            title = { Text("Recovery Phrase") },
            text = {
                if (mnemonic.isNotBlank()) {
                    Column {
                        Text(mnemonic)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Keep this safe — it\u2019s needed to decrypt your backup QR.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else if (generatedMnemonic.isNotBlank()) {
                    Column {
                        Text(generatedMnemonic)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "This is your new recovery phrase. Save it along with " +
                            "the QR code shown on screen — re-save the QR to your gallery.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        generatedQrBitmap?.let { bm ->
                            Spacer(Modifier.height(12.dp))
                            val imageBitmap = bm.asImageBitmap()
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = "New backup QR",
                                modifier = Modifier
                                    .size(200.dp)
                                    .align(Alignment.CenterHorizontally),
                            )
                        }
                    }
                } else {
                    Text("No recovery phrase saved. Generate one now to pair with a new backup QR.")
                }
            },
            confirmButton = {
                if (mnemonic.isNotBlank() || generatedMnemonic.isNotBlank()) {
                    TextButton(onClick = { showMnemonicDialog = false }) {
                        Text("OK")
                    }
                } else {
                    TextButton(onClick = {
                        val qrService = com.canary.service.QrService()
                        val phrase = qrService.generatePassphrase()
                        prefsManager.recoveryMnemonic = phrase
                        generatedMnemonic = phrase
                        generatedQrBitmap = qrService.generateQrBitmap(phrase, 400)
                    }) {
                        Text("Generate")
                    }
                }
            },
        )
    }

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
            if (nfcMessage.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Text(
                        nfcMessage,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Signing Key", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Fingerprint: ${viewModel.getFingerprint()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
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

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("GitHub", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Current: ${viewModel.getRepoOwner()}/${viewModel.getRepoName()}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "PAT: ${viewModel.getPatStatus()}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = githubRepoFull,
                        onValueChange = { githubRepoFull = it },
                        label = { Text("owner/repo") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("yourname/canary") },
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = githubPat,
                        onValueChange = { githubPat = it },
                        label = { Text("Fine-grained PAT") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val parts = githubRepoFull.trim().split("/")
                            if (parts.size == 2) {
                                viewModel.updateGithubConfig(parts[0].trim(), parts[1].trim(), githubPat.trim())
                            }
                        },
                        enabled = githubRepoFull.matches(Regex(".+/.+")) && githubPat.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Save")
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("RFID Tag", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (viewModel.isTagPaired()) "Paired ✓" else "Not paired",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (viewModel.isTagPaired()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(12.dp))

                    if (pairingTag || regenerating) {
                        Text(
                            "Waiting for NFC tag...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Button(
                            onClick = { pairingTag = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Pair Tag")
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Recovery Phrase", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "If you lose your device, you'll need this phrase to restore.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { showMnemonicDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Show Recovery Phrase")
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Reset", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { showRegenConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text("Reset Tags")
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.fillMaxWidth(),
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
