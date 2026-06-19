package com.manuelbena.synkron.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.manuelbena.synkron.domain.usecase.AnalyzeWeeklyProgressUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class WeeklyAuditorWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val analyzeWeeklyProgressUseCase: AnalyzeWeeklyProgressUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            analyzeWeeklyProgressUseCase()
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
