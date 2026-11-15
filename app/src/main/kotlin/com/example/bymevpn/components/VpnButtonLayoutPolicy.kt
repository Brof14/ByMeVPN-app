package com.example.bymevpn.components

/** Screen-aware VPN action size; keeps the existing logo dominant without overflow. */
internal object VpnButtonLayoutPolicy {
    fun diameterDp(screenWidthDp: Int, screenHeightDp: Int): Int {
        val width = screenWidthDp.coerceAtLeast(1)
        val height = screenHeightDp.coerceAtLeast(1)
        return minOf(270, (width * 0.74f).toInt(), (height * 0.41f).toInt())
            .coerceAtLeast(48)
    }
}
