package com.canary

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.canary.data.PreferencesManager
import com.canary.service.CryptoService
import com.canary.service.NfcService
import com.canary.ui.screens.SetupScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SetupScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun setupScreen_shows_welcome() {
        val activity = composeTestRule.activity
        composeTestRule.setContent {
            SetupScreen(
                nfcService = NfcService(activity),
                cryptoService = CryptoService(),
                prefsManager = PreferencesManager(activity),
                onSetupComplete = {},
            )
        }
        composeTestRule.onNodeWithText("Canary").assertExists()
        composeTestRule.onNodeWithText("Start Setup").assertExists()
    }

    @Test
    fun setupScreen_shows_recoveryButton() {
        val activity = composeTestRule.activity
        composeTestRule.setContent {
            SetupScreen(
                nfcService = NfcService(activity),
                cryptoService = CryptoService(),
                prefsManager = PreferencesManager(activity),
                onSetupComplete = {},
            )
        }
        composeTestRule.onNodeWithText("Recover from Backup").assertExists()
    }

    @Test
    fun setupScreen_startsSetupOnClick() {
        val activity = composeTestRule.activity
        composeTestRule.setContent {
            SetupScreen(
                nfcService = NfcService(activity),
                cryptoService = CryptoService(),
                prefsManager = PreferencesManager(activity),
                onSetupComplete = {},
            )
        }
        composeTestRule.onNodeWithText("Start Setup").performClick()
        composeTestRule.onNodeWithText("Generate Signing Key").assertExists()
    }
}
