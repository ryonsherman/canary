package com.canary.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canary.data.PreferencesManager
import com.canary.model.ChainHead
import com.canary.model.ChainState
import com.canary.service.ChainService
import com.canary.service.CryptoService
import com.canary.service.GithubService
import com.canary.service.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

class HomeViewModel(
    private val app: Application,
    private val prefsManager: PreferencesManager,
    private val cryptoService: CryptoService,
) : ViewModel() {

    private var githubService: GithubService? = null

    private var _lastCanaryTime: String by mutableStateOf("")
    fun lastCanaryTime(): String = _lastCanaryTime

    private var _chainState: ChainState? by mutableStateOf(null)
    fun chainState(): ChainState? = _chainState

    private val pushMutex = Mutex()
    private var _isPushing by mutableStateOf(false)
    fun isPushing(): Boolean = _isPushing

    fun refreshChain() {
        viewModelScope.launch {
            val gh = getGithubService() ?: return@launch
            val chainService = ChainService(gh)
            val state = withContext(Dispatchers.IO) {
                try {
                    chainService.verifyChain(5)
                } catch (e: Exception) {
                    null
                }
            }
            _chainState = state
            _lastCanaryTime = state?.lastCanaryTimestamp ?: ""
        }
    }

    suspend fun signAndPushCanary(): Result<String> = withContext(Dispatchers.IO) {
        if (!pushMutex.tryLock()) return@withContext Result.failure(Exception("Already pushing"))
        _isPushing = true
        try {
            val gh = getGithubService()
            if (gh == null) return@withContext Result.failure(Exception("GitHub not configured"))
            val chainService = ChainService(gh)

            val today = GithubService.todayDate()
            val now = GithubService.nowTimestamp()
            val head = gh.fetchChainHead()
            val counter = head.counter + 1
            val previousHash = head.previousHash

            val content = chainService.generateCanaryContent(today, now, counter, previousHash)
            val contentBytes = content.toByteArray()
            val signature = cryptoService.sign(contentBytes)
            val fingerprint = cryptoService.getPublicKeyFingerprint()

            gh.pushCanary(content, today, signature, fingerprint).getOrThrow()

            prefsManager.lastCheckinDate = today
            val scheduler = ReminderScheduler(app)
            scheduler.dismissNotification()
            scheduler.startHourlyReminders()

            Result.success(today)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _isPushing = false
            pushMutex.unlock()
        }
    }

    private fun getGithubService(): GithubService? {
        val pat = prefsManager.githubPat
        val owner = prefsManager.repoOwner
        val repo = prefsManager.repoName
        if (pat.isBlank() || owner.isBlank() || repo.isBlank()) return null

        if (githubService == null) {
            githubService = GithubService(pat, owner, repo)
        }
        return githubService
    }
}
