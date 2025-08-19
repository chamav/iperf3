package com.iperf3client.data.database.dao

import androidx.room.*
import com.iperf3client.data.database.entity.TestResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TestResultDao {
    
    @Query("SELECT * FROM test_results ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<TestResultEntity>>
    
    @Query("SELECT * FROM test_results ORDER BY startedAt DESC")
    suspend fun getAll(): List<TestResultEntity>
    
    @Query("SELECT * FROM test_results WHERE id = :id")
    suspend fun getById(id: Long): TestResultEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: TestResultEntity): Long
    
    @Update
    suspend fun update(result: TestResultEntity)
    
    @Delete
    suspend fun delete(result: TestResultEntity)
    
    @Query("DELETE FROM test_results WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM test_results")
    suspend fun deleteAll()
    
    @Query("""
        SELECT * FROM test_results 
        WHERE paramsJson LIKE '%' || :query || '%' 
           OR errorMessage LIKE '%' || :query || '%'
        ORDER BY startedAt DESC
    """)
    suspend fun search(query: String): List<TestResultEntity>
    
    @Query("""
        SELECT * FROM test_results 
        WHERE paramsJson LIKE '%"protocol":"' || :protocol || '"%'
        ORDER BY startedAt DESC
    """)
    suspend fun filterByProtocol(protocol: String): List<TestResultEntity>
    
    @Query("""
        SELECT * FROM test_results 
        WHERE startedAt >= :fromMillis AND startedAt <= :toMillis
        ORDER BY startedAt DESC
    """)
    suspend fun filterByDateRange(fromMillis: Long, toMillis: Long): List<TestResultEntity>
    
    @Query("SELECT * FROM test_results ORDER BY startedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<TestResultEntity>
    
    @Query("DELETE FROM test_results WHERE startedAt < :dateMillis")
    suspend fun deleteOlderThan(dateMillis: Long): Int
    
    @Query("SELECT COUNT(*) FROM test_results")
    suspend fun getCount(): Int
    
    @Query("SELECT COUNT(*) FROM test_results WHERE status = 'COMPLETED'")
    suspend fun getCompletedCount(): Int
    
    @Query("""
        SELECT SUM(
            CASE 
                WHEN summaryJson IS NOT NULL 
                THEN CAST(
                    json_extract(summaryJson, '$.totalBytes') AS INTEGER
                ) 
                ELSE 0 
            END
        ) FROM test_results WHERE status = 'COMPLETED'
    """)
    suspend fun getTotalDataTransferred(): Long?
}