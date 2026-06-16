package com.example.weatherapp.data.remote

import com.example.weatherapp.model.ForecastApiResponse
import com.example.weatherapp.model.WeatherApiResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * OPENWEATHERMAP API SERVICE
 *
 * Retrofit reads these annotations and generates the HTTP implementation.
 * Base URL: https://api.openweathermap.org/data/2.5/
 *
 * API Key is free at openweathermap.org — the one below is a demo key.
 * Replace it with your own if needed.
 */
interface WeatherApiService {

    /**
     * Get current weather by city name.
     * GET https://api.openweathermap.org/data/2.5/weather?q=Tbilisi&units=metric&appid=KEY
     *
     * @Query annotates URL query parameters (?key=value)
     * units=metric → Celsius temperatures
     */
    @GET("weather")
    suspend fun getCurrentWeatherByCity(
        @Query("q") cityName: String,
        @Query("units") units: String = "metric",
        @Query("appid") apiKey: String = WeatherApiConfig.API_KEY
    ): Response<WeatherApiResponse>

    /**
     * Get current weather by GPS coordinates (lat/lon).
     * This is called when the user taps the GPS button — NEW FEATURE.
     */
    @GET("weather")
    suspend fun getCurrentWeatherByLocation(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String = "metric",
        @Query("appid") apiKey: String = WeatherApiConfig.API_KEY
    ): Response<WeatherApiResponse>

    /**
     * Get 5-day / 3-hour forecast (we group by day to show 7 days).
     * cnt=40 = 40 time slots (5 days × 8 slots per day)
     */
    @GET("forecast")
    suspend fun getForecastByCity(
        @Query("q") cityName: String,
        @Query("cnt") count: Int = 40,
        @Query("units") units: String = "metric",
        @Query("appid") apiKey: String = WeatherApiConfig.API_KEY
    ): Response<ForecastApiResponse>

    /**
     * Get forecast by GPS coordinates.
     */
    @GET("forecast")
    suspend fun getForecastByLocation(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("cnt") count: Int = 40,
        @Query("units") units: String = "metric",
        @Query("appid") apiKey: String = WeatherApiConfig.API_KEY
    ): Response<ForecastApiResponse>
}
