package go

import android.content.Context

/**
 * Stub replacement for gomobile's go.Seq when the native libv2ray.aar binary
 * is not bundled in this environment.
 */
object Seq {
    @JvmStatic
    fun setContext(context: Context?) {
        // No-op stub when native libv2ray.aar is absent.
    }
}
