package cvv.test.android_app.api

import android.util.Log
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okio.Buffer
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

@Serializable
data class RegisterRequest(
    val username: String,
    val login: String,
    val password: String
)

@Serializable
data class AuthResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("token_type") val tokenType: String? = null,
    val detail: String? = null,
    val username: String? = null,
    val login: String? = null
)

@Serializable
data class UserProfile(
    val user_id: String? = null,
    val username: String? = null,
    val login: String? = null
)

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @FormUrlEncoded
    @POST("auth/login")
    suspend fun login(
        @Field("username") login: String,
        @Field("password") password: String
    ): Response<AuthResponse>

    @GET("users/me")
    suspend fun getMe(@Header("Authorization") token: String): Response<UserProfile>
}

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8000/"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request()
            Log.d("TestAPI", "--> SENDING ${request.method}: ${request.url}")
            
            request.body?.let { body ->
                val buffer = Buffer()
                body.writeTo(buffer)
                Log.d("TestAPI", "--> BODY: ${buffer.readUtf8()}")
            }
            
            chain.proceed(request)
        }
        .build()

    val authApi: AuthApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuthApi::class.java)
    }

    val chatsApi: ChatsApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ChatsApi::class.java)
    }
}
