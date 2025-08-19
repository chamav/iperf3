package com.iperf3client.data.database.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.iperf3client.domain.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant

@Entity(tableName = "test_results")
data class TestResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startedAt: Long, // Instant as epoch millis
    val finishedAt: Long?, // Instant as epoch millis
    val paramsJson: String, // TestParams serialized as JSON
    val summaryJson: String?, // TestSummary serialized as JSON
    val timelineJson: String, // List<LiveMetricsTick> serialized as JSON
    val rawLogPath: String?,
    val status: String, // TestStatus enum as string
    val errorMessage: String?
) {
    
    @Ignore
    private val json = Json { ignoreUnknownKeys = true }
    
    fun toDomain(): TestResult {
        return TestResult(
            id = id,
            startedAt = Instant.ofEpochMilli(startedAt),
            finishedAt = finishedAt?.let { Instant.ofEpochMilli(it) },
            params = json.decodeFromString(paramsJson),
            summary = summaryJson?.let { json.decodeFromString(it) },
            timeline = json.decodeFromString(timelineJson),
            rawLogPath = rawLogPath,
            status = TestStatus.valueOf(status),
            errorMessage = errorMessage
        )
    }
    
    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        
        fun fromDomain(result: TestResult): TestResultEntity {
            return TestResultEntity(
                id = result.id,
                startedAt = result.startedAt.toEpochMilli(),
                finishedAt = result.finishedAt?.toEpochMilli(),
                paramsJson = json.encodeToString(result.params),
                summaryJson = result.summary?.let { json.encodeToString(it) },
                timelineJson = json.encodeToString(result.timeline),
                rawLogPath = result.rawLogPath,
                status = result.status.name,
                errorMessage = result.errorMessage
            )
        }
    }
}