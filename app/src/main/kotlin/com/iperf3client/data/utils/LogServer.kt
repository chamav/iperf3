package com.iperf3client.data.utils

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import fi.iki.elonen.NanoHTTPD
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

class LogServer(private val context: Context, port: Int = 8080) : NanoHTTPD(port) {
    
    private val logger = Logger
    
    override fun serve(session: IHTTPSession?): Response {
        val uri = session?.uri ?: "/"
        
        return when {
            uri == "/" -> createIndexPage()
            uri == "/logs" -> getLogFile()
            uri == "/logcat" -> getLogcat()
            uri == "/clear-logs" -> clearLogs()
            uri == "/crash" -> getCrashReport()
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found")
        }
    }
    
    private fun createIndexPage(): Response {
        val ip = getWifiIpAddress()
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>IPerf3 Client - Logs</title>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }
                    .container { max-width: 1200px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; }
                    .header { background: #2196F3; color: white; padding: 15px; border-radius: 5px; margin-bottom: 20px; }
                    .info { background: #e3f2fd; padding: 10px; border-radius: 5px; margin-bottom: 20px; }
                    .btn { display: inline-block; padding: 10px 20px; background: #2196F3; color: white; text-decoration: none; border-radius: 5px; margin: 5px; }
                    .btn:hover { background: #1976D2; }
                    .danger { background: #f44336; }
                    .danger:hover { background: #d32f2f; }
                    .logs { background: #f5f5f5; padding: 15px; border-radius: 5px; font-family: monospace; max-height: 500px; overflow-y: scroll; }
                </style>
                <script>
                    function refreshLogs() {
                        fetch('/logcat')
                            .then(response => response.text())
                            .then(data => {
                                document.getElementById('liveLogs').innerHTML = '<pre>' + data + '</pre>';
                            });
                    }
                    
                    setInterval(refreshLogs, 3000); // Обновлять каждые 3 секунды
                </script>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔧 IPerf3 Client - Log Viewer</h1>
                        <p>Сервер запущен на: <strong>http://$ip:8080</strong></p>
                    </div>
                    
                    <div class="info">
                        <h3>📊 Доступные логи:</h3>
                        <a href="/logs" class="btn">📄 Файл логов приложения</a>
                        <a href="/logcat" class="btn">📱 Live Logcat</a>
                        <a href="/crash" class="btn">💥 Краш-репорты</a>
                        <a href="/clear-logs" class="btn danger">🗑️ Очистить логи</a>
                    </div>
                    
                    <div>
                        <h3>🔴 Live Logcat (обновляется автоматически):</h3>
                        <div id="liveLogs" class="logs">Загрузка логов...</div>
                    </div>
                    
                    <div style="margin-top: 20px; color: #666; font-size: 12px;">
                        <p>💡 Инструкции:</p>
                        <ul>
                            <li>Логи обновляются каждые 3 секунды</li>
                            <li>Используйте браузер на компьютере для удобного просмотра</li>
                            <li>Убедитесь что устройство и компьютер в одной WiFi сети</li>
                        </ul>
                    </div>
                </div>
                
                <script>refreshLogs();</script>
            </body>
            </html>
        """.trimIndent()
        
        return newFixedLengthResponse(Response.Status.OK, "text/html", html)
    }
    
    private fun getLogFile(): Response {
        return try {
            val logFile = logger.getLogFile()
            if (logFile?.exists() == true) {
                val content = logFile.readText()
                newFixedLengthResponse(Response.Status.OK, "text/plain", content)
            } else {
                newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Log file not found")
            }
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Error reading log file: ${e.message}")
        }
    }
    
    private fun getLogcat(): Response {
        return try {
            val process = Runtime.getRuntime().exec("logcat -d -s IperfClient:* *:E *:W")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            
            reader.use {
                var line: String?
                while (it.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
            }
            
            val result = output.toString().takeLast(10000) // Последние 10KB логов
            newFixedLengthResponse(Response.Status.OK, "text/plain", result)
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Error getting logcat: ${e.message}")
        }
    }
    
    private fun getCrashReport(): Response {
        return try {
            val crashFile = File(context.filesDir, "crash_report.txt")
            if (crashFile.exists()) {
                val content = crashFile.readText()
                newFixedLengthResponse(Response.Status.OK, "text/plain", content)
            } else {
                newFixedLengthResponse(Response.Status.OK, "text/plain", "No crash reports found")
            }
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Error reading crash report: ${e.message}")
        }
    }
    
    private fun clearLogs(): Response {
        return try {
            logger.clearLogFile()
            val crashFile = File(context.filesDir, "crash_report.txt")
            if (crashFile.exists()) {
                crashFile.delete()
            }
            // Очистить logcat
            Runtime.getRuntime().exec("logcat -c")
            
            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Логи очищены</title>
                    <meta http-equiv="refresh" content="2;url=/">
                </head>
                <body>
                    <h2>✅ Логи очищены!</h2>
                    <p>Перенаправление на главную страницу...</p>
                    <a href="/">← Назад</a>
                </body>
                </html>
            """.trimIndent()
            
            newFixedLengthResponse(Response.Status.OK, "text/html", html)
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Error clearing logs: ${e.message}")
        }
    }
    
    private fun getWifiIpAddress(): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            val ip = wifiInfo.ipAddress
            Formatter.formatIpAddress(ip)
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    fun startServer(): Boolean {
        return try {
            start()
            val ip = getWifiIpAddress()
            logger.i("LogServer", "HTTP log server started at http://$ip:8080")
            true
        } catch (e: IOException) {
            logger.e("LogServer", "Failed to start log server", e)
            false
        }
    }
    
    fun stopServer() {
        stop()
        logger.i("LogServer", "HTTP log server stopped")
    }
}