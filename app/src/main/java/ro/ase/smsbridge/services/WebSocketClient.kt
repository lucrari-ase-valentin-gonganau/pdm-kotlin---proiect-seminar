package ro.ase.smsbridge.services

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
import java.util.concurrent.TimeUnit

class WebSocketClient private constructor(context: Context) {
    private val appPreferences = AppPreferences(context)

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var currentUrl: String? = null
    private var isManuallyClosed = false
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var observeJob: Job? = null
    private var connectJob: Job? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    private val _messages = MutableSharedFlow<String>(replay = 5, extraBufferCapacity = 64)
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
                val trimmedUrl = url?.trim()
                if (!trimmedUrl.isNullOrBlank()) {
                    if (trimmedUrl != currentUrl || !_isConnected.value) {
                        connect(trimmedUrl)
                    }
                } else {
                    disconnect()
                }
            }
        }
    }

    fun connect(url: String? = currentUrl) {
        val targetUrl = url?.trim() ?: return
        if (targetUrl.isBlank()) return

        currentUrl = targetUrl
        isManuallyClosed = false
        _lastError.value = null

        connectJob?.cancel()
        connectJob = scope.launch {
            try {
                webSocket?.cancel()
                webSocket = null
                _isConnected.value = false

                val normalizedUrl = when {
                    targetUrl.startsWith("wss://", ignoreCase = true) -> targetUrl
                    targetUrl.startsWith("ws://", ignoreCase = true) -> targetUrl
                    targetUrl.startsWith("https://", ignoreCase = true) -> targetUrl.replaceFirst("https://", "wss://", ignoreCase = true)
                    targetUrl.startsWith("http://", ignoreCase = true) -> targetUrl.replaceFirst("http://", "ws://", ignoreCase = true)
                    targetUrl.contains(".") && !targetUrl.startsWith("10.") && !targetUrl.startsWith("192.") && !targetUrl.startsWith("localhost") -> "wss://$targetUrl"
                    else -> "ws://$targetUrl"
                }

                Log.d("WebSocketClient", "Connecting to: $normalizedUrl")
                val request = Request.Builder()
                    .url(normalizedUrl)
                    .addHeader("Origin", "http://localhost")
                    .addHeader("User-Agent", "SmsBridge-Android")
                    .build()

                webSocket = client.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        _isConnected.value = true
                        _lastError.value = null
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        scope.launch { _messages.emit(text) }
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        _isConnected.value = false
                        if (!isManuallyClosed) retryConnection()
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        val errorDetail = when {
                            response?.code == 404 -> "Serverul nu a răspuns (404)"
                            response?.code == 403 -> "Acces refuzat (403)"
                            t.message?.contains("cleartxt", ignoreCase = true) == true -> "Android blochează WS simplu. Folosește WSS."
                            t.message?.contains("Failed to connect", ignoreCase = true) == true -> "Server inaccesibil la $normalizedUrl"
                            else -> t.message ?: "Eroare rețea"
                        }
                        Log.e("WebSocketClient", "Failure: $errorDetail")
                        _lastError.value = errorDetail
                        _isConnected.value = false
                        if (!isManuallyClosed) retryConnection()
                    }
                })
            } catch (e: Exception) {
                _lastError.value = "Eroare: ${e.message}"
                Log.e("WebSocketClient", "Connect error: ${e.message}")
            }
        }
    }

    fun sendMessage(message: String) {
        scope.launch {
            if (_isConnected.value) {
                webSocket?.send(message)
            }
        }
    }

    private fun retryConnection() {
        if (isManuallyClosed) return
        scope.launch {
            delay(10000)
            currentUrl?.let {
                if (!_isConnected.value) connect(it)
            }
        }
    }

    fun disconnect() {
        isManuallyClosed = true
        webSocket?.close(1000, "Manual disconnect")
        webSocket = null
        _isConnected.value = false
        currentUrl = null
    }
}
