package com.example.todolist

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("О программе") },
        text = { Text("Это приложение для управления задачами.\n\n" +
                "1. Нажмите '+' для добавления задачи.\n" +
                "2. Нажмите на задачу для отметки ее выполненной.\n" +
                "3. Долгое нажатие на задачу открывает редактирование.\n" +
                "4. Установите время напоминания в окне добавления или редактирования.\n \n" +
                "") },
        confirmButton = {
            Button(onClick = { onDismiss() }) {
                Text("OK")
            }
        }
    )
}