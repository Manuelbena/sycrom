package com.manuelbena.synkron.data.remote.n8n

import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

// --- MODELOS (DTOs) ---
@JsonClass(generateAdapter = true)
data class IngestRequest(
    val source: String = "android-app",
    val idempotencyKey: String,
    val message: String,
    val locale: String = "es-ES"
)

@JsonClass(generateAdapter = true)
data class IngestResponse(
    val status: String? = null,
    val message: String? = null
)
