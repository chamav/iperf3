package com.iperf3client.domain.model

import kotlinx.coroutines.flow.Flow

interface IperfEngine {
    
    /**
     * Start a new iperf3 client test
     * @param params Test parameters
     * @return Flow of live metrics during the test
     */
    fun startClient(params: TestParams): Flow<IperfEvent>
    
    /**
     * Stop the currently running test
     * @param sessionId ID of the session to stop
     */
    suspend fun stop(sessionId: String)
    
    /**
     * Check if a test is currently running
     */
    fun isRunning(): Boolean
    
    /**
     * Get the current session ID
     */
    fun getCurrentSessionId(): String?
}

sealed class IperfEvent {
    data class Started(val sessionId: String) : IperfEvent()
    data class Progress(val tick: LiveMetricsTick) : IperfEvent()
    data class Completed(val result: TestResult) : IperfEvent()
    data class Error(val error: String, val sessionId: String? = null) : IperfEvent()
    data class Log(val line: String) : IperfEvent()
}

interface ProgressListener {
    fun onStarted(sessionId: String)
    fun onProgress(tick: LiveMetricsTick)
    fun onCompleted(result: TestResult)
    fun onError(error: String)
    fun onLog(line: String)
}