package cvv.test.android_app

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cvv.test.android_app.api.ChatManager
import cvv.test.android_app.ui.theme.AuthFieldBackground
import cvv.test.android_app.ui.theme.AuthTextColor

const val GROUP_ID = 315830186291499008L

@Composable
fun MainScreen() {
    val selectedTab = remember { mutableStateOf(0) }
    val context = LocalContext.current
    var messageText by remember { mutableStateOf("") }
    
    // collectAsState сам запускает корутину и слушает поток данных вечно
    val lastMessage by ChatManager.messages.collectAsState(initial = null)
    val statusText by ChatManager.connectionStatus.collectAsState(initial = "Disconnected")

    // Логируем каждое изменение lastMessage
    LaunchedEffect(lastMessage) {
        lastMessage?.let { msg ->
            Log.d("MainScreen", "--- NEW MESSAGE RECEIVED ---")
            Log.d("MainScreen", "Content: ${msg.content}")
            Log.d("MainScreen", "From: ${msg.senderId}")
            Log.d("MainScreen", "----------------------------")
        }
    }

    // Инициализация подключения (только один раз)
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
        val token = prefs.getString("access_token", "") ?: ""
        if (token.isNotEmpty()) {
            ChatManager.connect(token)
            ChatManager.joinChat(GROUP_ID)
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
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Режим отладки", color = AuthTextColor, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("Статус: $statusText", color = AuthTextColor.copy(alpha = 0.7f))
                            
                            // Вывод последнего сообщения прямо на экран для теста
                            lastMessage?.let {
                                Text(
                                    text = "Последнее: ${it.content}",
                                    color = Color.White,
                                    modifier = Modifier.padding(16.dp).background(Color.Black.copy(0.5f), RoundedCornerShape(8.dp)).padding(8.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = messageText,
                                    onValueChange = { messageText = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Сообщение...") },
                                    shape = RoundedCornerShape(25.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.White.copy(0.9f),
                                        unfocusedContainerColor = Color.White.copy(0.8f)
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        if (messageText.isNotBlank()) {
                                            ChatManager.sendMessage(GROUP_ID, messageText)
                                            messageText = ""
                                        }
                                    },
                                    modifier = Modifier.size(48.dp).clip(CircleShape).background(AuthFieldBackground)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = AuthTextColor)
                                }
                            }
                        }
                    }
                    1 -> ProfileScreen(username = "АНАТОЛИЙ")
                    else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("В разработке", color = AuthTextColor)
                    }
                }
            }
            CreateBottomPanel(selectedTab)
        }
    }
}

@Composable
fun ProfileScreen(username: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(120.dp).clip(CircleShape).background(AuthFieldBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(text = username.take(1).uppercase(), color = AuthTextColor, fontWeight = FontWeight.Black, fontSize = 64.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = username, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AuthTextColor)
        Spacer(modifier = Modifier.height(32.dp))
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).clip(RoundedCornerShape(24.dp)).background(AuthFieldBackground.copy(alpha = 0.8f))
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
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(text = label, fontSize = 14.sp, color = AuthTextColor.copy(alpha = 0.6f))
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = AuthTextColor)
    }
}

@Composable
fun CreateBottomPanel(selectedTab: MutableState<Int>) {
    Box(modifier = Modifier.padding(bottom = 24.dp, start = 24.dp, end = 24.dp), contentAlignment = Alignment.BottomCenter) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(72.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.9f),
            shadowElevation = 8.dp
        ) {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                BottomNavItem(icon = Icons.AutoMirrored.Default.Send, isSelected = selectedTab.value == 0, onClick = { selectedTab.value = 0 })
                BottomNavItem(icon = Icons.Default.CalendarToday, isSelected = selectedTab.value == 2, onClick = { selectedTab.value = 2 })
                BottomNavItem(icon = Icons.Default.Person, isSelected = selectedTab.value == 1, onClick = { selectedTab.value = 1 })
                BottomNavItem(icon = Icons.Default.Settings, isSelected = selectedTab.value == 3, onClick = { selectedTab.value = 3 })
            }
        }
    }
}

@Composable
fun BottomNavItem(icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val containerColor = if (isSelected) AuthFieldBackground else Color.Transparent
    val contentColor = if (isSelected) AuthTextColor else AuthTextColor.copy(alpha = 0.5f)
    Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(containerColor).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(28.dp))
    }
}

@Preview
@Composable
fun MainPreview() {
    MainScreen()
}
