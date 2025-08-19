package com.iperf3client.domain.repository

import com.iperf3client.domain.model.Protocol
import com.iperf3client.domain.model.TestResult
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface HistoryRepository {
    
    fun observeAll(): Flow<List<TestResult>>
    
    suspend fun getAll(): List<TestResult>
    
    suspend fun getById(id: Long): TestResult?
    
    suspend fun insert(result: TestResult): Long
    
    suspend fun update(result: TestResult)
    
    suspend fun delete(result: TestResult)
    
    suspend fun deleteById(id: Long)
    
    suspend fun deleteAll()
    
    suspend fun search(query: String): List<TestResult>
    
    suspend fun filterByProtocol(protocol: Protocol): List<TestResult>
    
    suspend fun filterByDateRange(from: Instant, to: Instant): List<TestResult>
    
    suspend fun getRecent(limit: Int = 10): List<TestResult>
    
    suspend fun deleteOlderThan(date: Instant): Int
    
    suspend fun getCount(): Int
    
    suspend fun getTotalDataTransferred(): Long
}