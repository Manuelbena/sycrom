package com.manuelbena.synkron.domain.models

data class WeatherModel(
    val temperature: Double,
    val description: String,
    val iconCode: String,
    val cityName: String
)
