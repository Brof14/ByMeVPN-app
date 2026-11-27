package com.example.bymevpn.vpn

import com.example.bymevpn.data.api.ServerNode

/**
 * Resolves the authoritative server list when the backend `/servers` endpoint
 * returns a 1-node primary summary alongside a multi-node `subscription_url`.
 */
internal object ServerListMergePolicy {

    fun chooseServers(
        apiNodes: List<ServerNode>,
        subscriptionNodes: List<ServerNode>,
        hasSubscriptionUrl: Boolean
    ): List<ServerNode> {
        if (subscriptionNodes.isEmpty()) return apiNodes
        if (apiNodes.isEmpty()) return subscriptionNodes

        // Preserve the full subscription node list when `/servers` only returns a
        // single primary node or fewer nodes than the active subscription feed.
        if (hasSubscriptionUrl && subscriptionNodes.size >= apiNodes.size) {
            return subscriptionNodes
        }

        return if (apiNodes.size >= subscriptionNodes.size) apiNodes else subscriptionNodes
    }
}
