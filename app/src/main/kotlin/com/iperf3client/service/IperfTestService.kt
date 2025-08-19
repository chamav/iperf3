package com.iperf3client.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.iperf3client.IperfApplication
import com.iperf3client.MainActivity
import com.iperf3client.R
import com.iperf3client.data.engine.IperfEngineImpl
import com.iperf3client.domain.model.IperfEvent
import com.iperf3client.domain.model.TestParams
import com.iperf3client.domain.model.TestStatus
import com.iperf3client.domain.usecase.RunTestUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class IperfTestService : Service() {
    
    private val binder = IperfTestBinder()
    private var serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private lateinit var runTestUseCase: RunTestUseCase
    private lateinit var notificationManager: NotificationManager
    private var wakeLock: PowerManager.WakeLock? = null
    
    private var currentTestParams: TestParams? = null
    private var currentStatus = TestStatus.IDLE
    private var currentSessionId: String? = null
    
    // Callbacks for UI updates
    private val statusCallbacks = mutableSetOf<(TestStatus) -> Unit>()
    private val progressCallbacks = mutableSetOf<(IperfEvent) -> Unit>()
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        const val ACTION_START_TEST = "com.iperf3client.START_TEST"
        const val ACTION_STOP_TEST = "com.iperf3client.STOP_TEST"
        const val EXTRA_TEST_PARAMS = "test_params"
        
        fun startTest(context: Context, params: TestParams) {
            val intent = Intent(context, IperfTestService::class.java).apply {
                action = ACTION_START_TEST
                putExtra(EXTRA_TEST_PARAMS, params)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopTest(context: Context) {
            val intent = Intent(context, IperfTestService::class.java).apply {
                action = ACTION_STOP_TEST
            }
            context.startService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        
        val app = application as IperfApplication
        val iperfEngine = IperfEngineImpl(this)
        
        runTestUseCase = RunTestUseCase(
            iperfEngine = iperfEngine,
            historyRepository = app.historyRepository,
            settingsRepository = app.settingsRepository,
            context = this
        )
        
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TEST -> {
                val params = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_TEST_PARAMS, TestParams::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_TEST_PARAMS)
                }
                
                if (params != null) {
                    startTest(params)
                }
            }
            ACTION_STOP_TEST -> {
                stopTest()
            }
        }
        
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent): IBinder = binder
    
    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        serviceScope.cancel()
    }
    
    private fun startTest(params: TestParams) {
        if (currentStatus == TestStatus.RUNNING) {
            return
        }
        
        currentTestParams = params
        updateStatus(TestStatus.RUNNING)
        
        startForeground(NOTIFICATION_ID, createNotification())
        acquireWakeLock()
        
        serviceScope.launch {
            try {
                runTestUseCase(params)
                    .onEach { result ->
                        when (result) {
                            is com.iperf3client.domain.usecase.TestExecutionResult.Started -> {
                                currentSessionId = result.sessionId
                                notifyProgress(IperfEvent.Started(result.sessionId))
                            }
                            is com.iperf3client.domain.usecase.TestExecutionResult.Progress -> {
                                updateNotification("Running: ${result.tick.throughputMbps} Mbps")
                                notifyProgress(IperfEvent.Progress(result.tick))
                            }
                            is com.iperf3client.domain.usecase.TestExecutionResult.Completed -> {
                                updateStatus(TestStatus.COMPLETED)
                                updateNotification("Test completed")
                                notifyProgress(IperfEvent.Completed(result.result))
                                stopSelf()
                            }
                            is com.iperf3client.domain.usecase.TestExecutionResult.Error -> {
                                updateStatus(TestStatus.ERROR)
                                updateNotification("Test failed: ${result.message}")
                                notifyProgress(IperfEvent.Error(result.message, currentSessionId))
                                stopSelf()
                            }
                            is com.iperf3client.domain.usecase.TestExecutionResult.Log -> {
                                notifyProgress(IperfEvent.Log(result.line))
                            }
                        }
                    }
                    .launchIn(this)
            } catch (e: Exception) {
                updateStatus(TestStatus.ERROR)
                updateNotification("Test failed: ${e.message}")
                notifyProgress(IperfEvent.Error(e.message ?: "Unknown error", currentSessionId))
                stopSelf()
            }
        }
    }
    
    private fun stopTest() {
        if (currentStatus == TestStatus.RUNNING) {
            serviceScope.launch {
                currentSessionId?.let { sessionId ->
                    // TODO: Stop the actual test
                    updateStatus(TestStatus.CANCELLED)
                    updateNotification("Test cancelled")
                    notifyProgress(IperfEvent.Error("Test cancelled by user", sessionId))
                }
                stopSelf()
            }
        }
    }
    
    private fun updateStatus(status: TestStatus) {
        currentStatus = status
        statusCallbacks.forEach { it(status) }
    }
    
    private fun notifyProgress(event: IperfEvent) {
        progressCallbacks.forEach { it(event) }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                IperfApplication.NOTIFICATION_CHANNEL_TEST,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(content: String = getString(R.string.notification_test_running)): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopIntent = Intent(this, IperfTestService::class.java).apply {
            action = ACTION_STOP_TEST
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, IperfApplication.NOTIFICATION_CHANNEL_TEST)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_stop,
                getString(R.string.stop_test),
                stopPendingIntent
            )
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }
    
    private fun updateNotification(content: String) {
        val notification = createNotification(content)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "IperfClient::TestWakeLock"
            ).apply {
                acquire(10 * 60 * 1000L) // 10 minutes max
            }
        } catch (e: Exception) {
            // Wake lock acquisition failed, continue without it
        }
    }
    
    private fun releaseWakeLock() {
        wakeLock?.let { wl ->
            if (wl.isHeld) {
                wl.release()
            }
            wakeLock = null
        }
    }
    
    inner class IperfTestBinder : Binder() {
        fun getService(): IperfTestService = this@IperfTestService
        
        fun getCurrentStatus(): TestStatus = currentStatus
        
        fun getCurrentParams(): TestParams? = currentTestParams
        
        fun addStatusCallback(callback: (TestStatus) -> Unit) {
            statusCallbacks.add(callback)
        }
        
        fun removeStatusCallback(callback: (TestStatus) -> Unit) {
            statusCallbacks.remove(callback)
        }
        
        fun addProgressCallback(callback: (IperfEvent) -> Unit) {
            progressCallbacks.add(callback)
        }
        
        fun removeProgressCallback(callback: (IperfEvent) -> Unit) {
            progressCallbacks.remove(callback)
        }
    }
}