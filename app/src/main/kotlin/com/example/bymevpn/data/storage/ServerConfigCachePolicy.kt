package com.example.bymevpn.data.storage

/** Cache lifetime for endpoint and protocol settings delivered by the subscription service. */
internal object ServerConfigCachePolicy {
    const val MAX_AGE_MS: Long = 6L * 60 * 60 * 1000

    fun isFresh(savedAt: Long, now: Long): Boolean {
        val age = now - savedAt
        return savedAt > 0L && age in 0..MAX_AGE_MS
    }
}
