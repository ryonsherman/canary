package com.canary

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.canary.data.PreferencesManager
import com.canary.service.CryptoService
import com.canary.service.NfcService
import com.canary.ui.screens.SettingsScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun settingsScreen_shows_recoveryCard() {
        val activity = composeTestRule.activity
        composeTestRule.setContent {
            SettingsScreen(
                nfcService = NfcService(activity),
                cryptoService = CryptoService(),
                prefsManager = PreferencesManager(activity),
            )
        }
        composeTestRule.onNodeWithText("Recovery").assertExists()
    }

    @Test
    fun settingsScreen_shows_recoveryPhraseButton() {
        val activity = composeTestRule.activity
        composeTestRule.setContent {
            SettingsScreen(
                nfcService = NfcService(activity),
                cryptoService = CryptoService(),
                prefsManager = PreferencesManager(activity),
            )
        }
        composeTestRule.onNodeWithText("Show Recovery Phrase").assertExists()
    }

    @Test
    fun settingsScreen_shows_recoveryPhraseDialog() {
        val activity = composeTestRule.activity
        composeTestRule.setContent {
            SettingsScreen(
                nfcService = NfcService(activity),
                cryptoService = CryptoService(),
                prefsManager = PreferencesManager(activity),
            )
        }
        composeTestRule.onNodeWithText("Show Recovery Phrase").performClick()
        composeTestRule.onNodeWithText("Recovery Phrase").assertExists()
    }

    @Test
    fun settingsScreen_shows_recoverFromBackupDialog() {
        val activity = composeTestRule.activity
        composeTestRule.setContent {
            SettingsScreen(
                nfcService = NfcService(activity),
                cryptoService = CryptoService(),
                prefsManager = PreferencesManager(activity),
            )
        }
        composeTestRule.onNodeWithText("Scan Recovery QR").performClick()
        composeTestRule.onNodeWithText("Start Recovery").assertExists()
    }

    @Test
    fun settingsScreen_recoverFromBackupDialogCancels() {
        val activity = composeTestRule.activity
        composeTestRule.setContent {
            SettingsScreen(
                nfcService = NfcService(activity),
                cryptoService = CryptoService(),
                prefsManager = PreferencesManager(activity),
            )
        }
        composeTestRule.onNodeWithText("Scan Recovery QR").performClick()
        composeTestRule.onNodeWithText("Start Recovery").assertExists()
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.onNodeWithText("Start Recovery").assertDoesNotExist()
    }
}
