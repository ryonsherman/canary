package com.canary.model

data class Canary(
    val date: String,
    val counter: Int,
    val previousHash: String,
    val fileHash: String,
    val gpgSigValid: Boolean? = null,
    val otsConfirmed: Boolean? = null,
    val tagExists: Boolean? = null,
)

data class ChainHead(
    val counter: Int,
    val previousHash: String,
)

data class ChainState(
    val canaries: List<Canary>,
    val totalCount: Int,
    val intact: Boolean,
    val breakAtIndex: Int? = null,
    val lastCanaryTimestamp: String? = null,
)

data class TagState(
    val paired: Boolean = false,
    val tagUid: String? = null,
    val secretHash: String? = null,
)

enum class CanaryStatus { HEALTHY, MISSING_TODAY, CHAIN_BROKEN, NOT_SETUP }
