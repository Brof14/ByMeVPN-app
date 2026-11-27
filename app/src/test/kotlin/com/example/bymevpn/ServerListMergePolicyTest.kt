package com.example.bymevpn

import com.example.bymevpn.data.api.ServerNode
import com.example.bymevpn.vpn.ServerListMergePolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class ServerListMergePolicyTest {

    private fun node(code: String) = ServerNode(
        nodeCode = code,
        country = code,
        countryCode = "nl",
        city = "",
        flag = "",
        pingMs = -1,
        loadPercent = 0,
        isPremium = false
    )

    @Test
    fun preservesMultiNodeSubscriptionOverSingleApiNode() {
        val apiNodes = listOf(node("nl-1"))
        val subNodes = listOf(node("nl-1"), node("de-1"), node("fi-1"), node("us-1"))

        val chosen = ServerListMergePolicy.chooseServers(
            apiNodes = apiNodes,
            subscriptionNodes = subNodes,
            hasSubscriptionUrl = true
        )

        assertEquals(4, chosen.size)
        assertEquals(listOf("nl-1", "de-1", "fi-1", "us-1"), chosen.map { it.nodeCode })
    }

    @Test
    fun fallsBackToApiNodesWhenSubscriptionListIsEmpty() {
        val apiNodes = listOf(node("nl-1"))
        val chosen = ServerListMergePolicy.chooseServers(
            apiNodes = apiNodes,
            subscriptionNodes = emptyList(),
            hasSubscriptionUrl = false
        )
        assertEquals(apiNodes, chosen)
    }

    @Test
    fun usesSubscriptionNodesWhenApiListIsEmpty() {
        val subNodes = listOf(node("de-1"), node("se-1"))
        val chosen = ServerListMergePolicy.chooseServers(
            apiNodes = emptyList(),
            subscriptionNodes = subNodes,
            hasSubscriptionUrl = true
        )
        assertEquals(subNodes, chosen)
    }
}
