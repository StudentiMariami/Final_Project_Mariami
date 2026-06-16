package com.example.weatherapp.data.local

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.weatherapp.model.CurrentWeather
import com.example.weatherapp.model.ForecastDay

/**
 * WEATHER DAO
 *
 * Data Access Object for current weather.
 * Room generates the SQL implementation at compile time from these annotations.
 */
@Dao
interface WeatherDao {

    /**
     * Insert or replace the current weather for a city.
     * OnConflictStrategy.REPLACE: if a row with the same cityName exists, overwrite it.
     * suspend = must run on a background coroutine (not the UI thread)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrentWeather(weather: CurrentWeather)

    /**
     * Get the cached current weather for a city.
     * Returns LiveData so the UI updates automatically when data changes.
     */
    @Query("SELECT * FROM current_weather WHERE cityName = :city LIMIT 1")
    fun getCurrentWeather(city: String): LiveData<CurrentWeather?>

    /** Delete all cached weather (used when switching cities) */
    @Query("DELETE FROM current_weather")
    suspend fun clearCurrentWeather()
}

/**
 * FORECAST DAO
 *
 * Data Access Object for the 7-day forecast list.
 */
@Dao
interface ForecastDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForecasts(forecasts: List<ForecastDay>)

    /**
     * Get all forecast days for a city, ordered by their ID (insertion order = date order).
     * Returns LiveData so RecyclerView updates automatically.
     */
    @Query("SELECT * FROM forecast WHERE cityName = :city ORDER BY id ASC")
    fun getForecastForCity(city: String): LiveData<List<ForecastDay>>

    /** Delete old forecast before inserting fresh data */
    @Query("DELETE FROM forecast WHERE cityName = :city")
    suspend fun clearForecastForCity(city: String)
}
