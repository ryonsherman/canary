package com.canary.service

import com.canary.model.Canary
import com.canary.model.ChainState
import java.security.MessageDigest

class ChainService(private val githubService: GithubService) {

    suspend fun verifyChain(depth: Int = 50): ChainState {
        val canaries = githubService.fetchRecentCanaries(depth)
        if (canaries.isEmpty()) {
            return ChainState(canaries = emptyList(), totalCount = 0, intact = true)
        }

        val sorted = canaries.sortedBy { it.date }
        var intact = true
        var breakAt: Int? = null

        for (i in 1 until sorted.size) {
            val current = sorted[i]
            val previous = sorted[i - 1]
            if (current.previousHash != previous.fileHash) {
                intact = false
                breakAt = i
                break
            }
        }

        val sortedDesc = sorted.reversed()
        return ChainState(
            canaries = sortedDesc,
            totalCount = sortedDesc.size,
            intact = intact,
            breakAtIndex = breakAt,
            lastCanaryTimestamp = sortedDesc.firstOrNull()?.let { "${it.date}Z" },
        )
    }

    fun generateCanaryContent(
        date: String,
        timestamp: String,
        counter: Int,
        previousHash: String,
    ): String {
        return buildString {
            appendLine("=== PROOF OF LIFE CANARY ===")
            appendLine("Status: Alive")
            appendLine("Date: ${date}Z")
            appendLine("Timestamp: $timestamp")
            appendLine("Counter: $counter")
            appendLine("Previous Hash (SHA256): $previousHash")
            appendLine("============================")
        }
    }
}
