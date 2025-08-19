package com.iperf3client.presentation.screen.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iperf3client.IperfApplication
import com.iperf3client.data.engine.IperfEngineImpl
import com.iperf3client.domain.model.*
import com.iperf3client.domain.usecase.ManageServersUseCase
import com.iperf3client.domain.usecase.RunTestUseCase
import com.iperf3client.domain.usecase.TestExecutionResult
import com.iperf3client.service.IperfTestService
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    
    private val app = application as IperfApplication
    
    private val runTestUseCase = RunTestUseCase(
        iperfEngine = IperfEngineImpl(application),
        historyRepository = app.historyRepository,
        settingsRepository = app.settingsRepository,
        context = application
    )
    
    private val manageServersUseCase = ManageServersUseCase(
        serversRepository = app.serversRepository
    )
    
    var uiState by mutableStateOf(HomeUiState())
        private set
    
    init {
        loadInitialData()
    }
    
    private fun loadInitialData() {
        viewModelScope.launch {
            // Load default server
            val defaultServer = manageServersUseCase.getDefaultServer()
            defaultServer?.let { server ->
                uiState = uiState.copy(
                    testParams = uiState.testParams.copy(
                        host = server.host,
                        port = server.port,
                        protocol = server.defaultProtocol
                    )
                )
            }
            
            // Load settings
            app.settingsRepository.getWifiOnlyDefault().onEach { wifiOnly ->
                uiState = uiState.copy(
                    testParams = uiState.testParams.copy(onlyWifi = wifiOnly)
                )
            }.launchIn(viewModelScope)
        }
        
        // Observe servers
        manageServersUseCase.observeServers().onEach { servers ->
            uiState = uiState.copy(availableServers = servers)
        }.launchIn(viewModelScope)
    }
    
    fun onHostChange(host: String) {
        uiState = uiState.copy(
            testParams = uiState.testParams.copy(host = host),
            validationError = null
        )
    }
    
    fun onPortChange(port: String) {
        val portInt = port.toIntOrNull()
        if (portInt != null && portInt in 1..65535) {
            uiState = uiState.copy(
                testParams = uiState.testParams.copy(port = portInt),
                validationError = null
            )
        }
    }
    
    fun onProtocolChange(protocol: Protocol) {
        uiState = uiState.copy(
            testParams = uiState.testParams.copy(
                protocol = protocol,
                udpBitrateMbps = if (protocol == Protocol.UDP) 50f else null
            ),
            validationError = null
        )
    }
    
    fun onDurationChange(duration: String) {
        val durationInt = duration.toIntOrNull()
        if (durationInt != null && durationInt > 0) {
            uiState = uiState.copy(
                testParams = uiState.testParams.copy(durationSec = durationInt),
                validationError = null
            )
        }
    }
    
    fun onParallelStreamsChange(streams: String) {
        val streamsInt = streams.toIntOrNull()
        if (streamsInt != null && streamsInt > 0) {
            uiState = uiState.copy(
                testParams = uiState.testParams.copy(parallelStreams = streamsInt),
                validationError = null
            )
        }
    }
    
    fun onReverseChange(reverse: Boolean) {
        uiState = uiState.copy(
            testParams = uiState.testParams.copy(reverse = reverse),
            validationError = null
        )
    }
    
    fun onUdpBitrateChange(bitrate: String) {
        val bitrateFloat = bitrate.toFloatOrNull()
        if (bitrateFloat != null && bitrateFloat > 0) {
            uiState = uiState.copy(
                testParams = uiState.testParams.copy(udpBitrateMbps = bitrateFloat),
                validationError = null
            )
        }
    }
    
    fun onWifiOnlyChange(wifiOnly: Boolean) {
        uiState = uiState.copy(
            testParams = uiState.testParams.copy(onlyWifi = wifiOnly),
            validationError = null
        )
    }
    
    fun onServerSelected(server: ServerItem) {
        uiState = uiState.copy(
            testParams = uiState.testParams.copy(
                host = server.host,
                port = server.port,
                protocol = server.defaultProtocol
            ),
            validationError = null
        )
    }
    
    fun startTest() {
        if (uiState.testStatus == TestStatus.RUNNING) {
            return
        }
        
        if (!uiState.testParams.isValid()) {
            uiState = uiState.copy(
                validationError = "Invalid test parameters"
            )
            return
        }
        
        uiState = uiState.copy(
            testStatus = TestStatus.RUNNING,
            currentTestResult = null,
            timeline = emptyList(),
            validationError = null
        )
        
        // Start foreground service for background testing
        IperfTestService.startTest(getApplication(), uiState.testParams)
        
        // Also run directly for UI updates
        viewModelScope.launch {
            runTestUseCase(uiState.testParams)
                .onEach { result ->
                    handleTestResult(result)
                }
                .launchIn(this)
        }
    }
    
    fun stopTest() {
        if (uiState.testStatus == TestStatus.RUNNING) {
            IperfTestService.stopTest(getApplication())
            uiState = uiState.copy(
                testStatus = TestStatus.CANCELLED
            )
        }
    }
    
    private fun handleTestResult(result: TestExecutionResult) {
        when (result) {
            is TestExecutionResult.Started -> {
                uiState = uiState.copy(
                    testStatus = TestStatus.RUNNING,
                    currentSessionId = result.sessionId
                )
            }
            is TestExecutionResult.Progress -> {
                val updatedTimeline = uiState.timeline + result.tick
                uiState = uiState.copy(
                    timeline = updatedTimeline,
                    currentTick = result.tick
                )
            }
            is TestExecutionResult.Completed -> {
                uiState = uiState.copy(
                    testStatus = TestStatus.COMPLETED,
                    currentTestResult = result.result,
                    timeline = result.result.timeline
                )
            }
            is TestExecutionResult.Error -> {
                uiState = uiState.copy(
                    testStatus = TestStatus.ERROR,
                    validationError = result.message
                )
            }
            is TestExecutionResult.Log -> {
                val updatedLogs = (uiState.logs + result.line).takeLast(100) // Keep last 100 lines
                uiState = uiState.copy(logs = updatedLogs)
            }
        }
    }
    
    fun clearError() {
        uiState = uiState.copy(validationError = null)
    }
    
    fun resetTest() {
        uiState = uiState.copy(
            testStatus = TestStatus.IDLE,
            currentTestResult = null,
            timeline = emptyList(),
            currentTick = null,
            currentSessionId = null,
            logs = emptyList(),
            validationError = null
        )
    }
}

