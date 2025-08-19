package com.iperf3client.domain.repository

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
}