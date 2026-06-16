package com.example.weatherapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.weatherapp.model.CurrentWeather
import com.example.weatherapp.model.ForecastDay

/**
 * ROOM DATABASE
 *
 * The main database class. Room generates the actual SQLite implementation.
 * @Database lists all entity tables and the schema version.
 *
 * Singleton pattern with @Volatile for thread safety —
 * only one database instance ever exists in the app.
 */
@Database(
    entities = [CurrentWeather::class, ForecastDay::class],
    version = 1,
    exportSchema = false
)
abstract class WeatherDatabase : RoomDatabase() {

    abstract fun weatherDao(): WeatherDao
    abstract fun forecastDao(): ForecastDao

    companion object {
        @Volatile
        private var INSTANCE: WeatherDatabase? = null

        fun getDatabase(context: Context): WeatherDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WeatherDatabase::class.java,
                    "weather_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
