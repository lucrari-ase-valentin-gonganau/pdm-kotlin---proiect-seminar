package ro.bitweb.smsbridge.services

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WebSocketClient private constructor(context: Context) {
    private val appPreferences = AppPreferences(context)
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()
    
    private var webSocket: WebSocket? = null
    private var currentUrl: String? = null
    private var isManuallyClosed = false
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var observeJob: Job? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    // ATENTIE: replay trebuie sa fie 0. Cu replay = 5, cand WebSocketService era
    // omorat si repornit (START_STICKY / redeschiderea aplicatiei), noul collector
    // primea din nou ultimele 5 mesaje - inclusiv comenzi send-sms deja executate -
    // si retrimitea SMS-urile. Buffer-ul extra ramane pentru a nu pierde mesaje
    // cand consumatorul e momentan ocupat.
    private val _messages = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 64)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    companion object {
        @Volatile
        private var INSTANCE: WebSocketClient? = null

        fun getInstance(context: Context): WebSocketClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WebSocketClient(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        startObservingSettings()
    }

    private fun startObservingSettings() {
        observeJob?.cancel()
        observeJob = scope.launch {
            appPreferences.get("ws_url").collectLatest { url ->
                if (url != currentUrl) {
                    currentUrl = url
                    if (!url.isNullOrBlank()) {
                        Log.d("WebSocketClient", "Connecting to: $url")
                        connect(url)
                    } else {
                        disconnect()
                    }
                }
            }
        }
    }

    private fun connect(url: String) {
        isManuallyClosed = false
        webSocket?.close(1000, "Reconnecting")
        
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocketClient", "Connected to $url")
                _isConnected.value = true
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WS", "Received: $text")
                
                // Trimite ACK imediat pe acelasi socket daca e un mesaj tip send-sms
                try {
                    val json = JSONObject(text)
                    if (json.optString("type") == "send-sms") {
                        val id = json.optString("id")
                        if (id.isNotEmpty()) {
                            val ack = JSONObject().apply {
                                put("type", "ack")
                                put("id", id)
                            }.toString()
                            webSocket.send(ack)
                            Log.d("WS", "Ack sent immediately: $ack")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WS", "Error parsing for auto-ack: ${e.message}")
                }

                scope.launch {
                    _messages.emit(text)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _isConnected.value = false
                if (!isManuallyClosed) retryConnection()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocketClient", "Failure: ${t.message}")
                _isConnected.value = false
                if (!isManuallyClosed) retryConnection()
            }
        })
    }

    fun sendMessage(message: String) {
        scope.launch {
            if (_isConnected.value) {
                val result = webSocket?.send(message) ?: false
                Log.d("WS", "Manual send: $message (Success: $result)")
            } else {
                Log.w("WebSocketClient", "Cannot send, not connected")
            }
        }
    }

    private fun retryConnection() {
        scope.launch {
            delay(5000)
            currentUrl?.let {
                if (!_isConnected.value && !isManuallyClosed) {
                    connect(it)
                }
            }
        }
    }

    fun disconnect() {
        isManuallyClosed = true
        webSocket?.close(1000, "Disconnected")
        _isConnected.value = false
    }
}
