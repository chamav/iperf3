package com.iperf3client.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.iperf3client.domain.model.Protocol
import com.iperf3client.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(
    private val context: Context
) : SettingsRepository {
    
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")
    
    companion object {
        // General settings
        private val MAX_DURATION_KEY = intPreferencesKey("max_duration")
        private val MAX_STREAMS_KEY = intPreferencesKey("max_streams")
        private val WIFI_ONLY_DEFAULT_KEY = booleanPreferencesKey("wifi_only_default")
        private val FOREGROUND_SERVICE_ONLY_KEY = booleanPreferencesKey("foreground_service_only")
        
        // Privacy settings
        private val COLLECT_DIAGNOSTICS_KEY = booleanPreferencesKey("collect_diagnostics")
        
        // Display settings
        private val DISPLAY_UNITS_KEY = stringPreferencesKey("display_units")
        private val THEME_KEY = stringPreferencesKey("theme")
        
        // Data retention
        private val RETENTION_DAYS_KEY = intPreferencesKey("retention_days")
        private val MAX_HISTORY_ENTRIES_KEY = intPreferencesKey("max_history_entries")
        
        // First run & onboarding
        private val FIRST_RUN_KEY = booleanPreferencesKey("first_run")
        private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
        
        // Default test settings
        private val DEFAULT_TEST_DURATION_KEY = intPreferencesKey("default_test_duration")
        private val DEFAULT_PARALLEL_STREAMS_KEY = intPreferencesKey("default_parallel_streams")
        private val DEFAULT_PROTOCOL_KEY = stringPreferencesKey("default_protocol")
        
        // UI settings
        private val KEEP_SCREEN_ON_KEY = booleanPreferencesKey("keep_screen_on")
        private val SHOW_NOTIFICATIONS_DURING_TEST_KEY = booleanPreferencesKey("show_notifications_during_test")
        private val AUTO_EXPORT_RESULTS_KEY = booleanPreferencesKey("auto_export_results")
        private val MAX_HISTORY_SIZE_KEY = intPreferencesKey("max_history_size")
        
        // Default values
        private const val DEFAULT_MAX_DURATION = 300 // 5 minutes
        private const val DEFAULT_MAX_STREAMS = 10
        private const val DEFAULT_WIFI_ONLY = false
        private const val DEFAULT_FOREGROUND_SERVICE_ONLY = true
        private const val DEFAULT_COLLECT_DIAGNOSTICS = false
        private const val DEFAULT_DISPLAY_UNITS = "Mbps"
        private const val DEFAULT_THEME = "system"
        private const val DEFAULT_RETENTION_DAYS = 30
        private const val DEFAULT_MAX_HISTORY_ENTRIES = 1000
        
        // New default values
        private const val DEFAULT_TEST_DURATION = 10
        private const val DEFAULT_PARALLEL_STREAMS = 1
        private const val DEFAULT_PROTOCOL = "TCP"
        private const val DEFAULT_KEEP_SCREEN_ON = true
        private const val DEFAULT_SHOW_NOTIFICATIONS_DURING_TEST = true
        private const val DEFAULT_AUTO_EXPORT_RESULTS = false
        private const val DEFAULT_MAX_HISTORY_SIZE = 100
    }
    
    // General settings
    override fun getMaxDuration(): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[MAX_DURATION_KEY] ?: DEFAULT_MAX_DURATION
        }
    }
    
    override suspend fun setMaxDuration(duration: Int) {
        context.dataStore.edit { preferences ->
            preferences[MAX_DURATION_KEY] = duration
        }
    }
    
    override fun getMaxStreams(): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[MAX_STREAMS_KEY] ?: DEFAULT_MAX_STREAMS
        }
    }
    
    override suspend fun setMaxStreams(streams: Int) {
        context.dataStore.edit { preferences ->
            preferences[MAX_STREAMS_KEY] = streams
        }
    }
    
    override fun getWifiOnlyDefault(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[WIFI_ONLY_DEFAULT_KEY] ?: DEFAULT_WIFI_ONLY
        }
    }
    
    override suspend fun setWifiOnlyDefault(wifiOnly: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[WIFI_ONLY_DEFAULT_KEY] = wifiOnly
        }
    }
    
    override fun getForegroundServiceOnly(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[FOREGROUND_SERVICE_ONLY_KEY] ?: DEFAULT_FOREGROUND_SERVICE_ONLY
        }
    }
    
    override suspend fun setForegroundServiceOnly(foregroundOnly: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FOREGROUND_SERVICE_ONLY_KEY] = foregroundOnly
        }
    }
    
    // Privacy settings
    override fun getCollectDiagnostics(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[COLLECT_DIAGNOSTICS_KEY] ?: DEFAULT_COLLECT_DIAGNOSTICS
        }
    }
    
    override suspend fun setCollectDiagnostics(collect: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[COLLECT_DIAGNOSTICS_KEY] = collect
        }
    }
    
    // Display settings
    override fun getDisplayUnits(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[DISPLAY_UNITS_KEY] ?: DEFAULT_DISPLAY_UNITS
        }
    }
    
    override suspend fun setDisplayUnits(units: String) {
        context.dataStore.edit { preferences ->
            preferences[DISPLAY_UNITS_KEY] = units
        }
    }
    
    override fun getTheme(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[THEME_KEY] ?: DEFAULT_THEME
        }
    }
    
    override suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }
    
    // Data retention
    override fun getRetentionDays(): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[RETENTION_DAYS_KEY] ?: DEFAULT_RETENTION_DAYS
        }
    }
    
    override suspend fun setRetentionDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[RETENTION_DAYS_KEY] = days
        }
    }
    
    override fun getMaxHistoryEntries(): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[MAX_HISTORY_ENTRIES_KEY] ?: DEFAULT_MAX_HISTORY_ENTRIES
        }
    }
    
    override suspend fun setMaxHistoryEntries(entries: Int) {
        context.dataStore.edit { preferences ->
            preferences[MAX_HISTORY_ENTRIES_KEY] = entries
        }
    }
    
    // First run & onboarding
    override fun isFirstRun(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[FIRST_RUN_KEY] ?: true
        }
    }
    
    override suspend fun setFirstRunCompleted() {
        context.dataStore.edit { preferences ->
            preferences[FIRST_RUN_KEY] = false
        }
    }
    
    override fun isOnboardingCompleted(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] ?: false
        }
    }
    
    override suspend fun setOnboardingCompleted() {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] = true
        }
    }
    
    // Clear all settings
    override suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    // Default test settings
    override fun getDefaultTestDuration(): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[DEFAULT_TEST_DURATION_KEY] ?: DEFAULT_TEST_DURATION
        }
    }
    
    override suspend fun setDefaultTestDuration(duration: Int) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_TEST_DURATION_KEY] = duration
        }
    }
    
    override fun getDefaultParallelStreams(): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[DEFAULT_PARALLEL_STREAMS_KEY] ?: DEFAULT_PARALLEL_STREAMS
        }
    }
    
    override suspend fun setDefaultParallelStreams(streams: Int) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_PARALLEL_STREAMS_KEY] = streams
        }
    }
    
    override fun getDefaultProtocol(): Flow<Protocol> {
        return context.dataStore.data.map { preferences ->
            val protocolString = preferences[DEFAULT_PROTOCOL_KEY] ?: DEFAULT_PROTOCOL
            try {
                Protocol.valueOf(protocolString)
            } catch (e: IllegalArgumentException) {
                Protocol.TCP
            }
        }
    }
    
    override suspend fun setDefaultProtocol(protocol: Protocol) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_PROTOCOL_KEY] = protocol.name
        }
    }
    
    // UI settings
    override fun getKeepScreenOn(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[KEEP_SCREEN_ON_KEY] ?: DEFAULT_KEEP_SCREEN_ON
        }
    }
    
    override suspend fun setKeepScreenOn(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEEP_SCREEN_ON_KEY] = enabled
        }
    }
    
    override fun getShowNotificationsDuringTest(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[SHOW_NOTIFICATIONS_DURING_TEST_KEY] ?: DEFAULT_SHOW_NOTIFICATIONS_DURING_TEST
        }
    }
    
    override suspend fun setShowNotificationsDuringTest(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_NOTIFICATIONS_DURING_TEST_KEY] = enabled
        }
    }
    
    override fun getAutoExportResults(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[AUTO_EXPORT_RESULTS_KEY] ?: DEFAULT_AUTO_EXPORT_RESULTS
        }
    }
    
    override suspend fun setAutoExportResults(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_EXPORT_RESULTS_KEY] = enabled
        }
    }
    
    override fun getMaxHistorySize(): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[MAX_HISTORY_SIZE_KEY] ?: DEFAULT_MAX_HISTORY_SIZE
        }
    }
    
    override suspend fun setMaxHistorySize(size: Int) {
        context.dataStore.edit { preferences ->
            preferences[MAX_HISTORY_SIZE_KEY] = size
        }
    }
}