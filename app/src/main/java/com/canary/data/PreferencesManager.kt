package com.canary.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PreferencesManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var githubPat: String
        get() = prefs.getString(KEY_GITHUB_PAT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GITHUB_PAT, value).apply()

    var repoOwner: String
        get() = prefs.getString(KEY_REPO_OWNER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_REPO_OWNER, value).apply()

    var repoName: String
        get() = prefs.getString(KEY_REPO_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_REPO_NAME, value).apply()

    var isSetupComplete: Boolean
        get() = prefs.getBoolean(KEY_SETUP_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_SETUP_DONE, value).apply()

    var lastCheckinDate: String
        get() = prefs.getString(KEY_LAST_CHECKIN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_CHECKIN, value).apply()

    companion object {
        private const val PREFS_NAME = "canary_secure_prefs"
        private const val KEY_GITHUB_PAT = "github_pat"
        private const val KEY_REPO_OWNER = "repo_owner"
        private const val KEY_REPO_NAME = "repo_name"
        private const val KEY_SETUP_DONE = "setup_complete"
        private const val KEY_LAST_CHECKIN = "last_checkin_date"
    }
}
