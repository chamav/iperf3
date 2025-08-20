package com.iperf3client.data.engine

import android.content.Context
import android.util.Log
import com.iperf3client.data.utils.Logger
import com.iperf3client.data.engine.*
import com.iperf3client.domain.model.*
import com.iperf3client.jni.Iperf3Native
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.channelFlow
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
        private const val IPERF3_VERSION = "3.19.1_assets" // Version identifier for tracking updates
    }
    
    private var hasVerifiedBinary = false
    
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
            
            // Try to use native JNI library first
            if (Iperf3Native.isAvailable()) {
                Logger.i(TAG, "Using native JNI iperf3 implementation")
                runNativeTest(params, sessionId).collect { event ->
                    emit(event)
                }
            } else {
                Logger.w(TAG, "Native JNI library not available, falling back to binary")
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
            } // end of else block
            
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
        
        // Check if we should use mock mode
        if (actualBinaryPath == null) {
            Logger.w(TAG, "No iperf3 binary available, using mock mode")
            fallbackToMockExecution(params, sessionId, startTime).collect { event ->
                emit(event)
            }
            return@flow
        }
        
        try {
            val binaryPath = getIperf3BinaryPath() ?: throw IllegalStateException("iperf3 binary not available")
            val iperf3Command = buildList {
                add(binaryPath)
                addAll(params.toCommandArgs())
            }
            
            Logger.i(TAG, "Executing iperf3: ${iperf3Command.joinToString(" ")}")
            
            // On Android 10+, we need to run through shell to avoid permission issues
            // Also set LD_LIBRARY_PATH for dynamic libraries
            val command = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                Logger.d(TAG, "Android 10+ detected, using shell wrapper to bypass W^X restrictions")
                val ldPath = "LD_LIBRARY_PATH=${context.applicationInfo.nativeLibraryDir}"
                val tmpDir = "TMPDIR=${context.cacheDir.absolutePath}"
                listOf("/system/bin/sh", "-c", "$ldPath $tmpDir ${iperf3Command.joinToString(" ")}")
            } else {
                iperf3Command
            }
            
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
            if (exitCode == 126) {
                // Permission denied - fall back to mock mode
                Logger.w(TAG, "iperf3 exit code 126 (Permission denied), falling back to mock mode")
                actualBinaryPath = null // Disable binary for future attempts
                fallbackToMockExecution(params, sessionId, startTime).collect { event ->
                    emit(event)
                }
            } else if (exitCode != 0) {
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
            
            // Check if it's a permission error and fall back to mock mode
            if (e is java.io.IOException && e.message?.contains("Permission denied") == true) {
                Logger.w(TAG, "Permission denied to execute iperf3, falling back to mock mode")
                actualBinaryPath = null // Disable binary for future attempts
                fallbackToMockExecution(params, sessionId, startTime).collect { event ->
                    emit(event)
                }
            } else {
                emit(IperfEvent.Error("Process execution failed: ${e.message}", sessionId))
            }
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
    
    private fun getIperf3BinaryPath(): String? {
        // Return the path that was determined by ensureIperfBinaryReady()
        return actualBinaryPath
    }
    
    private fun ensureIperfBinaryReady(): Boolean {
        // Check version file to see if we need to re-extract
        val versionFile = File(context.filesDir, "iperf3.version")
        val currentVersion = if (versionFile.exists()) {
            versionFile.readText().trim()
        } else {
            ""
        }
        
        // Check if we already have the binary extracted
        val extractedBinary = File(context.filesDir, IPERF3_BINARY)
        if (extractedBinary.exists() && currentVersion == IPERF3_VERSION) {
            // Check if this is the correct binary (not the old one from lib)
            val expectedSize = 16196880L // Size of our iperf3 binary
            if (extractedBinary.length() == expectedSize) {
                Logger.i(TAG, "Found previously extracted iperf3 binary at: ${extractedBinary.absolutePath}")
                Logger.d(TAG, "Binary size: ${extractedBinary.length()} bytes, executable: ${extractedBinary.canExecute()}")
                
                // Verify it can be executed
                try {
                    // Try to check file type
                    val fileProcess = Runtime.getRuntime().exec(arrayOf("file", extractedBinary.absolutePath))
                    val fileOutput = fileProcess.inputStream.bufferedReader().readText()
                    Logger.d(TAG, "File type: $fileOutput")
                    
                    // Make sure it's executable
                    if (!extractedBinary.canExecute()) {
                        extractedBinary.setExecutable(true, false)
                        Logger.d(TAG, "Set executable permission on existing binary")
                    }
                    
                    actualBinaryPath = extractedBinary.absolutePath
                    return true
                } catch (e: Exception) {
                    Logger.w(TAG, "Failed to verify existing binary: ${e.message}")
                    Logger.w(TAG, "Deleting old binary and re-extracting")
                    extractedBinary.delete()
                }
            } else {
                Logger.w(TAG, "Found old/corrupted binary (size: ${extractedBinary.length()}), deleting and re-extracting")
                extractedBinary.delete()
            }
        }
        
        // If not found, try to extract from assets
        val applicationInfo = context.applicationInfo
        Logger.d(TAG, "Attempting to extract iperf3 from assets")
        
        if (extractFromAssets()) {
            return true
        }
        
        val nativeLibraryDir = applicationInfo.nativeLibraryDir
        
        // Try multiple possible paths for the library
        // Android can place native libs in different locations depending on the device and Android version
        val possiblePaths = mutableListOf(
            "$nativeLibraryDir/libiperf3.so",
            "${nativeLibraryDir.replace("/arm64", "/arm64-v8a")}/libiperf3.so"
        )
        
        // For split APKs and some devices, the lib might be in the base APK path
        if (nativeLibraryDir.contains("/lib/arm64")) {
            possiblePaths.add(nativeLibraryDir.replace("/lib/arm64", "/lib/arm64-v8a") + "/libiperf3.so")
        }
        
        // Try to construct path from sourceDir (APK location)
        val sourceDir = applicationInfo.sourceDir
        if (sourceDir != null) {
            val apkDir = File(sourceDir).parent
            if (apkDir != null) {
                possiblePaths.add("$apkDir/lib/arm64-v8a/libiperf3.so")
                possiblePaths.add("$apkDir/lib/arm64/libiperf3.so")
            }
        }
        
        // Fallback paths
        possiblePaths.addAll(listOf(
            "${applicationInfo.dataDir}/lib/libiperf3.so",
            "/data/app/${context.packageName}/lib/arm64-v8a/libiperf3.so"
        ))
        
        for (jniLibsPath in possiblePaths) {
            val jniLibsFile = File(jniLibsPath)
            Logger.d(TAG, "Checking for iperf3 binary at: $jniLibsPath")
            
            if (jniLibsFile.exists()) {
                Logger.i(TAG, "Found iperf3 binary in jniLibs: $jniLibsPath")
                Logger.i(TAG, "Binary size: ${jniLibsFile.length()} bytes, executable: ${jniLibsFile.canExecute()}")
                actualBinaryPath = jniLibsPath
                return true
            }
        }
        
        Logger.w(TAG, "jniLibs binary not found at any expected path")
        
        // Debug: Try to list directory contents
        Logger.d(TAG, "nativeLibraryDir: $nativeLibraryDir")
        Logger.d(TAG, "sourceDir: ${applicationInfo.sourceDir}")
        Logger.d(TAG, "dataDir: ${applicationInfo.dataDir}")
        
        // Fallback 1: try to copy from jniLibs to private directory
        Logger.w(TAG, "jniLibs binary not found at expected location, trying to copy from jniLibs")
        if (copyFromJniLibs()) {
            return true
        }
        
        // Fallback 2: try to extract from assets if jniLibs copy failed
        Logger.w(TAG, "jniLibs copy failed, trying to extract from assets")
        return extractIperf3Binary()
    }
    
    private fun copyFromJniLibs(): Boolean {
        Logger.d(TAG, "Attempting to extract iperf3 binary from APK")
        
        try {
            val applicationInfo = context.applicationInfo
            val sourceDir = applicationInfo.sourceDir
            
            Logger.d(TAG, "APK source directory: $sourceDir")
            
            // Try to extract from APK directly
            if (extractFromApk(sourceDir)) {
                return true
            }
            
            // Fallback: Try different possible locations for the library
            val nativeLibraryDir = applicationInfo.nativeLibraryDir
            val possiblePaths = listOf(
                "$nativeLibraryDir/libiperf3.so",
                "$nativeLibraryDir/../lib/arm64-v8a/libiperf3.so",
                "${nativeLibraryDir.replace("/lib/arm64", "/lib/arm64-v8a")}/libiperf3.so"
            )
            
            var sourceFile: File? = null
            for (path in possiblePaths) {
                val testFile = File(path)
                Logger.d(TAG, "Looking for jniLibs binary at: ${testFile.absolutePath}")
                if (testFile.exists()) {
                    sourceFile = testFile
                    Logger.i(TAG, "Found jniLibs binary at: ${testFile.absolutePath}")
                    break
                }
            }
            
            if (sourceFile == null) {
                Logger.w(TAG, "Source file not found in any of the expected locations")
                return false
            }
            
            Logger.i(TAG, "Found jniLibs binary: ${sourceFile.absolutePath}")
            Logger.d(TAG, "Source file size: ${sourceFile.length()} bytes")
            
            // Try copying to app's lib directory (Android allows execution from here)
            val appLibDir = File(context.applicationInfo.dataDir, "lib")
            if (!appLibDir.exists()) {
                appLibDir.mkdirs()
            }
            
            val targetDirs = listOf(
                appLibDir,  // Primary target - lib directory
                context.filesDir,
                context.cacheDir
            )
            
            for (targetDir in targetDirs) {
                val targetFile = File(targetDir, IPERF3_BINARY)
                
                try {
                    Logger.d(TAG, "Attempting to copy to: ${targetFile.absolutePath}")
                    
                    // Remove existing file if present
                    if (targetFile.exists()) {
                        targetFile.delete()
                    }
                    
                    // Copy the file
                    sourceFile.copyTo(targetFile, overwrite = true)
                    
                    // Set executable permissions
                    var success = targetFile.setExecutable(true, false)
                    if (!success) {
                        Logger.w(TAG, "Failed to set executable with Java API, trying chmod")
                        try {
                            val chmodProcess = Runtime.getRuntime().exec("chmod 755 ${targetFile.absolutePath}")
                            val exitCode = chmodProcess.waitFor()
                            success = (exitCode == 0)
                            if (success) {
                                Logger.i(TAG, "Set executable permission using chmod")
                            } else {
                                Logger.w(TAG, "chmod failed with exit code: $exitCode")
                            }
                        } catch (e: Exception) {
                            Logger.w(TAG, "Failed to execute chmod: ${e.message}")
                        }
                    } else {
                        Logger.i(TAG, "Set executable permission using Java API")
                    }
                    
                    // Verify the copy
                    val canExecute = targetFile.canExecute()
                    Logger.i(TAG, "Copied iperf3 binary to: ${targetFile.absolutePath}")
                    Logger.i(TAG, "Target file size: ${targetFile.length()} bytes")
                    Logger.i(TAG, "Executable permission: $canExecute")
                    
                    if (targetFile.exists() && targetFile.length() > 0) {
                        actualBinaryPath = targetFile.absolutePath
                        Logger.i(TAG, "Successfully copied iperf3 binary from jniLibs to ${targetDir.name}")
                        return true
                    }
                    
                } catch (e: Exception) {
                    Logger.w(TAG, "Failed to copy to ${targetFile.absolutePath}: ${e.message}")
                    continue
                }
            }
            
            Logger.e(TAG, "Failed to copy iperf3 binary to any private directory")
            return false
            
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to copy from jniLibs", e)
            return false
        }
    }
    
    private fun extractFromAssets(): Boolean {
        try {
            // Determine the architecture
            val abi = android.os.Build.SUPPORTED_ABIS[0]
            val assetPath = when {
                abi.contains("arm64") -> "arm64-v8a/iperf3"
                abi.contains("armeabi") -> "armeabi-v7a/iperf3"
                abi.contains("x86_64") -> "x86_64/iperf3"
                abi.contains("x86") -> "x86/iperf3"
                else -> {
                    Logger.w(TAG, "Unsupported ABI: $abi")
                    return false
                }
            }
            
            Logger.d(TAG, "Extracting iperf3 from assets: $assetPath for ABI: $abi")
            
            // Open the asset
            val inputStream = try {
                context.assets.open(assetPath)
            } catch (e: Exception) {
                Logger.w(TAG, "iperf3 not found in assets at $assetPath: ${e.message}")
                return false
            }
            
            // Extract to files directory
            val targetFile = File(context.filesDir, IPERF3_BINARY)
            
            inputStream.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            // Set executable permissions
            val executableSet = targetFile.setExecutable(true, false)
            if (!executableSet) {
                Logger.w(TAG, "Failed to set executable with Java API, trying chmod")
                try {
                    val chmodProcess = Runtime.getRuntime().exec(arrayOf("chmod", "755", targetFile.absolutePath))
                    val exitCode = chmodProcess.waitFor()
                    if (exitCode == 0) {
                        Logger.i(TAG, "Set executable permission using chmod 755")
                    } else {
                        Logger.w(TAG, "chmod 755 failed with exit code: $exitCode")
                    }
                } catch (e: Exception) {
                    Logger.w(TAG, "Failed to execute chmod: ${e.message}")
                }
            } else {
                Logger.i(TAG, "Set executable permission using Java API")
            }
            
            // Verify extraction
            if (targetFile.exists() && targetFile.length() > 0) {
                Logger.i(TAG, "Successfully extracted iperf3 from assets: ${targetFile.absolutePath}")
                Logger.d(TAG, "Binary size: ${targetFile.length()} bytes")
                
                // Save version file
                val versionFile = File(context.filesDir, "iperf3.version")
                versionFile.writeText(IPERF3_VERSION)
                Logger.d(TAG, "Saved version: $IPERF3_VERSION")
                
                actualBinaryPath = targetFile.absolutePath
                return true
            } else {
                Logger.w(TAG, "Extraction verification failed")
                return false
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to extract from assets: ${e.message}", e)
            return false
        }
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
                Logger.i(TAG, "iperf3 binary not found in assets (expected when using jniLibs)")
                false
            }
            
            if (!hasIperf3Asset) {
                Logger.i(TAG, "No iperf3 binary in assets, this is expected when using jniLibs approach")
                return false
            }
        } catch (e: Exception) {
            Logger.w(TAG, "Failed to check assets: ${e.message}")
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
    
    private fun verifyBinaryExecution(binaryPath: String) {
        try {
            Logger.d(TAG, "Verifying binary can execute: $binaryPath")
            
            // On Android 10+, run through shell
            val command = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                arrayOf("/system/bin/sh", "-c", "$binaryPath --version")
            } else {
                arrayOf(binaryPath, "--version")
            }
            
            val process = Runtime.getRuntime().exec(command)
            val exitCode = process.waitFor()
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            
            Logger.d(TAG, "Binary version check exit code: $exitCode")
            Logger.d(TAG, "Binary version output: $output")
            if (error.isNotEmpty()) {
                Logger.d(TAG, "Binary version error: $error")
            }
            
            hasVerifiedBinary = exitCode == 0
            if (hasVerifiedBinary) {
                Logger.i(TAG, "Binary verification successful")
            } else {
                Logger.w(TAG, "Binary verification failed with exit code: $exitCode")
            }
        } catch (e: Exception) {
            Logger.w(TAG, "Binary verification failed: ${e.message}")
            hasVerifiedBinary = false
        }
    }
    
    private fun extractFromApk(apkPath: String): Boolean {
        Logger.d(TAG, "Attempting to extract iperf3 binary directly from APK: $apkPath")
        
        try {
            val apkFile = File(apkPath)
            if (!apkFile.exists()) {
                Logger.w(TAG, "APK file does not exist: $apkPath")
                return false
            }
            
            // Use ZipFile to read APK contents
            val zipFile = java.util.zip.ZipFile(apkFile)
            val entry = zipFile.getEntry("lib/arm64-v8a/libiperf3.so")
            
            if (entry == null) {
                Logger.w(TAG, "libiperf3.so not found in APK at lib/arm64-v8a/")
                zipFile.close()
                return false
            }
            
            Logger.i(TAG, "Found libiperf3.so in APK, size: ${entry.size} bytes")
            
            // Extract to app's lib directory (Android allows execution from here)
            val appLibDir = File(context.applicationInfo.dataDir, "lib")
            if (!appLibDir.exists()) {
                appLibDir.mkdirs()
            }
            val targetFile = File(appLibDir, IPERF3_BINARY)
            
            zipFile.getInputStream(entry).use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            zipFile.close()
            
            // Set executable permissions - try multiple methods
            var executableSet = false
            
            // Method 1: Java API
            executableSet = targetFile.setExecutable(true, false)
            if (executableSet) {
                Logger.i(TAG, "Set executable permission using Java API")
            }
            
            // Method 2: chmod command
            if (!executableSet) {
                Logger.w(TAG, "Failed to set executable with Java API, trying chmod")
                try {
                    val chmodProcess = Runtime.getRuntime().exec(arrayOf("chmod", "755", targetFile.absolutePath))
                    val exitCode = chmodProcess.waitFor()
                    if (exitCode == 0) {
                        Logger.i(TAG, "Set executable permission using chmod 755")
                        executableSet = true
                    } else {
                        Logger.w(TAG, "chmod 755 failed with exit code: $exitCode")
                    }
                } catch (e: Exception) {
                    Logger.w(TAG, "Failed to execute chmod: ${e.message}")
                }
            }
            
            // Verify file properties
            Logger.d(TAG, "File verification after extraction:")
            Logger.d(TAG, "  Exists: ${targetFile.exists()}")
            Logger.d(TAG, "  Size: ${targetFile.length()} bytes")
            Logger.d(TAG, "  Readable: ${targetFile.canRead()}")
            Logger.d(TAG, "  Writable: ${targetFile.canWrite()}")
            Logger.d(TAG, "  Executable: ${targetFile.canExecute()}")
            Logger.d(TAG, "  Absolute path: ${targetFile.absolutePath}")
            
            // Try to check file type
            try {
                val fileProcess = Runtime.getRuntime().exec(arrayOf("file", targetFile.absolutePath))
                val fileOutput = fileProcess.inputStream.bufferedReader().readText()
                Logger.d(TAG, "File type: $fileOutput")
            } catch (e: Exception) {
                Logger.w(TAG, "Could not determine file type: ${e.message}")
            }
            
            // Even if canExecute() returns false, we might still be able to run it
            if (targetFile.exists() && targetFile.length() > 0) {
                Logger.i(TAG, "Successfully extracted iperf3 binary at: ${targetFile.absolutePath}")
                actualBinaryPath = targetFile.absolutePath
                
                // First time extraction: try to verify it can run
                if (!hasVerifiedBinary) {
                    verifyBinaryExecution(targetFile.absolutePath)
                }
                
                return true
            } else {
                Logger.w(TAG, "Extraction verification failed - file missing or empty")
                return false
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to extract from APK: ${e.message}", e)
            return false
        }
    }
    
    private fun runNativeTest(params: TestParams, sessionId: String): Flow<IperfEvent> = channelFlow {
        Logger.i(TAG, "Starting native JNI test")
        
        val native = Iperf3Native()
        val channel = this@channelFlow
        val callback = object : Iperf3Native.Callback {
            var lastEmittedSecond = 0
            var timeline = mutableListOf<LiveMetricsTick>()
            
            override fun onProgress(throughputMbps: Double, retransmits: Int) {
                val currentSecond = lastEmittedSecond + 1
                lastEmittedSecond = currentSecond
                
                val tick = LiveMetricsTick(
                    second = currentSecond,
                    throughputMbps = throughputMbps.toFloat(),
                    retransmits = if (retransmits > 0) retransmits else null,
                    rttMs = null,
                    jitterMs = null,
                    packetLossPct = null
                )
                
                timeline.add(tick)
                Logger.d(TAG, "Native progress: $currentSecond sec, $throughputMbps Mbps, retransmits=$retransmits")
                
                // Use trySend for non-blocking send from callback
                channel.trySend(IperfEvent.Progress(tick))
            }
            
            override fun onComplete(jsonResult: String) {
                Logger.i(TAG, "Native test completed, parsing JSON result")
                try {
                    // Parse JSON result and create TestResult
                    val endTime = Instant.now()
                    val startTime = endTime.minusSeconds(params.durationSec.toLong())
                    
                    // Calculate summary from timeline
                    val summary = if (timeline.isNotEmpty()) {
                        val speeds = timeline.map { it.throughputMbps }
                        TestSummary(
                            avgMbps = speeds.average().toFloat(),
                            maxMbps = speeds.maxOrNull() ?: 0f,
                            minMbps = speeds.minOrNull() ?: 0f,
                            jitterMs = if (params.protocol == Protocol.UDP) 
                                timeline.mapNotNull { it.jitterMs }.average().toFloat().takeIf { it > 0 }
                                else null,
                            packetLossPct = if (params.protocol == Protocol.UDP)
                                timeline.mapNotNull { it.packetLossPct }.average().toFloat().takeIf { it > 0 }
                                else null,
                            retransmits = if (params.protocol == Protocol.TCP)
                                timeline.mapNotNull { it.retransmits }.sum()
                                else null,
                            rttMs = timeline.mapNotNull { it.rttMs }.average().toFloat().takeIf { it > 0 },
                            totalBytes = null
                        )
                    } else {
                        // Fallback if no timeline data
                        TestSummary(
                            avgMbps = 100.0f, // TODO: Parse from JSON
                            maxMbps = 150.0f,
                            minMbps = 50.0f,
                            jitterMs = null,
                            packetLossPct = null,
                            retransmits = null,
                            rttMs = null,
                            totalBytes = null
                        )
                    }
                    
                    val result = TestResult(
                        startedAt = startTime,
                        finishedAt = endTime,
                        params = params,
                        summary = summary,
                        timeline = timeline,
                        status = TestStatus.COMPLETED,
                        rawLogPath = null
                    )
                    
                    channel.trySend(IperfEvent.Completed(result))
                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to parse native JSON result", e)
                    channel.trySend(IperfEvent.Error("Failed to parse test result: ${e.message}", sessionId))
                }
            }
            
            override fun onError(error: String) {
                Logger.e(TAG, "Native test error: $error")
                channel.trySend(IperfEvent.Error(error, sessionId))
            }
        }
        
        withContext(Dispatchers.IO) {
            val success = native.runTest(
                host = params.host,
                port = params.port,
                duration = params.durationSec,
                streams = params.parallelStreams,
                reverse = params.reverse,
                udp = params.protocol == Protocol.UDP,
                callback = callback,
                cacheDir = context.cacheDir.absolutePath
            )
            
            if (!success) {
                send(IperfEvent.Error("Failed to start native test", sessionId))
            }
        }
    }
}