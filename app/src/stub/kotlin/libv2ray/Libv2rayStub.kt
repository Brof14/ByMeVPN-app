package libv2ray

/**
 * Stub replacements for libv2ray (gomobile Xray-core bindings) when the
 * prebuilt native app/libs/libv2ray.aar binary is not present.
 */
interface CoreCallbackHandler {
    fun startup(): Long
    fun shutdown(): Long
    fun onEmitStatus(status: Long, msg: String?): Long
}

class CoreController internal constructor(
    private val callback: CoreCallbackHandler?
) {
    @Volatile
    var isRunning: Boolean = false
        private set

    fun startLoop(configContent: String, tunFd: Int) {
        isRunning = false
        throw UnsupportedOperationException(
            "Native libv2ray.aar is not bundled in this build"
        )
    }

    fun stopLoop() {
        if (isRunning) {
            isRunning = false
            callback?.shutdown()
        }
    }

    fun queryAllOutboundTrafficStats(): String = ""
}

object Libv2ray {
    @JvmStatic
    fun initCoreEnv(assetPath: String, key: String) {
        // No-op stub when native libv2ray.aar is absent.
    }

    @JvmStatic
    fun checkVersionX(): String = "stub (libv2ray.aar unavailable)"

    @JvmStatic
    fun newCoreController(callback: CoreCallbackHandler?): CoreController =
        CoreController(callback)

    @JvmStatic
    fun measureOutboundDelay(configContent: String, url: String): Long = -1L
}
