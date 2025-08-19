package com.iperf3client.data.repository

import com.iperf3client.data.database.dao.ServerDao
import com.iperf3client.data.database.entity.ServerEntity
import com.iperf3client.domain.model.ServerItem
import com.iperf3client.domain.repository.ServersRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ServersRepositoryImpl(
    private val serverDao: ServerDao
) : ServersRepository {
    
    override fun observeAll(): Flow<List<ServerItem>> {
        return serverDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getAll(): List<ServerItem> {
        return serverDao.getAll().map { it.toDomain() }
    }
    
    override suspend fun getById(id: Long): ServerItem? {
        return serverDao.getById(id)?.toDomain()
    }
    
    override suspend fun getDefault(): ServerItem? {
        return serverDao.getDefault()?.toDomain()
    }
    
    override suspend fun insert(server: ServerItem): Long {
        val entity = ServerEntity.fromDomain(server)
        return serverDao.insert(entity)
    }
    
    override suspend fun update(server: ServerItem) {
        val entity = ServerEntity.fromDomain(server)
        serverDao.update(entity)
    }
    
    override suspend fun delete(server: ServerItem) {
        val entity = ServerEntity.fromDomain(server)
        serverDao.delete(entity)
    }
    
    override suspend fun deleteById(id: Long) {
        serverDao.deleteById(id)
    }
    
    override suspend fun setAsDefault(id: Long) {
        serverDao.setAsDefault(id)
    }
    
    override suspend fun clearDefault() {
        serverDao.clearAllDefaults()
    }
    
    override suspend fun getCount(): Int {
        return serverDao.getCount()
    }
}