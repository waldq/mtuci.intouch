package cvv.test.android_app.ui.auth

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cvv.test.android_app.R
import cvv.test.android_app.api.RegisterRequest
import cvv.test.android_app.api.RetrofitClient
import cvv.test.android_app.ui.components.DashedDivider
import cvv.test.android_app.ui.theme.AuthFieldBackground
import cvv.test.android_app.ui.theme.AuthTextColor
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(onSwitchToLogin: () -> Unit, onNavigateToChats: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(AuthFieldBackground)
        ) {
            AuthTextField(
                value = username,
                onValueChange = { username = it },
                placeholder = "Имя пользователя"
            )
            DashedDivider(color = AuthTextColor.copy(alpha = 0.2f))
            AuthTextField(
                value = login,
                onValueChange = { login = it },
                placeholder = "Логин"
            )
            DashedDivider(color = AuthTextColor.copy(alpha = 0.2f))
            AuthTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Пароль",
                isPassword = true
            )
            DashedDivider(color = AuthTextColor.copy(alpha = 0.2f))
            AuthTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = "Подтвердите пароль",
                isPassword = true
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        AuthButton(
            text = if (isLoading) "Регистрация..." else "Отправить",
            enabled = !isLoading,
            onClick = {
                if (username.isBlank() || login.isBlank()) {
                    Toast.makeText(context, "Заполните все поля!", Toast.LENGTH_SHORT).show()
                    return@AuthButton
                }
                if (password.length < 8) {
                    Toast.makeText(
                        context,
                        "Пароль должен содержать минимум 8 символов!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@AuthButton
                }
                if (password != confirmPassword) {
                    Toast.makeText(context, "Пароли не совпадают!", Toast.LENGTH_SHORT).show()
                    return@AuthButton
                }

                scope.launch {
                    isLoading = true
                    try {
                        val response = RetrofitClient.authApi.register(
                            RegisterRequest(username, login.lowercase().trim(), password)
                        )
                        if (response.code() == 201) {
                            Toast.makeText(
                                context,
                                "Регистрация прошла успешно!",
                                Toast.LENGTH_SHORT
                            ).show()
                            onSwitchToLogin()
                        } else if (response.code() == 409) {
                            Toast.makeText(
                                context,
                                "Пользователь с таким логином уже существует",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(context, "Ошибка регистрации", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.d("Exc", e.message.toString())
                        Toast.makeText(context, "Ошибка соединения", Toast.LENGTH_SHORT).show()
                    } finally {
                        isLoading = false
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Есть аккаунт?",
            color = AuthTextColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            fontFamily = NunitoSansFamily,
            modifier = Modifier.clickable { onSwitchToLogin() }
        )
    }
}
