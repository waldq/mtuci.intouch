package cvv.test.android_app

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation.Companion.None
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import cvv.test.android_app.ui.theme.AuthFieldBackground
import cvv.test.android_app.ui.theme.AuthTextColor
import kotlinx.coroutines.launch
import retrofit2.Response


private val NunitoSansFamily = FontFamily(
    Font(R.font.nunito_sans, FontWeight.Normal)
)
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
            DashedDivider(color = AuthTextColor.copy(alpha = 0.2f), )
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
                        val response: Response<AuthResponse> = RetrofitClient.authApi.login(login.lowercase().trim(), password)
                        if (response.isSuccessful) {
                            val data = response.body()
                            data?.accessToken?.let {
                                saveToken(context, it)
                                Toast.makeText(context, "Вход выполнен успешно!", Toast.LENGTH_SHORT).show()
                                onNavigateToChats()
                            }
                        } else if (response.code() == 401) {
                            Toast.makeText(context, "Неверный логин или пароль", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Ошибка входа: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Ошибка соединения: ${e.message}", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(context, "Пароль должен содержать минимум 8 символов!", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(context, "Регистрация прошла успешно!", Toast.LENGTH_SHORT).show()
                            onSwitchToLogin()
                        } else if (response.code() == 409) {
                            Toast.makeText(context, "Пользователь с таким логином уже существует", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Ошибка регистрации", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
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

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = placeholder,
                    textAlign = TextAlign.Center,
                    color = AuthTextColor.copy(alpha = 0.7f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = NunitoSansFamily,
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = AuthTextColor,
            focusedTextColor = AuthTextColor,
            unfocusedTextColor = AuthTextColor
        ),
        textStyle = LocalTextStyle.current.copy(
            textAlign = TextAlign.Center,
            color = AuthTextColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            fontFamily = NunitoSansFamily
        ),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else None,
        keyboardOptions = if (isPassword) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default,
        singleLine = true
    )
}

@Composable
fun AuthButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = RoundedCornerShape(30.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AuthFieldBackground,
            contentColor = AuthTextColor,
            disabledContainerColor = AuthFieldBackground.copy(alpha = 0.5f),
            disabledContentColor = AuthTextColor.copy(alpha = 0.5f)
        )
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            fontFamily = NunitoSansFamily
        )
    }
}

private fun saveToken(context: Context, token: String) {
    val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    prefs.edit { putString("access_token", token) }
}

@Preview(showBackground = true)
@Composable
fun StartPreview() {
    StartScreen({}, {})
}

@Preview(showBackground = true)
@Composable
fun LoginPreview() {
    LoginScreen({}, {})
}

@Preview(showBackground = true)
@Composable
fun RegisterPreview() {
    RegisterScreen({}, {})
}

@Composable
fun DashedDivider(
    color: Color = AuthTextColor.copy(alpha = 0.2f),
    thickness: Dp = 1.dp,
    dashWidth: Dp = 4.dp,
    gapWidth: Dp = 4.dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { thickness.toPx() }
    val dashWidthPx = with(density) { dashWidth.toPx() }
    val gapWidthPx = with(density) { gapWidth.toPx() }

    Canvas(modifier.fillMaxWidth().height(thickness)) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = strokeWidthPx,
            pathEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(dashWidthPx, gapWidthPx),
                phase = 0f
            )
        )
    }
}
