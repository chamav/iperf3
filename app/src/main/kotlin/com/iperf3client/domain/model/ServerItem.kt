package com.iperf3client.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.Instant

@Parcelize
@Serializable
data class ServerItem(
    val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int = 5201,
    val defaultProtocol: Protocol = Protocol.TCP,
    val note: String? = null,
    val isDefault: Boolean = false,
    @Transient val createdAt: Instant = Instant.now(),
    @Transient val updatedAt: Instant = Instant.now()
) : Parcelable {
    
    fun isValid(): Boolean {
        return name.isNotBlank() && 
               host.isNotBlank() && 
               port in 1..65535
    }
    
    fun toTestParams(): TestParams {
        return TestParams(
            host = host,
            port = port,
            protocol = defaultProtocol
        )
    }
    
    fun getDisplayName(): String {
        return if (name.isNotBlank()) name else host
    }
    
    fun getFullAddress(): String {
        return "$host:$port"
    }
}