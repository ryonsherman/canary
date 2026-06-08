package com.canary.viewmodel

import androidx.lifecycle.ViewModel
import com.canary.data.PreferencesManager
import com.canary.service.CryptoService

class SettingsViewModel(
    private val prefsManager: PreferencesManager,
    private val cryptoService: CryptoService,
) : ViewModel() {

    fun getPublicKeyPem(): String = cryptoService.getPublicKeyPem()

    fun getFingerprint(): String = cryptoService.getPublicKeyFingerprint()

    fun getRepoOwner(): String = prefsManager.repoOwner

    fun getRepoName(): String = prefsManager.repoName

    fun getPatStatus(): String {
        val pat = prefsManager.githubPat
        return if (pat.isBlank()) "Not configured" else "${pat.take(4)}...${pat.takeLast(4)}"
    }

    fun updateGithubConfig(owner: String, repo: String, pat: String) {
        prefsManager.repoOwner = owner.trim()
        prefsManager.repoName = repo.trim()
        prefsManager.githubPat = pat.trim()
    }

    fun isTagPaired(): Boolean = prefsManager.tagRawSecret != null
}
