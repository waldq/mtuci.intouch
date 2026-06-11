package cvv.test.android_app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(val icon: ImageVector? = null) {
    CHATS(Icons.AutoMirrored.Filled.Send),
    TIMETABLE(Icons.Default.CalendarToday),
    PROFILE(Icons.Default.Person),
    SETTINGS(Icons.Default.Settings),
    VIEW_USER_PROFILE // Экран просмотра чужого профиля (без иконки в меню)
}
