package cvv.test.android_app

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cvv.test.android_app.ui.theme.AuthFieldBackground
import cvv.test.android_app.ui.theme.AuthTextColor

import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import cvv.test.android_app.api.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ChatItem(
    val id: Int,
    val name: String,
    val lastMessage: String,
    val time: String
)

@Composable
fun MainScreen() {
    val selectedTab = remember { mutableStateOf(0) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var statusText by remember { mutableStateOf("Нажмите для проверки") }
    var lastChatId by remember { mutableStateOf<Long?>(null) }

    // Подключаемся к сокету при загрузке экрана
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
        val token = prefs.getString("access_token", "") ?: ""
        if (token.isNotEmpty()) {
            ChatManager.connect(token)
        }

        ChatManager.connectionStatus.collect { status ->
            statusText = "Статус: $status"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .paint(
                painter = painterResource(id = R.drawable.background),
                contentScale = ContentScale.Crop
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab.value) {
                    0 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = statusText,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = AuthTextColor,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )

                            // 1. GET: Получить чаты пользователя
                            TestButton("Получить все чаты (GET)") {
                                val response = RetrofitClient.chatsApi.getUserChats("Bearer ${getToken(context)}")
                                if (response.isSuccessful) {
                                    val chats = response.body()
                                    statusText = "Чатов: ${chats?.size}"
                                    Log.d("TestAPI", "Чаты: $chats")
                                } else {
                                    Log.e("TestAPI", "Error getting chats: ${response.code()} ${response.errorBody()?.string()}")
                                }
                            }

                            // 2. POST: Создать личный чат
                            TestButton("Создать личный чат (POST)") {
                                val response = RetrofitClient.chatsApi.createDirectChat(
                                    token = "Bearer ${getToken(context)}",
                                    memberId = 315408804688695296L
                                )
                                if (response.isSuccessful) {
                                    val chat = response.body()
                                    lastChatId = chat?.id
                                    statusText = "Создан личный чат: ${chat?.id}"
                                    Log.d("TestAPI", "Direct response: $chat")
                                } else {
                                    Log.e("TestAPI", "Error creating direct chat: ${response.code()} ${response.errorBody()?.string()}")
                                }
                            }

                            // 3. POST: Создать групповой чат
                            TestButton("Создать группу (POST)") {
                                val response = RetrofitClient.chatsApi.createGroupChat(
                                    token = "Bearer ${getToken(context)}",
                                    payload = CreateGroupChatPayload(
                                        chatData = ChatGroupCreate(title = "Тестовая группа"),
                                        membersData = listOf(315408804688695296L)
                                    )
                                )
                                if (response.isSuccessful) {
                                    val chat = response.body()
                                    lastChatId = chat?.id
                                    statusText = "Создана группа: ${chat?.id}"
                                    Log.d("TestAPI", "Group response: $chat")
                                } else {
                                    Log.e("TestAPI", "Error creating group chat: ${response.code()} ${response.errorBody()?.string()}")
                                }
                            }

                            // 4. PATCH: Обновить инфо чата
                            TestButton("Обновить инфо (PATCH)") {
                                lastChatId?.let { id ->
                                    val response = RetrofitClient.chatsApi.updateChatInfo(
                                        token = "Bearer ${getToken(context)}",
                                        chatId = id,
                                        updateData = ChatUpdate(title = "Обновленное название")
                                    )
                                    if (response.isSuccessful) {
                                        statusText = "Название обновлено для $id"
                                    } else {
                                        Log.e("TestAPI", "Error updating chat: ${response.code()} ${response.errorBody()?.string()}")
                                    }
                                } ?: run { Log.w("TestAPI", "Attempted update without chatId") }
                            }

                            // 5. POST: Пригласить пользователя
                            TestButton("Пригласить юзера (POST)") {
                                lastChatId?.let { id ->
                                    val response = RetrofitClient.chatsApi.inviteUser(
                                        token = "Bearer ${getToken(context)}",
                                        chatId = id,
                                        membersData = listOf(315408804688695296L)
                                    )
                                    if (response.isSuccessful) {
                                        statusText = "Юзер приглашен в $id"
                                    } else {
                                        Log.e("TestAPI", "Error inviting user: ${response.code()} ${response.errorBody()?.string()}")
                                    }
                                } ?: run { Log.w("TestAPI", "Attempted invite without chatId") }
                            }

                            // 6. DELETE: Исключить пользователя
                            TestButton("Кикнуть юзера (DELETE)") {
                                lastChatId?.let { id ->
                                    val response = RetrofitClient.chatsApi.kickUser(
                                        token = "Bearer ${getToken(context)}",
                                        chatId = id,
                                        toKickId = 315408804688695296L
                                    )
                                    if (response.isSuccessful) {
                                        statusText = "Юзер кикнут из $id"
                                    } else {
                                        Log.e("TestAPI", "Error kicking user: ${response.code()} ${response.errorBody()?.string()}")
                                    }
                                } ?: run { Log.w("TestAPI", "Attempted kick without chatId") }
                            }

                            // 7. DELETE: Удалить чат
                            TestButton("Удалить чат (DELETE)") {
                                lastChatId?.let { id ->
                                    val response = RetrofitClient.chatsApi.deleteChat(
                                        token = "Bearer ${getToken(context)}",
                                        chatId = id
                                    )
                                    if (response.isSuccessful) {
                                        statusText = "Чат $id удален"
                                        lastChatId = null
                                    } else {
                                        Log.e("TestAPI", "Error deleting chat: ${response.code()} ${response.errorBody()?.string()}")
                                    }
                                } ?: run { Log.w("TestAPI", "Attempted delete without chatId") }
                            }
                        }
                    }
                    1 -> {
                        ProfileScreen(username = "АНАТОЛИЙ")
                    }
                    else -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("В разработке", color = AuthTextColor)
                        }
                    }
                }
            }
            CreateBottomPanel(selectedTab)
        }
    }
}

