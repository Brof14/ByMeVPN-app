package com.example.bymevpn.data.api

/**
 * Keep ordinary Android requests on the same device-header contract as Windows.
 * Signed-entitlement staging builds still attach identity to every API request
 * because the server validates account/device-bound snapshots during auth.
 */
internal object ApiDeviceHeaderPolicy {
    fun includeHwid(
        method: String,
        endpoint: String,
        signedEntitlementsEnabled: Boolean
    ): Boolean = signedEntitlementsEnabled ||
        endpoint == "/servers" ||
        endpoint == "/devices" ||
        (endpoint == "/vpn/session" && method.equals("POST", ignoreCase = true))

    fun includeDeviceMetadata(
        method: String,
        endpoint: String,
        signedEntitlementsEnabled: Boolean
    ): Boolean = signedEntitlementsEnabled ||
        endpoint == "/devices" ||
        (endpoint == "/vpn/session" && method.equals("POST", ignoreCase = true))
}
