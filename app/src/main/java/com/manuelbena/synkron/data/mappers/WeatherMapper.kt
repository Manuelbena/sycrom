package com.manuelbena.synkron.data.mappers

import com.manuelbena.synkron.data.remote.models.WeatherResponseDto
import com.manuelbena.synkron.domain.models.WeatherModel

fun WeatherResponseDto.toDomain(): WeatherModel {
    val current = currentWeather ?: return WeatherModel(0.0, "N/A", "01d", "Desconocido")
    
    val (description, icon) = mapWmoCode(current.weatherCode)
    
    return WeatherModel(
        temperature = current.temperature,
        description = description,
        iconCode = icon,
        cityName = "" // Open-Meteo no devuelve el nombre de la ciudad por coordenadas de esta forma
    )
}

private fun mapWmoCode(code: Int): Pair<String, String> {
    return when (code) {
        0 -> "Despejado" to "01d"
        1, 2, 3 -> "Parcialmente nublado" to "02d"
        45, 48 -> "Niebla" to "50d"
        51, 53, 55 -> "Llovizna" to "09d"
        61, 63, 65 -> "Lluvia" to "10d"
        71, 73, 75 -> "Nieve" to "13d"
        77 -> "Granizo fino" to "13d"
        80, 81, 82 -> "Chubascos" to "09d"
        85, 86 -> "Chubascos de nieve" to "13d"
        95, 96, 99 -> "Tormenta" to "11d"
        else -> "Nublado" to "03d"
    }
}
