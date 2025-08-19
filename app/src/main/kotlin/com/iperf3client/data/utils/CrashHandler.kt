package com.iperf3client.data.utils

import android.content.Context
import kotlin.system.exitProcess

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {
    
    private val defaultHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()
    
    override fun uncaughtException(thread: Thread, exception: Throwable) {
        Logger.crash("CrashHandler", "Uncaught exception in thread: ${thread.name}", exception)
        
        // Сохраняем детали краша
        saveCrashDetails(thread, exception)
        
        // Вызываем стандартный обработчик если он есть
        defaultHandler?.uncaughtException(thread, exception)
        
        // Завершаем приложение
        exitProcess(1)
    }
    
    private fun saveCrashDetails(thread: Thread, exception: Throwable) {
        try {
            val crashInfo = buildString {
                appendLine("=== CRASH REPORT ===")
                appendLine("Timestamp: ${java.util.Date()}")
                appendLine("Thread: ${thread.name}")
                appendLine("Exception: ${exception.javaClass.name}")
                appendLine("Message: ${exception.message}")
                appendLine("Stack trace:")
                appendLine(exception.stackTraceToString())
                appendLine("===================")
            }
            
            val crashFile = java.io.File(context.filesDir, "crash_report.txt")
            crashFile.writeText(crashInfo)
            
        } catch (e: Exception) {
            Logger.e("CrashHandler", "Failed to save crash details", e)
        }
    }
}