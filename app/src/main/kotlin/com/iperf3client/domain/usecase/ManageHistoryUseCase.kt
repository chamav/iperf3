package com.iperf3client.domain.usecase

import com.iperf3client.domain.model.TestResult
import com.iperf3client.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow

class ManageHistoryUseCase(
    private val historyRepository: HistoryRepository
) {
    
    fun observeTestResults(): Flow<List<TestResult>> {
        return historyRepository.observeAll()
    }
    
    suspend fun getAll(): List<TestResult> {
        return historyRepository.getAll()
    }
    
    suspend fun getById(id: Long): TestResult? {
        return historyRepository.getById(id)
    }
    
    suspend fun deleteTestResult(testResult: TestResult): Result<Unit> {
        return try {
            historyRepository.delete(testResult)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun clearAllHistory(): Result<Unit> {
        return try {
            historyRepository.deleteAll()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getRecentResults(limit: Int = 10): List<TestResult> {
        return historyRepository.getAll()
            .sortedByDescending { it.startedAt }
            .take(limit)
    }
    
    suspend fun getResultsByHost(host: String): List<TestResult> {
        return historyRepository.getAll()
            .filter { it.params.host.equals(host, ignoreCase = true) }
            .sortedByDescending { it.startedAt }
    }
    
    suspend fun getSuccessfulResults(): List<TestResult> {
        return historyRepository.getAll()
            .filter { it.status == com.iperf3client.domain.model.TestStatus.COMPLETED }
    }
    
    suspend fun getFailedResults(): List<TestResult> {
        return historyRepository.getAll()
            .filter { it.status == com.iperf3client.domain.model.TestStatus.ERROR }
    }
}