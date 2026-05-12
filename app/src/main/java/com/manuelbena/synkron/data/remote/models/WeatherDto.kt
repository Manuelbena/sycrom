package com.manuelbena.synkron.data.remote.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherResponseDto(
    @Json(name = "main") val main: MainDto,
    @Json(name = "weather") val weather: List<WeatherDescriptionDto>,
    @Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class MainDto(
    @Json(name = "temp") val temp: Double
)

@JsonClass(generateAdapter = true)
data class WeatherDescriptionDto(
    @Json(name = "description") val description: String,
    @Json(name = "icon") val icon: String
)
