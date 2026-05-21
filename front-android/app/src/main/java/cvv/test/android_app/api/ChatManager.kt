package cvv.test.android_app.api

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query



@Serializable
data class MessageContent(
    val content: String,
    @SerialName("msg_type") val msgType: String = "text",
    @SerialName("reply_to_id") val replyToId: Long? = null
)

@Serializable
data class SocketMessage(
    @SerialName("room_id") val roomId: Long,
    val message: MessageContent
)

@Serializable
data class IncomingMessage(
    @SerialName("sender_id") val senderId: Long,
    @SerialName("chat_id") val chatId: Long,
    val content: String,
    @SerialName("msg_type") val msgType: String,
    val timestamp: String? = null
)

object ChatManager {
    private const val SOCKET_URL = "http://10.0.2.2:8000"
    private var socket: Socket? = null

    private val _messages = MutableSharedFlow<IncomingMessage>(extraBufferCapacity = 64)
    val messages = _messages.asSharedFlow()

    private val _connectionStatus = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val connectionStatus = _connectionStatus.asSharedFlow()

    private val _newChatCreated = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val newChatCreated = _newChatCreated.asSharedFlow()

    fun connect(token: String) {
        if (socket?.connected() == true) return

        try {
            Log.d("SocketIO", "Connecting to $SOCKET_URL")
            val options = IO.Options.builder()
                .setAuth(mapOf("token" to token))
                .setQuery("token=$token")
                .build()

            socket = IO.socket(SOCKET_URL, options)

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d("SocketIO", "Connected successfully")
                _connectionStatus.tryEmit("Connected")
            }

            socket?.on(Socket.EVENT_DISCONNECT) { args ->
                Log.d("SocketIO", "Disconnected: ${args.getOrNull(0)}")
                _connectionStatus.tryEmit("Disconnected")
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e("SocketIO", "Connection Error: ${args.getOrNull(0)}")
                _connectionStatus.tryEmit("Error")
            }

            socket?.on("receive_message") { args ->
                val data = args[0] as JSONObject
                try {
                    val message = Json.decodeFromString<IncomingMessage>(data.toString())
                    _messages.tryEmit(message)
                } catch (e: Exception) {
                    Log.e("SocketIO", "Error parsing incoming message: ${e.message}")
                }
            }

            socket?.on("chat_created") { args ->
                try {
                    val data = args[0] as JSONObject
                    val chatId = data.getLong("chat_id")
                    _newChatCreated.tryEmit(chatId)
                    Log.d("SocketIO", "Chat created with ID: $chatId")
                } catch (e: Exception) {
                    Log.e("SocketIO", "Error parsing chat_created: ${e.message}")
                }
            }

            socket?.connect()
        } catch (e: Exception) {
            Log.e("SocketIO", "Init Error: ${e.message}")
        }
    }

    fun sendMessage(roomId: Long?, text: String) {
        try {
            val messageObj = JSONObject().apply {
                put("content", text)
                put("msg_type", "text")
                put("reply_to_id", JSONObject.NULL)
            }

            val data = JSONObject().apply {
                put("room_id", roomId)
                put("message", messageObj)
            }

            Log.d("SocketIO", "Sending message to room $roomId: $data")
            socket?.emit("send_message", data)
        } catch (e: Exception) {
            Log.e("SocketIO", "Send Error: ${e.message}")
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
    }
}
