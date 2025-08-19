package com.iperf3client.domain.usecase

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.iperf3client.domain.model.*
import com.iperf3client.domain.repository.HistoryRepository
import com.iperf3client.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class RunTestUseCase(
    private val iperfEngine: IperfEngine,
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val context: Context
) {
    
    suspend operator fun invoke(params: TestParams): Flow<TestExecutionResult> {
        // Validate network conditions
        val networkValidation = validateNetworkConditions(params)
        if (networkValidation != null) {
            return kotlinx.coroutines.flow.flowOf(TestExecutionResult.Error(networkValidation))
        }
        
        // Validate test parameters against settings
        val paramValidation = validateTestParameters(params)
        if (paramValidation != null) {
            return kotlinx.coroutines.flow.flowOf(TestExecutionResult.Error(paramValidation))
        }
        
        // Start the test
        return iperfEngine.startClient(params)
            .map { event ->
                when (event) {
                    is IperfEvent.Started -> TestExecutionResult.Started(event.sessionId)
                    is IperfEvent.Progress -> TestExecutionResult.Progress(event.tick)
                    is IperfEvent.Completed -> {
                        // Save to history
                        val savedId = historyRepository.insert(event.result)
                        TestExecutionResult.Completed(event.result.copy(id = savedId))
                    }
                    is IperfEvent.Error -> TestExecutionResult.Error(event.error)
                    is IperfEvent.Log -> TestExecutionResult.Log(event.line)
                }
            }
            .onEach { result ->
                // Handle intermediate results if needed
                when (result) {
                    is TestExecutionResult.Started -> {
                        // Could save initial test record here
                    }
                    is TestExecutionResult.Progress -> {
                        // Could update live progress in database
                    }
                    else -> { /* No action needed */ }
                }
            }
    }
    
    private suspend fun validateNetworkConditions(params: TestParams): String? {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        
        if (networkCapabilities == null) {
            return "No active network connection"
        }
        
        // Check WiFi only requirement
        if (params.onlyWifi && !networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return "WiFi connection required but not available"
        }
        
        // Check if network has internet capability
        if (!networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return "Network does not have internet capability"
        }
        
        return null
    }
    
    private suspend fun validateTestParameters(params: TestParams): String? {
        val maxDuration = settingsRepository.getMaxDuration().first()
        val maxStreams = settingsRepository.getMaxStreams().first()
        
        if (params.durationSec > maxDuration) {
            return "Test duration exceeds maximum allowed ($maxDuration seconds)"
        }
        
        if (params.parallelStreams > maxStreams) {
            return "Number of parallel streams exceeds maximum allowed ($maxStreams)"
        }
        
        if (!params.isValid()) {
            return "Invalid test parameters"
        }
        
        return null
    }
}

sealed class TestExecutionResult {
    data class Started(val sessionId: String) : TestExecutionResult()
    data class Progress(val tick: LiveMetricsTick) : TestExecutionResult()
    data class Completed(val result: TestResult) : TestExecutionResult()
    data class Error(val message: String) : TestExecutionResult()
    data class Log(val line: String) : TestExecutionResult()
}