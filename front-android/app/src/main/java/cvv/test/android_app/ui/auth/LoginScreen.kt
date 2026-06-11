package cvv.test.android_app.ui.auth

import android.content.Context
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
import androidx.core.content.edit
import cvv.test.android_app.R
import cvv.test.android_app.api.AuthResponse
import cvv.test.android_app.api.RetrofitClient
import cvv.test.android_app.core.data.ACCESS_TOKEN_KEY
import cvv.test.android_app.core.data.AUTH_PREFS
import cvv.test.android_app.ui.components.DashedDivider
import cvv.test.android_app.ui.theme.AuthFieldBackground
import cvv.test.android_app.ui.theme.AuthTextColor
import kotlinx.coroutines.launch
import retrofit2.Response

@Composable
fun LoginScreen(onSwitchToRegister: () -> Unit, onNavigateToChats: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
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
        }

        Spacer(modifier = Modifier.height(20.dp))

        AuthButton(
            text = if (isLoading) "Вход..." else "Войти",
            enabled = !isLoading,
            onClick = {
                if (login.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "Заполните все поля!", Toast.LENGTH_SHORT).show()
                    return@AuthButton
                }
                scope.launch {
                    isLoading = true
                    try {
                        val response: Response<AuthResponse> =
                            RetrofitClient.authApi.login(login.lowercase().trim(), password)
                        if (response.isSuccessful) {
                            val data = response.body()
                            data?.accessToken?.let {
                                saveToken(context, it)
                                Toast.makeText(
                                    context,
                                    "Вход выполнен успешно!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onNavigateToChats()
                            }
                        } else if (response.code() == 401) {
                            Toast.makeText(context, "Неверный логин или пароль", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            Toast.makeText(
                                context,
                                "Ошибка входа: ${response.code()}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Ошибка соединения: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } finally {
                        isLoading = false
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Нет аккаунта",
            color = AuthTextColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            fontFamily = NunitoSansFamily,
            modifier = Modifier.clickable { onSwitchToRegister() },
        )
    }
}

private fun saveToken(context: Context, token: String) {
    val prefs = context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
    prefs.edit { putString(ACCESS_TOKEN_KEY, token) }
}
