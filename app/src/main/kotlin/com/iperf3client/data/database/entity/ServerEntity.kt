package com.iperf3client.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.iperf3client.domain.model.Protocol
import com.iperf3client.domain.model.ServerItem
import java.time.Instant

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int,
    val defaultProtocol: String, // Protocol enum as string
    val note: String?,
    val isDefault: Boolean,
    val createdAt: Long, // Instant as epoch millis
    val updatedAt: Long  // Instant as epoch millis
) {
    
    fun toDomain(): ServerItem {
        return ServerItem(
            id = id,
            name = name,
            host = host,
            port = port,
            defaultProtocol = Protocol.valueOf(defaultProtocol),
            note = note,
            isDefault = isDefault,
            createdAt = Instant.ofEpochMilli(createdAt),
            updatedAt = Instant.ofEpochMilli(updatedAt)
        )
    }
    
    companion object {
        fun fromDomain(server: ServerItem): ServerEntity {
            return ServerEntity(
                id = server.id,
                name = server.name,
                host = server.host,
                port = server.port,
                defaultProtocol = server.defaultProtocol.name,
                note = server.note,
                isDefault = server.isDefault,
                createdAt = server.createdAt.toEpochMilli(),
                updatedAt = server.updatedAt.toEpochMilli()
            )
        }
    }
}