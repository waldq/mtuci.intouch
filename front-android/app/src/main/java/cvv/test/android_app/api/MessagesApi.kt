package cvv.test.android_app.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

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
    @SerialName("reply_to_id") val replyToId: String? = null,
)

@Serializable
data class IncomingMessage(
    val id: String? = null,
    @SerialName("sender_id") val senderId: String? = null,
    @SerialName("sender_username") val senderUsername: String? = null,
    @SerialName("chat_id") val chatId: String? = null,
    val content: String = "",
    @SerialName("msg_type") val msgType: MessageType = MessageType.TEXT,
    @SerialName("reply_to_id") val replyToId: String? = null,
    @SerialName("created_at") val timestamp: String? = null,
)

@Serializable
data class SentMessage(
    @SerialName("room_id") val roomId: String,
    val message: Message,
)

interface MessagesApi {
    @GET("messages/chat_messages")
    suspend fun getChatMessages(
        @Header("Authorization") token: String,
        @Query("chat_id") chatId: Long,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): Response<List<IncomingMessage>>
}
