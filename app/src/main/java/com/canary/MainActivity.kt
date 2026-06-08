package com.canary

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.canary.data.PreferencesManager
import com.canary.service.CryptoService
import com.canary.service.NfcService
import com.canary.ui.CanaryNavHost
import com.canary.ui.theme.CanaryTheme
import java.util.concurrent.atomic.AtomicReference

class MainActivity : ComponentActivity() {

    private lateinit var nfcService: NfcService
    private lateinit var cryptoService: CryptoService
    private lateinit var prefsManager: PreferencesManager
    private var biometricAuthenticated by mutableStateOf(false)

    private val credentialLauncher = registerForActivityResult(
        StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            biometricAuthenticated = true
            requestNotificationPermission()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        RequestPermission(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcService = NfcService(this)
        cryptoService = CryptoService()
        prefsManager = PreferencesManager(applicationContext)

        setContent {
            CanaryTheme {
                if (biometricAuthenticated) {
                    val nfc = remember { nfcService }
                    val crypto = remember { cryptoService }
                    val prefs = remember { prefsManager }
                    CanaryNavHost(
                        nfcService = nfc,
                        cryptoService = crypto,
                        prefsManager = prefs,
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(32.dp),
                        ) {
                            Text(
                                "Canary",
                                style = MaterialTheme.typography.displayLarge,
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Authentication required",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Spacer(Modifier.height(24.dp))
                            Button(onClick = { authenticate() }) {
                                Text("Unlock")
                            }
                        }
                    }
                }
            }
        }

        authenticate()
    }

    override fun onResume() {
        super.onResume()
        if (biometricAuthenticated) {
            nfcService.enableForegroundDispatch()
        }
    }

    override fun onPause() {
        super.onPause()
        nfcService.disableForegroundDispatch()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        nfcIntentHandler.get()?.invoke(intent)
    }

    private fun authenticate() {
        if (biometricAuthenticated) return
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (Build.VERSION.SDK_INT >= 28 && keyguardManager.isDeviceSecure) {
            val intent = keyguardManager.createConfirmDeviceCredentialIntent(
                "Canary",
                "Authenticate to open the app",
            ) ?: return
            credentialLauncher.launch(intent)
        } else {
            biometricAuthenticated = true
        }
    }

    companion object {
        val nfcIntentHandler = AtomicReference<((Intent) -> Unit)?>(null)
    }
}
