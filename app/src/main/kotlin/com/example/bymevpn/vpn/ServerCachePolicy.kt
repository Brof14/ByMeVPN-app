package com.example.bymevpn.vpn

private const val SERVER_CACHE_DURATION_MS = 6L * 60 * 60 * 1000

internal fun isServerCacheFresh(savedAt: Long, now: Long): Boolean {
    if (savedAt <= 0L || now < savedAt) return false
    return now - savedAt < SERVER_CACHE_DURATION_MS
}
