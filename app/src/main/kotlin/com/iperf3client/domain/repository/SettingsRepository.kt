package com.iperf3client.domain.repository

import com.iperf3client.domain.model.Protocol
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    
    // General settings
    fun getMaxDuration(): Flow<Int>
    suspend fun setMaxDuration(duration: Int)
    
    fun getMaxStreams(): Flow<Int>
    suspend fun setMaxStreams(streams: Int)
    
    fun getWifiOnlyDefault(): Flow<Boolean>
    suspend fun setWifiOnlyDefault(wifiOnly: Boolean)
    
    fun getForegroundServiceOnly(): Flow<Boolean>
    suspend fun setForegroundServiceOnly(foregroundOnly: Boolean)
    
    // Privacy settings
    fun getCollectDiagnostics(): Flow<Boolean>
    suspend fun setCollectDiagnostics(collect: Boolean)
    
    // Units and display
    fun getDisplayUnits(): Flow<String>
    suspend fun setDisplayUnits(units: String)
    
    fun getTheme(): Flow<String>
    suspend fun setTheme(theme: String)
    
    // Data retention
    fun getRetentionDays(): Flow<Int>
    suspend fun setRetentionDays(days: Int)
    
    fun getMaxHistoryEntries(): Flow<Int>
    suspend fun setMaxHistoryEntries(entries: Int)
    
    // First run
    fun isFirstRun(): Flow<Boolean>
    suspend fun setFirstRunCompleted()
    
    // Onboarding
    fun isOnboardingCompleted(): Flow<Boolean>
    suspend fun setOnboardingCompleted()
    
    // Clear all settings
    suspend fun clearAll()
    
    // Default test settings
    fun getDefaultTestDuration(): Flow<Int>
    suspend fun setDefaultTestDuration(duration: Int)
    
    fun getDefaultParallelStreams(): Flow<Int>
    suspend fun setDefaultParallelStreams(streams: Int)
    
    fun getDefaultProtocol(): Flow<Protocol>
    suspend fun setDefaultProtocol(protocol: Protocol)
    
    // UI settings
    fun getKeepScreenOn(): Flow<Boolean>
    suspend fun setKeepScreenOn(enabled: Boolean)
    
    fun getShowNotificationsDuringTest(): Flow<Boolean>
    suspend fun setShowNotificationsDuringTest(enabled: Boolean)
    
    fun getAutoExportResults(): Flow<Boolean>
    suspend fun setAutoExportResults(enabled: Boolean)
    
    fun getMaxHistorySize(): Flow<Int>
    suspend fun setMaxHistorySize(size: Int)
}