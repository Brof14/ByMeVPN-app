package com.example.bymevpn.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "bymevpn_settings")

data class AppSettings(
    val protocol: String = "VLESS + Reality",
    val bypassRussianTraffic: Boolean = false, // Privacy-first default: avoid direct Russian traffic bypass
    val autoConnectOnBoot: Boolean = false,
    val autoConnectOpenWifi: Boolean = false,
    val autoReconnect: Boolean = true,
    val killSwitch: Boolean = false,
    val splitTunnelingEnabled: Boolean = false,
    val excludedApps: Set<String> = emptySet(),
    val language: String = "system", // ru | en | system
    val showSpeedInNotification: Boolean = true,
    val connectionNotifications: Boolean = true,
    val dnsMode: String = "cloudflare" // cloudflare | adguard | google
)

class SettingsRepository(private val context: Context) {

    companion object {
        private val KEY_PROTOCOL = stringPreferencesKey("protocol")
        private val KEY_BYPASS_RU = booleanPreferencesKey("bypass_ru")
        private val KEY_AUTO_BOOT = booleanPreferencesKey("auto_boot")
        private val KEY_AUTO_WIFI = booleanPreferencesKey("auto_wifi")
        private val KEY_AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        private val KEY_KILL_SWITCH = booleanPreferencesKey("kill_switch")
        private val KEY_SPLIT_ENABLED = booleanPreferencesKey("split_enabled")
        private val KEY_EXCLUDED_APPS = stringSetPreferencesKey("excluded_apps")
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private val KEY_SHOW_SPEED = booleanPreferencesKey("show_speed")
        private val KEY_NOTIF_ENABLED = booleanPreferencesKey("notif_enabled")
        private val KEY_DNS_MODE = stringPreferencesKey("dns_mode")

        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            protocol = prefs[KEY_PROTOCOL] ?: "VLESS + Reality",
            bypassRussianTraffic = prefs[KEY_BYPASS_RU] ?: false,
            autoConnectOnBoot = prefs[KEY_AUTO_BOOT] ?: false,
            autoConnectOpenWifi = prefs[KEY_AUTO_WIFI] ?: false,
            autoReconnect = prefs[KEY_AUTO_RECONNECT] ?: true,
            killSwitch = prefs[KEY_KILL_SWITCH] ?: false,
            splitTunnelingEnabled = prefs[KEY_SPLIT_ENABLED] ?: false,
            excludedApps = prefs[KEY_EXCLUDED_APPS] ?: emptySet(),
            language = prefs[KEY_LANGUAGE] ?: "system",
            showSpeedInNotification = prefs[KEY_SHOW_SPEED] ?: true,
            connectionNotifications = prefs[KEY_NOTIF_ENABLED] ?: true,
            dnsMode = prefs[KEY_DNS_MODE] ?: "cloudflare"
        )
    }

    suspend fun updateDnsMode(mode: String) {
        context.dataStore.edit { it[KEY_DNS_MODE] = mode }
    }

    suspend fun updateBypassRussianTraffic(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BYPASS_RU] = enabled }
    }

    suspend fun updateAutoBoot(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_BOOT] = enabled }
    }

    suspend fun updateAutoWifi(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_WIFI] = enabled }
    }

    suspend fun updateAutoReconnect(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_RECONNECT] = enabled }
    }

    suspend fun updateKillSwitch(enabled: Boolean) {
        context.dataStore.edit { it[KEY_KILL_SWITCH] = enabled }
    }

    suspend fun updateSplitTunneling(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SPLIT_ENABLED] = enabled }
    }

    suspend fun setExcludedApps(apps: Set<String>) {
        context.dataStore.edit { it[KEY_EXCLUDED_APPS] = apps }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[KEY_LANGUAGE] = lang }
    }

    suspend fun updateShowSpeed(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_SPEED] = enabled }
    }

    suspend fun updateNotifications(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIF_ENABLED] = enabled }
    }

    val autoConnectOnLaunch: Flow<Boolean> = settingsFlow.map { it.autoConnectOnBoot }
    val bypassRussianSites: Flow<Boolean> = settingsFlow.map { it.bypassRussianTraffic }
    val batteryOptimizationDismissed: Flow<Boolean> = settingsFlow.map { it.autoReconnect }
    val adBlockEnabled: Flow<Boolean> = settingsFlow.map { false }
    val killSwitchEnabled: Flow<Boolean> = settingsFlow.map { it.killSwitch }
    val dnsMode: Flow<String> = settingsFlow.map { it.dnsMode }

    fun setAutoConnectOnLaunch(enabled: Boolean) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            updateAutoBoot(enabled)
        }
    }

    fun setBypassRussianSites(enabled: Boolean) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            updateBypassRussianTraffic(enabled)
        }
    }

    fun setBatteryOptimizationDismissed(enabled: Boolean) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            updateAutoReconnect(enabled)
        }
    }

    fun setAdBlockEnabled(enabled: Boolean) {
        // Stored locally
    }

    fun setKillSwitchEnabled(enabled: Boolean) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            updateKillSwitch(enabled)
        }
    }
}
