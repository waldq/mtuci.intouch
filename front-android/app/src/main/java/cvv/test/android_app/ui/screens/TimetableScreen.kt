package cvv.test.android_app.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cvv.test.android_app.api.RetrofitClient
import cvv.test.android_app.api.TimetableRequest
import cvv.test.android_app.ui.components.DashedDivider
import cvv.test.android_app.ui.theme.AuthFieldBackground
import cvv.test.android_app.ui.theme.AuthTextColor
import kotlinx.coroutines.launch

@Composable
fun TimetableScreen() {
    var group by remember { mutableStateOf("") }
    var month by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Расписание",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = AuthTextColor
        )
        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(AuthFieldBackground.copy(alpha = 0.8f))
        ) {
            OutlinedTextField(
                value = group,
                onValueChange = { group = it },
                placeholder = { Text("Группа (БПИ2502)", color = AuthTextColor.copy(0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            DashedDivider(color = AuthTextColor.copy(alpha = 0.2f))

            OutlinedTextField(
                value = month,
                onValueChange = { month = it },
                placeholder = { Text("Месяц (1-12)", color = AuthTextColor.copy(0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val m = month.toIntOrNull() ?: 9
                scope.launch {
                    try {
                        val request = TimetableRequest(group = group, month = m)
                        val response = RetrofitClient.timetableApi.getTimetable(request)
                        if (response.isSuccessful) {
                            Log.d("Timetable", "SUCCESS! Body: ${response.body()}")
                        }
                    } catch (e: Exception) {
                        Log.e("Timetable", "Error: ${e.message}")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AuthFieldBackground),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text("Запросить JSON", color = AuthTextColor, fontWeight = FontWeight.Bold)
        }
    }
}
