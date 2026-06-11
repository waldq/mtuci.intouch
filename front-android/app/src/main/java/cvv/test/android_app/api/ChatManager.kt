package cvv.test.android_app.api

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject

@Serializable
data class UserSearchResult(
    @SerialName("user_id") val userId: String? = null,
    val username: String? = null,
    val tag: String? = null,
    val bio: String? = null,
)

//Singleton-Object для обработки событий в SocketIO
object ChatManager {
    //Специальный адрес, чтобы эмулятор видел локальный сервер
    //Используйте 10.0.2.2 для эмулятора Android или локальный IP вашего ПК
    private const val SOCKET_URL = "http://192.168.240.1:8000"
    private var socket: Socket? = null
    private var currentRoomId: String? = null

    /**
     * Настройка парсера JSON с мягкими правилами валидации
     * Предотвращает падение приложения при изменениях в структуре данных на бэкенде
     */
    private val json = Json {
        // Не падать, если сервер прислал новые поля, которых нет в нашей data-модели
        ignoreUnknownKeys = true

        // Разрешать нестрогий JSON (например, строки без кавычек или с одинарными кавычками)
        isLenient = true

        // Если сервер прислал null в поле, где у нас есть значение по умолчанию — подставить наше значение
        coerceInputValues = true

        // Всегда отправлять поля со значениями по умолчанию
        encodeDefaults = true
    }


    /**
     * Внутренний "горячий" поток (шина событий) для трансляции входящих сообщений.
     * * replay = 1: Новый подписчик (например, открывшийся экран) сразу получит 1 последнее сообщение.
     * extraBufferCapacity = 64: Запасной буфер, чтобы сообщения не терялись, если UI не успевает их обрабатывать.
     */
    private val _messages = MutableSharedFlow<IncomingMessage>(
        replay = 1,
        extraBufferCapacity = 64
    )

    //Публичный поток только для чтения, на который подписываются экраны или сервисы
    val messages = _messages.asSharedFlow()

    private val _searchResults = MutableSharedFlow<List<UserSearchResult>>(replay = 1, extraBufferCapacity = 64)
    val searchResults = _searchResults.asSharedFlow()

    private val _connectionStatus = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val connectionStatus = _connectionStatus.asSharedFlow()

