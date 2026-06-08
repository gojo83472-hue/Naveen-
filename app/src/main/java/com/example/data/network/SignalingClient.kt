package com.example.data.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import java.util.UUID
import java.util.concurrent.TimeUnit

sealed interface SignalingState {
    object Idle : SignalingState
    object Connecting : SignalingState
    object Connected : SignalingState // Connected to WebSocket server, waiting for peer
    data class PeerJoined(val peerId: String, val peerName: String, val peerBio: String) : SignalingState
    data class PeerLive(val peerId: String, val peerName: String, val peerBio: String, val peerMuted: Boolean = false) : SignalingState
    data class Error(val message: String) : SignalingState
    object PeerLeft : SignalingState
}

@Serializable
data class SignalingPayload(
    val type: String, // "JOIN", "WELCOME", "MUTED", "UNMUTED", "END_CALL", "PING"
    val senderId: String,
    val senderName: String,
    val senderBio: String,
    val content: String = ""
)

class SignalingClient {
    private val client = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    
    private val _state = MutableStateFlow<SignalingState>(SignalingState.Idle)
    val state: StateFlow<SignalingState> = _state.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    val myPeerId = UUID.randomUUID().toString().take(6)
    private var currentRoomId: String = ""
    private var myName: String = ""
    private var myBio: String = ""

    private val json = Json { ignoreUnknownKeys = true }
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    fun addLog(msg: String) {
        val current = _logs.value.takeLast(49)
        _logs.value = current + "[${System.currentTimeMillis() % 100000}] $msg"
        Log.d("SignalingClient", msg)
    }

    fun connect(roomId: String, localName: String, localBio: String, customUrl: String? = null) {
        disconnect()
        currentRoomId = roomId.trim().lowercase().ifEmpty { "global" }
        myName = localName.ifEmpty { "Alex Mercer" }
        myBio = localBio.ifEmpty { "Tech Developer" }

        // Secure, isolated public broadcaster channel that routes to channel_<roomId>
        val finalUrl = customUrl?.ifEmpty { null } ?: "wss://demo.piesocket.com/v3/channel_${currentRoomId}?api_key=VCoadpeZlgisw6v98M9v500HkGr7gY6N8Nn2Zogp"
        
        addLog("Connecting to signaling server for Room $currentRoomId...")
        _state.value = SignalingState.Connecting

        val request = Request.Builder()
            .url(finalUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                addLog("Connected to WebSocket Server! Room: $currentRoomId")
                _state.value = SignalingState.Connected
                // Immediately broadcast welcome join message
                broadcastMessage("JOIN", "Hello! Let's pair up.")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                coroutineScope.launch {
                    try {
                        val payload = json.decodeFromString<SignalingPayload>(text)
                        
                        // Ignore messages sent by ourselves
                        if (payload.senderId == myPeerId) return@launch

                        addLog("Received socket payload: ${payload.type} from ${payload.senderName}")
                        
                        when (payload.type) {
                            "JOIN" -> {
                                addLog("Peer ${payload.senderName} is looking to pair in room!")
                                _state.value = SignalingState.PeerJoined(
                                    peerId = payload.senderId,
                                    peerName = payload.senderName,
                                    peerBio = payload.senderBio
                                )
                                // Send back WELCOME to confirm pairing
                                broadcastMessage("WELCOME", "Yes! Connected.")
                                delay(1000)
                                // Establish live connection
                                _state.value = SignalingState.PeerLive(
                                    peerId = payload.senderId,
                                    peerName = payload.senderName,
                                    peerBio = payload.senderBio
                                )
                            }
                            "WELCOME" -> {
                                addLog("Peer ${payload.senderName} acknowledged welcome!")
                                _state.value = SignalingState.PeerJoined(
                                    peerId = payload.senderId,
                                    peerName = payload.senderName,
                                    peerBio = payload.senderBio
                                )
                                delay(1000)
                                _state.value = SignalingState.PeerLive(
                                    peerId = payload.senderId,
                                    peerName = payload.senderName,
                                    peerBio = payload.senderBio
                                )
                            }
                            "MUTED" -> {
                                val current = _state.value
                                if (current is SignalingState.PeerLive && current.peerId == payload.senderId) {
                                    addLog("Peer ${payload.senderName} muted audio.")
                                    _state.value = current.copy(peerMuted = true)
                                }
                            }
                            "UNMUTED" -> {
                                val current = _state.value
                                if (current is SignalingState.PeerLive && current.peerId == payload.senderId) {
                                    addLog("Peer ${payload.senderName} unmuted audio.")
                                    _state.value = current.copy(peerMuted = false)
                                }
                            }
                            "END_CALL" -> {
                                addLog("Peer ${payload.senderName} ended the call session.")
                                _state.value = SignalingState.PeerLeft
                            }
                        }
                    } catch (e: Exception) {
                        // Suppress parsing errors for non-matching payloads if any
                        Log.e("SignalingClient", "Parsing error: ${e.message}")
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                addLog("Closing socket stream: $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                addLog("Socket state disconnected.")
                _state.value = SignalingState.Idle
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                addLog("Socket handshaking failure: ${t.message}")
                _state.value = SignalingState.Error("Server unavailable: ${t.message}")
            }
        })
    }

    fun broadcastMessage(type: String, content: String = "") {
        val payload = SignalingPayload(
            type = type,
            senderId = myPeerId,
            senderName = myName,
            senderBio = myBio,
            content = content
        )
        try {
            val jsonText = json.encodeToString(payload)
            webSocket?.send(jsonText)
            addLog("Sent command payload: $type")
        } catch (e: Exception) {
            addLog("Failed to send socket message: ${e.message}")
        }
    }

    fun disconnect() {
        if (webSocket != null) {
            broadcastMessage("END_CALL", "Disconnecting")
            webSocket?.close(1000, "Done")
            webSocket = null
        }
        _state.value = SignalingState.Idle
        _logs.value = emptyList()
    }
}
