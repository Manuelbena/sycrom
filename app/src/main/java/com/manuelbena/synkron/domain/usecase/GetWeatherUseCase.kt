package com.manuelbena.synkron.domain.usecase

import com.manuelbena.synkron.domain.interfaces.WeatherRepository
import com.manuelbena.synkron.domain.models.WeatherModel
import javax.inject.Inject

class GetWeatherUseCase @Inject constructor(
    private val repository: WeatherRepository
) {
    suspend operator fun invoke(lat: Double, lon: Double): Result<WeatherModel> {
        return repository.getWeather(lat, lon)
    }
}
