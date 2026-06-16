package com.example.weatherapp.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * API CONFIGURATION
 *
 * API_KEY: Get a free key at https://openweathermap.org/api
 * After registering, the free tier allows 60 calls/minute — more than enough.
 *
 * ICON_BASE_URL: OpenWeatherMap provides weather icons.
 * Usage: "${ICON_BASE_URL}${iconCode}@2x.png"
 * Example: https://openweathermap.org/img/wn/11d@2x.png (thunderstorm)
 */
object WeatherApiConfig {
    // ⚠️  Replace this with your own free API key from openweathermap.org
    const val API_KEY = "bd5e378503939ddaee76f12ad7a97608"
    const val BASE_URL = "https://api.openweathermap.org/data/2.5/"
    const val ICON_BASE_URL = "https://openweathermap.org/img/wn/"
}

/**
 * RETROFIT SINGLETON
 *
 * Creates one Retrofit instance shared across the whole app.
 * OkHttpClient adds logging so you can see API calls in Logcat.
 */
object RetrofitInstance {

    // Logging interceptor prints every request/response body to Logcat (debug only)
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    // Lazy: only built when first accessed
    val api: WeatherApiService by lazy {
        Retrofit.Builder()
            .baseUrl(WeatherApiConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create()) // JSON → Kotlin data class
            .build()
            .create(WeatherApiService::class.java)
    }
}