@Composable
fun TestButton(text: String, onClick: suspend () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    Button(
        onClick = {
            scope.launch {
                try {
                    onClick()
                } catch (e: Exception) {
                    Log.e("TestAPI", "Network/Unexpected error", e)
                }
            }
        },
        modifier = Modifier
            .padding(horizontal = 40.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(25.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AuthFieldBackground,
            contentColor = AuthTextColor
        )
    ) {
        Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

fun getToken(context: android.content.Context): String {
    val prefs = context.getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
    return prefs.getString("access_token", "") ?: ""
}

@Composable
fun ProfileScreen(username: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(AuthFieldBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = username.take(1).uppercase(),
                color = AuthTextColor,
                fontWeight = FontWeight.Black,
                fontSize = 64.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = username,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = AuthTextColor
        )

        Text(
            text = "@${username.lowercase()}",
            fontSize = 16.sp,
            color = AuthTextColor.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(AuthFieldBackground.copy(alpha = 0.8f))
        ) {
            ProfileItem(label = "Группа", value = "БПИ2502")
            DashedDivider(color = AuthTextColor.copy(alpha = 0.2f))
            ProfileItem(label = "Имя пользователя", value = "@${username.lowercase()}")
            DashedDivider(color = AuthTextColor.copy(alpha = 0.2f))
            ProfileItem(label = "О себе", value = "Занят")
        }
    }
}

@Composable
fun ProfileItem(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Action */ }
            .padding(16.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = AuthTextColor.copy(alpha = 0.6f)
        )
        if (value.isNotEmpty()) {
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = AuthTextColor
            )
        }
    }
}

@Composable
fun ChatListItem(chat: ChatItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Open Chat */ }
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Placeholder
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AuthFieldBackground),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chat.name.take(1),
                    color = AuthTextColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuthTextColor
                    )
                    Text(
                        text = chat.time,
                        fontSize = 12.sp,
                        color = AuthTextColor.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = chat.lastMessage,
                    fontSize = 14.sp,
                    color = AuthTextColor.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        DashedDivider(color = AuthTextColor.copy(alpha = 0.2f), thickness = 1.dp)
    }
}

@Composable
fun CreateBottomPanel(selectedTab: MutableState<Int>) {
    Box(
        modifier = Modifier
            .padding(bottom = 24.dp, start = 24.dp, end = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.9f),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    icon = Icons.AutoMirrored.Default.Send,
                    isSelected = selectedTab.value == 0,
                    onClick = { selectedTab.value = 0 }
                )
                BottomNavItem(
                    icon = Icons.Default.CalendarToday,
                    isSelected = selectedTab.value == 2,
                    onClick = { selectedTab.value = 2 }
                )
                BottomNavItem(
                    icon = Icons.Default.Person,
                    isSelected = selectedTab.value == 1,
                    onClick = { selectedTab.value = 1 }
                )
                BottomNavItem(
                    icon = Icons.Default.Settings,
                    isSelected = selectedTab.value == 3,
                    onClick = { selectedTab.value = 3 }
                )
            }
        }
    }
}

@Composable
fun BottomNavItem(icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val containerColor = if (isSelected) AuthFieldBackground else Color.Transparent
    val contentColor = if (isSelected) AuthTextColor else AuthTextColor.copy(alpha = 0.5f)

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Preview
@Composable
fun ProfilePreview() {
    Box(modifier = Modifier.paint(painterResource(id = R.drawable.background), contentScale = ContentScale.Crop)) {
        ProfileScreen(username = "Константин")
    }
}

@Preview
@Composable
fun MainPreview() {
    MainScreen()
}
