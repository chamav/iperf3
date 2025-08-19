package com.iperf3client.data.database.dao

import androidx.room.*
import com.iperf3client.data.database.entity.ServerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
    
    @Query("SELECT * FROM servers ORDER BY isDefault DESC, name ASC")
    fun observeAll(): Flow<List<ServerEntity>>
    
    @Query("SELECT * FROM servers ORDER BY isDefault DESC, name ASC")
    suspend fun getAll(): List<ServerEntity>
    
    @Query("SELECT * FROM servers WHERE id = :id")
    suspend fun getById(id: Long): ServerEntity?
    
    @Query("SELECT * FROM servers WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): ServerEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(server: ServerEntity): Long
    
    @Update
    suspend fun update(server: ServerEntity)
    
    @Delete
    suspend fun delete(server: ServerEntity)
    
    @Query("DELETE FROM servers WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Transaction
    suspend fun setAsDefault(id: Long) {
        clearAllDefaults()
        setDefault(id)
    }
    
    @Query("UPDATE servers SET isDefault = 0")
    suspend fun clearAllDefaults()
    
    @Query("UPDATE servers SET isDefault = 1 WHERE id = :id")
    suspend fun setDefault(id: Long)
    
    @Query("SELECT COUNT(*) FROM servers")
    suspend fun getCount(): Int
    
    @Query("DELETE FROM servers")
    suspend fun deleteAll()
}