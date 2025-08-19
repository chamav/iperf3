package com.iperf3client.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class RecentHost(
    val host: String,
    val lastUsedTimestamp: Long = System.currentTimeMillis()
)