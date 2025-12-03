package com.manuelbena.synkron.data.remote.n8n.models

import com.squareup.moshi.Json

// Request: Lo que enviamos
data class N8nChatRequest(
    @Json(name = "message") val message: String
)

// Response
data class N8nChatResponse(
    @Json(name = "id") val id: String?,
    @Json(name = "title") val title: String?,
    @Json(name = "description") val description: String?,
    @Json(name = "location") val location: String?,

    // --- ESTE ES EL CAMPO CLAVE QUE FALTABA ---
    @Json(name = "subTasks") val subTasksString: String?,

    @Json(name = "typeTask") val typeTask: String?,
    @Json(name = "priority") val priority: String?,
    @Json(name = "isActive") val isActive: String?,
    @Json(name = "isDone") val isDone: String?,
    @Json(name = "start") val startTimestamp: String?,
    @Json(name = "end") val endTimestamp: String?,
    @Json(name = "categoryIcon") val categoryIcon: String?,
    @Json(name = "categoryColor") val categoryColor: String?
)

// --- ESTA CLASE TAMBIÉN FALTABA ---
data class N8nSubTaskDto(
    @Json(name = "id") val id: String?,
    @Json(name = "title") val title: String?,
    @Json(name = "isDone") val isDone: Boolean?
)