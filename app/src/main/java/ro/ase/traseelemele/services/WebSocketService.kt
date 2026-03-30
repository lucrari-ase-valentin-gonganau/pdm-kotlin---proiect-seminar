package ro.ase.traseelemele.services

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
import ro.ase.traseelemele.SmsSender
import ro.ase.traseelemele.data.AppDatabase
import ro.ase.traseelemele.data.Message

class WebSocketService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var webSocketClient: WebSocketClient
    private lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()
        Log.d("WebSocketService", "Service onCreate started")
        webSocketClient = WebSocketClient.getInstance(applicationContext)
        database = AppDatabase.getDatabase(applicationContext)

        createNotificationChannel()
        startForeground(1, createNotification())
        
        observeMessages()
    }

    private fun observeMessages() {
        serviceScope.launch {
            Log.d("WebSocketService", "Starting to observe messages flow")
            // Folosim collect (nu collectLatest) pentru a ne asigura ca fiecare mesaj este procesat complet
            webSocketClient.messages.collect { jsonString ->
                Log.d("WebSocketService", "Flow emitted: $jsonString")
                if (jsonString.isNotBlank()) {
                    processIncomingMessage(jsonString)
                }
            }
        }
    }

    private suspend fun processIncomingMessage(jsonString: String) {
        try {
            Log.d("WebSocketService", "Processing message: $jsonString")
            val json = Json.parseToJsonElement(jsonString).jsonObject
            val type = json["type"]?.jsonPrimitive?.content
            Log.d("WebSocketService", "Message type identified: $type")
            
            when (type) {
                "connection" -> {
                    val clientId = json["clientIdSavedInDatabase"]?.jsonPrimitive?.content
                    Log.d("WebSocketService", "Server confirmed connection. Client ID: $clientId")
                }
                "send-sms" -> {
                    val id = json["id"]?.jsonPrimitive?.content
                    val phone = json["phone"]?.jsonPrimitive?.content
                    val body = json["message"]?.jsonPrimitive?.content
                    
                    Log.d("WebSocketService", "SMS details - ID: $id, Phone: $phone, Message: $body")

                    if (id != null) {
                        // Trimite ACK IMEDIAT
                        val ackResponse = buildJsonObject {
                            put("type", "ack")
                            put("id", id)
                        }.toString()
                        
                        Log.d("WebSocketService", "Attempting to send ACK for ID: $id")
                        webSocketClient.sendMessage(ackResponse)

                        if (phone != null && body != null) {
                            try {
                                Log.d("WebSocketService", "Sending SMS to $phone...")
                                SmsSender.trimite(applicationContext, phone, body)
                                
                                Log.d("WebSocketService", "Saving message to database...")
                                val messageEntry = Message(
                                    sender = phone,
                                    message = body,
                                    status = "Sent via WebSocket"
                                )
                                database.messageDao().insert(messageEntry)
                                Log.d("WebSocketService", "SMS sent and saved successfully")
                            } catch (e: Exception) {
                                Log.e("WebSocketService", "Error during SMS send/save: ${e.message}", e)
                            }
                        } else {
                            Log.w("WebSocketService", "Missing phone or body for send-sms")
                        }
                    } else {
                        Log.w("WebSocketService", "Missing ID for send-sms")
                    }
                }
                else -> {
                    Log.d("WebSocketService", "Received unknown or unhandled message type: $type")
                }
            }
        } catch (e: Exception) {
            Log.e("WebSocketService", "Error parsing/processing message: ${e.message}", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("WebSocketService", "onStartCommand received")
        serviceScope.launch {
            webSocketClient.isConnected.collectLatest { connected ->
                updateNotification(connected)
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "websocket_channel",
            "Serviciu WebSocket",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(isConnected: Boolean = false): Notification {
        val statusText = if (isConnected) "Conectat la server - Gata de trimitere" else "Deconectat - Se reîncearcă..."
        return NotificationCompat.Builder(this, "websocket_channel")
            .setContentTitle("Traseele Mele - SMS Sync")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(isConnected: Boolean) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(1, createNotification(isConnected))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d("WebSocketService", "Service onDestroy")
        super.onDestroy()
        webSocketClient.disconnect()
        serviceScope.cancel()
    }
}
