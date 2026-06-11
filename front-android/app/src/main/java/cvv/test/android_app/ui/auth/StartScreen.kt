package cvv.test.android_app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cvv.test.android_app.R
import cvv.test.android_app.ui.theme.AuthTextColor

@Composable
fun StartScreen(onNavigateToLogin: () -> Unit, onNavigateToRegister: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .paint(
                painter = painterResource(id = R.drawable.background),
                contentScale = ContentScale.Crop
            )
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "InTouch",
            fontSize = 54.sp,
            fontWeight = FontWeight.Black,
            fontFamily = NunitoSansFamily,
            color = AuthTextColor,
            modifier = Modifier.padding(bottom = 80.dp)
        )

        AuthButton(
            text = "Войти",
            onClick = onNavigateToLogin
        )

        Spacer(modifier = Modifier.height(16.dp))

        AuthButton(
            text = "Создать аккаунт",
            onClick = onNavigateToRegister
        )
    }
}
