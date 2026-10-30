package com.example.bymevpn.vpn.xray

import com.example.bymevpn.data.api.VpnSessionConfig
import org.json.JSONArray
import org.json.JSONObject

/**
 * Generates standard production-grade Xray-core client JSON configurations
 * for VLESS + XTLS-Vision + Reality and Hysteria 2.
 */
object XrayConfigGenerator {

    const val SOCKS_LOCAL_PORT = 10808
    const val HTTP_LOCAL_PORT = 10809

    /**
     * Generates a complete, valid Xray JSON client config object from VpnSessionConfig.
     */
    fun generateJson(config: VpnSessionConfig, bypassRussianTraffic: Boolean = false, dnsMode: String = "cloudflare"): JSONObject {
        val root = JSONObject()

        // 1. Log configuration
        root.put("log", JSONObject().apply {
            put("loglevel", "warning")
            put("access", "none")
        })

        // 2. DNS configuration (Fast UDP DNS routed via proxy, UseIPv4 prevents AAAA hangs)
        root.put("dns", JSONObject().apply {
            put("servers", JSONArray().apply {
                when (dnsMode) {
                    "adguard" -> { put("94.140.14.14"); put("94.140.15.15") }
                    "google" -> { put("8.8.8.8"); put("8.8.4.4") }
                    else -> { put("1.1.1.1"); put("8.8.8.8"); put("77.88.8.8") }
                }
            })
            put("queryStrategy", "UseIPv4")
        })

        // 3. Stats & Policy
        root.put("stats", JSONObject())
        root.put("policy", JSONObject().apply {
            put("levels", JSONObject().apply {
                put("8", JSONObject().apply {
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

        // 4. Inbounds: Layer 3 Native TUN + Local SOCKS5 & HTTP
        val inboundsArray = JSONArray().apply {
            // TUN inbound - captures Android VPN traffic routed through the TUN fd
            put(JSONObject().apply {
                put("tag", "tun")
                put("port", 0)
                put("protocol", "tun")
                put("settings", JSONObject().apply {
                    put("name", "xray0")
                    put("mtu", 1500)
                    put("MTU", 1500)
                    put("userLevel", 8)
                    put("gateway", JSONArray().apply {
                        put("26.26.26.2/24")
                    })
                })
                put("sniffing", JSONObject().apply {
                    put("enabled", true)
                    put("destOverride", JSONArray().apply {
                        put("http")
                        put("tls")
                    })
                    put("routeOnly", true)
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
                    put("userLevel", 8)
                })
                put("sniffing", JSONObject().apply {
                    put("enabled", true)
                    put("destOverride", JSONArray().apply {
                        put("http")
                        put("tls")
                    })
                    put("routeOnly", true)
                })
            })

            // HTTP inbound
            put(JSONObject().apply {
                put("tag", "http")
                put("port", HTTP_LOCAL_PORT)
                put("listen", "127.0.0.1")
                put("protocol", "http")
                put("settings", JSONObject().apply {
                    put("userLevel", 8)
                })
            })
        }
        root.put("inbounds", inboundsArray)

        // 5. Outbounds (Primary VLESS+Reality / Hysteria 2 proxy outbound, direct, dns-out, block)
        val outboundsArray = JSONArray()
        val serverAddress = config.server.ifBlank { "2.27.120.247" }

        // Main proxy outbound
        val proxyOutbound = JSONObject().apply {
            put("tag", "proxy")
            val proto = config.protocol.lowercase()

            if (proto == "hysteria2" || proto == "hy2") {
                put("protocol", "hysteria")
                put("settings", JSONObject().apply {
                    put("version", 2)
                    put("address", serverAddress)
                    put("port", config.port)
                })

                val streamSettings = JSONObject().apply {
                    put("network", "hysteria")
                    put("security", "tls")
                    put("hysteriaSettings", JSONObject().apply {
                        put("version", 2)
                        put("auth", config.uuid)
                    })
                    val tlsSettings = JSONObject().apply {
                        put("serverName", config.serverName)
                        put("fingerprint", config.fingerprint.ifEmpty { "firefox" })
                        put("alpn", JSONArray().apply { put("h3") })
                    }
                    put("tlsSettings", tlsSettings)

                    if (config.encryption.isNotBlank()) {
                        require(config.encryption.equals("salamander", ignoreCase = true)) { "Unsupported Hysteria obfuscation" }
                        put("finalmask", JSONObject().apply {
                            put("udp", JSONArray().put(JSONObject().apply {
                                put("type", config.encryption)
                                put("settings", JSONObject().put("password", config.flow))
                            }))
                        })
                    }
                }
                put("streamSettings", streamSettings)

            } else {
                // Default: VLESS
                put("protocol", "vless")

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

                val streamSettings = JSONObject().apply {
                    put("network", config.network.ifEmpty { "tcp" })
                    if (config.network.equals("xhttp", ignoreCase = true)) {
                        put("xhttpSettings", JSONObject().apply {
                            if (config.transportPath.isNotBlank()) put("path", config.transportPath)
                            if (config.transportHost.isNotBlank()) put("host", config.transportHost)
                            if (config.transportMode.isNotBlank()) put("mode", config.transportMode)
                        })
                    }
                    val sec = config.security.lowercase()
                    put("security", sec)

                    if (sec == "reality") {
                        val sni = config.serverName
                        val realitySettings = JSONObject().apply {
                            put("show", false)
                            put("fingerprint", config.fingerprint.ifEmpty { "chrome" })
                            put("serverName", sni)
                            put("publicKey", config.publicKey)
                            put("shortId", config.shortId)
                            put("spiderX", "")
                        }
                        put("realitySettings", realitySettings)
                    } else if (sec == "tls") {
                        val tlsSni = config.serverName.ifEmpty { "bymevpn.duckdns.org" }
                        val tlsSettings = JSONObject().apply {
                            put("serverName", tlsSni)
                            put("fingerprint", config.fingerprint.ifEmpty { "chrome" })
                            put("alpn", JSONArray().apply {
                                put("h2")
                                put("http/1.1")
                            })
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

        // Dedicated DNS Outbound - internal Xray DNS handler
        outboundsArray.put(JSONObject().apply {
            put("tag", "dns-out")
            put("protocol", "dns")
        })

        // Block outbound
        outboundsArray.put(JSONObject().apply {
            put("tag", "block")
            put("protocol", "blackhole")
        })

        root.put("outbounds", outboundsArray)

        // 6. Routing Rules
        val rulesArray = JSONArray().apply {
            // Rule 1: Intercept DNS traffic (port 53) from TUN inbound exclusively to dns-out (prevents internal DNS loop)
            put(JSONObject().apply {
                put("type", "field")
                put("inboundTag", JSONArray().apply { put("tun") })
                put("port", "53")
                put("network", "udp,tcp")
                put("outboundTag", "dns-out")
            })

            // Rule 2: Block UDP 443 (QUIC) so browsers instantly fall back cleanly to HTTP/2 over TLS (prevents QUIC protocol stall)
            put(JSONObject().apply {
                put("type", "field")
                put("port", "443")
                put("network", "udp")
                put("outboundTag", "block")
            })

            if (bypassRussianTraffic) {
                put(JSONObject().apply {
                    put("type", "field")
                    put("domain", JSONArray().put("geosite:category-ru"))
                    put("outboundTag", "direct")
                })
                put(JSONObject().apply {
                    put("type", "field")
                    put("ip", JSONArray().put("geoip:ru"))
                    put("outboundTag", "direct")
                })
            }

            // Rule 3: Route private LAN IPs direct
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

            // Rule 4: All remaining internet traffic unconditionally routes to proxy
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
    fun generateString(config: VpnSessionConfig, bypassRussianTraffic: Boolean = false, indentSpaces: Int = 2, dnsMode: String = "cloudflare"): String {
        return generateJson(config, bypassRussianTraffic, dnsMode).toString(indentSpaces)
    }
}
