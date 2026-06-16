package com.example.weatherapp.repository

import androidx.lifecycle.LiveData
import com.example.weatherapp.data.local.ForecastDao
import com.example.weatherapp.data.local.WeatherDao
import com.example.weatherapp.data.remote.WeatherApiService
import com.example.weatherapp.model.CurrentWeather
import com.example.weatherapp.model.ForecastDay
import com.example.weatherapp.model.ForecastApiResponse
import java.text.SimpleDateFormat
import java.util.*

/**
 * REPOSITORY — Single source of truth for all weather data
 *
 * The ViewModel talks ONLY to the Repository.
 * The Repository decides whether to use Room (cached data) or Retrofit (fresh API data).
 *
 * sealed class Result: a type-safe way to represent success or failure
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

class WeatherRepository(
    private val weatherDao: WeatherDao,
    private val forecastDao: ForecastDao,
    private val apiService: WeatherApiService
) {

    /**
     * Get cached current weather as LiveData.
     * The UI observes this — it updates automatically when Room data changes.
     */
    fun getCachedWeather(city: String): LiveData<CurrentWeather?> =
        weatherDao.getCurrentWeather(city)

    /**
     * Get cached forecast as LiveData.
     */
    fun getCachedForecast(city: String): LiveData<List<ForecastDay>> =
        forecastDao.getForecastForCity(city)

    /**
     * Fetch fresh weather from the API by city name, then cache to Room.
     * Returns a Result (success with data, or error with message).
     */
    suspend fun fetchWeatherByCity(city: String): Result<CurrentWeather> {
        return try {
            val response = apiService.getCurrentWeatherByCity(city)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val weather = CurrentWeather(
                    cityName = body.name,
                    temperature = body.main.temp,
                    feelsLike = body.main.feelsLike,
                    humidity = body.main.humidity,
                    windSpeed = body.wind.speed,
                    description = body.weather.firstOrNull()?.description ?: "",
                    iconCode = body.weather.firstOrNull()?.icon ?: "01d",
                    country = body.sys.country
                )
                // Save to Room cache
                weatherDao.clearCurrentWeather()
                weatherDao.insertCurrentWeather(weather)
                // Also fetch forecast
                fetchForecastByCity(body.name)
                Result.Success(weather)
            } else {
                Result.Error("City not found. Please check the spelling.")
            }
        } catch (e: Exception) {
            Result.Error("No internet connection. Showing cached data.")
        }
    }

    /**
     * Fetch weather by GPS coordinates — the NEW FEATURE (FusedLocationProvider).
     */
    suspend fun fetchWeatherByLocation(lat: Double, lon: Double): Result<CurrentWeather> {
        return try {
            val response = apiService.getCurrentWeatherByLocation(lat, lon)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val weather = CurrentWeather(
                    cityName = body.name,
                    temperature = body.main.temp,
                    feelsLike = body.main.feelsLike,
                    humidity = body.main.humidity,
                    windSpeed = body.wind.speed,
                    description = body.weather.firstOrNull()?.description ?: "",
                    iconCode = body.weather.firstOrNull()?.icon ?: "01d",
                    country = body.sys.country
                )
                weatherDao.clearCurrentWeather()
                weatherDao.insertCurrentWeather(weather)
                fetchForecastByLocation(lat, lon, body.name)
                Result.Success(weather)
            } else {
                Result.Error("Could not get weather for your location.")
            }
        } catch (e: Exception) {
            Result.Error("No internet connection.")
        }
    }

    /**
     * Fetch 5-day forecast and convert to ForecastDay list for Room.
     * We take the noon slot (12:00:00) for each day as the representative forecast.
     */
    private suspend fun fetchForecastByCity(city: String) {
        try {
            val response = apiService.getForecastByCity(city)
            if (response.isSuccessful && response.body() != null) {
                saveForecast(city, response.body()!!)
            }
        } catch (e: Exception) { /* silent fail — current weather already saved */ }
    }

    private suspend fun fetchForecastByLocation(lat: Double, lon: Double, city: String) {
        try {
            val response = apiService.getForecastByLocation(lat, lon)
            if (response.isSuccessful && response.body() != null) {
                saveForecast(city, response.body()!!)
            }
        } catch (e: Exception) { /* silent fail */ }
    }

    /**
     * Convert the raw API forecast list into ForecastDay objects and save to Room.
     * Groups by date and picks one slot per day.
     */
    private suspend fun saveForecast(city: String, apiResponse: ForecastApiResponse) {
        forecastDao.clearForecastForCity(city)

        // Group forecast items by date (yyyy-MM-dd) and pick one per day
        val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFormat = SimpleDateFormat("EEE", Locale.getDefault()) // "Mon", "Tue"...

        val grouped = apiResponse.list.groupBy { item ->
            dayFormat.format(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .parse(item.dateText) ?: Date())
        }

        val forecastDays = grouped.entries.take(7).map { (dateStr, items) ->
            // Pick noon slot if available, otherwise first slot
            val noon = items.firstOrNull { it.dateText.contains("12:00") } ?: items.first()
            val date = dayFormat.parse(dateStr) ?: Date()

            ForecastDay(
                cityName = city,
                date = displayFormat.format(date),
                tempMax = items.maxOf { it.main.tempMax },
                tempMin = items.minOf { it.main.tempMin },
                description = noon.weather.firstOrNull()?.description ?: "",
                iconCode = noon.weather.firstOrNull()?.icon ?: "01d",
                humidity = noon.main.humidity,
                windSpeed = noon.wind.speed,
                chanceOfRain = (noon.pop * 100).toInt()
            )
        }

        forecastDao.insertForecasts(forecastDays)
    }
}
