package com.manuelbena.synkron.data.remote.n8n

import com.manuelbena.synkron.data.remote.n8n.models.N8nChatRequest
import com.manuelbena.synkron.data.remote.n8n.models.N8nChatResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface N8nApi {
    // ✅ URL corregida para apuntar a tu webhook de pruebas
    @POST("webhook-test/android/events")
    suspend fun sendChatMessage(@Body request: N8nChatRequest): Response<N8nChatResponse>
    @POST("webhook-test/android/events")
    suspend fun sendEvent(@Body body: IngestRequest): Response<IngestResponse>
}