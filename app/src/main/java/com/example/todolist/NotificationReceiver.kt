package com.example.todolist

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.util.Log

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskText = intent.getStringExtra("taskText") ?: "Задача"
        val isDayBefore = intent.getBooleanExtra("isDayBefore", false)
        val isHourBefore = intent.getBooleanExtra("isHourBefore", false)
        Log.d("NotificationReceiver", "onReceive triggered for: $taskText, isDayBefore: $isDayBefore, isHourBefore: $isHourBefore at ${java.util.Date(System.currentTimeMillis())}")

        val notification = NotificationCompat.Builder(context, "taskChannel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Напоминание")
            .setContentText(
                when {
                    isHourBefore -> "Через 1 час: Время для задачи: $taskText"
                    isDayBefore -> "Через 1 день: Время для задачи: $taskText"
                    else -> "Время для задачи: $taskText"
                }
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setAutoCancel(true)
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
            Log.d("NotificationReceiver", "Notification displayed for: $taskText, type: ${if (isHourBefore) "hour before" else if (isDayBefore) "day before" else "main"}")
        } catch (e: SecurityException) {
            Log.e("NotificationReceiver", "SecurityException: Permission not granted", e)
        } catch (e: Exception) {
            Log.e("NotificationReceiver", "Error displaying notification: ${e.message}", e)
        }
    }
}