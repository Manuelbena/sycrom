package com.manuelbena.synkron.data.remote.n8n.models

import com.squareup.moshi.Json

// Request: Lo que enviamos
data class N8nChatRequest(
    // En Moshi se usa @Json(name = "value") en lugar de @SerializedName
    @Json(name = "message") val message: String
)

// Response: Lo que recibimos
data class N8nChatResponse(
    @Json(name = "title") val title: String?,
    @Json(name = "date") val date: String?,
    @Json(name = "time") val time: String?,
    @Json(name = "category") val category: String?,
    @Json(name = "tags") val tags: List<String>?
)