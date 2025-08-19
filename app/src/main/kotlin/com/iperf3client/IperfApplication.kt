package com.iperf3client

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.room.Room
import com.iperf3client.data.database.IperfDatabase
import com.iperf3client.data.repository.HistoryRepositoryImpl
import com.iperf3client.data.repository.ServersRepositoryImpl
import com.iperf3client.data.repository.SettingsRepositoryImpl
import com.iperf3client.data.utils.CrashHandler
import com.iperf3client.data.utils.Logger
import com.iperf3client.BuildConfig
import com.iperf3client.domain.repository.HistoryRepository
import com.iperf3client.domain.repository.ServersRepository
import com.iperf3client.domain.repository.SettingsRepository
import io.sentry.android.core.SentryAndroid

class IperfApplication : Application() {
    
    // Database
    val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            IperfDatabase::class.java,
            "iperf_database"
        ).build()
    }
    
    // Repositories
    val serversRepository: ServersRepository by lazy {
        ServersRepositoryImpl(database.serverDao())
    }
    
    val historyRepository: HistoryRepository by lazy {
        HistoryRepositoryImpl(database.testResultDao())
    }
    
    val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(applicationContext)
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Инициализируем Sentry для сбора ошибок и логов
        SentryAndroid.init(this) { options ->
            options.dsn = BuildConfig.SENTRY_DSN
            // Определяем режим отладки через ApplicationInfo
            val isDebug = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            options.isDebug = isDebug
            // Настройки для production
            options.environment = if (isDebug) "development" else "production"
            options.release = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
            // Включаем автоматический сбор производительности
            options.tracesSampleRate = if (isDebug) 1.0 else 0.1
        }
        
        // Инициализируем логирование
        Logger.init(this)
        Logger.i("Application", "IperfApplication started")
        
        // Устанавливаем обработчик для необработанных исключений
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))
        
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Channel for test notifications
            val testChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_TEST,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
            }
            
            notificationManager.createNotificationChannel(testChannel)
        }
    }
    
    companion object {
        const val NOTIFICATION_CHANNEL_TEST = "iperf_test_channel"
    }
}