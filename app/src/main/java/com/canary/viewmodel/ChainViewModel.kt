package com.canary.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canary.data.PreferencesManager
import com.canary.model.ChainState
import com.canary.service.ChainService
import com.canary.service.GithubService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChainViewModel(
    private val prefsManager: PreferencesManager,
) : ViewModel() {

    private var _chainState: ChainState? by mutableStateOf(null)
    fun chainState(): ChainState? = _chainState

    private var _isLoading: Boolean by mutableStateOf(false)
    fun isLoading(): Boolean = _isLoading

    fun loadChain(depth: Int = 50) {
        viewModelScope.launch {
            val gh = getGithubService() ?: return@launch
            val chainService = ChainService(gh)
            _isLoading = true

            val state = withContext(Dispatchers.IO) {
                chainService.verifyChain(depth)
            }
            _chainState = state
            _isLoading = false
        }
    }

    private fun getGithubService(): GithubService? {
        val pat = prefsManager.githubPat
        val owner = prefsManager.repoOwner
        val repo = prefsManager.repoName
        if (pat.isBlank() || owner.isBlank() || repo.isBlank()) return null
        return GithubService(pat, owner, repo)
    }
}