    //Функция подключения пользователя
    fun connect(token: String) {
        // Если соединение уже активно — ничего не делаем, чтобы не плодить дубли
        if (socket?.connected() == true) return

        try {
            // Настройка параметров подключения к Socket.IO
            val options = IO.Options.builder()
                .setAuth(mapOf("token" to token)) // Передача токена в объекте авторизации
                .setQuery("token=$token")          // Проброс токена в query-параметры URL (для подстраховки)
                .setTransports(arrayOf(WebSocket.NAME)) // Отключаем медленный HTTP-polling, сразу используем WebSockets
                .build()

            // Создаем экземпляр сокета для базового URL с нашими настройками
            socket = IO.socket(SOCKET_URL, options)

            //Далее разные события
            socket?.on(Socket.EVENT_CONNECT) {
                Log.d("SocketIO", "CONNECTED TO SERVER")
                _connectionStatus.tryEmit("Connected")
                
                currentRoomId?.let { id ->
                    Log.d("SocketIO", "Emitting join_chat for room: $id")
                    socket?.emit("join_chat", id)
                }
            }

            socket?.on(Socket.EVENT_DISCONNECT) { args ->
                Log.d("SocketIO", "DISCONNECTED: ${args.getOrNull(0)}")
                _connectionStatus.tryEmit("Disconnected")
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e("SocketIO", "CONNECTION ERROR: ${args.getOrNull(0)}")
                _connectionStatus.tryEmit("Error")
            }

            socket?.on("receive_message") { args ->
                Log.d("SocketIO", ">>> INCOMING EVENT: receive_message")

                // Аргументы от Socket.IO приходят в виде массива. Берём самый первый элемент (наше сообщение)
                val rawData = args.getOrNull(0)
                Log.d("SocketIO", "Raw data: $rawData")

                //На всякий случай проверяем, тк может прийти как JSON, так и строка
                val jsonString = when (rawData) {
                    is JSONObject -> rawData.toString() // Если это JSONObject — переводим в строку
                    is String -> rawData                // Если это уже строка — оставляем как есть
                    else -> rawData?.toString()         // Для любых других типов пытаемся вызвать toString()
                }

                if (jsonString != null) {
                    try {
                        // Парсим JSON-строку в наш Kotlin-объект IncomingMessage
                        val message = json.decodeFromString<IncomingMessage>(jsonString)
                        Log.d("SocketIO", "Message parsed successfully: $message")

                        //Отправляет сообщение в память телефона, в MutableSharedFlow
                        _messages.tryEmit(message)

                    } catch (e: Exception) {
                        //Отлавливаем ошибки парсинга
                        Log.e("SocketIO", "PARSING FAILED: ${e.message}")
                        Log.e("SocketIO", "Problematic JSON: $jsonString")
                    }
                }
            }

            socket?.on("chat_messages_result") { args ->
                Log.d("SocketIO", ">>> INCOMING EVENT: chat_messages_result")
                val rawData = args.getOrNull(0)

                // Если пришел объект с ошибкой {"result": "..."}, просто логируем и выходим
                if (rawData is JSONObject && rawData.has("result")) {
                    Log.w("SocketIO", "Server returned error in chat_messages_result: $rawData")
                    return@on
                }

                val jsonString = when (rawData) {
                    is JSONArray -> rawData.toString()
                    is JSONObject -> rawData.toString()
                    is String -> rawData
                    else -> rawData?.toString()
                }

                if (jsonString != null) {
                    try {
                        val messages = json.decodeFromString<List<IncomingMessage>>(jsonString)
                        Log.d("SocketIO", "History parsed successfully: ${messages.size} messages")
                        messages.forEach { _messages.tryEmit(it) }
                    } catch (e: Exception) {
                        Log.e("SocketIO", "HISTORY PARSING FAILED: ${e.message}")
                    }
                }
            }

            // Дополнительный логгер для любых событий (если бэкенд шлет что-то другое)
            socket?.on("chat_created") { Log.d("SocketIO", "Event: chat_created received") }

            socket?.on("search_user_results") { args ->
                Log.d("SocketIO", ">>> INCOMING EVENT: search_user_results")
                val rawData = args.getOrNull(0)
                Log.d("SocketIO", "Raw data: $rawData")

                val jsonString = when (rawData) {
                    is JSONArray -> rawData.toString()
                    is JSONObject -> rawData.toString()
                    is String -> rawData
                    else -> rawData?.toString()
                }

                if (jsonString != null) {
                    try {
                        val results = json.decodeFromString<List<UserSearchResult>>(jsonString)
                        Log.d("SocketIO", "Search results parsed: $results")
                        _searchResults.tryEmit(results)
                    } catch (e: Exception) {
                        Log.e("SocketIO", "SEARCH PARSING FAILED: ${e.message}")
                    }
                }
            }

            socket?.connect()
        } catch (e: Exception) {
            Log.e("SocketIO", "INIT ERROR: ${e.message}")
        }
    }

    //Функция подключения к чату
    fun joinChat(chatId: String) {
        currentRoomId = chatId
        if (socket?.connected() == true) {
            Log.d("SocketIO", "Already connected. Joining room: $chatId")
            //Отправка данных на сервер
            socket?.emit("join_chat", chatId)
            
            // Запрашиваем историю сообщений через сокет
            val data = JSONObject()
            data.put("chat_id", chatId)
            socket?.emit("get_chat_messages", data)
        } else {
            Log.d("SocketIO", "Waiting for connection to join room: $chatId")
        }
    }

    //Функция отправки сообщения
    fun sendMessage(roomId: String, text: String, type: MessageType = MessageType.TEXT) {
        try {
            val sentMessage = SentMessage(
                roomId = roomId,
                message = Message(content = text, msgType = type)
            )
            //Преобразуем объект SentMessage в строку
            val jsonString = json.encodeToString(SentMessage.serializer(), sentMessage)
            //Теперь преобразуем его в JSON
            val data = JSONObject(jsonString)

            Log.d("SocketIO", "SENDING MESSAGE: $data")
            //Отправка данных на сервер
            socket?.emit("send_message", data)
        } catch (e: Exception) {
            Log.e("SocketIO", "SEND ERROR: ${e.message}")
        }
    }

    fun searchUser(query: String) {
        try {
            val data = JSONObject()
            data.put("username_or_tag", query)
            Log.d("SocketIO", "SEARCHING USER: $data")
            socket?.emit("search_user", data)
        } catch (e: Exception) {
            Log.e("SocketIO", "SEARCH ERROR: ${e.message}")
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
    }
}
