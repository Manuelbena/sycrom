package com.manuelbena.synkron.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.manuelbena.synkron.data.remote.n8n.IngestRequest
import com.manuelbena.synkron.data.remote.n8n.N8nApi

import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.UUID

@HiltWorker
class N8nIngestWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val api: N8nApi
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val message = inputData.getString("KEY_MESSAGE") ?: return Result.failure()

        return try {
            val request = IngestRequest(
                idempotencyKey = UUID.randomUUID().toString(),
                message = message
            )
            // Llamada síncrona dentro del worker (ya estamos en background)
            val response = api.sendEvent(request)

            if (response.isSuccessful) Result.success() else Result.retry()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}