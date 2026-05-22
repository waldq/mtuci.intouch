package cvv.test.android_app.api

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONObject

@Serializable
enum class MessageType {
    @SerialName("text") TEXT,
    @SerialName("image") IMAGE,
    @SerialName("file") FILE
}

@Serializable
data class Message(
    val content: String?,
    @SerialName("msg_type") val msgType: MessageType = MessageType.TEXT,
    @SerialName("reply_to_id") val replyToId: Long? = null,
)

@Serializable
data class IncomingMessage(
    val id: String? = null,
    @SerialName("sender_id") val senderId: String? = null,
    @SerialName("chat_id") val chatId: String? = null,
    val content: String = "",
    @SerialName("msg_type") val msgType: MessageType = MessageType.TEXT,
    @SerialName("reply_to_id") val replyToId: String? = null,
    @SerialName("created_at") val timestamp: String? = null,
)

@Serializable
data class SentMessage(
    @SerialName("room_id") val roomId: Long,
    val message: Message,
)

object ChatManager {
    private const val SOCKET_URL = "http://10.0.2.2:8000"
    private var socket: Socket? = null
    private var currentRoomId: String? = null

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val _messages = MutableSharedFlow<IncomingMessage>(
        replay = 1, 
        extraBufferCapacity = 64
    )
    val messages = _messages.asSharedFlow()

    private val _connectionStatus = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val connectionStatus = _connectionStatus.asSharedFlow()

    fun connect(token: String) {
        if (socket?.connected() == true) return

        try {
            val options = IO.Options.builder()
                .setAuth(mapOf("token" to token))
                .setQuery("token=$token")
                .setTransports(arrayOf(WebSocket.NAME))
                .build()

            socket = IO.socket(SOCKET_URL, options)

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d("SocketIO", "CONNECTED TO SERVER")
                _connectionStatus.tryEmit("Connected")
                
                currentRoomId?.let { id ->
                    Log.d("SocketIO", "Emitting join_chat for room: $id")
                    socket?.emit("join_chat", id)
                }
            }

            socket?.on(Socket.EVENT_DISCONNECT) { args ->
                Log.d("SocketIO", "DISCONNECTED: ${args.getOrNull(0)}")
                _connectionStatus.tryEmit("Disconnected")
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e("SocketIO", "CONNECTION ERROR: ${args.getOrNull(0)}")
                _connectionStatus.tryEmit("Error")
            }

            // Слушаем ВСЕ сообщения для отладки
            socket?.on("receive_message") { args ->
                Log.d("SocketIO", ">>> INCOMING EVENT: receive_message")
                val rawData = args.getOrNull(0)
                Log.d("SocketIO", "Raw data: $rawData")

                val jsonString = when (rawData) {
                    is JSONObject -> rawData.toString()
                    is String -> rawData
                    else -> rawData?.toString()
                }

                if (jsonString != null) {
                    try {
                        val message = json.decodeFromString<IncomingMessage>(jsonString)
                        Log.d("SocketIO", "Message parsed successfully: $message")
                        _messages.tryEmit(message)
                    } catch (e: Exception) {
                        Log.e("SocketIO", "PARSING FAILED: ${e.message}")
                        Log.e("SocketIO", "Problematic JSON: $jsonString")
                    }
                }
            }

            // Дополнительный логгер для любых событий (если бэкенд шлет что-то другое)
            socket?.on("chat_created") { Log.d("SocketIO", "Event: chat_created received") }

            socket?.connect()
        } catch (e: Exception) {
            Log.e("SocketIO", "INIT ERROR: ${e.message}")
        }
    }

    fun joinChat(chatId: Long) {
        val idStr = chatId.toString()
        currentRoomId = idStr
        if (socket?.connected() == true) {
            Log.d("SocketIO", "Already connected. Joining room: $idStr")
            socket?.emit("join_chat", idStr)
        } else {
            Log.d("SocketIO", "Waiting for connection to join room: $idStr")
        }
    }

    fun sendMessage(roomId: Long, text: String, type: MessageType = MessageType.TEXT) {
        try {
            val sentMessage = SentMessage(
                roomId = roomId,
                message = Message(content = text, msgType = type)
            )
            val jsonString = json.encodeToString(SentMessage.serializer(), sentMessage)
            val data = JSONObject(jsonString)

            Log.d("SocketIO", "SENDING MESSAGE: $data")
            socket?.emit("send_message", data)
        } catch (e: Exception) {
            Log.e("SocketIO", "SEND ERROR: ${e.message}")
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
    }
}
