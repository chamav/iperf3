package com.iperf3client.domain.repository

import com.iperf3client.domain.model.ServerItem
import kotlinx.coroutines.flow.Flow

interface ServersRepository {
    
    fun observeAll(): Flow<List<ServerItem>>
    
    suspend fun getAll(): List<ServerItem>
    
    suspend fun getById(id: Long): ServerItem?
    
    suspend fun getDefault(): ServerItem?
    
    suspend fun insert(server: ServerItem): Long
    
    suspend fun update(server: ServerItem)
    
    suspend fun delete(server: ServerItem)
    
    suspend fun deleteById(id: Long)
    
    suspend fun setAsDefault(id: Long)
    
    suspend fun clearDefault()
    
    suspend fun getCount(): Int
}