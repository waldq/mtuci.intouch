package cvv.test.android_app.api

import android.util.Log
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okio.Buffer
import retrofit2.Retrofit

object RetrofitClient {

    //Используйте 10.0.2.2 для эмулятора Android или локальный IP вашего ПК
    private const val BASE_URL = "http://192.168.240.1:8000/"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    //Создание объекта для работы с Http запросами
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request()
            //Логируем отправку любого запроса на сервер(для отладки)
            Log.d("TestAPI", "--> SENDING ${request.method}: ${request.url}")

            request.body?.let { body ->
                val buffer = Buffer()
                body.writeTo(buffer)
                //Логируем содержимое запроса
                Log.d("TestAPI", "--> BODY: ${buffer.readUtf8()}")
            }

            val response = chain.proceed(request)
            
            // Логируем сырой ответ сервера
            val responseBody = response.body
            val source = responseBody?.source()
            source?.request(Long.MAX_VALUE)
            val buffer = source?.buffer
            val responseText = buffer?.clone()?.readUtf8()
            Log.d("TestAPI", "<-- RESPONSE [${response.code}]: $responseText")
            
            response
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

    val timetableApi: TimetableApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TimetableApi::class.java)
    }

    val messagesApi: MessagesApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MessagesApi::class.java)
    }
}
