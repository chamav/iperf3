package com.iperf3client.data.utils

import android.content.Context
import io.sentry.Sentry
import kotlin.system.exitProcess

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {
    
    private val defaultHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()
    
    override fun uncaughtException(thread: Thread, exception: Throwable) {
        // Отправляем краш-репорт в Sentry немедленно
        Sentry.captureException(exception) { scope ->
            scope.setTag("thread", thread.name)
            scope.setTag("crash_type", "uncaught_exception")
            scope.level = io.sentry.SentryLevel.FATAL
            scope.setExtra("thread_id", thread.id.toString())
            scope.setExtra("thread_state", thread.state.name)
            scope.setExtra("thread_isAlive", thread.isAlive.toString())
            scope.setExtra("thread_isDaemon", thread.isDaemon.toString())
        }
        
        Logger.crash("CrashHandler", "Uncaught exception in thread: ${thread.name}", exception)
        
        // Сохраняем детали краша локально как backup
        saveCrashDetails(thread, exception)
        
        // Принудительно отправляем все pending события в Sentry
        Sentry.flush(2000) // ждем до 2 секунд
        
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