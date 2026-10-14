package com.example.todolist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskList(tasks: List<Task>, onTaskClick: (Task) -> Unit, onTaskLongClick: (Task) -> Unit) {
    LazyColumn(
        modifier = Modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tasks) { task ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onTaskClick(task) },
                        onLongClick = { onTaskLongClick(task) }
                    )
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
                            onCheckedChange = { onTaskClick(task) },
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
}