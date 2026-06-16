package com.example.weatherapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * MODEL LAYER (the "M" in MVVM)
 *
 * CurrentWeather — represents the current weather for a city.
 * @Entity(tableName = "current_weather") makes this a Room database table.
 * Room saves the last fetched weather so the app works offline.
 */
@Entity(tableName = "current_weather")
data class CurrentWeather(
    @PrimaryKey
    val cityName: String,          // Used as the primary key (one entry per city)

    val temperature: Double,       // Temp in Celsius
    val feelsLike: Double,         // "Feels like" temperature
    val humidity: Int,             // Humidity percentage
    val windSpeed: Double,         // Wind speed in m/s
    val description: String,       // e.g. "thunderstorm", "clear sky"
    val iconCode: String,          // e.g. "11d" — used to build icon URL for Glide
    val country: String,           // Country code e.g. "GE"
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * ForecastDay — one day in the 7-day forecast list.
 * @Entity makes this a separate Room table.
 */
@Entity(tableName = "forecast")
data class ForecastDay(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val cityName: String,          // Which city this forecast belongs to
    val date: String,              // e.g. "Mon", "Tue"
    val tempMax: Double,           // High temperature for the day
    val tempMin: Double,           // Low temperature for the day
    val description: String,       // Weather description
    val iconCode: String,          // Icon code for Glide
    val humidity: Int,
    val windSpeed: Double,
    val chanceOfRain: Int          // Precipitation probability 0-100%
)

// ── Retrofit response models ──────────────────────────────────────────────────
// These mirror the JSON structure from OpenWeatherMap API.
// @SerializedName maps JSON field names to Kotlin property names.

data class WeatherApiResponse(
    @SerializedName("name") val name: String,
    @SerializedName("main") val main: Main,
    @SerializedName("weather") val weather: List<WeatherDescription>,
    @SerializedName("wind") val wind: Wind,
    @SerializedName("sys") val sys: Sys
)

data class ForecastApiResponse(
    @SerializedName("list") val list: List<ForecastItem>,
    @SerializedName("city") val city: City
)

data class ForecastItem(
    @SerializedName("dt_txt") val dateText: String,
    @SerializedName("main") val main: Main,
    @SerializedName("weather") val weather: List<WeatherDescription>,
    @SerializedName("wind") val wind: Wind,
    @SerializedName("pop") val pop: Double  // Probability of precipitation (0.0-1.0)
)

data class Main(
    @SerializedName("temp") val temp: Double,
    @SerializedName("feels_like") val feelsLike: Double,
    @SerializedName("temp_min") val tempMin: Double,
    @SerializedName("temp_max") val tempMax: Double,
    @SerializedName("humidity") val humidity: Int
)

data class WeatherDescription(
    @SerializedName("description") val description: String,
    @SerializedName("icon") val icon: String
)

data class Wind(
    @SerializedName("speed") val speed: Double
)

data class Sys(
    @SerializedName("country") val country: String
)

data class City(
    @SerializedName("name") val name: String,
    @SerializedName("country") val country: String
)
