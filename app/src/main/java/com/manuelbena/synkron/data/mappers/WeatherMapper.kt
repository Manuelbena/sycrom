package com.manuelbena.synkron.data.mappers

import com.manuelbena.synkron.data.remote.models.WeatherResponseDto
import com.manuelbena.synkron.domain.models.WeatherModel

fun WeatherResponseDto.toDomain(): WeatherModel {
    return WeatherModel(
        temperature = main.temp,
        description = weather.firstOrNull()?.description ?: "",
        iconCode = weather.firstOrNull()?.icon ?: "",
        cityName = name
    )
}
