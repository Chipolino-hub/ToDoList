package com.example.todolist

data class Task(
    val text: String,
    var isCompleted: Boolean,
    var reminderTime: Long = 0L,
    var reminderDayBefore: Boolean = false,
    var reminderHourBefore: Boolean = false
)