package com.iperf3client.jni

/**
 * Native JNI interface for iperf3 library
 */
class Iperf3Native {
    
    companion object {
        @Volatile
        private var libraryLoaded = false
        
        init {
            try {
                System.loadLibrary("iperf3wrapper")
                libraryLoaded = true
            } catch (e: UnsatisfiedLinkError) {
                // Library loading failed
                e.printStackTrace()
                libraryLoaded = false
            }
        }
        
        fun isAvailable(): Boolean = libraryLoaded
    }
    
    /**
     * Callback interface for iperf3 events
     */
    interface Callback {
        fun onProgress(throughputMbps: Double, retransmits: Int)
        fun onComplete(jsonResult: String)
        fun onError(error: String)
    }
    
    // Native methods
    external fun createTest(): Long
    external fun setTestParams(
        testPtr: Long,
        host: String,
        port: Int,
        duration: Int,
        streams: Int,
        reverse: Boolean,
        udp: Boolean
    )
    external fun setCallback(callback: Callback)
    external fun runClient(testPtr: Long): Int
    external fun stopTest(testPtr: Long)
    external fun freeTest(testPtr: Long)
    
    /**
     * Run iperf3 client test
     */
    fun runTest(
        host: String,
        port: Int,
        duration: Int,
        streams: Int,
        reverse: Boolean,
        udp: Boolean,
        callback: Callback,
        cacheDir: String? = null
    ): Boolean {
        // Set TMPDIR for iperf3 temporary files
        if (cacheDir != null) {
            try {
                val runtime = Runtime.getRuntime()
                runtime.exec(arrayOf("sh", "-c", "export TMPDIR=$cacheDir"))
                System.setProperty("TMPDIR", cacheDir)
            } catch (e: Exception) {
                // Ignore errors setting TMPDIR
            }
        }
        
        val testPtr = createTest()
        if (testPtr == 0L) {
            callback.onError("Failed to create test")
            return false
        }
        
        try {
            setTestParams(testPtr, host, port, duration, streams, reverse, udp)
            setCallback(callback)
            
            val result = runClient(testPtr)
            return result == 0
        } finally {
            freeTest(testPtr)
        }
    }
}