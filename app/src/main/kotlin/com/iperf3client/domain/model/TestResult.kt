package com.iperf3client.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.Instant

@Parcelize
data class TestResult(
    val id: Long = 0,
    val startedAt: Instant,
    val finishedAt: Instant?,
    val params: TestParams,
    val summary: TestSummary?,
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

enum class TestStatus {
    IDLE,
    RUNNING,
    COMPLETED,
    ERROR,
    CANCELLED
}