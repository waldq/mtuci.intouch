package cvv.test.android_app.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

//Класс для отправки на сервер, аннотация @Serializable говорит о том, что класс можно превращать в файлы разного типа
//В нашем случаем JSON
@Serializable
data class RegisterRequest(
    val username: String,
    val login: String,
    val password: String
)

//Класс для получения ответа от сервера
@Serializable
data class AuthResponse(
    //@SerialName говорит, как нам записать поле, при конвертации класса в файл
    //т.е. мы можем назвать моле value, но указать @SerialName("access_token"), тогда в JSON будет
    // { "access_token": value }
    @SerialName("access_token") val accessToken: String? = null, //Даем всем значениям поля null, на всякий случай
    @SerialName("token_type") val tokenType: String? = null, //Если сервер вернет пустой JSON
    val detail: String? = null,
    val username: String? = null,
    val login: String? = null
)

@Serializable
data class UserProfile(
    @SerialName("user_id") val userId: String? = null,
    val username: String? = null,
    val login: String? = null
)

/**
 * Контракт (интерфейс) сетевого API для аутентификации.
 * Используется Retrofit для генерации реальной реализации сетевых запросов.
 */
interface AuthApi {

    // Регистрация пользователя через JSON в теле запроса
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    // Авторизация через стандартную x-www-form-urlencoded форму
    @FormUrlEncoded
    @POST("auth/login")
    suspend fun login(
        @Field("username") login: String,
        @Field("password") password: String
    ): Response<AuthResponse>

    // Получение профиля текущего пользователя с передачей Токена в заголовке
    @GET("users/me")
    suspend fun getMe(@Header("Authorization") token: String): Response<UserProfile>
}
