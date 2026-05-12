package com.manuelbena.synkron.data.repository

import com.manuelbena.synkron.data.mappers.toDomain
import com.manuelbena.synkron.data.remote.api.WeatherApi
import com.manuelbena.synkron.domain.interfaces.WeatherRepository
import com.manuelbena.synkron.domain.models.WeatherModel
import javax.inject.Inject

class WeatherRepositoryImpl @Inject constructor(
    private val api: WeatherApi
) : WeatherRepository {

    // Nota: Deberías poner tu API KEY real aquí o inyectarla por Hilt
    private val API_KEY = "80f8f0477265a7f23a1a1b41505c9285" // Reemplazar con una válida si esta no funciona

    override suspend fun getWeather(lat: Double, lon: Double): Result<WeatherModel> {
        return try {
            val response = api.getCurrentWeather(lat, lon, API_KEY)
            Result.success(response.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
