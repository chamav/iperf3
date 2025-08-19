package com.iperf3client.data.utils

import android.content.Context
import android.util.Log
import io.sentry.Sentry
import io.sentry.SentryLevel
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
    private var sentryEnabled = true
    
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
        // Отправляем debug логи в Sentry только в development окружении
        if (isDebugMode() && isSentryInitialized()) {
            try {
                Sentry.addBreadcrumb("[$tag] $message", "debug")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send debug breadcrumb to Sentry", e)
            }
        }
    }
    
    fun i(tag: String, message: String) {
        Log.i("$TAG:$tag", message)
        writeToFile("I", tag, message)
        // Отправляем важные info сообщения как breadcrumbs
        if (sentryEnabled && isSentryInitialized()) {
            try {
                Sentry.addBreadcrumb("[$tag] $message", "info")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send breadcrumb to Sentry", e)
            }
        }
    }
    
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w("$TAG:$tag", message, throwable)
        writeToFile("W", tag, message, throwable)
        // Отправляем предупреждения в Sentry
        if (sentryEnabled && isSentryInitialized()) {
            try {
                Sentry.captureMessage("[$tag] $message", SentryLevel.WARNING)
                throwable?.let { Sentry.captureException(it) }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send warning to Sentry", e)
            }
        }
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e("$TAG:$tag", message, throwable)
        writeToFile("E", tag, message, throwable)
        // Отправляем ошибки в Sentry
        if (sentryEnabled && isSentryInitialized()) {
            try {
                Sentry.captureMessage("[$tag] $message", SentryLevel.ERROR)
                throwable?.let { Sentry.captureException(it) }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send error to Sentry", e)
            }
        }
    }
    
    fun crash(tag: String, message: String, throwable: Throwable) {
        Log.wtf("$TAG:$tag", message, throwable)
        writeToFile("CRASH", tag, message, throwable)
        
        // Отправляем критические ошибки в Sentry (всегда, независимо от настройки пользователя)
        if (isSentryInitialized()) {
            try {
                Sentry.captureMessage("CRASH [$tag] $message", SentryLevel.FATAL)
                Sentry.captureException(throwable)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send crash to Sentry", e)
            }
        }
        
        // Дополнительные действия при критических ошибках
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            e("UncaughtException", "Uncaught exception in thread ${thread.name}", exception)
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
    
    fun setSentryEnabled(enabled: Boolean) {
        sentryEnabled = enabled
        d("Logger", "Sentry logging enabled: $enabled")
    }
    
    private fun isDebugMode(): Boolean {
        // Простая проверка - всегда включаем debug breadcrumbs в debug сборке
        // В production сборке это будет false (minifyEnabled)
        return android.util.Log.isLoggable(TAG, android.util.Log.DEBUG)
    }
    
    private fun isSentryInitialized(): Boolean {
        return try {
            Sentry.isEnabled()
        } catch (e: Exception) {
            false
        }
    }
}