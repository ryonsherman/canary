package com.canary

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.canary.data.PreferencesManager
import com.canary.service.CryptoService
import com.canary.service.NfcService
import com.canary.ui.screens.HomeScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun homeScreen_shows_canaryTitle() {
        val activity = composeTestRule.activity
        composeTestRule.setContent {
            HomeScreen(
                nfcService = NfcService(activity),
                cryptoService = CryptoService(),
                prefsManager = PreferencesManager(activity),
                onNavigateToChain = {},
                onNavigateToSettings = {},
            )
        }
        composeTestRule.onNodeWithText("Canary").assertExists()
    }

    @Test
    fun homeScreen_shows_settingsButton() {
        val activity = composeTestRule.activity
        composeTestRule.setContent {
            HomeScreen(
                nfcService = NfcService(activity),
                cryptoService = CryptoService(),
                prefsManager = PreferencesManager(activity),
                onNavigateToChain = {},
                onNavigateToSettings = {},
            )
        }
        composeTestRule.onNodeWithText("Settings").assertExists()
    }

    @Test
    fun homeScreen_shows_chainLink() {
        val activity = composeTestRule.activity
        composeTestRule.setContent {
            HomeScreen(
                nfcService = NfcService(activity),
                cryptoService = CryptoService(),
                prefsManager = PreferencesManager(activity),
                onNavigateToChain = {},
                onNavigateToSettings = {},
            )
        }
        composeTestRule.onNodeWithText("Chain").assertExists()
    }
}
