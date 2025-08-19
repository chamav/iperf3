package com.iperf3client.data.engine

import android.content.Context
import android.util.Log
import com.iperf3client.domain.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import java.util.*
import kotlin.random.Random

class IperfEngineImpl(
    private val context: Context
) : IperfEngine {
    
    private var currentSessionId: String? = null
    private var isTestRunning = false
    
    private val json = Json { ignoreUnknownKeys = true }
    
    companion object {
        private const val TAG = "IperfEngine"
        private const val IPERF3_BINARY = "iperf3"
    }
    
    override fun startClient(params: TestParams): Flow<IperfEvent> = flow {
        if (isTestRunning) {
            emit(IperfEvent.Error("Test already running"))
            return@flow
        }
        
        val sessionId = UUID.randomUUID().toString()
        currentSessionId = sessionId
        isTestRunning = true
        
        try {
            emit(IperfEvent.Started(sessionId))
            Log.d(TAG, "Starting iperf3 test with params: $params")
            
            // Validate parameters
            if (!params.isValid()) {
                emit(IperfEvent.Error("Invalid test parameters", sessionId))
                return@flow
            }
            
            // Start test simulation or real iperf3 execution
            val startTime = Instant.now()
            val timeline = mutableListOf<LiveMetricsTick>()
            
            // Simulate test execution
            for (second in 1..params.durationSec) {
                if (!isTestRunning) {
                    emit(IperfEvent.Error("Test cancelled", sessionId))
                    return@flow
                }
                
                val tick = generateMockTick(second, params.protocol)
                timeline.add(tick)
                emit(IperfEvent.Progress(tick))
                emit(IperfEvent.Log("[$second/${params.durationSec}] ${tick.throughputMbps} Mbits/sec"))
                
                delay(1000) // Simulate 1 second interval
            }
            
            val endTime = Instant.now()
            val summary = calculateSummary(timeline, params.protocol)
            
            val result = TestResult(
                startedAt = startTime,
                finishedAt = endTime,
                params = params,
                summary = summary,
                timeline = timeline,
                status = TestStatus.COMPLETED,
                rawLogPath = saveRawLog(sessionId, timeline)
            )
            
            emit(IperfEvent.Completed(result))
            Log.d(TAG, "Test completed successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Test failed", e)
            emit(IperfEvent.Error(e.message ?: "Unknown error", sessionId))
        } finally {
            isTestRunning = false
            currentSessionId = null
        }
    }
    
    override suspend fun stop(sessionId: String) {
        if (currentSessionId == sessionId) {
            isTestRunning = false
            Log.d(TAG, "Stopping test session: $sessionId")
        }
    }
    
    override fun isRunning(): Boolean = isTestRunning
    
    override fun getCurrentSessionId(): String? = currentSessionId
    
    private fun generateMockTick(second: Int, protocol: Protocol): LiveMetricsTick {
        // Generate realistic network performance data
        val baseSpeed = when (protocol) {
            Protocol.TCP -> Random.nextFloat() * 900 + 100 // 100-1000 Mbps
            Protocol.UDP -> Random.nextFloat() * 500 + 50  // 50-550 Mbps
        }
        
        // Add some variance based on time
        val variance = Math.sin(second * 0.5) * 50
        val speed = (baseSpeed + variance).coerceAtLeast(1f)
        
        return when (protocol) {
            Protocol.TCP -> LiveMetricsTick(
                second = second,
                throughputMbps = speed,
                retransmits = if (Random.nextFloat() < 0.1) Random.nextInt(0, 5) else null,
                rttMs = Random.nextFloat() * 50 + 10 // 10-60ms RTT
            )
            Protocol.UDP -> LiveMetricsTick(
                second = second,
                throughputMbps = speed,
                jitterMs = Random.nextFloat() * 5 + 0.1f, // 0.1-5.1ms jitter
                packetLossPct = Random.nextFloat() * 2f // 0-2% packet loss
            )
        }
    }
    
    private fun calculateSummary(timeline: List<LiveMetricsTick>, protocol: Protocol): TestSummary {
        val speeds = timeline.map { it.throughputMbps }
        val avgSpeed = speeds.average().toFloat()
        val maxSpeed = speeds.maxOrNull() ?: 0f
        val minSpeed = speeds.minOrNull() ?: 0f
        
        return when (protocol) {
            Protocol.TCP -> TestSummary(
                avgMbps = avgSpeed,
                maxMbps = maxSpeed,
                minMbps = minSpeed,
                retransmits = timeline.mapNotNull { it.retransmits }.sum(),
                rttMs = timeline.mapNotNull { it.rttMs }.average().toFloat(),
                totalBytes = (avgSpeed * timeline.size * 1024 * 1024 / 8).toLong() // Approximate bytes
            )
            Protocol.UDP -> TestSummary(
                avgMbps = avgSpeed,
                maxMbps = maxSpeed,
                minMbps = minSpeed,
                jitterMs = timeline.mapNotNull { it.jitterMs }.average().toFloat(),
                packetLossPct = timeline.mapNotNull { it.packetLossPct }.average().toFloat(),
                totalBytes = (avgSpeed * timeline.size * 1024 * 1024 / 8).toLong()
            )
        }
    }
    
    private fun saveRawLog(sessionId: String, timeline: List<LiveMetricsTick>): String {
        val logDir = File(context.filesDir, "logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        
        val logFile = File(logDir, "iperf3_$sessionId.log")
        
        try {
            logFile.writeText(buildString {
                appendLine("iperf3 simulation log - Session: $sessionId")
                appendLine("Timestamp: ${Instant.now()}")
                appendLine("---")
                
                timeline.forEach { tick ->
                    appendLine("[${tick.second}] ${tick.throughputMbps} Mbits/sec ${tick.jitterMs?.let { "jitter: ${it}ms" } ?: ""} ${tick.packetLossPct?.let { "loss: ${it}%" } ?: ""} ${tick.retransmits?.let { "retr: $it" } ?: ""}")
                }
            })
            
            return logFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save log file", e)
            return ""
        }
    }
    
    // TODO: Replace with actual iperf3 binary execution
    private suspend fun executeIperf3(params: TestParams): Flow<String> = flow {
        // This is where the real iperf3 binary would be executed
        // For now, we simulate the output
        
        val command = buildList {
            add(getIperf3BinaryPath())
            addAll(params.toCommandArgs())
        }
        
        Log.d(TAG, "Would execute: ${command.joinToString(" ")}")
        
        // Real implementation would use ProcessBuilder or NDK
        // and parse actual iperf3 JSON output
        
        emit("Simulated iperf3 output")
    }
    
    private fun getIperf3BinaryPath(): String {
        // In real implementation, extract iperf3 binary from assets
        // to internal storage and return the path
        return "${context.filesDir}/$IPERF3_BINARY"
    }
    
    private fun extractIperf3Binary(): Boolean {
        try {
            val assetManager = context.assets
            val inputStream = assetManager.open(IPERF3_BINARY)
            val outputFile = File(context.filesDir, IPERF3_BINARY)
            
            outputFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            
            // Make executable
            outputFile.setExecutable(true)
            
            Log.d(TAG, "iperf3 binary extracted successfully")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract iperf3 binary", e)
            return false
        }
    }
}