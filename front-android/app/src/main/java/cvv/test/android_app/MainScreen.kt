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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import cvv.test.android_app.ui.theme.AuthFieldBackground
import cvv.test.android_app.ui.theme.AuthTextColor
import kotlinx.coroutines.launch

const val GROUP_ID = 315830186291499008L

@Composable
fun MainScreen() {
    //Принцип работы как в react, приложение запоминает состояние каких то объектов, и при их изменении меняет отрисовку экрана и тп.
    val selectedTab = remember { mutableStateOf(0) } //state(состояние) для выбранной вкладки
    val context = LocalContext.current //текущее состояние(контекст) приложения для запуска корутин
    var messageText by remember { mutableStateOf("") } //state для кнопок
    val messages = remember { mutableStateListOf<IncomingMessage>() }
    val listState = rememberLazyListState() //state для отрисовки сообщений
    val scope = rememberCoroutineScope()
    var myUserId by remember { mutableStateOf<String?>(null) }


    //Compose функция для отрисовки сообщений и получения их с бэкенда
    LaunchedEffect(Unit) {
        ChatManager.messages.collect { msg ->
            messages.add(msg)
            //запуск корутины которая проверяет, приходит ли что-то с бэка, события описаны в ChatManager
            scope.launch {
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.size - 1)
                }
            }
        }
    }

    //Получения токена, валидация и подключение к группе
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
        val token = prefs.getString("access_token", "") ?: ""
        if (token.isNotEmpty()) {
            val bearerToken = "Bearer $token"
            ChatManager.connect(token)
            ChatManager.joinChat(GROUP_ID)

            // Получаем свой профиль, чтобы знать свой ID
            try {
                val response = RetrofitClient.authApi.getMe(bearerToken)
                if (response.isSuccessful) {
                    myUserId = response.body()?.userId
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
                            //Compose функция отличается от Column только тем, что элементы отрисовываются только когда они на экране
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.Bottom,
                                contentPadding = PaddingValues(
                                    bottom = 16.dp
                                )
                            ) {
                                //Итерируемся по сообщениям
                                items(messages) { msg ->
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
                                    //При нажатии вызываем метод у ChatManager и отправляем сообщение
                                    onClick = {
                                        if (messageText.isNotBlank()) {
                                            ChatManager.sendMessage(GROUP_ID, messageText)
                                            messageText = ""
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
                        }
                    }

                    //Отрисовка профиля пользователя, собственная Compose функция, задающая шаблон профиля
                    1 -> ProfileScreen(username = "АНАТОЛИЙ")

                    //Аналогично тому, что выше
                    2 -> TimetableScreen()
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
                        val request = TimetableRequest(
                            group = group,
                            month = m
                        )
                        Log.d("TestAPI", request.toString())
                        val response =
                            RetrofitClient.timetableApi.getTimetable(request)
                        if (response.isSuccessful) {
                            Log.d("Timetable", "SUCCESS! Body: ${response.body()}")
                        } else {
                            //функция/переменная + ? дает понять котлину, что может прийти null
                            val errorMsg = response.errorBody()?.string()
                            Log.e("Timetable", "SERVER ERROR: ${response.code()} - $errorMsg")
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
fun ProfileScreen(username: String) {
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
            ProfileItem(label = "Имя пользователя", value = "@${username.lowercase()}")
            DashedDivider(color = AuthTextColor.copy(alpha = 0.2f))
            ProfileItem(label = "О себе", value = "Занят")
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
            .clickable { onClick() }, contentAlignment = Alignment.Center
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

@Preview(showBackground = true)
@Composable
fun TimetablePreview() {
    TimetableScreen()
}

@Preview(showBackground = true)
@Composable
fun ProfilePreview() {
    ProfileScreen("random Username")
}