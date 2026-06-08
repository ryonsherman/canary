package com.canary.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import com.canary.data.PreferencesManager
import com.canary.model.ChainState
import com.canary.service.ChainService
import com.canary.service.CryptoService
import com.canary.service.GithubService
import com.canary.service.ReminderScheduler

class HomeViewModel(
    private val app: Application,
    private val prefsManager: PreferencesManager,
    private val cryptoService: CryptoService,
) : ViewModel() {

    private var githubService: GithubService? = null

    private var _lastCanaryTime: String = ""
    fun lastCanaryTime(): String = _lastCanaryTime

    private var _chainState: ChainState? = null
    fun chainState(): ChainState? = _chainState

    private var _isPushing = false
    fun isPushing(): Boolean = _isPushing

    fun refreshChain() {
        val gh = getGithubService() ?: return
        val chainService = ChainService(gh)
        kotlinx.coroutines.runBlocking {
            _chainState = chainService.verifyChain(5)
            _chainState?.lastCanaryTimestamp?.let { _lastCanaryTime = it }
        }
    }

    fun signAndPushCanary(): Result<String> {
        val gh = getGithubService() ?: return Result.failure(Exception("GitHub not configured"))
        val chainService = ChainService(gh)

        _isPushing = true
        try {
            val today = GithubService.todayDate()
            val now = GithubService.nowTimestamp()
            val counter = gh.getLatestCounter() + 1
            val previousHash = gh.getPreviousHash()

            val content = chainService.generateCanaryContent(today, now, counter, previousHash)
            val contentBytes = content.toByteArray()
            val signature = cryptoService.sign(contentBytes)
            val fingerprint = cryptoService.getPublicKeyFingerprint()

            gh.pushCanary(content, today, signature, fingerprint).getOrThrow()

            prefsManager.lastCheckinDate = today
            val scheduler = ReminderScheduler(app)
            scheduler.dismissNotification()
            scheduler.startHourlyReminders()

            return Result.success(today)
        } catch (e: Exception) {
            return Result.failure(e)
        } finally {
            _isPushing = false
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
