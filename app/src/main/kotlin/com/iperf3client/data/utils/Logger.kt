package com.iperf3client.data.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

object Logger {
    
    private const val TAG = "IperfClient"
    private const val LOG_FILE_NAME = "iperf_client.log"
    private var logFile: File? = null
    private var isInitialized = false
    
    fun init(context: Context) {
        if (!isInitialized) {
            val logDir = File(context.filesDir, "logs")
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            logFile = File(logDir, LOG_FILE_NAME)
            isInitialized = true
            d("Logger", "Logger initialized, log file: ${logFile?.absolutePath}")
        }
    }
    
    fun d(tag: String, message: String) {
        Log.d("$TAG:$tag", message)
        writeToFile("D", tag, message)
    }
    
    fun i(tag: String, message: String) {
        Log.i("$TAG:$tag", message)
        writeToFile("I", tag, message)
    }
    
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w("$TAG:$tag", message, throwable)
        writeToFile("W", tag, message, throwable)
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e("$TAG:$tag", message, throwable)
        writeToFile("E", tag, message, throwable)
    }
    
    fun crash(tag: String, message: String, throwable: Throwable) {
        Log.wtf("$TAG:$tag", message, throwable)
        writeToFile("CRASH", tag, message, throwable)
        
        // Дополнительные действия при критических ошибках
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            e("UncaughtException", "Uncaught exception in thread ${thread.name}", exception)
            // Здесь можно добавить отправку краш-репорта
        }
    }
    
    private fun writeToFile(level: String, tag: String, message: String, throwable: Throwable? = null) {
        try {
            logFile?.let { file ->
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
                val logEntry = buildString {
                    append("$timestamp [$level] $tag: $message")
                    throwable?.let { t ->
                        append("\n")
                        append(t.stackTraceToString())
                    }
                    append("\n")
                }
                
                FileWriter(file, true).use { writer ->
                    writer.write(logEntry)
                    writer.flush()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write to log file", e)
        }
    }
    
    fun getLogFile(): File? = logFile
    
    fun clearLogFile() {
        try {
            logFile?.delete()
            d("Logger", "Log file cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear log file", e)
        }
    }
}