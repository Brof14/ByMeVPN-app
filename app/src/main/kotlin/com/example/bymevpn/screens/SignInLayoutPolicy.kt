package com.example.bymevpn.screens

/** Adapt the branded login hero when vertical space or text scale is constrained. */
internal object SignInLayoutPolicy {
    fun isCompact(screenHeightDp: Int, fontScale: Float): Boolean =
        screenHeightDp < 730 || fontScale >= 1.3f
}
