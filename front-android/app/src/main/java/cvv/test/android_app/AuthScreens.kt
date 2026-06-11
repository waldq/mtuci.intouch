package cvv.test.android_app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cvv.test.android_app.ui.auth.LoginScreen
import cvv.test.android_app.ui.auth.RegisterScreen
import cvv.test.android_app.ui.auth.StartScreen

@Composable
fun AuthScreen() {
    var currentScreen by remember { mutableStateOf("start") }

    when (currentScreen) {
        "start" -> StartScreen(
            onNavigateToLogin = { currentScreen = "login" },
            onNavigateToRegister = { currentScreen = "register" }
        )

        "login" -> LoginScreen(
            onSwitchToRegister = { currentScreen = "register" },
            onNavigateToChats = { currentScreen = "chats" }
        )

        "register" -> RegisterScreen(
            onSwitchToLogin = { currentScreen = "login" },
            onNavigateToChats = { currentScreen = "chats" }
        )

        "chats" -> MainScreen()
    }
}
