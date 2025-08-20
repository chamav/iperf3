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
    external fun setEnvironmentVariable(name: String, value: String): Boolean
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
        // CRITICAL: Set TMPDIR BEFORE creating test
        // Android blocks access to /data/local/tmp, so we must use app's cache dir
        if (cacheDir == null) {
            callback.onError("Cache directory is required for Android")
            return false
        }
        
        // Set TMPDIR using native method which calls setenv()
        val envSet = setEnvironmentVariable("TMPDIR", cacheDir)
        if (!envSet) {
            callback.onError("Failed to set TMPDIR environment variable")
            return false
        }
        
        // Log for debugging
        android.util.Log.i("Iperf3Native", "TMPDIR set to: $cacheDir")
        
        val testPtr = createTest()
        if (testPtr == 0L) {
            callback.onError("Failed to create test - check logcat for details")
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