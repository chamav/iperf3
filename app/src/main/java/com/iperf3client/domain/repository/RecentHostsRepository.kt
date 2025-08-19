package com.iperf3client.domain.repository

import com.iperf3client.domain.model.RecentHost
import kotlinx.coroutines.flow.Flow

interface RecentHostsRepository {
    
    suspend fun addHost(host: String)
    
    fun getRecentHosts(): Flow<List<RecentHost>>
    
    suspend fun clearRecentHosts()
}