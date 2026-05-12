package com.manuelbena.synkron.data.remote.api

import com.manuelbena.synkron.data.remote.models.WeatherResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("v1/forecast")
    suspend fun getCurrentWeather(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current_weather") current: Boolean = true
    ): WeatherResponseDto

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/"
    }
}
