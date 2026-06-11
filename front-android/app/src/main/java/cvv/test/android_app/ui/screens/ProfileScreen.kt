package cvv.test.android_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cvv.test.android_app.ui.components.DashedDivider
import cvv.test.android_app.ui.theme.AuthFieldBackground
import cvv.test.android_app.ui.theme.AuthTextColor

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
