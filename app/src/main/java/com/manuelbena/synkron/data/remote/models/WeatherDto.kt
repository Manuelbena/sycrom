package com.manuelbena.synkron.data.remote.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherResponseDto(
    @Json(name = "current_weather") val currentWeather: CurrentWeatherDto? = null
)

@JsonClass(generateAdapter = true)
data class CurrentWeatherDto(
    @Json(name = "temperature") val temperature: Double,
    @Json(name = "weathercode") val weatherCode: Int
)
