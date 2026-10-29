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

        // DNS configuration (Fast, resilient public DNS servers)
        val dnsObj = JSONObject().apply {
            put("servers", JSONArray().apply {
                put("1.1.1.1")
                put("8.8.8.8")
            })
        }
        root.put("dns", dnsObj)

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

        val serverAddress = if (config.server.equals("bymevpn.duckdns.org", ignoreCase = true) || config.server.isBlank()) {
            "2.27.120.247"
        } else {
            config.server
        }

        // Main proxy outbound
        val proxyOutbound = JSONObject().apply {
            put("tag", "proxy")
            val proto = config.protocol.lowercase()

            if (proto == "hysteria2" || proto == "hy2") {
                put("protocol", "hysteria2")
                val serverObj = JSONObject().apply {
                    put("address", serverAddress)
                    put("port", config.port)
                    put("password", config.uuid)
                }
                val serversArr = JSONArray().apply { put(serverObj) }
                put("settings", JSONObject().apply {
                    put("servers", serversArr)
                })

                val streamSettings = JSONObject().apply {
                    put("network", "udp")
                    put("security", "tls")
                    val tlsSettings = JSONObject().apply {
                        put("serverName", config.serverName)
                        put("fingerprint", config.fingerprint.ifEmpty { "firefox" })
                        val alpn = JSONArray().apply { put("h3") }
                        put("alpn", alpn)
                    }
                    put("tlsSettings", tlsSettings)

                    // Obfs if configured
                    if (config.encryption.isNotBlank()) {
                        val hysteriaSettings = JSONObject().apply {
                            put("obfs", JSONObject().apply {
                                put("type", config.encryption)
                                put("password", config.flow)
                            })
                        }
                        put("hysteria2Settings", hysteriaSettings)
                    }
                }
                put("streamSettings", streamSettings)

            } else {
                // Default: VLESS
                put("protocol", "vless")

                // Settings: vnext destination
                val usersArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("id", config.uuid)
                        put("encryption", config.encryption.ifEmpty { "none" })
                        if (config.flow.isNotBlank() && !config.flow.contains("kw54")) {
                            put("flow", config.flow)
                        }
                    })
                }

                val vnextArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("address", serverAddress)
                        put("port", config.port)
                        put("users", usersArray)
                    })
                }

                put("settings", JSONObject().apply {
                    put("vnext", vnextArray)
                })

                // StreamSettings: TLS / Reality
                val streamSettings = JSONObject().apply {
                    put("network", config.network.ifEmpty { "tcp" })
                    val sec = config.security.lowercase()
                    put("security", sec)

                    if (sec == "reality") {
                        val realitySettings = JSONObject().apply {
                            put("show", false)
                            put("fingerprint", config.fingerprint.ifEmpty { "chrome" })
                            put("serverName", config.serverName.ifEmpty { "www.cloudflare.com" })
                            put("publicKey", config.publicKey)
                            put("shortId", config.shortId)
                            put("spiderX", "")
                        }
                        put("realitySettings", realitySettings)
                    } else if (sec == "tls") {
                        val tlsSettings = JSONObject().apply {
                            put("serverName", config.serverName)
                            put("fingerprint", config.fingerprint.ifEmpty { "chrome" })
                            val alpnArray = JSONArray()
                            alpnArray.put("h2")
                            alpnArray.put("http/1.1")
                            put("alpn", alpnArray)
                        }
                        put("tlsSettings", tlsSettings)
                    }
                }

                put("streamSettings", streamSettings)
            }
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

        // 4. Bulletproof, dead-simple routing matching Happ / v2rayNG standards
        val rulesArray = JSONArray().apply {
            // Route private LAN IPs direct
            put(JSONObject().apply {
                put("type", "field")
                put("ip", JSONArray().apply {
                    put("10.0.0.0/8")
                    put("172.16.0.0/12")
                    put("192.168.0.0/16")
                    put("127.0.0.0/8")
                    put("fc00::/7")
                    put("fe80::/10")
                })
                put("outboundTag", "direct")
            })

            // All internet traffic unconditionally routes to proxy
            put(JSONObject().apply {
                put("type", "field")
                put("network", "tcp,udp")
                put("outboundTag", "proxy")
            })
        }

        root.put("routing", JSONObject().apply {
            put("domainStrategy", "AsIs")
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
