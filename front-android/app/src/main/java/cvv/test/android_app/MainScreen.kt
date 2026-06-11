package cvv.test.android_app

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cvv.test.android_app.api.ChatManager
import cvv.test.android_app.api.IncomingMessage
import cvv.test.android_app.api.RetrofitClient
import cvv.test.android_app.api.TimetableRequest
import cvv.test.android_app.api.UserSearchResult
import cvv.test.android_app.ui.theme.AuthFieldBackground
import cvv.test.android_app.ui.theme.AuthTextColor
import kotlinx.coroutines.launch

const val GROUP_ID = 323239634983194624L
//Добавить поиск по юзерам,
//При нахождении юзера вывести его профиль/информацию -> Добавить кнопку перейти в чат
//-> при отправке нового соо

@Composable
fun MainScreen() {
    //Принцип работы как в react, приложение запоминает состояние каких то объектов, и при их изменении меняет отрисовку экрана и тп.
    val selectedTab = remember { mutableStateOf(0) } //state(состояние) для выбранной вкладки
    val context = LocalContext.current //текущее состояние(контекст) приложения для запуска корутин
    var messageText by remember { mutableStateOf("") } //state для кнопок

    // Глобальное хранилище сообщений по чатам и список активных диалогов
    val allMessagesByChat =
        remember { mutableStateMapOf<Long, SnapshotStateList<IncomingMessage>>() }
    val activeChats = remember { mutableStateMapOf<Long, UserSearchResult>() }

    val searchResults = remember { mutableStateListOf<UserSearchResult>() }
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState() //state для отрисовки сообщений
    val scope = rememberCoroutineScope()
    var myUserId by remember { mutableStateOf<String?>(null) }
    var viewingUser by remember { mutableStateOf<UserSearchResult?>(null) }
    var currentChatId by remember { mutableStateOf(GROUP_ID) }
    var myUsername by remember { mutableStateOf("АНАТОЛИЙ") }
    var pendingChatUserId by remember { mutableStateOf<String?>(null) }
    var activeChatPartner by remember { mutableStateOf<UserSearchResult?>(null) }

    // Сообщения текущего выбранного чата
    val currentMessages =
        allMessagesByChat[currentChatId] ?: remember(currentChatId) { mutableStateListOf() }

    //Compose функция для отрисовки сообщений и получения их с бэкенда
    LaunchedEffect(Unit) {
        ChatManager.messages.collect { msg ->
            val chatId = msg.chatId?.toLongOrNull() ?: GROUP_ID

            // Добавляем сообщение в соответствующий список чата
            val list = allMessagesByChat.getOrPut(chatId) { mutableStateListOf() }
            list.add(msg)

            //запуск корутины которая проверяет, приходит ли что-то с бэка, события описаны в ChatManager
            scope.launch {
                if (chatId == currentChatId && list.isNotEmpty()) {
                    listState.animateScrollToItem(list.size - 1)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        ChatManager.searchResults.collect { results ->
            searchResults.clear()
            searchResults.addAll(results)
        }
    }

    // Очистка поиска при переходе на вкладку чатов
    LaunchedEffect(selectedTab.value) {
        if (selectedTab.value == 0) {
            searchQuery = ""
            searchResults.clear()
        }
    }

    //Получения токена, валидация и подключение к группе
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("auth_prefs",
            android.content.Context.MODE_PRIVATE)
        val token = prefs.getString("access_token", "") ?: ""
        if (token.isNotEmpty()) {
            val bearerToken = "Bearer $token"
            ChatManager.connect(token)
            ChatManager.joinChat(GROUP_ID)

            // Получаем свой профиль, чтобы знать свой ID
            try {
                val response = RetrofitClient.authApi.getMe(bearerToken)
                if (response.isSuccessful) {
                    val profile = response.body()
                    myUserId = profile?.userId
                    myUsername = profile?.username ?: "АНАТОЛИЙ"
                }
            } catch (e: Exception) {
                Log.e("MainScreen", "Failed to fetch profile: ${e.message}")
            }
        }
    }

    //Compose функция, в которой будут располагаться элементы, в этом случае растянута на весь экран из-за .fillMaxSize()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .paint(
                painter = painterResource(id = R.drawable.background),
                contentScale = ContentScale.Crop
            )
    ) {
        //Compose функция, которая располагает элементы в колонну, в нашем случае это блок сообщений + поле ввода и кнопка
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                //Проверяем какая вкладка выбрана, и в зависимости от этого отрисовываем экран
                when (selectedTab.value) {
                    0 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            // Плашка собеседника сверху (если выбран активный чат)
                            if (activeChatPartner != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(0.8f))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = {
                                        activeChatPartner = null
                                        currentChatId = GROUP_ID
                                        pendingChatUserId = null
                                    }) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = null
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                activeChatPartner?.let {
                                                    viewingUser = it
                                                    selectedTab.value = 4
                                                }
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(AuthFieldBackground),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text =
                                                    (activeChatPartner?.username ?: "Ч").take(1)
                                                    .uppercase(),
                                                color = AuthTextColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = activeChatPartner?.username ?: "Чат",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }

                            // Поле поиска (показываем только если нет активного чата)
                            if (activeChatPartner == null) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = {
                                        searchQuery = it
                                        if (it.isNotEmpty()) {
                                            ChatManager.searchUser(it)
                                        } else {
                                            searchResults.clear()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    placeholder = { Text("Поиск пользователей...") },
                                    shape = RoundedCornerShape(25.dp),
                                    trailingIcon = {
                                        Icon(Icons.Default.Search, contentDescription = null)
                                    },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.White.copy(0.9f),
                                        unfocusedContainerColor = Color.White.copy(0.8f)
                                    )
                                )

                                // Результаты поиска
                                if (searchResults.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                Color.White.copy(0.4f),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .padding(8.dp)
                                    ) {
                                        searchResults.forEach { user ->
                                            ChatItem(
                                                name = user.username ?: "Unknown",
                                                lastMsg = user.tag ?: "",
                                                time = "",
                                                onClick = {
                                                    viewingUser = user
                                                    selectedTab.value = 4
                                                }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            // Список активных диалогов (если не в чате)
                            if (activeChatPartner == null) {
                                if (activeChats.isNotEmpty()) {
                                    Text(
                                        "Ваши чаты",
                                        fontWeight = FontWeight.Bold,
                                        color = AuthTextColor,
                                        modifier = Modifier.padding(
                                            start = 8.dp,
                                            bottom = 8.dp,
                                            top = 8.dp
                                        )
                                    )
                                    LazyColumn(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(activeChats.values.toList()) { partner ->
                                            val chatId =
                                                activeChats.entries.find { it.value.userId == partner.userId }?.key
                                            val lastMsgObj = allMessagesByChat[chatId]?.lastOrNull()

                                            ChatItem(
                                                name = partner.username ?: "Unknown",
                                                lastMsg = lastMsgObj?.content ?: "Нет сообщений",
                                                time = formatTime(lastMsgObj?.timestamp),
                                                onClick = {
                                                    activeChatPartner = partner
                                                    if (chatId != null) {
                                                        currentChatId = chatId
                                                        ChatManager.joinChat(chatId)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            if (activeChatPartner != null) {
                                //Compose функция отличается от Column только тем, что элементы отрисовываются только когда они на экране
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.Bottom,
                                    contentPadding = PaddingValues(bottom = 16.dp)
                                ) {
                                    //Итерируемся по сообщениям
                                    items(currentMessages) { msg ->
                                        //Проверяем, кто отправитель сообщения, это влияет на дизайн и отображение сообщения
                                        val isMe = msg.senderId == myUserId
                                        //Собственная Compose функция, которая задает шаблон для дизайна сообщений
                                        MessageBubble(
                                            text = msg.content,
                                            isMe = isMe,
                                            senderId = msg.senderId
                                        )
                                    }
                                }

                                //Как Column, только элементы располагаются горизонтально
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    //Поле ввода
                                    OutlinedTextField(
                                        value = messageText,
                                        onValueChange = { messageText = it },
                                        modifier = Modifier.weight(1f),
                                        placeholder = { Text("Сообщение...") },
                                        shape = RoundedCornerShape(25.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.White.copy(0.9f),
                                            unfocusedContainerColor = Color.White.copy(0.8f)
                                        )
                                    )

                                    //Отступ между полем ввода и кнопкой отправки сообщения
                                    Spacer(modifier = Modifier.width(8.dp))

                                    //Иконка-Кнопка
                                    IconButton(
                                        //При нажатии отправляем сообщение (создаем чат, если его еще нет)
                                        onClick = {
                                            if (messageText.isNotBlank()) {
                                                scope.launch {
                                                    // Если чат еще не создан (первое сообщение)
                                                    if (pendingChatUserId != null && currentChatId == GROUP_ID) {
                                                        try {
                                                            val prefs =
                                                                context.getSharedPreferences(
                                                                    "auth_prefs",
                                                                    android.content.Context.MODE_PRIVATE
                                                                )
                                                            val token =
                                                                prefs.getString("access_token", "")
                                                                    ?: ""
                                                            val userIdLong =
                                                                pendingChatUserId?.toLongOrNull()

                                                            if (token.isNotEmpty() && userIdLong != null) {
                                                                val response =
                                                                    RetrofitClient.chatsApi.createDirectChat(
                                                                        token = "Bearer $token",
                                                                        memberId = userIdLong
                                                                    )
                                                                if (response.isSuccessful) {
                                                                    val chat = response.body()
                                                                    chat?.id?.let { newChatId ->
                                                                        currentChatId = newChatId
                                                                        activeChats[newChatId] =
                                                                            activeChatPartner!!
                                                                        ChatManager.joinChat(
                                                                            newChatId
                                                                        )
                                                                        pendingChatUserId = null
                                                                        ChatManager.sendMessage(
                                                                            newChatId,
                                                                            messageText
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        } catch (e: Exception) {
                                                            Log.e(
                                                                "MainScreen",
                                                                "Error auto-creating chat: ${e.message}"
                                                            )
                                                        }
                                                    } else {
                                                        ChatManager.sendMessage(
                                                            currentChatId,
                                                            messageText
                                                        )
                                                    }
                                                    messageText = ""
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(AuthFieldBackground)
                                    ) {
                                        //Сама иконка
                                        Icon(
                                            Icons.AutoMirrored.Filled.Send,
                                            contentDescription = null,
                                            tint = AuthTextColor
                                        )
                                    }
                                }
                            // Если чатов еще нет, то показываем заглушку
                            } else if (activeChats.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Выберите пользователя для начала общения",
                                        color = AuthTextColor.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }

                    //Отрисовка профиля пользователя, собственная Compose функция, задающая шаблон профиля
                    1 -> ProfileScreen(username = myUsername)

                    //Аналогично тому, что выше
                    2 -> TimetableScreen()

                    4 -> {
                        viewingUser?.let { user ->
                            ProfileScreen(
                                username = user.username ?: "Unknown",
                                tag = user.tag,
                                bio = user.bio,
                                isMyProfile = false,
                                onSendMessage = {
                                    // Если мы переходим к тому же пользователю, с которым уже общаемся - не сбрасываем историю
                                    if (activeChatPartner?.userId != user.userId) {
                                        pendingChatUserId = user.userId
                                        activeChatPartner = user

                                        // Если чат с ним уже есть в активных, переключаемся на него
                                        val existingChatId =
                                            activeChats.entries.find { it.value.userId == user.userId }?.key
                                        if (existingChatId != null) {
                                            currentChatId = existingChatId
                                            pendingChatUserId = null
                                        } else {
                                            currentChatId = GROUP_ID
                                        }
                                    }
                                    selectedTab.value = 0
                                }
                            )
                        }
                    }

                    else -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("В разработке", color = AuthTextColor)
                    }
                }
            }

            //Создание нижней панели, в зависимости от текущей вкладки
            //Выбранная кнопка выделяется другим цветом
            CreateBottomPanel(selectedTab)
        }
    }
}


@Composable
fun ChatItem(
    name: String,
    lastMsg: String,
    time: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(0.7f))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Аватарка
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(AuthFieldBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(1).uppercase(),
                color = AuthTextColor,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Имя и последнее сообщение
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black,
                    maxLines = 1
                )
                Text(
                    text = time,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = lastMsg,
                fontSize = 14.sp,
                color = Color.Gray,
                maxLines = 2,
                lineHeight = 18.sp
            )
        }
    }
}

fun formatTime(isoString: String?): String {
    if (isoString == null) return ""
    return try {
        // Упрощенный парсинг для примера (2024-03-20T12:34:56.789)
        val timePart = isoString.substringAfter('T').take(5)
        timePart
    } catch (e: Exception) {
        ""
    }
}

//Функция отрисовки экрана расписания
@Composable
fun TimetableScreen() {
    //states для полей ввода
    var group by remember { mutableStateOf("") }
    var month by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Расписание",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = AuthTextColor
        )
        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(AuthFieldBackground.copy(alpha = 0.8f))
        ) {
            OutlinedTextField(
                value = group,
                onValueChange = { group = it },
                placeholder = { Text("Группа (БПИ2502)", color = AuthTextColor.copy(0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            //Кастомный пунктирный разделитель
            DashedDivider(color = AuthTextColor.copy(alpha = 0.2f))

            OutlinedTextField(
                value = month,
                onValueChange = { month = it },
                placeholder = { Text("Месяц (1-12)", color = AuthTextColor.copy(0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        //Кнопка запроса расписания у бэкенда
        Button(
            //При нажатии отправляем запрос и (пока что) логируем полученные данные
            onClick = {
                val m = month.toIntOrNull() ?: 9
                scope.launch {
                    try {
                        val request = TimetableRequest(group = group, month = m)
                        val response = RetrofitClient.timetableApi.getTimetable(request)
                        if (response.isSuccessful) {
                            Log.d("Timetable", "SUCCESS! Body: ${response.body()}")
                        }
                    } catch (e: Exception) {
                        Log.e("Timetable", "Error: ${e.message}")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AuthFieldBackground),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text("Запросить JSON", color = AuthTextColor, fontWeight = FontWeight.Bold)
        }
    }
}

//Шаблон отрисовки сообщения
@Composable
fun MessageBubble(text: String, isMe: Boolean, senderId: String?) {
    //Параметры отображения сообщения в зависимости от того, кто написал
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val bubbleColor =
        if (isMe) AuthFieldBackground.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.8f)
    val textColor = if (isMe) AuthTextColor else Color.Black
    val label = if (isMe) "me" else "user${senderId?.takeLast(4) ?: "???"}"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        //username
        Text(
            text = label,
            fontSize = 11.sp,
            color = AuthTextColor.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
        //Сам блок сообщения
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp
                    )
                )
                .background(bubbleColor)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(text = text, color = textColor, fontSize = 16.sp)
        }
    }
}

//Экран профиля
@Composable
fun ProfileScreen(
    username: String,
    tag: String? = null,
    bio: String? = null,
    isMyProfile: Boolean = true,
    onSendMessage: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        //Аватарка юзера, пока что первая буква username
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
        //Отступ между аватаркой и ником
        Spacer(modifier = Modifier.height(16.dp))
        //Ник
        Text(text = username, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AuthTextColor)

        if (!isMyProfile) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSendMessage,
                colors = ButtonDefaults.buttonColors(containerColor = AuthFieldBackground),
                shape = RoundedCornerShape(25.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp)
                    .height(48.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = AuthTextColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Отправить сообщение", color = AuthTextColor, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        //Сами данные профиля
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(AuthFieldBackground.copy(alpha = 0.8f))
        ) {
            ProfileItem(label = "Группа", value = "БПИ2502")
            DashedDivider(color = AuthTextColor.copy(alpha = 0.2f))
            ProfileItem(label = "Имя пользователя", value = "@${tag ?: username.lowercase()}")
            DashedDivider(color = AuthTextColor.copy(alpha = 0.2f))
            ProfileItem(label = "О себе", value = bio ?: "Занят")
        }
    }
}

//Шаблон для отображения данных профиля
@Composable
fun ProfileItem(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(text = label, fontSize = 14.sp, color = AuthTextColor.copy(alpha = 0.6f))
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = AuthTextColor)
    }
}

//Функция для создания панели переключения между экранами
@Composable
fun CreateBottomPanel(selectedTab: MutableState<Int>) {
    Box(
        modifier = Modifier.padding(bottom = 24.dp, start = 24.dp, end = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        //Тот же Box, только с доп функциями дизайна
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.9f),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                //Кнопки
                BottomNavItem(
                    icon = Icons.AutoMirrored.Default.Send,
                    //При нажатии кнопки даем понять фреймворку, какая страница выбрана
                    //Чтобы он отрисовал нужный экран
                    isSelected = selectedTab.value == 0,
                    onClick = { selectedTab.value = 0 })
                BottomNavItem(
                    icon = Icons.Default.CalendarToday,
                    isSelected = selectedTab.value == 2,
                    onClick = { selectedTab.value = 2 })
                BottomNavItem(
                    icon = Icons.Default.Person,
                    isSelected = selectedTab.value == 1,
                    onClick = { selectedTab.value = 1 })
                BottomNavItem(
                    icon = Icons.Default.Settings,
                    isSelected = selectedTab.value == 3,
                    onClick = { selectedTab.value = 3 })
            }
        }
    }
}

//Шаблон для нижней кнопки
@Composable
fun BottomNavItem(icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    //При выборе кнопки меняется цвет
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

//Функции для отладки приложения, по факту нигде не вызываются и ни на что не влияют
@Preview
@Composable
fun MainPreview() {
    MainScreen()
}
