package com.manuelbena.synkron.data.repository

import com.manuelbena.synkron.data.mappers.toDomain
import com.manuelbena.synkron.data.remote.api.WeatherApi
import com.manuelbena.synkron.domain.interfaces.WeatherRepository
import com.manuelbena.synkron.domain.models.WeatherModel
import javax.inject.Inject

class WeatherRepositoryImpl @Inject constructor(
    private val api: WeatherApi
) : WeatherRepository {

    override suspend fun getWeather(lat: Double, lon: Double): Result<WeatherModel> {
        return try {
            val response = api.getCurrentWeather(lat, lon)
            Result.success(response.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
