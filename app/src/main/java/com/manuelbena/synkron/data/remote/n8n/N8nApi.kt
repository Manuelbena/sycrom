package com.manuelbena.synkron.data.remote.n8n

import com.manuelbena.synkron.data.remote.n8n.models.IngestRequest
import com.manuelbena.synkron.data.remote.n8n.models.IngestResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface N8nApi {
    @POST("webhook-test/android/events")
    suspend fun sendEvent(@Body body: IngestRequest): Response<IngestResponse>
}