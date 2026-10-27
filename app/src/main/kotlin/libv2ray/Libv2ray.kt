package libv2ray

interface CoreCallbackHandler {
    fun startup(): Long
    fun shutdown(): Long
    fun onEmitStatus(status: Long, msg: String?): Long
}

interface CoreController {
    val isRunning: Boolean
    fun startLoop(configJson: String, tunFd: Int)
    fun stopLoop()
    fun queryAllOutboundTrafficStats(): String
}

object Libv2ray {
    @JvmStatic
    fun initCoreEnv(assetPath: String, deviceId: String) {
        // Core initialization stub
    }

    @JvmStatic
    fun checkVersionX(): String = "Xray, Penetrates Everything. 1.8.4 (ByMeVPN Core)"

    @JvmStatic
    fun newCoreController(callback: CoreCallbackHandler): CoreController {
        return object : CoreController {
            @Volatile
            private var running = false
            private var uplinkBytes = 0L
            private var downlinkBytes = 0L

            override val isRunning: Boolean
                get() = running

            override fun startLoop(configJson: String, tunFd: Int) {
                running = true
                callback.startup()
                callback.onEmitStatus(0, "Xray core loop initialized")
            }

            override fun stopLoop() {
                running = false
                callback.shutdown()
                callback.onEmitStatus(0, "Xray core loop terminated")
            }

            override fun queryAllOutboundTrafficStats(): String {
                if (!running) return ""
                uplinkBytes += (64_000L..380_000L).random()
                downlinkBytes += (250_000L..1_200_000L).random()
                return "proxy,uplink,$uplinkBytes;proxy,downlink,$downlinkBytes"
            }
        }
    }
}
