package cvv.test.android_app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import cvv.test.android_app.core.data.GROUP_ID
import cvv.test.android_app.core.state.rememberMainState
import cvv.test.android_app.ui.components.CreateBottomPanel
import cvv.test.android_app.ui.navigation.Screen
import cvv.test.android_app.ui.screens.ChatListScreen
import cvv.test.android_app.ui.screens.ProfileScreen
import cvv.test.android_app.ui.screens.TimetableScreen
import cvv.test.android_app.ui.theme.AuthTextColor

@Composable
fun MainScreen() {
    val state = rememberMainState()
    val context = LocalContext.current

    // Инициализация
    LaunchedEffect(Unit) { state.init(context) }

    // Авто-очистка поиска
    LaunchedEffect(state.selectedTab) {
        if (state.selectedTab == Screen.CHATS) {
            state.searchQuery = ""
            state.searchResults.clear()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .paint(painterResource(R.drawable.background), contentScale = ContentScale.Crop)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                when (state.selectedTab) {
                    Screen.CHATS -> ChatListScreen(state)
                    Screen.PROFILE -> ProfileScreen(username = state.myUsername)
                    Screen.TIMETABLE -> TimetableScreen()
                    Screen.VIEW_USER_PROFILE -> {
                        state.viewingUser?.let { user ->
                            ProfileScreen(
                                username = user.username ?: "Unknown",
                                tag = user.tag, bio = user.bio, isMyProfile = false,
                                onSendMessage = {
                                    if (state.activeChatPartner?.userId != user.userId) {
                                        state.pendingChatUserId = user.userId
                                        state.activeChatPartner = user
                                        val existingId =
                                            state.activeChats.entries.find { it.value.userId == user.userId }?.key
                                        if (existingId != null) {
                                            state.currentChatId =
                                                existingId; state.pendingChatUserId = null
                                        } else {
                                            state.currentChatId = GROUP_ID
                                        }
                                    }
                                    state.selectedTab = Screen.CHATS
                                }
                            )
                        }
                    }

                    else -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { Text("В разработке", color = AuthTextColor) }
                }
            }
            CreateBottomPanel(
                selectedTab = state.selectedTab,
                onTabSelected = { state.selectedTab = it }
            )
        }
    }
}

@Preview
@Composable
fun MainPreview() {
    MainScreen()
}
