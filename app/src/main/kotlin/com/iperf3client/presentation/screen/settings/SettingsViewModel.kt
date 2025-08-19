package com.iperf3client.presentation.screen.settings

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iperf3client.IperfApplication
import com.iperf3client.domain.model.Protocol
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val app = application as IperfApplication
    private val settingsRepository = app.settingsRepository
    
    var uiState by mutableStateOf(SettingsUiState())
        private set
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        combine(
            settingsRepository.getWifiOnlyDefault(),
            settingsRepository.getDefaultTestDuration(),
            settingsRepository.getDefaultParallelStreams(),
            settingsRepository.getDefaultProtocol(),
            settingsRepository.getKeepScreenOn(),
            settingsRepository.getShowNotificationsDuringTest(),
            settingsRepository.getAutoExportResults(),
            settingsRepository.getMaxHistorySize()
        ) { wifiOnly, duration, streams, protocol, keepScreen, notifications, autoExport, maxHistory ->
            SettingsUiState(
                wifiOnlyDefault = wifiOnly,
                defaultTestDuration = duration,
                defaultParallelStreams = streams,
                defaultProtocol = protocol,
                keepScreenOn = keepScreen,
                showNotificationsDuringTest = notifications,
                autoExportResults = autoExport,
                maxHistorySize = maxHistory
            )
        }.launchIn(viewModelScope.apply {
            launch {
                settingsRepository.getWifiOnlyDefault().collect { wifiOnly ->
                    uiState = uiState.copy(wifiOnlyDefault = wifiOnly)
                }
            }
        })
        
        // Load each setting individually to ensure proper state updates
        viewModelScope.launch {
            settingsRepository.getWifiOnlyDefault().collect { wifiOnly ->
                uiState = uiState.copy(wifiOnlyDefault = wifiOnly)
            }
        }
        
        viewModelScope.launch {
            settingsRepository.getDefaultTestDuration().collect { duration ->
                uiState = uiState.copy(defaultTestDuration = duration)
            }
        }
        
        viewModelScope.launch {
            settingsRepository.getDefaultParallelStreams().collect { streams ->
                uiState = uiState.copy(defaultParallelStreams = streams)
            }
        }
        
        viewModelScope.launch {
            settingsRepository.getDefaultProtocol().collect { protocol ->
                uiState = uiState.copy(defaultProtocol = protocol)
            }
        }
        
        viewModelScope.launch {
            settingsRepository.getKeepScreenOn().collect { keepScreen ->
                uiState = uiState.copy(keepScreenOn = keepScreen)
            }
        }
        
        viewModelScope.launch {
            settingsRepository.getShowNotificationsDuringTest().collect { notifications ->
                uiState = uiState.copy(showNotificationsDuringTest = notifications)
            }
        }
        
        viewModelScope.launch {
            settingsRepository.getAutoExportResults().collect { autoExport ->
                uiState = uiState.copy(autoExportResults = autoExport)
            }
        }
        
        viewModelScope.launch {
            settingsRepository.getMaxHistorySize().collect { maxHistory ->
                uiState = uiState.copy(maxHistorySize = maxHistory)
            }
        }
    }
    
    fun setWifiOnlyDefault(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setWifiOnlyDefault(enabled)
        }
    }
    
    fun setDefaultTestDuration(duration: Int) {
        if (duration > 0) {
            viewModelScope.launch {
                settingsRepository.setDefaultTestDuration(duration)
            }
        }
    }
    
    fun setDefaultParallelStreams(streams: Int) {
        if (streams > 0) {
            viewModelScope.launch {
                settingsRepository.setDefaultParallelStreams(streams)
            }
        }
    }
    
    fun setDefaultProtocol(protocol: Protocol) {
        viewModelScope.launch {
            settingsRepository.setDefaultProtocol(protocol)
        }
    }
    
    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setKeepScreenOn(enabled)
        }
    }
    
    fun setShowNotificationsDuringTest(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowNotificationsDuringTest(enabled)
        }
    }
    
    fun setAutoExportResults(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoExportResults(enabled)
        }
    }
    
    fun setMaxHistorySize(size: Int) {
        if (size >= 0) {
            viewModelScope.launch {
                settingsRepository.setMaxHistorySize(size)
            }
        }
    }
    
    fun resetToDefaults() {
        viewModelScope.launch {
            settingsRepository.setWifiOnlyDefault(false)
            settingsRepository.setDefaultTestDuration(10)
            settingsRepository.setDefaultParallelStreams(1)
            settingsRepository.setDefaultProtocol(Protocol.TCP)
            settingsRepository.setKeepScreenOn(true)
            settingsRepository.setShowNotificationsDuringTest(true)
            settingsRepository.setAutoExportResults(false)
            settingsRepository.setMaxHistorySize(100)
        }
    }
}

data class SettingsUiState(
    val wifiOnlyDefault: Boolean = false,
    val defaultTestDuration: Int = 10,
    val defaultParallelStreams: Int = 1,
    val defaultProtocol: Protocol = Protocol.TCP,
    val keepScreenOn: Boolean = true,
    val showNotificationsDuringTest: Boolean = true,
    val autoExportResults: Boolean = false,
    val maxHistorySize: Int = 100
)