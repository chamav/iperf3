package com.iperf3client.presentation.screen.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iperf3client.IperfApplication
import com.iperf3client.data.engine.IperfEngineImpl
import com.iperf3client.data.utils.Logger
import com.iperf3client.domain.model.*
import com.iperf3client.domain.repository.RecentHostsRepository
import com.iperf3client.domain.usecase.ManageServersUseCase
import com.iperf3client.domain.usecase.RunTestUseCase
import com.iperf3client.domain.usecase.TestExecutionResult
import com.iperf3client.data.repository.RecentHostsRepositoryImpl
import com.iperf3client.service.IperfTestService
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.Intent
import android.os.IBinder

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    
    private val app = application as IperfApplication
    private var serviceBinder: IperfTestService.IperfTestBinder? = null
    private var serviceConnection: ServiceConnection? = null
    
    private val runTestUseCase = RunTestUseCase(
        iperfEngine = IperfEngineImpl(application),
        historyRepository = app.historyRepository,
        settingsRepository = app.settingsRepository,
        context = application
    )
    
    private val manageServersUseCase = ManageServersUseCase(
        serversRepository = app.serversRepository
    )
    
    private val recentHostsRepository: RecentHostsRepository = RecentHostsRepositoryImpl(application)
    
    var uiState by mutableStateOf(HomeUiState())
        private set
    
    companion object {
        private const val TAG = "HomeViewModel"
    }
    
    init {
        bindToService()
    }
    
    private fun bindToService() {
        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                Logger.i(TAG, "Connected to IperfTestService")
                serviceBinder = service as? IperfTestService.IperfTestBinder
                serviceBinder?.apply {
                    addStatusCallback { status ->
                        uiState = uiState.copy(testStatus = status)
                    }
                    addProgressCallback { event ->
                        handleServiceEvent(event)
                    }
                }
            }
            
            override fun onServiceDisconnected(name: ComponentName?) {
                Logger.i(TAG, "Disconnected from IperfTestService")
                serviceBinder = null
            }
        }
        
        val intent = Intent(getApplication(), IperfTestService::class.java)
        getApplication<Application>().bindService(intent, serviceConnection!!, Context.BIND_AUTO_CREATE)
    }
    
    private fun handleServiceEvent(event: IperfEvent) {
        when (event) {
            is IperfEvent.Started -> {
                uiState = uiState.copy(
                    currentSessionId = event.sessionId,
                    testStatus = TestStatus.RUNNING
                )
            }
            is IperfEvent.Progress -> {
                val timeline = uiState.timeline + event.tick
                uiState = uiState.copy(
                    currentTick = event.tick,
                    timeline = timeline
                )
            }
            is IperfEvent.Completed -> {
                Logger.i(TAG, "Test completed event received")
                Logger.d(TAG, "Result summary: avg=${event.result.summary?.avgMbps}, " +
                        "max=${event.result.summary?.maxMbps}, min=${event.result.summary?.minMbps}")
                Logger.d(TAG, "Timeline size: ${event.result.timeline.size}")
                
                uiState = uiState.copy(
                    testStatus = TestStatus.COMPLETED,
                    currentTestResult = event.result,
                    timeline = event.result.timeline
                )
            }
            is IperfEvent.Error -> {
                uiState = uiState.copy(
                    testStatus = TestStatus.ERROR,
                    validationError = event.error
                )
            }
            is IperfEvent.Log -> {
                // Log events are handled internally
                Logger.d(TAG, "Log event: ${event.line}")
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        Logger.d(TAG, "HomeViewModel cleared")
        serviceConnection?.let {
            getApplication<Application>().unbindService(it)
        }
        serviceBinder = null
    }
    
    init {
        Logger.d(TAG, "HomeViewModel initialized")
        loadInitialData()
    }
    
    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                Logger.d(TAG, "Loading initial data")
                // Load default server
                val defaultServer = manageServersUseCase.getDefaultServer()
                defaultServer?.let { server ->
                    Logger.d(TAG, "Loaded default server: ${server.host}:${server.port}")
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
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to load initial data", e)
                uiState = uiState.copy(
                    validationError = "Failed to initialize: ${e.message}"
                )
            }
        }
        
        // Observe servers
        try {
            manageServersUseCase.observeServers().onEach { servers ->
                Logger.d(TAG, "Servers updated: ${servers.size} servers available")
                uiState = uiState.copy(availableServers = servers)
            }.launchIn(viewModelScope)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to observe servers", e)
        }
        
        // Observe recent hosts
        try {
            recentHostsRepository.getRecentHosts().onEach { recentHosts ->
                Logger.d(TAG, "Recent hosts updated: ${recentHosts.size} hosts available")
                uiState = uiState.copy(recentHosts = recentHosts)
            }.launchIn(viewModelScope)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to observe recent hosts", e)
        }
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
            Logger.w(TAG, "Attempted to start test while another test is running")
            return
        }
        
        if (!uiState.testParams.isValid()) {
            Logger.w(TAG, "Invalid test parameters: ${uiState.testParams}")
            uiState = uiState.copy(
                validationError = "Invalid test parameters"
            )
            return
        }
        
        val params = uiState.testParams
        Logger.i(TAG, "Starting iperf3 test: host=${params.host}, port=${params.port}, protocol=${params.protocol}, duration=${params.durationSec}s, streams=${params.parallelStreams}, reverse=${params.reverse}")
        Logger.d(TAG, "Full test parameters: $params")
        
        // Save host to recent hosts
        viewModelScope.launch {
            try {
                recentHostsRepository.addHost(uiState.testParams.host)
                Logger.d(TAG, "Host added to recent hosts: ${uiState.testParams.host}")
            } catch (e: Exception) {
                Logger.w(TAG, "Failed to save host to recent hosts", e)
            }
        }
        
        uiState = uiState.copy(
            testStatus = TestStatus.RUNNING,
            currentTestResult = null,
            timeline = emptyList(),
            validationError = null
        )
        
        try {
            // Start foreground service for background testing
            // The service will handle the test execution and send updates
            IperfTestService.startTest(getApplication(), uiState.testParams)
            
            // DO NOT run test directly here - it will cause double execution!
            // The service is already running the test and will send updates via callbacks
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to start test service", e)
            uiState = uiState.copy(
                testStatus = TestStatus.ERROR,
                validationError = "Failed to start test: ${e.message}"
            )
        }
    }
    
    fun stopTest() {
        if (uiState.testStatus == TestStatus.RUNNING) {
            Logger.i(TAG, "Stopping test, session: ${uiState.currentSessionId}")
            try {
                IperfTestService.stopTest(getApplication())
                uiState = uiState.copy(
                    testStatus = TestStatus.CANCELLED
                )
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to stop test", e)
                uiState = uiState.copy(
                    testStatus = TestStatus.ERROR,
                    validationError = "Failed to stop test: ${e.message}"
                )
            }
        } else {
            Logger.w(TAG, "Attempted to stop test but no test is running")
        }
    }
    
    private fun handleTestResult(result: TestExecutionResult) {
        try {
            when (result) {
                is TestExecutionResult.Started -> {
                    Logger.i(TAG, "Test started, session: ${result.sessionId}")
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
                    Logger.i(TAG, "Test completed successfully, avg speed: ${result.result.summary?.avgMbps ?: "unknown"} Mbps")
                    uiState = uiState.copy(
                        testStatus = TestStatus.COMPLETED,
                        currentTestResult = result.result,
                        timeline = result.result.timeline
                    )
                }
                is TestExecutionResult.Error -> {
                    Logger.w(TAG, "Test error: ${result.message}")
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
        } catch (e: Exception) {
            Logger.e(TAG, "Error handling test result: $result", e)
            uiState = uiState.copy(
                testStatus = TestStatus.ERROR,
                validationError = "Internal error: ${e.message}"
            )
        }
    }
    
    fun clearError() {
        uiState = uiState.copy(validationError = null)
    }
    
    fun resetTest() {
        Logger.d(TAG, "Resetting test state")
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
    
    fun clearRecentHosts() {
        viewModelScope.launch {
            try {
                recentHostsRepository.clearRecentHosts()
                Logger.d(TAG, "Recent hosts cleared")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to clear recent hosts", e)
            }
        }
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
    val recentHosts: List<RecentHost> = emptyList(),
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