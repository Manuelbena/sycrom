package com.manuelbena.synkron.data.remote.api

import com.manuelbena.synkron.data.remote.models.WeatherResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "es"
    ): WeatherResponseDto

    companion object {
        const val BASE_URL = "https://api.openweathermap.org/data/2.5/"
    }
}
