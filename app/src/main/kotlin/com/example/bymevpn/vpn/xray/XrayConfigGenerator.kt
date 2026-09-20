package com.example.bymevpn.vpn.xray

import com.example.bymevpn.data.api.VpnSessionConfig
import org.json.JSONArray
import org.json.JSONObject

/**
 * Generates standard production-grade Xray-core client JSON configurations
 * for VLESS + XTLS-Vision + Reality.
 */
object XrayConfigGenerator {

    const val SOCKS_LOCAL_PORT = 10808
    const val HTTP_LOCAL_PORT = 10809

    /**
     * Generates a complete, valid Xray JSON client config object from VpnSessionConfig.
     * When bypassRussianTraffic is true, all RU domains (.ru, .рф, .su, Russian banks and gov services)
     * and RU IPs (geoip:ru) route direct without going through the VPN tunnel.
     */
    fun generateJson(config: VpnSessionConfig, bypassRussianTraffic: Boolean = true): JSONObject {
        val root = JSONObject()

        // 1. Log configuration
        val logObj = JSONObject().apply {
            put("loglevel", "warning")
        }
        root.put("log", logObj)

        // 2. Stats & Policy (for real-time uplink/downlink traffic telemetry)
        root.put("stats", JSONObject())
        root.put("policy", JSONObject().apply {
            put("levels", JSONObject().apply {
                put("0", JSONObject().apply {
                    put("handshake", 4)
                    put("connIdle", 300)
                    put("uplinkOnly", 1)
                    put("downlinkOnly", 1)
                    put("statsUserUplink", true)
                    put("statsUserDownlink", true)
                })
            })
            put("system", JSONObject().apply {
                put("statsInboundUplink", true)
                put("statsInboundDownlink", true)
                put("statsOutboundUplink", true)
                put("statsOutboundDownlink", true)
            })
        })

        // 3. Inbounds: TUN (virtual network interface) + Local SOCKS5 & HTTP
        val inboundsArray = JSONArray().apply {
            // TUN inbound - captures Android VPN traffic routed through the TUN fd
            put(JSONObject().apply {
                put("tag", "tun")
                put("protocol", "tun")
                put("settings", JSONObject().apply {
                    put("name", "tun0")
                    put("MTU", 1500)
                    put("userLevel", 0)
                })
                put("sniffing", JSONObject().apply {
                    put("enabled", true)
                    put("destOverride", JSONArray().apply {
                        put("http")
                        put("tls")
                        put("quic")
                    })
                })
            })

            // SOCKS5 inbound
            put(JSONObject().apply {
                put("tag", "socks-in")
                put("port", SOCKS_LOCAL_PORT)
                put("listen", "127.0.0.1")
                put("protocol", "socks")
                put("settings", JSONObject().apply {
                    put("auth", "noauth")
                    put("udp", true)
                    put("userLevel", 0)
                })
                put("sniffing", JSONObject().apply {
                    put("enabled", true)
                    put("destOverride", JSONArray().apply {
                        put("http")
                        put("tls")
                        put("quic")
                    })
                })
            })

            // HTTP inbound
            put(JSONObject().apply {
                put("tag", "http-in")
                put("port", HTTP_LOCAL_PORT)
                put("listen", "127.0.0.1")
                put("protocol", "http")
            })
        }
        root.put("inbounds", inboundsArray)

        // 3. Outbounds (Primary VLESS+Reality proxy outbound, direct fallback, block outbound)
        val outboundsArray = JSONArray()

        // Main proxy outbound
        val proxyOutbound = JSONObject().apply {
            put("tag", "proxy")
            put("protocol", "vless")

            // Settings: vnext destination
            val usersArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", config.uuid)
                    put("encryption", config.encryption.ifEmpty { "none" })
                    put("flow", config.flow.ifEmpty { "xtls-rprx-vision" })
                })
            }

            val vnextArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("address", config.server)
                    put("port", config.port)
                    put("users", usersArray)
                })
            }

            put("settings", JSONObject().apply {
                put("vnext", vnextArray)
            })

            // StreamSettings: Reality + XTLS
            val realitySettings = JSONObject().apply {
                put("show", false)
                put("fingerprint", config.fingerprint.ifEmpty { "chrome" })
                put("serverName", config.serverName)
                put("publicKey", config.publicKey)
                put("shortId", config.shortId)
                put("spiderX", "")
            }

            val streamSettings = JSONObject().apply {
                put("network", config.network.ifEmpty { "tcp" })
                put("security", config.security.ifEmpty { "reality" })
                put("realitySettings", realitySettings)
            }

            put("streamSettings", streamSettings)
        }
        outboundsArray.put(proxyOutbound)

        // Direct outbound
        outboundsArray.put(JSONObject().apply {
            put("tag", "direct")
            put("protocol", "freedom")
        })

        // Block outbound
        outboundsArray.put(JSONObject().apply {
            put("tag", "block")
            put("protocol", "blackhole")
        })

        root.put("outbounds", outboundsArray)

        // 4. Routing
        val rulesArray = JSONArray().apply {
            // Route private IPs direct to bypass local LAN traffic
            put(JSONObject().apply {
                put("type", "field")
                put("ip", JSONArray().apply {
                    put("geoip:private")
                })
                put("outboundTag", "direct")
            })

            // Bypass Russian services, banks, gov sites and RU IP blocks directly
            if (bypassRussianTraffic) {
                put(JSONObject().apply {
                    put("type", "field")
                    put("domain", JSONArray().apply {
                        put("geosite:ru")
                        put("regexp:.*\\.ru$")
                        put("regexp:.*\\.su$")
                        put("regexp:.*\\.рф$")
                        put("domain:gosuslugi.ru")
                        put("domain:mos.ru")
                        put("domain:sberbank.ru")
                        put("domain:sber.ru")
                        put("domain:tbank.ru")
                        put("domain:tinkoff.ru")
                        put("domain:vtb.ru")
                        put("domain:alfabank.ru")
                        put("domain:raiffeisen.ru")
                        put("domain:yandex.ru")
                        put("domain:ya.ru")
                        put("domain:vk.com")
                        put("domain:mail.ru")
                        put("domain:ozon.ru")
                        put("domain:wildberries.ru")
                        put("domain:avito.ru")
                    })
                    put("ip", JSONArray().apply {
                        put("geoip:ru")
                    })
                    put("outboundTag", "direct")
                })
            }

            // Default rule: all proxyable traffic routes to proxy
            put(JSONObject().apply {
                put("type", "field")
                put("network", "tcp,udp")
                put("outboundTag", "proxy")
            })
        }

        root.put("routing", JSONObject().apply {
            put("domainStrategy", "IPIfNonMatch")
            put("rules", rulesArray)
        })

        return root
    }

    /**
     * Converts to formatted JSON string.
     */
    fun generateString(config: VpnSessionConfig, bypassRussianTraffic: Boolean = true, indentSpaces: Int = 2): String {
        return generateJson(config, bypassRussianTraffic).toString(indentSpaces)
    }
}
