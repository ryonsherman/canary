package com.canary.viewmodel

import androidx.lifecycle.ViewModel
import com.canary.data.PreferencesManager
import com.canary.model.ChainState
import com.canary.service.ChainService
import com.canary.service.GithubService

class ChainViewModel(
    private val prefsManager: PreferencesManager,
) : ViewModel() {

    private var _chainState: ChainState? = null
    fun chainState(): ChainState? = _chainState

    private var _isLoading = false
    fun isLoading(): Boolean = _isLoading

    fun loadChain(depth: Int = 50) {
        val gh = getGithubService() ?: return
        val chainService = ChainService(gh)
        _isLoading = true

        kotlinx.coroutines.runBlocking {
            _chainState = chainService.verifyChain(depth)
        }

        _isLoading = false
    }

    private fun getGithubService(): GithubService? {
        val pat = prefsManager.githubPat
        val owner = prefsManager.repoOwner
        val repo = prefsManager.repoName
        if (pat.isBlank() || owner.isBlank() || repo.isBlank()) return null
        return GithubService(pat, owner, repo)
    }
}
