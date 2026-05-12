package com.manuelbena.synkron.domain.interfaces

import com.manuelbena.synkron.domain.models.WeatherModel

interface WeatherRepository {
    suspend fun getWeather(lat: Double, lon: Double): Result<WeatherModel>
}
