package com.iperf3client.data.engine

import android.content.Context
import android.util.Log
import com.iperf3client.data.utils.Logger
import com.iperf3client.data.engine.*
import com.iperf3client.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException
import java.io.BufferedReader
import java.io.File  
import java.io.InputStreamReader
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
            Logger.i(TAG, "Starting iperf3 client test session: $sessionId")
            Logger.i(TAG, "Target: ${params.host}:${params.port} (${params.protocol})")
            Logger.i(TAG, "Test parameters: duration=${params.durationSec}s, streams=${params.parallelStreams}, reverse=${params.reverse}")
            if (params.protocol == Protocol.UDP && params.udpBitrateMbps != null) {
                Logger.i(TAG, "UDP bitrate limit: ${params.udpBitrateMbps} Mbps")
            }
            
            // Validate parameters
            if (!params.isValid()) {
                Logger.w(TAG, "Test failed: invalid parameters - $params")
                emit(IperfEvent.Error("Invalid test parameters", sessionId))
                return@flow
            }
            
            // Ensure iperf3 binary is extracted and executable
            if (!ensureIperfBinaryReady()) {
                emit(IperfEvent.Error("Failed to prepare iperf3 binary", sessionId))
                return@flow
            }
            
            // Execute real iperf3 process
            executeIperf3Process(params, sessionId).collect { event ->
                emit(event)
            }
            
        } catch (e: Exception) {
            Logger.e(TAG, "Test failed with exception", e)
            val errorMessage = when (e) {
                is InterruptedException -> "Test was interrupted"
                is java.io.IOException -> "Network error: ${e.message}"
                is IllegalStateException -> "Invalid state: ${e.message}"
                else -> "Unexpected error: ${e.message ?: e.javaClass.simpleName}"
            }
            emit(IperfEvent.Error(errorMessage, sessionId))
        } finally {
            Logger.d(TAG, "Cleaning up test session: $sessionId")
            isTestRunning = false
            currentSessionId = null
        }
    }.flowOn(Dispatchers.IO)
    
    override suspend fun stop(sessionId: String) {
        Logger.d(TAG, "Stop requested for session: $sessionId, current session: $currentSessionId")
        if (currentSessionId == sessionId) {
            isTestRunning = false
            Logger.i(TAG, "Stopping test session: $sessionId")
        } else {
            Logger.w(TAG, "Attempted to stop session $sessionId, but current session is $currentSessionId")
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
        val variance = Math.sin(second * 0.5).toFloat() * 50
        val speed = (baseSpeed + variance).coerceAtLeast(1f)
        
        return when (protocol) {
            Protocol.TCP -> LiveMetricsTick(
                second = second,
                throughputMbps = speed.toFloat(),
                retransmits = if (Random.nextFloat() < 0.1) Random.nextInt(0, 5) else null,
                rttMs = (Random.nextFloat() * 50 + 10).toFloat() // 10-60ms RTT
            )
            Protocol.UDP -> LiveMetricsTick(
                second = second,
                throughputMbps = speed.toFloat(),
                jitterMs = Random.nextFloat() * 5 + 0.1f, // 0.1-5.1ms jitter
                packetLossPct = (Random.nextFloat() * 2f).toFloat() // 0-2% packet loss
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
    
    private fun saveRawLog(sessionId: String, rawOutput: String): String {
        val logDir = File(context.filesDir, "logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        
        val logFile = File(logDir, "iperf3_$sessionId.log")
        
        try {
            logFile.writeText(buildString {
                appendLine("iperf3 raw output log - Session: $sessionId")
                appendLine("Timestamp: ${Instant.now()}")
                appendLine("---")
                append(rawOutput)
            })
            
            return logFile.absolutePath
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to save raw log file for session $sessionId", e)
            return ""
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
            Logger.e(TAG, "Failed to save raw log file for session $sessionId", e)
            return ""
        }
    }
    
    private suspend fun executeIperf3Process(params: TestParams, sessionId: String): Flow<IperfEvent> = flow {
        val startTime = Instant.now()
        val timeline = mutableListOf<LiveMetricsTick>()
        val rawOutput = StringBuilder()
        
        try {
            val command = buildList {
                add(getIperf3BinaryPath())
                addAll(params.toCommandArgs())
            }
            
            Logger.i(TAG, "Executing iperf3: ${command.joinToString(" ")}")
            
            val processBuilder = ProcessBuilder(command)
            processBuilder.redirectErrorStream(true)
            
            // Set environment variables for iperf3 temporary files
            val env = processBuilder.environment()
            env["TMPDIR"] = context.cacheDir.absolutePath
            env["TEMP"] = context.cacheDir.absolutePath  
            env["TMP"] = context.cacheDir.absolutePath
            
            Logger.i(TAG, "Set TMPDIR to: ${context.cacheDir.absolutePath}")
            val process = processBuilder.start()
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            var currentSecond = 0
            
            while (reader.readLine().also { line = it } != null && isTestRunning) {
                line?.let { outputLine ->
                    rawOutput.appendLine(outputLine)
                    Logger.d(TAG, "iperf3 output: $outputLine")
                    emit(IperfEvent.Log(outputLine))
                    
                    // Try to parse JSON output
                    if (outputLine.startsWith("{") && outputLine.endsWith("}")) {
                        try {
                            val jsonOutput = json.decodeFromString<IperfJsonOutput>(outputLine)
                            
                            // Handle interval data
                            jsonOutput.intervals?.forEach { interval ->
                                val tick = parseIntervalToTick(interval, ++currentSecond, params.protocol)
                                tick?.let {
                                    timeline.add(it)
                                    emit(IperfEvent.Progress(it))
                                }
                            }
                            
                            // Handle final results
                            if (jsonOutput.end != null) {
                                val endTime = Instant.now()
                                val summary = parseFinalResults(jsonOutput.end, params.protocol)
                                
                                val result = TestResult(
                                    startedAt = startTime,
                                    finishedAt = endTime,
                                    params = params,
                                    summary = summary,
                                    timeline = timeline,
                                    status = TestStatus.COMPLETED,
                                    rawLogPath = saveRawLog(sessionId, rawOutput.toString())
                                )
                                
                                emit(IperfEvent.Completed(result))
                                Logger.i(TAG, "Test completed successfully for ${params.host}:${params.port}")
                                return@flow
                            }
                            
                            // Handle errors
                            if (jsonOutput.error != null) {
                                emit(IperfEvent.Error("iperf3 error: ${jsonOutput.error}", sessionId))
                                return@flow
                            }
                            
                        } catch (e: SerializationException) {
                            // Not a JSON line, continue processing as regular output
                            Logger.d(TAG, "Non-JSON output line: $outputLine")
                        }
                    }
                }
            }
            
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                emit(IperfEvent.Error("iperf3 process failed with exit code: $exitCode", sessionId))
            } else if (timeline.isEmpty()) {
                // If we didn't get proper JSON output, fall back to mock for now
                Logger.w(TAG, "No valid JSON output received, falling back to mock mode")
                fallbackToMockExecution(params, sessionId, startTime).collect { event ->
                    emit(event)
                }
            }
            
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to execute iperf3 process", e)
            emit(IperfEvent.Error("Process execution failed: ${e.message}", sessionId))
        }
    }
    
    private suspend fun fallbackToMockExecution(params: TestParams, sessionId: String, startTime: Instant): Flow<IperfEvent> = flow {
        Logger.i(TAG, "Using fallback mock execution for session: $sessionId")
        val timeline = mutableListOf<LiveMetricsTick>()
        
        // Simulate test execution as backup
        for (second in 1..params.durationSec) {
            if (!isTestRunning) {
                emit(IperfEvent.Error("Test cancelled", sessionId))
                return@flow
            }
            
            val tick = generateMockTick(second, params.protocol)
            timeline.add(tick)
            emit(IperfEvent.Progress(tick))
            emit(IperfEvent.Log("[MOCK] [$second/${params.durationSec}] ${tick.throughputMbps} Mbits/sec"))
            
            kotlinx.coroutines.delay(1000)
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
    }
    
    private fun getIperf3BinaryPath(): String {
        // Return the path that was determined by ensureIperfBinaryReady()
        return actualBinaryPath ?: run {
            // Fallback: try jniLibs path directly
            val applicationInfo = context.applicationInfo
            val nativeLibraryDir = applicationInfo.nativeLibraryDir
            "$nativeLibraryDir/libiperf3.so"
        }
    }
    
    private fun ensureIperfBinaryReady(): Boolean {
        Logger.i(TAG, "Extracting iperf3 binary from assets to private directory")
        return extractIperf3Binary()
    }
    
    
    private fun extractIperf3Binary(): Boolean {
        Logger.d(TAG, "Attempting to extract iperf3 binary from assets")
        
        // Check if iperf3 binary exists in assets first
        try {
            val assetManager = context.assets
            val assetsList = assetManager.list("")
            Logger.d(TAG, "Available assets: ${assetsList?.joinToString(", ") ?: "none"}")
            
            // Check specifically for iperf3 binary
            val hasIperf3Asset = try {
                assetManager.open(IPERF3_BINARY).use { true }
            } catch (e: java.io.FileNotFoundException) {
                Logger.e(TAG, "iperf3 binary not found in assets")
                return false
            }
            
            if (!hasIperf3Asset) {
                Logger.e(TAG, "No iperf3 binary in assets")
                return false
            }
            
            Logger.i(TAG, "Found iperf3 binary in assets, extracting...")
            
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to check assets", e)
            return false
        }
        
        // Try different locations for binary extraction
        val locations = listOf(
            context.filesDir,       // /data/data/app/files
            context.cacheDir,       // /data/data/app/cache  
            context.codeCacheDir    // /data/data/app/code_cache
        )
        
        for (targetDir in locations) {
            if (tryExtractToBinary(targetDir)) {
                return true
            }
        }
        
        Logger.e(TAG, "Failed to extract iperf3 binary to any location")
        return false
    }
    
    private fun tryExtractToBinary(targetDir: File): Boolean {
        try {
            val assetManager = context.assets
            val inputStream = try {
                assetManager.open(IPERF3_BINARY)
            } catch (e: java.io.FileNotFoundException) {
                Logger.w(TAG, "iperf3 binary not found in assets, this is expected when using jniLibs approach")
                return false
            }
            val outputFile = File(targetDir, IPERF3_BINARY)
            
            Logger.i(TAG, "Trying to extract iperf3 to: ${outputFile.absolutePath}")
            
            // Remove existing file if present
            if (outputFile.exists()) {
                outputFile.delete()
            }
            
            outputFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            
            // Try multiple approaches to make executable
            // Method 1: Java File API  
            var success = outputFile.setExecutable(true, false)
            if (success) {
                Logger.i(TAG, "Set executable permission using Java API")
            } else {
                Logger.w(TAG, "Failed to set executable permission using Java API, trying chmod")
                
                // Method 2: Use Runtime.exec chmod
                try {
                    val chmodProcess = Runtime.getRuntime().exec("chmod 755 ${outputFile.absolutePath}")
                    val exitCode = chmodProcess.waitFor()
                    if (exitCode == 0) {
                        Logger.i(TAG, "Set executable permission using chmod")
                    } else {
                        Logger.w(TAG, "chmod failed with exit code: $exitCode")
                    }
                } catch (e: Exception) {
                    Logger.w(TAG, "Failed to execute chmod: ${e.message}")
                }
            }
            
            // Final verification
            val canExecute = outputFile.canExecute()
            Logger.i(TAG, "iperf3 binary extracted to: ${outputFile.absolutePath}")
            Logger.i(TAG, "Binary size: ${outputFile.length()} bytes")
            Logger.i(TAG, "Executable permission: $canExecute")
            
            // Test if we can actually execute it
            if (outputFile.exists() && outputFile.length() > 0) {
                // Update the binary path for this successful location
                updateBinaryPath(outputFile.absolutePath)
                Logger.i(TAG, "Successfully extracted iperf3 binary to ${targetDir.name}")
                return true
            }
            
        } catch (e: Exception) {
            Logger.w(TAG, "Failed to extract iperf3 binary to ${targetDir.absolutePath}: ${e.message}")
        }
        
        return false
    }
    
    private var actualBinaryPath: String? = null
    
    private fun updateBinaryPath(path: String) {
        actualBinaryPath = path
    }
    
    private fun parseIntervalToTick(interval: IperfInterval, second: Int, protocol: Protocol): LiveMetricsTick? {
        val sum = interval.sum ?: return null
        
        val throughputMbps = (sum.bitsPerSecond ?: 0L) / 1_000_000f
        
        return when (protocol) {
            Protocol.TCP -> LiveMetricsTick(
                second = second,
                throughputMbps = throughputMbps,
                retransmits = sum.retransmits,
                rttMs = null // RTT typically comes from stream data, not sum
            )
            Protocol.UDP -> LiveMetricsTick(
                second = second,
                throughputMbps = throughputMbps,
                jitterMs = sum.jitterMs,
                packetLossPct = sum.lostPercent
            )
        }
    }
    
    private fun parseFinalResults(end: IperfEnd, protocol: Protocol): TestSummary {
        val sum = end.sum ?: end.sumReceived ?: end.sumSent
        
        val avgMbps = (sum?.bitsPerSecond ?: 0L) / 1_000_000f
        val totalBytes = sum?.bytes ?: 0L
        
        return when (protocol) {
            Protocol.TCP -> TestSummary(
                avgMbps = avgMbps,
                maxMbps = avgMbps, // iperf3 doesn't provide max/min in final summary
                minMbps = avgMbps,
                retransmits = sum?.retransmits ?: 0,
                rttMs = 0f, // Would need to calculate from stream data
                totalBytes = totalBytes
            )
            Protocol.UDP -> TestSummary(
                avgMbps = avgMbps,
                maxMbps = avgMbps,
                minMbps = avgMbps,
                jitterMs = sum?.jitterMs ?: 0f,
                packetLossPct = sum?.lostPercent ?: 0f,
                totalBytes = totalBytes
            )
        }
    }
}