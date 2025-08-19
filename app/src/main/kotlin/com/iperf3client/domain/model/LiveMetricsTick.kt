package com.iperf3client.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LiveMetricsTick(
    val second: Int,
    val throughputMbps: Float,
    val jitterMs: Float? = null,        // UDP only
    val packetLossPct: Float? = null,   // UDP only
    val retransmits: Int? = null,       // TCP only
    val rttMs: Float? = null,           // Optional
    val rawLine: String? = null         // Raw iperf3 output line
) : Parcelable {
    
    fun isValidForProtocol(protocol: Protocol): Boolean {
        return when (protocol) {
            Protocol.TCP -> jitterMs == null && packetLossPct == null
            Protocol.UDP -> retransmits == null
        }
    }
}