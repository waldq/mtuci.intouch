package cvv.test.android_app.core.state

import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import cvv.test.android_app.api.*
import cvv.test.android_app.core.data.ACCESS_TOKEN_KEY
import cvv.test.android_app.core.data.AUTH_PREFS
import cvv.test.android_app.core.data.GROUP_ID
import cvv.test.android_app.ui.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
class MainState(
    selectedTab: MutableState<Screen>,
    searchQuery: MutableState<String>,
    val searchResults: SnapshotStateList<UserSearchResult>,
    val allMessagesByChat: MutableMap<Long, SnapshotStateList<IncomingMessage>>,
    val activeChats: MutableMap<Long, UserSearchResult>,
    currentChatId: MutableLongState,
    activeChatPartner: MutableState<UserSearchResult?>,
    viewingUser: MutableState<UserSearchResult?>,
    pendingChatUserId: MutableState<String?>,
    myUserId: MutableState<String?>,
    myUsername: MutableState<String>,
    messageText: MutableState<String>,
    private val scope: CoroutineScope
) {
    var selectedTab by selectedTab
    var searchQuery by searchQuery
    var currentChatId by currentChatId
    var activeChatPartner by activeChatPartner
    var viewingUser by viewingUser
    var pendingChatUserId by pendingChatUserId
    var myUserId by myUserId
    var myUsername by myUsername
    var messageText by messageText

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
        val token = prefs.getString(ACCESS_TOKEN_KEY, "") ?: ""
        if (token.isNotEmpty()) {
            ChatManager.connect(token)
            ChatManager.joinChat(GROUP_ID)
            scope.launch {
                try {
                    val response = RetrofitClient.authApi.getMe("Bearer $token")
                    if (response.isSuccessful) {
                        val profile = response.body()
                        myUserId = profile?.userId
                        myUsername = profile?.username ?: "АНАТОЛИЙ"
                    }
                } catch (e: Exception) {
                    Log.e("MainState", "Profile error: ${e.message}")
                }
            }
        }

        scope.launch {
            ChatManager.messages.collect { msg ->
                val chatId = msg.chatId?.toLongOrNull() ?: GROUP_ID
                val list = allMessagesByChat.getOrPut(chatId) { mutableStateListOf() }
                list.add(msg)
            }
        }

        scope.launch {
            ChatManager.searchResults.collect { results ->
                searchResults.clear()
                searchResults.addAll(results)
            }
        }
    }

    fun onSearchChange(query: String) {
        searchQuery = query
        if (query.isNotEmpty()) {
            ChatManager.searchUser(query)
        } else {
            searchResults.clear()
        }
    }

    fun sendMessage(context: Context) {
        if (messageText.isBlank()) return
        scope.launch {
            if (pendingChatUserId != null && currentChatId == GROUP_ID) {
                val prefs = context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
                val token = prefs.getString(ACCESS_TOKEN_KEY, "") ?: ""
                val response = RetrofitClient.chatsApi.createDirectChat("Bearer $token", pendingChatUserId?.toLongOrNull() ?: 0L)
                if (response.isSuccessful) {
                    response.body()?.id?.let { id ->
                        currentChatId = id
                        activeChats[id] = activeChatPartner!!
                        ChatManager.joinChat(id)
                        pendingChatUserId = null
                        ChatManager.sendMessage(id, messageText)
                    }
                }
            } else {
                ChatManager.sendMessage(currentChatId, messageText)
            }
            messageText = ""
        }
    }
}

@Composable
fun rememberMainState(scope: CoroutineScope = rememberCoroutineScope()): MainState {
    val selectedTab = remember { mutableStateOf(Screen.CHATS) }
    val searchQuery = remember { mutableStateOf("") }
    val searchResults = remember { mutableStateListOf<UserSearchResult>() }
    val allMessagesByChat = remember { mutableStateMapOf<Long, SnapshotStateList<IncomingMessage>>() }
    val activeChats = remember { mutableStateMapOf<Long, UserSearchResult>() }
    val currentChatId = remember { mutableLongStateOf(GROUP_ID) }
    val activeChatPartner = remember { mutableStateOf<UserSearchResult?>(null) }
    val viewingUser = remember { mutableStateOf<UserSearchResult?>(null) }
    val pendingChatUserId = remember { mutableStateOf<String?>(null) }
    val myUserId = remember { mutableStateOf<String?>(null) }
    val myUsername = remember { mutableStateOf("АНАТОЛИЙ") }
    val messageText = remember { mutableStateOf("") }

    return remember {
        MainState(
            selectedTab = selectedTab,
            searchQuery = searchQuery,
            searchResults = searchResults,
            allMessagesByChat = allMessagesByChat,
            activeChats = activeChats,
            currentChatId = currentChatId,
            activeChatPartner = activeChatPartner,
            viewingUser = viewingUser,
            pendingChatUserId = pendingChatUserId,
            myUserId = myUserId,
            myUsername = myUsername,
            messageText = messageText,
            scope = scope
        )
    }
}
