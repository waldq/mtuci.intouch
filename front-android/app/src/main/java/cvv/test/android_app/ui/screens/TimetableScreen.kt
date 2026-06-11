package cvv.test.android_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import cvv.test.android_app.core.data.DEFAULT_GROUP
import cvv.test.android_app.core.state.MainState
import cvv.test.android_app.ui.theme.AuthFieldBackground
import cvv.test.android_app.ui.theme.AuthTextColor
import kotlinx.serialization.json.*

data class Lesson(
    val title: String,
    val teacher: String,
    val type: String,
    val time: String,
    val room: String,
    val color: Color
)

@Composable
fun TimetableScreen(state: MainState) {
    var isMonthView by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) }
    
    val monthNames = listOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    )

    LaunchedEffect(state.currentMonth) {
        state.fetchTimetable()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Заголовок с переключением месяца
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { state.changeMonth(-1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = AuthTextColor)
            }
            Text(
                text = monthNames.getOrElse(state.currentMonth - 1) { "Месяц" },
                color = AuthTextColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            IconButton(onClick = { state.changeMonth(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = AuthTextColor)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Переключатель День/Месяц
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(AuthFieldBackground.copy(alpha = 0.6f)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabItem(
                    text = "День",
                    isSelected = !isMonthView,
                    onClick = { isMonthView = false },
                    modifier = Modifier.weight(1f)
                )
                TabItem(
                    text = "Месяц",
                    isSelected = isMonthView,
                    onClick = { isMonthView = true },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(
                onClick = { /* Можно добавить открытие системного календаря */ },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AuthFieldBackground.copy(alpha = 0.6f))
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = AuthTextColor)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isMonthView) {
            MonthCalendarView(state) { selectedDay = it.toInt(); isMonthView = false }
        } else {
            DayScheduleView(state, selectedDay) { selectedDay = it }
        }
    }
}

@Composable
fun TabItem(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val backgroundColor = if (isSelected) AuthFieldBackground else Color.Transparent
    val textColor = if (isSelected) AuthTextColor else AuthTextColor.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = textColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun MonthCalendarView(state: MainState, onDayClick: (String) -> Unit) {
    val daysOfWeek = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб")
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.MONTH, state.currentMonth - 1)
    val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    
    // Формируем ключи для проверки наличия занятий в этом месяце
    val daysData = state.timetableData?.get("data")?.jsonObject?.get("days")?.jsonObject

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(AuthFieldBackground.copy(alpha = 0.8f))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            daysOfWeek.forEach { day ->
                Text(text = day, color = AuthTextColor.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        val days = (1..maxDays).toList()
        val rows = days.chunked(6)

        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                row.forEachIndexed { index, day ->
                    val dateKey = "${day.toString().padStart(2, '0')}.${state.currentMonth.toString().padStart(2, '0')}.2026"
                    
                    val lessonsJson = daysData?.get(dateKey)?.jsonArray
                    val colors = lessonsJson?.map { it.jsonObject }?.map { json ->
                        val typeStr = json["UF_TYPE"]?.jsonPrimitive?.content ?: ""
                        when {
                            typeStr.contains("Лек", ignoreCase = true) -> Color(0xFF66BB6A)
                            typeStr.contains("Прак", ignoreCase = true) -> Color(0xFF5C6BC0)
                            typeStr.contains("Лаб", ignoreCase = true) -> Color(0xFFFFA726)
                            else -> Color.Gray
                        }
                    } ?: emptyList()

                    CalendarDayItem(
                        day = day.toString().padStart(2, '0'),
                        isToday = day == Calendar.getInstance().get(Calendar.DAY_OF_MONTH) && state.currentMonth == (Calendar.getInstance().get(Calendar.MONTH) + 1),
                        isWeekend = index == 5,
                        lessonColors = colors,
                        modifier = Modifier.weight(1f).clickable { onDayClick(day.toString()) }
                    )
                }
                repeat(6 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun CalendarDayItem(day: String, isToday: Boolean, isWeekend: Boolean, lessonColors: List<Color>, modifier: Modifier = Modifier) {
    val backgroundColor = when {
        isToday -> Color(0xFF2E6144).copy(alpha = 0.9f)
        isWeekend -> Color(0xFF634D1E).copy(alpha = 0.9f)
        else -> AuthFieldBackground.copy(alpha = 0.4f)
    }

    Column(
        modifier = modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = day, color = AuthTextColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        
        if (lessonColors.isNotEmpty()) {
            val dotRows = lessonColors.take(6).chunked(3)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                dotRows.forEach { rowColors ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        rowColors.forEach { color -> Dot(color) }
                    }
                }
            }
        }
    }
}

@Composable
fun Dot(color: Color) {
    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
}

@Composable
fun DayScheduleView(state: MainState, selectedDay: Int, onDateSelect: (Int) -> Unit) {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.MONTH, state.currentMonth - 1)
    val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val days = (1..maxDays).toList()

    Column {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(days) { day ->
                val isSelected = day == selectedDay
                DateItem(
                    day = day,
                    isSelected = isSelected,
                    onClick = { onDateSelect(day) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "${selectedDay.toString().padStart(2, '0')}.${state.currentMonth.toString().padStart(2, '0')}.2026", 
            color = AuthTextColor, 
            fontWeight = FontWeight.Bold, 
            fontSize = 18.sp
        )
        Text(text = "Группа: $DEFAULT_GROUP", color = AuthTextColor.copy(alpha = 0.6f), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // Формируем ключ для поиска данных в JSON (например, "01.06.2026")
        val dateKey = "${selectedDay.toString().padStart(2, '0')}.${state.currentMonth.toString().padStart(2, '0')}.2026"
        val daysData = state.timetableData?.get("data")?.jsonObject?.get("days")?.jsonObject
        val lessonsJson = daysData?.get(dateKey)?.jsonArray

        val lessons = lessonsJson?.map { it.jsonObject }?.map { json ->
            val typeStr = json["UF_TYPE"]?.jsonPrimitive?.content ?: ""
            // Собираем всех преподавателей
            val teachers = json["UF_TEACHER"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content }?.joinToString(", ") ?: "Не указан"
            // Собираем все аудитории
            val rooms = json["UF_AUDIENCE"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content }?.joinToString(", ") ?: "Не указана"
            
            Lesson(
                title = json["UF_DISCIPLINE"]?.jsonPrimitive?.content ?: "Без названия",
                teacher = teachers,
                type = typeStr,
                time = "${json["UF_TIME_START"]?.jsonPrimitive?.content} – ${json["UF_TIME_END"]?.jsonPrimitive?.content}",
                room = "Ауд. $rooms",
                color = when {
                    typeStr.contains("Лек", ignoreCase = true) -> Color(0xFF66BB6A) // Зеленый для лекций
                    typeStr.contains("Прак", ignoreCase = true) -> Color(0xFF5C6BC0) // Синий для практик
                    typeStr.contains("Лаб", ignoreCase = true) -> Color(0xFFFFA726) // Оранжевый для лаб
                    else -> Color.Gray
                }
            )
        } ?: emptyList()

        if (lessons.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Занятий нет", color = AuthTextColor.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                items(lessons) { lesson -> LessonCard(lesson) }
            }
        }
    }
}

@Composable
fun DateItem(day: Int, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) AuthFieldBackground.copy(alpha = 0.8f) else Color.Transparent

    Column(
        modifier = Modifier
            .width(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = day.toString().padStart(2, '0'), color = if (isSelected) AuthTextColor else AuthTextColor.copy(alpha = 0.6f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun LessonCard(lesson: Lesson) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AuthFieldBackground.copy(alpha = 0.8f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = lesson.title, color = AuthTextColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(text = lesson.teacher, color = AuthTextColor.copy(0.7f), fontSize = 12.sp)
            Text(text = lesson.type, color = AuthTextColor.copy(0.7f), fontSize = 12.sp)
            Text(text = "${lesson.time} • ${lesson.room}", color = AuthTextColor.copy(0.5f), fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.width(4.dp).height(40.dp).clip(CircleShape).background(lesson.color))
    }
}
