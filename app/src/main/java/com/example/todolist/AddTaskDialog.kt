package com.example.todolist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Calendar
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddTaskDialog(
    task: Task,
    onSave: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(task.text) }
    var reminderTime by remember { mutableStateOf(task.reminderTime) }
    var isTimeSet by remember { mutableStateOf(task.reminderTime != 0L) }
    var reminderDayBefore by remember { mutableStateOf(task.reminderDayBefore) }
    var reminderHourBefore by remember { mutableStateOf(task.reminderHourBefore) }
    val context = LocalContext.current
    val currentCalendar = Calendar.getInstance()
    val calendar = Calendar.getInstance().apply {
        timeInMillis = if (reminderTime != 0L) reminderTime else currentCalendar.timeInMillis
    }
    var year by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var month by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var day by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    var hour by remember { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableStateOf(calendar.get(Calendar.MINUTE)) }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(if (task.text.isEmpty()) "Добавить задачу" else "Редактировать задачу") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Задача") },
                    modifier = Modifier.padding(8.dp)
                )
                Button(
                    onClick = {
                        val currentYear = currentCalendar.get(Calendar.YEAR)
                        val currentMonth = currentCalendar.get(Calendar.MONTH)
                        val currentDay = currentCalendar.get(Calendar.DAY_OF_MONTH)

                        DatePickerDialog(
                            context,
                            { _, selectedYear, selectedMonth, selectedDay ->
                                year = selectedYear
                                month = selectedMonth
                                day = selectedDay
                                val selectedCalendar = Calendar.getInstance().apply {
                                    set(year, month, day, 0, 0, 0)
                                }
                                val isToday = (selectedYear == currentYear && selectedMonth == currentMonth && selectedDay == currentDay)
                                val currentHour = if (isToday) currentCalendar.get(Calendar.HOUR_OF_DAY) else 0
                                val currentMinute = if (isToday) currentCalendar.get(Calendar.MINUTE) else 0

                                TimePickerDialog(
                                    context,
                                    { _, selectedHour, selectedMinute ->
                                        hour = selectedHour
                                        minute = selectedMinute
                                        reminderTime = Calendar.getInstance().apply {
                                            set(year, month, day, hour, minute, 0)
                                            set(Calendar.MILLISECOND, 0)
                                            val currentTime = currentCalendar.timeInMillis
                                            if (timeInMillis < currentTime) {
                                                timeInMillis = currentTime
                                                val adjustedCalendar = Calendar.getInstance().apply {
                                                    timeInMillis = currentTime
                                                }
                                                year = adjustedCalendar.get(Calendar.YEAR)
                                                month = adjustedCalendar.get(Calendar.MONTH)
                                                day = adjustedCalendar.get(Calendar.DAY_OF_MONTH)
                                                hour = adjustedCalendar.get(Calendar.HOUR_OF_DAY)
                                                minute = adjustedCalendar.get(Calendar.MINUTE)
                                            }
                                        }.timeInMillis
                                        isTimeSet = true
                                    },
                                    hour,
                                    minute,
                                    true
                                ).apply {
                                    if (isToday) {
                                        updateTime(currentHour, currentMinute)
                                    }
                                }.show()
                            },
                            year,
                            month,
                            day
                        ).apply {
                            datePicker.minDate = currentCalendar.timeInMillis
                        }.show()
                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(if (isTimeSet) "Изменить дату и время" else "Выбрать дату и время")
                }
                if (isTimeSet) {
                    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                    Text(
                        text = "Выбрано: ${dateFormat.format(Date(reminderTime))}",
                        modifier = Modifier.padding(8.dp)
                    )
                    val timeDifference = reminderTime - currentCalendar.timeInMillis
                    val oneDayInMillis = 24 * 60 * 60 * 1000L
                    val oneHourInMillis = 60 * 60 * 1000L
                    if (timeDifference > oneDayInMillis) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = reminderDayBefore,
                                onCheckedChange = { reminderDayBefore = it },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "Напомнить за 1 день",
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }
                    if (timeDifference > oneHourInMillis) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = reminderHourBefore,
                                onCheckedChange = { reminderHourBefore = it },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "Напомнить за 1 час",
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                if (task.text.isNotEmpty()) {
                    Button(
                        onClick = {
                            onDelete(task)
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Удалить")
                    }
                }
                Button(onClick = {
                    if (text.isNotEmpty()) {
                        val newTask = task.copy(
                            text = text,
                            reminderTime = if (isTimeSet) reminderTime else 0L,
                            reminderDayBefore = reminderDayBefore,
                            reminderHourBefore = reminderHourBefore
                        )
                        onSave(newTask)
                    }
                }) {
                    Text("Сохранить")
                }
            }
        },
        dismissButton = {
            Button(onClick = { onDismiss() }) {
                Text("Отмена")
            }
        }
    )
}