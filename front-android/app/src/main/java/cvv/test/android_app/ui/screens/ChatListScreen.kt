package cvv.test.android_app.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cvv.test.android_app.api.ChatManager
import cvv.test.android_app.core.data.GROUP_ID
import cvv.test.android_app.core.state.MainState
import cvv.test.android_app.ui.components.ChatItem
import cvv.test.android_app.ui.components.MessageBubble
import cvv.test.android_app.ui.navigation.Screen
import cvv.test.android_app.ui.theme.AuthFieldBackground
import cvv.test.android_app.ui.theme.AuthTextColor
import cvv.test.android_app.utils.formatTime

@Composable
fun ChatListScreen(state: MainState) {
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val currentMessages = state.allMessagesByChat[state.currentChatId]
        ?: remember(state.currentChatId) { mutableStateListOf() }

    // Скролл к последнему сообщению
    LaunchedEffect(currentMessages.size) {
        if (currentMessages.isNotEmpty()) listState.animateScrollToItem(currentMessages.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Шапка чата
        if (state.activeChatPartner != null) {
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
                    state.activeChatPartner = null; state.currentChatId = GROUP_ID
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            state.viewingUser = state.activeChatPartner; state.selectedTab =
                            Screen.VIEW_USER_PROFILE
                        }, verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AuthFieldBackground), contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (state.activeChatPartner?.username ?: "Ч").take(1).uppercase(),
                            color = AuthTextColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = state.activeChatPartner?.username ?: "Чат",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }

        // Поиск и список (если не в чате)
        if (state.activeChatPartner == null) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { state.onSearchChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                placeholder = { Text("Поиск пользователей...") },
                shape = RoundedCornerShape(25.dp),
                trailingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(0.9f),
                    unfocusedContainerColor = Color.White.copy(0.8f)
                )
            )

            if (state.searchResults.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(0.4f), RoundedCornerShape(16.dp))
                        .padding(8.dp)
                ) {
                    state.searchResults.forEach { user ->
                        ChatItem(
                            name = user.username ?: "Unknown",
                            lastMsg = user.tag ?: "",
                            time = "",
                            onClick = {
                                state.viewingUser = user; state.selectedTab =
                                Screen.VIEW_USER_PROFILE
                            })
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (state.activeChats.isNotEmpty()) {
                Text(
                    "Ваши чаты",
                    fontWeight = FontWeight.Bold,
                    color = AuthTextColor,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp, top = 8.dp)
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(state.activeChats.values.toList()) { partner ->
                        val chatId =
                            state.activeChats.entries.find { it.value.userId == partner.userId }?.key
                        ChatItem(
                            name = partner.username ?: "Unknown",
                            lastMsg = state.allMessagesByChat[chatId]?.lastOrNull()?.content
                                ?: "Нет сообщений",
                            time = formatTime(state.allMessagesByChat[chatId]?.lastOrNull()?.timestamp),
                            onClick = {
                                state.activeChatPartner = partner; chatId?.let {
                                state.currentChatId = it; ChatManager.joinChat(it)
                            }
                            })
                    }
                }
            }
        }

        // Окно сообщений
        if (state.activeChatPartner != null) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Bottom,
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(currentMessages) { msg ->
                    MessageBubble(
                        text = msg.content,
                        isMe = msg.senderId == state.myUserId,
                        senderId = msg.senderId,
                        time = formatTime(msg.timestamp)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.messageText,
                    onValueChange = { state.messageText = it },
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
                    onClick = { state.sendMessage(context) },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AuthFieldBackground)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = AuthTextColor
                    )
                }
            }
        } else if (state.activeChats.isEmpty()) {
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
