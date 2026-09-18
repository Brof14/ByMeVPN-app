package com.example.bymevpn.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "bymevpn_settings")

data class AppSettings(
    val protocol: String = "VLESS + Reality",
    val bypassRussianTraffic: Boolean = true, // Default ON: Russian traffic bypasses VPN directly
    val autoConnectOnBoot: Boolean = false,
    val autoConnectOpenWifi: Boolean = false,
    val autoReconnect: Boolean = true,
    val killSwitch: Boolean = false,
    val splitTunnelingEnabled: Boolean = false,
    val excludedApps: Set<String> = emptySet(),
    val language: String = "system", // ru | en | system
    val showSpeedInNotification: Boolean = true,
    val connectionNotifications: Boolean = true
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
            bypassRussianTraffic = prefs[KEY_BYPASS_RU] ?: true,
            autoConnectOnBoot = prefs[KEY_AUTO_BOOT] ?: false,
            autoConnectOpenWifi = prefs[KEY_AUTO_WIFI] ?: false,
            autoReconnect = prefs[KEY_AUTO_RECONNECT] ?: true,
            killSwitch = prefs[KEY_KILL_SWITCH] ?: false,
            splitTunnelingEnabled = prefs[KEY_SPLIT_ENABLED] ?: false,
            excludedApps = prefs[KEY_EXCLUDED_APPS] ?: emptySet(),
            language = prefs[KEY_LANGUAGE] ?: "system",
            showSpeedInNotification = prefs[KEY_SHOW_SPEED] ?: true,
            connectionNotifications = prefs[KEY_NOTIF_ENABLED] ?: true
        )
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
}
