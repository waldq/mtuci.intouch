package cvv.test.android_app.utils

fun formatTime(isoString: String?): String {
    if (isoString == null) return ""
    return try {
        // Упрощенный парсинг для примера (2024-03-20T12:34:56.789)
        val timePart = isoString.substringAfter('T').take(5)
        timePart
    } catch (e: Exception) {
        ""
    }
}
