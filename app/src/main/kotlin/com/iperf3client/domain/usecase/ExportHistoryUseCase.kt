package com.iperf3client.domain.usecase

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.iperf3client.domain.model.TestResult
import com.iperf3client.domain.repository.HistoryRepository
import com.opencsv.CSVWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class ExportHistoryUseCase(
    private val historyRepository: HistoryRepository,
    private val context: Context
) {
    
    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true 
    }
    
    suspend fun exportToCsv(testIds: List<Long>): Result<File> = withContext(Dispatchers.IO) {
        try {
            val tests = if (testIds.isEmpty()) {
                historyRepository.getAll()
            } else {
                testIds.mapNotNull { historyRepository.getById(it) }
            }
            
            val exportDir = File(context.getExternalFilesDir(null), "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val csvFile = File(exportDir, "iperf3_results_$timestamp.csv")
            
            CSVWriter(FileWriter(csvFile)).use { writer ->
                // Write header
                writer.writeNext(arrayOf(
                    "ID", "Start Time", "End Time", "Duration (sec)", "Host", "Port", "Protocol",
                    "Parallel Streams", "Reverse", "UDP Bitrate", "Avg Speed (Mbps)", "Max Speed (Mbps)",
                    "Min Speed (Mbps)", "Jitter (ms)", "Packet Loss (%)", "Retransmits", "RTT (ms)",
                    "Total Bytes", "Status", "Error Message"
                ))
                
                // Write data
                tests.forEach { test ->
                    writer.writeNext(arrayOf(
                        test.id.toString(),
                        test.startedAt.toString(),
                        test.finishedAt?.toString() ?: "",
                        test.durationMs?.toString() ?: "",
                        test.params.host,
                        test.params.port.toString(),
                        test.params.protocol.toString(),
                        test.params.parallelStreams.toString(),
                        test.params.reverse.toString(),
                        test.params.udpBitrateMbps?.toString() ?: "",
                        test.summary?.avgMbps?.toString() ?: "",
                        test.summary?.maxMbps?.toString() ?: "",
                        test.summary?.minMbps?.toString() ?: "",
                        test.summary?.jitterMs?.toString() ?: "",
                        test.summary?.packetLossPct?.toString() ?: "",
                        test.summary?.retransmits?.toString() ?: "",
                        test.summary?.rttMs?.toString() ?: "",
                        test.summary?.totalBytes?.toString() ?: "",
                        test.status.toString(),
                        test.errorMessage ?: ""
                    ))
                }
            }
            
            Result.success(csvFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun exportToJson(testIds: List<Long>): Result<File> = withContext(Dispatchers.IO) {
        try {
            val tests = if (testIds.isEmpty()) {
                historyRepository.getAll()
            } else {
                testIds.mapNotNull { historyRepository.getById(it) }
            }
            
            val exportDir = File(context.getExternalFilesDir(null), "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val jsonFile = File(exportDir, "iperf3_results_$timestamp.json")
            
            val exportData = ExportData(
                exportedAt = Date(),
                version = "1.0",
                testCount = tests.size,
                tests = tests
            )
            
            jsonFile.writeText(json.encodeToString(exportData))
            
            Result.success(jsonFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun shareResults(file: File): Result<Intent> {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = when (file.extension.lowercase()) {
                    "csv" -> "text/csv"
                    "json" -> "application/json"
                    else -> "text/plain"
                }
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "iperf3 Test Results")
                putExtra(Intent.EXTRA_TEXT, "Network performance test results from iperf3 Client")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            Result.success(Intent.createChooser(intent, "Share Results"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun exportDetailedCsv(testId: Long): Result<File> = withContext(Dispatchers.IO) {
        try {
            val test = historyRepository.getById(testId)
                ?: return@withContext Result.failure(IllegalArgumentException("Test not found"))
            
            val exportDir = File(context.getExternalFilesDir(null), "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val csvFile = File(exportDir, "iperf3_detailed_${test.id}_$timestamp.csv")
            
            CSVWriter(FileWriter(csvFile)).use { writer ->
                // Write test info header
                writer.writeNext(arrayOf("Test Information"))
                writer.writeNext(arrayOf("ID", test.id.toString()))
                writer.writeNext(arrayOf("Start Time", test.startedAt.toString()))
                writer.writeNext(arrayOf("End Time", test.finishedAt?.toString() ?: ""))
                writer.writeNext(arrayOf("Host", test.params.host))
                writer.writeNext(arrayOf("Protocol", test.params.protocol.toString()))
                writer.writeNext(arrayOf())
                
                // Write timeline header
                writer.writeNext(arrayOf(
                    "Second", "Throughput (Mbps)", "Jitter (ms)", "Packet Loss (%)", 
                    "Retransmits", "RTT (ms)"
                ))
                
                // Write timeline data
                test.timeline.forEach { tick ->
                    writer.writeNext(arrayOf(
                        tick.second.toString(),
                        tick.throughputMbps.toString(),
                        tick.jitterMs?.toString() ?: "",
                        tick.packetLossPct?.toString() ?: "",
                        tick.retransmits?.toString() ?: "",
                        tick.rttMs?.toString() ?: ""
                    ))
                }
            }
            
            Result.success(csvFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    @kotlinx.serialization.Serializable
    data class ExportData(
        val exportedAt: Date,
        val version: String,
        val testCount: Int,
        val tests: List<TestResult>
    )
}