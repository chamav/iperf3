package com.iperf3client.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.Instant

@Parcelize
@Serializable
data class TestResult(
    val id: Long = 0,
    @Transient val startedAt: Instant = Instant.now(),
    @Transient val finishedAt: Instant? = null,
    val params: TestParams,
    val summary: TestSummary? = null,
    val timeline: List<LiveMetricsTick> = emptyList(),
    val rawLogPath: String? = null,
    val status: TestStatus = TestStatus.RUNNING,
    val errorMessage: String? = null
) : Parcelable {
    
    val durationMs: Long?
        get() = finishedAt?.let { it.toEpochMilli() - startedAt.toEpochMilli() }
    
    val isCompleted: Boolean
        get() = status == TestStatus.COMPLETED
    
    val isSuccessful: Boolean
        get() = isCompleted && summary != null
}

@Parcelize
@Serializable
data class TestSummary(
    val avgMbps: Float,
    val maxMbps: Float,
    val minMbps: Float,
    val jitterMs: Float? = null,        // UDP only
    val packetLossPct: Float? = null,   // UDP only
    val retransmits: Int? = null,       // TCP only
    val rttMs: Float? = null,           // Optional
    val totalBytes: Long? = null
) : Parcelable

@Serializable
enum class TestStatus {
    IDLE,
    RUNNING,
    COMPLETED,
    ERROR,
    CANCELLED
}