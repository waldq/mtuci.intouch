package cvv.test.android_app.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.*

@Serializable
data class ChatDirectCreate(
    @SerialName("chat_type") val chatType: String = "direct",
    val title: String? = null
)

@Serializable
data class ChatGroupCreate(
    @SerialName("title") val title: String
)

@Serializable
data class CreateGroupChatPayload(
    @SerialName("chat_data") val chatData: ChatGroupCreate,
    @SerialName("members_data") val membersData: List<Long>
)

@Serializable
data class ChatUpdate(
    val title: String
)

@Serializable
data class ChatInviteRequest(
    @SerialName("chat_id") val chatId: Long,
    @SerialName("members_data") val membersData: List<Long>
)

@Serializable
data class ChatResponse(
    val id: Long,
    @SerialName("chat_type") val chatType: String,
    val title: String? = null
)

interface ChatsApi {
    @POST("chats/create_direct")
    suspend fun createDirectChat(
        @Header("Authorization") token: String,
        @Query("member_id") memberId: Long,
        @Body chatData: ChatDirectCreate = ChatDirectCreate()
    ): Response<ChatResponse>

    @POST("chats/create_group")
    suspend fun createGroupChat(
        @Header("Authorization") token: String,
        @Body payload: CreateGroupChatPayload
    ): Response<ChatResponse>

    @GET("chats/user_chats")
    suspend fun getUserChats(
        @Header("Authorization") token: String
    ): Response<List<ChatResponse>>

    @PATCH("chats/update_chat_info")
    suspend fun updateChatInfo(
        @Header("Authorization") token: String,
        @Query("chat_id") chatId: Long,
        @Body updateData: ChatUpdate
    ): Response<ChatResponse>

    @DELETE("chats/delete_chat")
    suspend fun deleteChat(
        @Header("Authorization") token: String,
        @Query("chat_id") chatId: Long
    ): Response<Map<String, String>>

    @POST("chats/invite_user")
    suspend fun inviteUser(
        @Header("Authorization") token: String,
        @Query("chat_id") chatId: Long,
        @Body membersData: List<Long>
    ): Response<Unit>

    @DELETE("chats/kick_user")
    suspend fun kickUser(
        @Header("Authorization") token: String,
        @Query("chat_id") chatId: Long,
        @Query("to_kick_id") toKickId: Long
    ): Response<Map<String, String>>
}
