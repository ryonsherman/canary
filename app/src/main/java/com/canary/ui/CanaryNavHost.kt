package com.canary.ui

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.canary.data.PreferencesManager
import com.canary.service.CryptoService
import com.canary.service.NfcService
import com.canary.ui.screens.*

object Routes {
    const val SETUP = "setup"
    const val HOME = "home"
    const val CHAIN = "chain"
    const val SETTINGS = "settings"
}

@Composable
fun CanaryNavHost(
    nfcService: NfcService,
    cryptoService: CryptoService,
    prefsManager: PreferencesManager,
) {
    val navController = rememberNavController()
    val startDestination = if (prefsManager.isSetupComplete) Routes.HOME else Routes.SETUP

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.SETUP) {
            SetupScreen(
                nfcService = nfcService,
                cryptoService = cryptoService,
                prefsManager = prefsManager,
                onSetupComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SETUP) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                nfcService = nfcService,
                cryptoService = cryptoService,
                prefsManager = prefsManager,
                onNavigateToChain = { navController.navigate(Routes.CHAIN) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.CHAIN) {
            ChainScreen(prefsManager = prefsManager)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                nfcService = nfcService,
                cryptoService = cryptoService,
                prefsManager = prefsManager,
            )
        }
    }
}
