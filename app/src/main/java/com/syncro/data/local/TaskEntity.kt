package com.syncro.data.local

// TODO: Add Room annotations
data class TaskEntity(
    val id: String,
    val title: String,
    val description: String,
    val isCompleted: Boolean
)
