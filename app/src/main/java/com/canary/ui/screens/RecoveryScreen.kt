package com.canary.ui.screens

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.canary.MainActivity
import com.canary.data.PreferencesManager
import com.canary.service.CryptoService
import com.canary.service.GithubService
import com.canary.service.NfcService
import com.canary.service.QrService
import com.canary.ui.components.QrScannerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class RecoveryStep {
    SCAN_QR,
    ENTER_PHRASE,
    RE_PAIR,
    DONE,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryScreen(
    nfcService: NfcService,
    cryptoService: CryptoService,
    prefsManager: PreferencesManager,
    onComplete: () -> Unit,
) {
    var step by remember { mutableStateOf(RecoveryStep.SCAN_QR) }
    var repoOwner by remember { mutableStateOf("") }
    var repoName by remember { mutableStateOf("") }
    var phrase by remember { mutableStateOf("") }
    var phraseError by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("") }
    var tagPaired by remember { mutableStateOf(false) }
    var nfcMessage by remember { mutableStateOf("") }
    var scanCooldown by remember { mutableStateOf(false) }

    val qrService = remember { QrService() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun handleQrContent(value: String) {
        if (!value.startsWith("canary:v1:")) {
            status = "Not a canary recovery QR"
            return
        }
        val parts = value.removePrefix("canary:v1:").split("/")
        if (parts.size != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            status = "Invalid QR format"
            return
        }
        repoOwner = parts[0]
        repoName = parts[1]
        step = RecoveryStep.ENTER_PHRASE
        status = "Repo identified: $repoOwner/$repoName"
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        @Suppress("DEPRECATION")
                        val name = c.getString(nameIndex)
                        if (name != null && name.endsWith(".png")) {
                            val inputStream = context.contentResolver.openInputStream(uri)
                            val bytes = inputStream?.readBytes()
                            inputStream?.close()
                            if (bytes != null) {
                                try {
                                    val bm = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    val pixels = IntArray(bm.width * bm.height)
                                    bm.getPixels(pixels, 0, bm.width, 0, 0, bm.width, bm.height)
                                    val source = com.google.zxing.RGBLuminanceSource(bm.width, bm.height, pixels)
                                    val zxBitmap = com.google.zxing.BinaryBitmap(
                                        com.google.zxing.common.HybridBinarizer(source)
                                    )
                                    val result = com.google.zxing.qrcode.QRCodeReader().decode(zxBitmap)
                                    handleQrContent(result.text)
                                } catch (_: Exception) {
                                    status = "Could not decode QR from image"
                                }
                            }
                        } else {
                            status = "Please select a PNG image"
                        }
                    }
                }
            }
        }
    }

    fun tryRecover(owner: String, repo: String, words: String): String {
        val mnemonic = words.trim().lowercase().split("\\s+".toRegex())
        if (mnemonic.size != 12) return "Must be exactly 12 words"
        val entropy = qrService.mnemonicToEntropy(mnemonic)
        if (entropy == null) return "Invalid phrase — checksum mismatch"
        val encryptedB64 = GithubService("", owner, repo).fetchRecoveryData(owner, repo)
            ?: return "No recovery data found in $owner/$repo"
        val encrypted = android.util.Base64.decode(encryptedB64, android.util.Base64.DEFAULT)
        val decrypted = qrService.decryptBackupData(encrypted, mnemonic)
            ?: return "Wrong phrase for this QR"
        val config = org.json.JSONObject(String(decrypted))
        val pat = config.getString("pat")
        val savedOwner = config.getString("owner")
        val savedRepo = config.getString("repo")
        if (savedOwner != owner || savedRepo != repo) return "QR/repo mismatch"
        prefsManager.githubPat = pat
        prefsManager.repoOwner = owner
        prefsManager.repoName = repo
        prefsManager.recoveryMnemonic = words.trim()
        return ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recovery") },
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
        ) {
            when (step) {
                RecoveryStep.SCAN_QR -> {
                    Text("Scan Recovery QR", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Scan the QR code saved during setup.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(16.dp))

                    QrScannerView(
                        onQrDetected = { value ->
                            if (!scanCooldown) {
                                scanCooldown = true
                                handleQrContent(value)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        resetTrigger = scanCooldown,
                    )

                    if (status.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                status,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                        LaunchedEffect(status) {
                            kotlinx.coroutines.delay(3000)
                            scanCooldown = false
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Pick from Gallery")
                    }

                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = repoOwner,
                        onValueChange = { repoOwner = it },
                        label = { Text("Or type owner/repo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("yourname/canary") },
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val parts = repoOwner.trim().split("/")
                            if (parts.size == 2) {
                                repoOwner = parts[0].trim()
                                repoName = parts[1].trim()
                                step = RecoveryStep.ENTER_PHRASE
                            } else {
                                status = "Enter as owner/repo"
                            }
                        },
                        enabled = repoOwner.matches(Regex(".+/.+")),
                    ) {
                        Text("Enter Recovery Phrase")
                    }
                }

                RecoveryStep.ENTER_PHRASE -> {
                    Text("Enter Recovery Phrase", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Repo: $repoOwner/$repoName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Enter the 12-word passphrase from setup.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = phrase,
                        onValueChange = { phrase = it; phraseError = null },
                        label = { Text("12-word passphrase") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = phraseError != null,
                    )
                    if (phraseError != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(phraseError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(Modifier.height(12.dp))
                    var recovering by remember { mutableStateOf(false) }
                    Button(
                        onClick = {
                            recovering = true
                            phraseError = null
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val err = tryRecover(repoOwner, repoName, phrase)
                                    withContext(Dispatchers.Main) {
                                        if (err.isNotBlank()) {
                                            phraseError = err
                                        } else {
                                            status = "Recovery data restored"
                                            step = RecoveryStep.RE_PAIR
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        phraseError = "Recovery failed: ${e.message}"
                                    }
                                } finally {
                                    withContext(Dispatchers.Main) {
                                        recovering = false
                                    }
                                }
                            }
                        },
                        enabled = phrase.isNotBlank() && !recovering,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (recovering) "Recovering..." else "Restore")
                    }
                }

                RecoveryStep.RE_PAIR -> {
                    Text("Re-pair RFID Tag", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tap your RFID tag to pair it with this device.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(32.dp))

                    if (!tagPaired) {
                        Text("Waiting for NFC tag...", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                        DisposableEffect(Unit) {
                            val handler: (Intent) -> Unit = { intent ->
                                try {
                                    val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
                                    if (tag != null) {
                                        val secret = cryptoService.generateTagSecret()
                                        prefsManager.tagRawSecret = secret
                                        val success = nfcService.writeTagSecret(tag, secret)
                                        if (success) { tagPaired = true; nfcMessage = "Tag paired!" }
                                        else { nfcMessage = "Failed to write" }
                                    } else { nfcMessage = "No NFC tag" }
                                } catch (e: Exception) { nfcMessage = "NFC error: ${e.message}" }
                            }
                            MainActivity.nfcIntentHandler.set(handler)
                            onDispose {
                                if (MainActivity.nfcIntentHandler.get() === handler) {
                                    MainActivity.nfcIntentHandler.set(null)
                                }
                            }
                        }
                    }

                    if (nfcMessage.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(nfcMessage, color = MaterialTheme.colorScheme.primary)
                    }
                    if (tagPaired) {
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { step = RecoveryStep.DONE }) { Text("Continue") }
                    }
                }

                RecoveryStep.DONE -> {
                    Text("Recovery Complete", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(16.dp))
                    Text("Your GitHub config is restored and tag is paired.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { prefsManager.isSetupComplete = true; onComplete() }) { Text("Done") }
                }
            }
        }
    }
}