data class HomeUiState(
    val testParams: TestParams = TestParams(
        host = "",
        port = 5201,
        protocol = Protocol.TCP,
        durationSec = 10,
        parallelStreams = 1,
        reverse = false,
        udpBitrateMbps = null,
        onlyWifi = false
    ),
    val testStatus: TestStatus = TestStatus.IDLE,
    val currentTestResult: TestResult? = null,
    val timeline: List<LiveMetricsTick> = emptyList(),
    val currentTick: LiveMetricsTick? = null,
    val currentSessionId: String? = null,
    val logs: List<String> = emptyList(),
    val availableServers: List<ServerItem> = emptyList(),
    val validationError: String? = null
) {
    val isTestRunning: Boolean
        get() = testStatus == TestStatus.RUNNING
    
    val canStartTest: Boolean
        get() = testStatus != TestStatus.RUNNING && testParams.isValid()
    
    val currentSpeed: Float?
        get() = currentTick?.throughputMbps
    
    val averageSpeed: Float?
        get() = if (timeline.isNotEmpty()) {
            timeline.map { it.throughputMbps }.average().toFloat()
        } else null
    
    val maxSpeed: Float?
        get() = timeline.maxOfOrNull { it.throughputMbps }
    
    val minSpeed: Float?
        get() = timeline.minOfOrNull { it.throughputMbps }
}