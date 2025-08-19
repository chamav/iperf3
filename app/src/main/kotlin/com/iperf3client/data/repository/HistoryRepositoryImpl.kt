package com.iperf3client.data.repository

import com.iperf3client.data.database.dao.TestResultDao
import com.iperf3client.data.database.entity.TestResultEntity
import com.iperf3client.domain.model.Protocol
import com.iperf3client.domain.model.TestResult
import com.iperf3client.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class HistoryRepositoryImpl(
    private val testResultDao: TestResultDao
) : HistoryRepository {
    
    override fun observeAll(): Flow<List<TestResult>> {
        return testResultDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getAll(): List<TestResult> {
        return testResultDao.getAll().map { it.toDomain() }
    }
    
    override suspend fun getById(id: Long): TestResult? {
        return testResultDao.getById(id)?.toDomain()
    }
    
    override suspend fun insert(result: TestResult): Long {
        val entity = TestResultEntity.fromDomain(result)
        return testResultDao.insert(entity)
    }
    
    override suspend fun update(result: TestResult) {
        val entity = TestResultEntity.fromDomain(result)
        testResultDao.update(entity)
    }
    
    override suspend fun delete(result: TestResult) {
        val entity = TestResultEntity.fromDomain(result)
        testResultDao.delete(entity)
    }
    
    override suspend fun deleteById(id: Long) {
        testResultDao.deleteById(id)
    }
    
    override suspend fun deleteAll() {
        testResultDao.deleteAll()
    }
    
    override suspend fun search(query: String): List<TestResult> {
        return testResultDao.search(query).map { it.toDomain() }
    }
    
    override suspend fun filterByProtocol(protocol: Protocol): List<TestResult> {
        return testResultDao.filterByProtocol(protocol.name).map { it.toDomain() }
    }
    
    override suspend fun filterByDateRange(from: Instant, to: Instant): List<TestResult> {
        return testResultDao.filterByDateRange(
            fromMillis = from.toEpochMilli(),
            toMillis = to.toEpochMilli()
        ).map { it.toDomain() }
    }
    
    override suspend fun getRecent(limit: Int): List<TestResult> {
        return testResultDao.getRecent(limit).map { it.toDomain() }
    }
    
    override suspend fun deleteOlderThan(date: Instant): Int {
        return testResultDao.deleteOlderThan(date.toEpochMilli())
    }
    
    override suspend fun getCount(): Int {
        return testResultDao.getCount()
    }
    
    override suspend fun getTotalDataTransferred(): Long {
        return testResultDao.getTotalDataTransferred() ?: 0L
    }
}