package ro.ase.smsbridge.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import ro.ase.smsbridge.data.AppDatabase
import ro.ase.smsbridge.data.Message

class WebSocketService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var webSocketClient: WebSocketClient
    private lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()
        webSocketClient = WebSocketClient.getInstance(applicationContext)
        database = AppDatabase.getDatabase(applicationContext)

        createNotificationChannel()
        startForeground(1, createNotification())

        observeMessages()
        observeConnectionState()
    }

    private fun observeConnectionState() {
        serviceScope.launch {
            webSocketClient.isConnected.collectLatest { connected ->
                updateNotification(connected)
            }
        }
    }

    private fun observeMessages() {
        serviceScope.launch {
            webSocketClient.messages.collect { jsonString ->
                if (jsonString.isNotBlank()) processIncomingMessage(jsonString)
            }
        }
    }

    private suspend fun processIncomingMessage(jsonString: String) {
        try {
            val json = Json.parseToJsonElement(jsonString).jsonObject
            when (json["type"]?.jsonPrimitive?.content) {
                "connection" -> Unit
                "send-sms" -> {
                    val id = json["id"]?.jsonPrimitive?.content ?: return
                    val phone = json["phone"]?.jsonPrimitive?.content
                    val body = json["message"]?.jsonPrimitive?.content

                    webSocketClient.sendMessage(buildJsonObject {
                        put("type", "ack")
                        put("id", id)
                    }.toString())

                    if (phone != null && body != null) {
                        try {
                            SmsSender.trimite(applicationContext, phone, body)
                            database.messageDao().insert(Message(sender = phone, message = body, status = "Sent via WebSocket"))
                        } catch (e: Exception) {
                            Log.e("WebSocketService", "SMS send error: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WebSocketService", "Message parse error: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel("websocket_channel", "Serviciu WebSocket", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createNotification(isConnected: Boolean = false): Notification {
        val statusText = if (isConnected) "Conectat la server - Gata de trimitere" else "Deconectat - Se reîncearcă..."
        return NotificationCompat.Builder(this, "websocket_channel")
            .setContentTitle("SMS Bridge - Sync")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(isConnected: Boolean) {
        getSystemService(NotificationManager::class.java).notify(1, createNotification(isConnected))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
