package com.example.todolist

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val gson = Gson()
    private val PREFS_NAME = "TodoPrefs"
    private val TASKS_KEY = "tasks"
    private var editTaskIndex by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        createNotificationChannel()
        setContent {
            var showAddDialog by remember { mutableStateOf(false) }
            var showAboutDialog by remember { mutableStateOf(false) }
            val tasks = remember { mutableStateListOf<Task>().apply { addAll(loadTasks()) } }

            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "Задачи",
                                fontSize = 36.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        itemsIndexed(tasks) { index, task ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTaskClick(index, tasks) }
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = {
                                                editTaskIndex = index
                                            }
                                        )
                                    }
                                    .padding(horizontal = 8.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = task.isCompleted,
                                            onCheckedChange = { onTaskClick(index, tasks) },
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text(
                                            text = task.text,
                                            fontSize = 18.sp,
                                            modifier = Modifier.weight(1f),
                                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                        )
                                    }
                                    if (task.reminderTime != 0L) {
                                        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                                        val reminderDate = Date(task.reminderTime)
                                        Text(
                                            text = "Напоминание: ${dateFormat.format(reminderDate)}",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            modifier = Modifier.padding(start = 40.dp, top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 16.dp, end = 16.dp)
                            .size(60.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "+",
                            fontSize = 24.sp,
                            modifier = Modifier.padding(0.dp)
                        )
                    }
                    Button(
                        onClick = { showAboutDialog = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 16.dp, end = 16.dp)
                            .size(40.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text(
                            text = "i",
                            fontSize = 18.sp
                        )
                    }
                }

                if (showAddDialog || editTaskIndex != null) {
                    val taskToEdit = editTaskIndex?.let { index ->
                        if (index in tasks.indices) tasks[index] else null
                    } ?: Task("", false, 0L)

                    AddTaskDialog(
                        task = taskToEdit,
                        onSave = { updatedTask ->
                            if (editTaskIndex != null && editTaskIndex in tasks.indices) {
                                cancelExistingNotifications(tasks[editTaskIndex!!])
                                tasks[editTaskIndex!!] = updatedTask
                            } else {
                                tasks.add(updatedTask)
                            }
                            if (updatedTask.reminderTime != 0L) {
                                scheduleNotification(updatedTask)
                            }
                            saveTasksAsync(tasks)
                            editTaskIndex = null
                            showAddDialog = false
                        },
                        onDelete = { taskToDelete ->
                            val indexToDelete = tasks.indexOf(taskToDelete)
                            if (indexToDelete != -1) {
                                cancelExistingNotifications(taskToDelete)
                                tasks.removeAt(indexToDelete)
                            }
                            saveTasksAsync(tasks)
                            editTaskIndex = null
                            showAddDialog = false
                        },
                        onDismiss = {
                            showAddDialog = false
                            editTaskIndex = null
                        }
                    )
                }

                if (showAboutDialog) {
                    AboutDialog(
                        onDismiss = { showAboutDialog = false }
                    )
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                    Log.d("Notification", "Notification permission already granted")
                }
                ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.POST_NOTIFICATIONS) -> {
                    Log.w("Notification", "Should show rationale for notification permission")
                }
                else -> {
                    registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                        if (isGranted) {
                            Log.d("Notification", "Permission granted for notifications")
                        } else {
                            Log.w("Notification", "Permission denied for notifications")
                        }
                    }.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            Log.d("Notification", "No permission needed for notifications on this API level")
        }
    }

    private fun onTaskClick(index: Int, tasks: MutableList<Task>) {
        if (index in tasks.indices) {
            val task = tasks[index]
            val updatedTask = task.copy(isCompleted = !task.isCompleted)
            tasks[index] = updatedTask
            saveTasksAsync(tasks)
        }
    }

    private fun saveTasksAsync(tasks: MutableList<Task>) {
        lifecycleScope.launch(Dispatchers.IO) {
            val editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
            val json = gson.toJson(tasks)
            editor.putString(TASKS_KEY, json)
            editor.apply()
            Log.d("SaveTasks", "Tasks saved: $json")
        }
    }

    private fun loadTasks(): List<Task> {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val json = prefs.getString(TASKS_KEY, null)
        Log.d("LoadTasks", "Loaded JSON: $json")
        return if (json != null) {
            val type = object : TypeToken<List<Task>>() {}.type
            gson.fromJson(json, type)
        } else emptyList()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "TaskReminders"
            val channel = NotificationChannel(
                "taskChannel",
                name,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for task reminders"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setShowBadge(true)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d("Notification", "Notification channel created: $name")
        }
    }

    private fun scheduleNotification(task: Task) {
        val currentTime = System.currentTimeMillis()
        val intent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("taskText", task.text)
            putExtra("isDayBefore", false)
        }
        val alarmManager = getSystemService(AlarmManager::class.java)

        Log.d("Notification", "Current time: ${Date(currentTime)}")
        Log.d("Notification", "Task reminder time: ${Date(task.reminderTime)}")

        if (task.reminderTime != 0L && task.reminderTime > currentTime) {
            val requestCode = System.currentTimeMillis().toInt()
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )
            Log.d("Notification", "Scheduling main reminder for ${task.text} at ${Date(task.reminderTime)} with requestCode: $requestCode")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.reminderTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, task.reminderTime, pendingIntent)
            }
        } else {
            Log.w("Notification", "Main reminder time is invalid or in past for ${task.text}: ${Date(task.reminderTime)}")
        }

        if (task.reminderDayBefore && task.reminderTime != 0L) {
            val dayBeforeTime = task.reminderTime - (24 * 60 * 60 * 1000)
            Log.d("Notification", "Day-before time: ${Date(dayBeforeTime)}")
            if (dayBeforeTime > currentTime) {
                val dayBeforeIntent = Intent(this, NotificationReceiver::class.java).apply {
                    putExtra("taskText", task.text)
                    putExtra("isDayBefore", true)
                }
                val requestCode = (System.currentTimeMillis() + 1).toInt()
                val pendingIntent = PendingIntent.getBroadcast(
                    this,
                    requestCode,
                    dayBeforeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                )
                Log.d("Notification", "Scheduling day-before reminder for ${task.text} at ${Date(dayBeforeTime)} with requestCode: $requestCode")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dayBeforeTime, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, dayBeforeTime, pendingIntent)
                }
            } else {
                Log.w("Notification", "Day-before reminder time is in past for ${task.text}: ${Date(dayBeforeTime)}")
            }
        }

        if (task.reminderHourBefore && task.reminderTime != 0L) {
            val hourBeforeTime = task.reminderTime - (60 * 60 * 1000)
            Log.d("Notification", "Hour-before time: ${Date(hourBeforeTime)}")
            if (hourBeforeTime > currentTime) {
                val hourBeforeIntent = Intent(this, NotificationReceiver::class.java).apply {
                    putExtra("taskText", task.text)
                    putExtra("isHourBefore", true)
                }
                val requestCode = (System.currentTimeMillis() + 2).toInt()
                val pendingIntent = PendingIntent.getBroadcast(
                    this,
                    requestCode,
                    hourBeforeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                )
                Log.d("Notification", "Scheduling hour-before reminder for ${task.text} at ${Date(hourBeforeTime)} with requestCode: $requestCode")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, hourBeforeTime, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, hourBeforeTime, pendingIntent)
                }
            } else {
                Log.w("Notification", "Hour-before reminder time is in past for ${task.text}: ${Date(hourBeforeTime)}")
            }
        }
    }

    private fun cancelExistingNotifications(task: Task) {
        val alarmManager = getSystemService(AlarmManager::class.java)
        val mainIntent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("taskText", task.text)
            putExtra("isDayBefore", false)
        }
        val mainPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_NO_CREATE or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )
        if (mainPendingIntent != null) {
            alarmManager.cancel(mainPendingIntent)
            Log.d("Notification", "Cancelled main reminder for ${task.text}")
            mainPendingIntent.cancel()
        }

        if (task.reminderDayBefore) {
            val dayBeforeIntent = Intent(this, NotificationReceiver::class.java).apply {
                putExtra("taskText", task.text)
                putExtra("isDayBefore", true)
            }
            val dayBeforePendingIntent = PendingIntent.getBroadcast(
                this,
                1,
                dayBeforeIntent,
                PendingIntent.FLAG_NO_CREATE or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )
            if (dayBeforePendingIntent != null) {
                alarmManager.cancel(dayBeforePendingIntent)
                Log.d("Notification", "Cancelled day-before reminder for ${task.text}")
                dayBeforePendingIntent.cancel()
            }
        }

        if (task.reminderHourBefore) {
            val hourBeforeIntent = Intent(this, NotificationReceiver::class.java).apply {
                putExtra("taskText", task.text)
                putExtra("isHourBefore", true)
            }
            val hourBeforePendingIntent = PendingIntent.getBroadcast(
                this,
                2,
                hourBeforeIntent,
                PendingIntent.FLAG_NO_CREATE or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )
            if (hourBeforePendingIntent != null) {
                alarmManager.cancel(hourBeforePendingIntent)
                Log.d("Notification", "Cancelled hour-before reminder for ${task.text}")
                hourBeforePendingIntent.cancel()
            }
        }
    }
}