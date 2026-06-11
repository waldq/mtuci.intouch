package cvv.test.android_app.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

@Serializable
data class TimetableRequest(
    val group: String,
    val month: Int,
)


interface TimetableApi {
    @Headers(
        "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "X-Requested-With: XMLHttpRequest"
    )
    @POST("misc/timetable")
    suspend fun getTimetable(
        @Body timetableRequest: TimetableRequest,
    ) : Response<JsonObject>
}