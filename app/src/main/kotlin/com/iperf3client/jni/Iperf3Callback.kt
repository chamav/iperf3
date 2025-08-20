package com.iperf3client.jni

interface Iperf3Callback {
    fun onProgress(mbps: Double, retransmits: Int)
    fun onComplete(jsonResult: String)
    fun onError(error: String)
}