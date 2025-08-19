package com.iperf3client.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class TestParams(
    val host: String,
    val port: Int = 5201,
    val protocol: Protocol = Protocol.TCP,
    val durationSec: Int = 10,
    val parallelStreams: Int = 1,
    val reverse: Boolean = false,
    val udpBitrateMbps: Float? = null,
    val onlyWifi: Boolean = false
) : Parcelable {
    
    fun isValid(): Boolean {
        return host.isNotBlank() && 
               port in 1..65535 && 
               durationSec > 0 && 
               parallelStreams > 0 &&
               (protocol == Protocol.TCP || udpBitrateMbps != null)
    }
    
    fun toCommandArgs(): List<String> {
        return buildList {
            add("-c")
            add(host)
            add("-p")
            add(port.toString())
            add("-t")
            add(durationSec.toString())
            add("-P")
            add(parallelStreams.toString())
            add("-J") // JSON output
            
            if (reverse) {
                add("-R")
            }
            
            when (protocol) {
                Protocol.TCP -> {
                    // TCP is default
                }
                Protocol.UDP -> {
                    add("-u")
                    udpBitrateMbps?.let { bitrate ->
                        add("-b")
                        add("${bitrate}M")
                    }
                }
            }
        }
    }
}

@Serializable
enum class Protocol {
    TCP, UDP
}