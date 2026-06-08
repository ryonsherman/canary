package com.canary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.canary.data.PreferencesManager
import com.canary.service.CryptoService
import com.canary.service.NfcService
import com.canary.ui.theme.CanaryTheme
import com.canary.ui.CanaryNavHost

class MainActivity : ComponentActivity() {

    private lateinit var nfcService: NfcService
    private lateinit var cryptoService: CryptoService
    private lateinit var prefsManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcService = NfcService(this)
        cryptoService = CryptoService()
        prefsManager = PreferencesManager(applicationContext)

        setContent {
            CanaryTheme {
                val nfc = remember { nfcService }
                val crypto = remember { cryptoService }
                val prefs = remember { prefsManager }

                CanaryNavHost(
                    nfcService = nfc,
                    cryptoService = crypto,
                    prefsManager = prefs,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcService.enableForegroundDispatch()
    }

    override fun onPause() {
        super.onPause()
        nfcService.disableForegroundDispatch()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        nfcIntentHandler?.invoke(intent)
    }

    companion object {
        var nfcIntentHandler: ((android.content.Intent) -> Unit)? = null
    }
}
