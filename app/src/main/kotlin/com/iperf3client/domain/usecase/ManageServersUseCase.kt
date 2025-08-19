package com.iperf3client.domain.usecase

import com.iperf3client.domain.model.ServerItem
import com.iperf3client.domain.repository.ServersRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant

class ManageServersUseCase(
    private val serversRepository: ServersRepository
) {
    
    fun observeServers(): Flow<List<ServerItem>> {
        return serversRepository.observeAll()
    }
    
    suspend fun addServer(
        name: String,
        host: String,
        port: Int = 5201,
        defaultProtocol: com.iperf3client.domain.model.Protocol = com.iperf3client.domain.model.Protocol.TCP,
        note: String? = null,
        setAsDefault: Boolean = false
    ): Result<Long> {
        return try {
            val server = ServerItem(
                name = name.trim(),
                host = host.trim(),
                port = port,
                defaultProtocol = defaultProtocol,
                note = note?.trim(),
                isDefault = false,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            
            if (!server.isValid()) {
                return Result.failure(IllegalArgumentException("Invalid server parameters"))
            }
            
            // Check if server already exists
            val existing = serversRepository.getAll().find { 
                it.host.equals(host.trim(), ignoreCase = true) && it.port == port 
            }
            if (existing != null) {
                return Result.failure(IllegalArgumentException("Server already exists"))
            }
            
            val id = serversRepository.insert(server)
            
            if (setAsDefault) {
                serversRepository.setAsDefault(id)
            }
            
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateServer(server: ServerItem): Result<Unit> {
        return try {
            if (!server.isValid()) {
                return Result.failure(IllegalArgumentException("Invalid server parameters"))
            }
            
            val updatedServer = server.copy(updatedAt = Instant.now())
            serversRepository.update(updatedServer)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteServer(server: ServerItem): Result<Unit> {
        return try {
            serversRepository.delete(server)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun setDefaultServer(serverId: Long): Result<Unit> {
        return try {
            serversRepository.setAsDefault(serverId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getDefaultServer(): ServerItem? {
        return serversRepository.getDefault()
    }
    
    suspend fun getServerById(id: Long): ServerItem? {
        return serversRepository.getById(id)
    }
    
    suspend fun validateServerConnection(server: ServerItem): Result<Boolean> {
        // TODO: Implement actual connectivity test
        // For now, just validate the format
        return try {
            if (!server.isValid()) {
                Result.failure(IllegalArgumentException("Invalid server configuration"))
            } else {
                // Could perform a simple socket connection test here
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun importServers(servers: List<ServerItem>): Result<Int> {
        return try {
            var imported = 0
            servers.forEach { server ->
                if (server.isValid()) {
                    try {
                        serversRepository.insert(server)
                        imported++
                    } catch (e: Exception) {
                        // Skip invalid servers but continue with others
                    }
                }
            }
            Result.success(imported)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun exportServers(): Result<List<ServerItem>> {
        return try {
            val servers = serversRepository.getAll()
            Result.success(servers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}