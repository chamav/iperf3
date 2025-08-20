package com.iperf3client.data.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IperfJsonOutput(
    val start: IperfStart? = null,
    val intervals: List<IperfInterval>? = null,
    val end: IperfEnd? = null,
    val error: String? = null
)

@Serializable
data class IperfStart(
    val connected: List<IperfConnection>? = null,
    @SerialName("test_start") val testStart: IperfTestStart? = null
)

@Serializable
data class IperfConnection(
    val socket: Int? = null,
    @SerialName("local_host") val localHost: String? = null,
    @SerialName("local_port") val localPort: Int? = null,
    @SerialName("remote_host") val remoteHost: String? = null,
    @SerialName("remote_port") val remotePort: Int? = null
)

@Serializable
data class IperfTestStart(
    val protocol: String? = null,
    @SerialName("num_streams") val numStreams: Int? = null,
    @SerialName("blksize") val blksize: Int? = null,
    val omit: Int? = null,
    val duration: Int? = null,
    @SerialName("bytes") val bytes: Long? = null,
    @SerialName("blocks") val blocks: Long? = null,
    val reverse: Int? = null
)

@Serializable
data class IperfInterval(
    val streams: List<IperfStream>? = null,
    val sum: IperfSum? = null
)

@Serializable
data class IperfStream(
    val socket: Int? = null,
    val start: Float? = null,
    val end: Float? = null,
    val seconds: Float? = null,
    val bytes: Long? = null,
    @SerialName("bits_per_second") val bitsPerSecond: Long? = null,
    val retransmits: Int? = null,
    @SerialName("snd_cwnd") val sndCwnd: Long? = null,
    val rtt: Long? = null,
    val rttvar: Long? = null,
    val pmtu: Long? = null,
    val omitted: Boolean? = null,
    
    // UDP specific fields
    val packets: Long? = null,
    @SerialName("lost_packets") val lostPackets: Long? = null,
    @SerialName("lost_percent") val lostPercent: Float? = null,
    @SerialName("jitter_ms") val jitterMs: Float? = null
)

@Serializable
data class IperfSum(
    val start: Float? = null,
    val end: Float? = null,
    val seconds: Float? = null,
    val bytes: Long? = null,
    @SerialName("bits_per_second") val bitsPerSecond: Long? = null,
    val retransmits: Int? = null,
    val omitted: Boolean? = null,
    
    // UDP specific fields
    val packets: Long? = null,
    @SerialName("lost_packets") val lostPackets: Long? = null,
    @SerialName("lost_percent") val lostPercent: Float? = null,
    @SerialName("jitter_ms") val jitterMs: Float? = null
)

@Serializable
data class IperfEnd(
    val streams: List<IperfEndStream>? = null,
    val sum: IperfEndSum? = null,
    @SerialName("sum_sent") val sumSent: IperfEndSum? = null,
    @SerialName("sum_received") val sumReceived: IperfEndSum? = null,
    @SerialName("cpu_utilization_percent") val cpuUtilizationPercent: IperfCpuUtilization? = null
)

@Serializable
data class IperfEndStream(
    val socket: Int? = null,
    val start: Float? = null,
    val end: Float? = null,
    val seconds: Float? = null,
    val bytes: Long? = null,
    @SerialName("bits_per_second") val bitsPerSecond: Long? = null,
    val retransmits: Int? = null,
    @SerialName("max_snd_cwnd") val maxSndCwnd: Long? = null,
    @SerialName("max_rtt") val maxRtt: Long? = null,
    @SerialName("min_rtt") val minRtt: Long? = null,
    @SerialName("mean_rtt") val meanRtt: Long? = null,
    
    // UDP specific fields
    val packets: Long? = null,
    @SerialName("lost_packets") val lostPackets: Long? = null,
    @SerialName("lost_percent") val lostPercent: Float? = null,
    @SerialName("jitter_ms") val jitterMs: Float? = null
)

@Serializable
data class IperfEndSum(
    val start: Float? = null,
    val end: Float? = null,
    val seconds: Float? = null,
    val bytes: Long? = null,
    @SerialName("bits_per_second") val bitsPerSecond: Long? = null,
    val retransmits: Int? = null,
    
    // UDP specific fields
    val packets: Long? = null,
    @SerialName("lost_packets") val lostPackets: Long? = null,
    @SerialName("lost_percent") val lostPercent: Float? = null,
    @SerialName("jitter_ms") val jitterMs: Float? = null
)

@Serializable
data class IperfCpuUtilization(
    @SerialName("host_total") val hostTotal: Float? = null,
    @SerialName("host_user") val hostUser: Float? = null,
    @SerialName("host_system") val hostSystem: Float? = null,
    @SerialName("remote_total") val remoteTotal: Float? = null,
    @SerialName("remote_user") val remoteUser: Float? = null,
    @SerialName("remote_system") val remoteSystem: Float? = null
)